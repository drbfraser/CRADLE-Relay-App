package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayRequestBody
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestData
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestPhase
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayResponseData
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkResult
import com.cradleplatform.cradle_vsa_sms_relay.network.RestApi
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.utilities.RelayPacket
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Throws


class RelayRequestFailedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class JsonProcessingException(message: String, cause: Throwable) : Exception(message, cause)

class MessageReceiver(
    context: Context,
    private val coroutineScope: CoroutineScope
) :
    BroadcastReceiver() {

    @Inject
    lateinit var smsFormatter: SMSFormatter

    @Inject
    lateinit var smsRelayRepository: SmsRelayRepository

    @Inject
    lateinit var restApi: RestApi

    private val requestChannels: ConcurrentHashMap<String, Channel<RelayPacket>> =
        ConcurrentHashMap()

    init {
        (context.applicationContext as MyApp).component.inject(this)

        // update time last listened to sms (TODO: Why is this useful?)
        val sharedPreferences = context.getSharedPreferences(LAST_RUN_PREF, Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(LAST_RUN_TIME, System.currentTimeMillis())
            .apply()

        // Cancel all previously active requests. We already clean up gracefully. This is to account
        // for unexpected crashes.
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                smsRelayRepository.terminateAllActiveRequests()
            }
        }
    }

    /// This method is the entry point for receiving SMS messages.
    override fun onReceive(context: Context, intent: Intent) {
        // getMessagesFromIntent returns an array because long SMS Messages (above 160 characters)
        // will be split into multiple messages. However, in the context of our SMS Tunnelling
        // protocol, we always stay under that limit. So, we can consider each element in this array
        // to be a standalone packet. This is different from our old code which assumed packets
        // maybe split across multiple array elements (which is problematic if the said array returns
        // multiple messages and each is a separate packet).
        val relayPackets = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            .mapNotNull { smsFormatter.smsMessageToRelayPacket(it) }

        // Key things to note about the following:
        // - This array almost always have length 1. Not sure if it can be > 1 so we have this for loop.
        // - We have a co-routine for each request in progress. That co-routine has a channel it can
        //   consume packets associated with that request. This is where we send to such channels
        // - We know a request is in progress when `requestChannels` has a channel for the phone number
        //   - This also highlights one of the key aspects of the protocol: We can only handle one
        //      request at a time for a given phone number. (Though this can be trivially changed)
        for (pkt in relayPackets) {
            when {
                // We received an ACK packet from a phone number with a request in already progress.
                // So we forward it to the appropriate channel
                pkt is RelayPacket.AckPacket && requestChannels.containsKey(pkt.phoneNumber) -> {
                    coroutineScope.launch {
                        requestChannels[pkt.phoneNumber]!!.send(pkt)
                    }
                }

                // We received the First Packet. If a request is already in progress then it should be
                // a duplicate - we still send to its channel. If no request in progress, then we have
                // a new request and so launch a new co-routine to handle this packet and the rest coming later
                pkt is RelayPacket.FirstPacket -> {
                    if (requestChannels.containsKey(pkt.phoneNumber)) {
                        coroutineScope.launch {
                            requestChannels[pkt.phoneNumber]!!.send(pkt)
                        }
                    } else {
                        coroutineScope.launch {
                            handleRequest(pkt)
                        }
                    }
                }

                // Unlike the First Packet, a RestPacket/non first packet do not have requestId info.
                // So we must assume this packet follows a previous First Packet. This is yet another
                // reason why we can only handle one request at a time per phone number.
                pkt is RelayPacket.RestPacket && requestChannels.containsKey(pkt.phoneNumber) -> {
                    coroutineScope.launch {
                        requestChannels[pkt.phoneNumber]!!.send(pkt)
                    }
                }

                // Unexpected packets seems to happen all the time. Though its usually a sign of
                // in efficiency in the protocol impl (especially on the CRADLE-Mobile side)
                else -> {
                    Log.d(TAG, "Discarding unexpected packet: $pkt")
                }
            }
        }
    }

    private suspend fun handleRequest(relayPacket: RelayPacket.FirstPacket) {
        val (request, channel) = startNewRequest(relayPacket)
        try {
            // PHASE 1: Receive Relay Request from CRADLE-Mobile via multiple packets
            receiveFromMobile(request, channel)

            // PHASE 2: Relay HTTP Request from CRADLE-Mobile to server (TODO: Useless phase)
            // AND PHASE 3: Receive HTTP Response from server
            val serverNetworkResult = relayToServer(request)

            // PHASE 4: Relay the HTTP Response from server to CRADLE-Mobile
            relayToMobile(request, channel, serverNetworkResult)

            smsRelayRepository.markRelayRequestSuccess(request)
            Log.d(
                TAG,
                "Request success: Phone=${relayPacket.phoneNumber} ID=${relayPacket.requestId}"
            )
        } catch (e: RelayRequestFailedException) {
            smsRelayRepository.markRelayRequestError(
                request,
                errorMessage = e.message ?: "Unknown error"
            )
            Log.d(
                TAG,
                "Request error: Phone=${relayPacket.phoneNumber} ID=${relayPacket.requestId} error=${e.message}"
            )
        } catch (e: CancellationException) {
            smsRelayRepository.markRelayRequestError(request, errorMessage = "Request cancelled")
            Log.d(
                TAG,
                "Request cancelled: Phone=${relayPacket.phoneNumber} ID=${relayPacket.requestId}"
            )
            throw e
        } finally {
            requestChannels.remove(relayPacket.phoneNumber)
        }
    }

    private fun startNewRequest(pkt: RelayPacket.FirstPacket): Pair<RelayRequest, Channel<RelayPacket>> {
        Log.d(TAG, "Received first packet from mobile: $pkt")
        requestChannels[pkt.phoneNumber]?.cancel()

        val newChannel = Channel<RelayPacket>()
        requestChannels[pkt.phoneNumber] = newChannel

        val currTimeMs = System.currentTimeMillis()

        // We allocate the array up front to allow receiving packets out of order (not possible with
        // the protocol but good for robustness). As well, it makes it easier to handle duplicate
        // packets
        val dataPacketsFromMobile: MutableList<RelayRequestData?> =
            MutableList(pkt.expectedNumPackets) { null }

        dataPacketsFromMobile[0] = RelayRequestData(
            data = pkt.data,
            timeMsReceived = currTimeMs
        )

        val newRelayRequest = RelayRequest(
            requestId = pkt.requestId,
            requestPhase = RelayRequestPhase.RECEIVING_FROM_MOBILE,
            expectedNumPackets = pkt.expectedNumPackets,
            timeMsInitiated = currTimeMs,
            timeMsLastReceived = currTimeMs,
            phoneNumber = pkt.phoneNumber,
            requestResult = RelayRequestResult.PENDING,
            dataPacketsFromMobile = dataPacketsFromMobile,
            dataPacketsToMobile = mutableListOf()
        )

        smsRelayRepository.insertRelayRequest(newRelayRequest)
        smsFormatter.sendAckMessage(newRelayRequest, pkt.packetNumber)

        return Pair(newRelayRequest, newChannel)
    }

    @Throws(RelayRequestFailedException::class)
    private suspend fun receiveFromMobile(request: RelayRequest, channel: Channel<RelayPacket>) {
        // NOTE: Cannot use a simple counter because there could be duplicates of each packet
        var numReceivedPackets = request.dataPacketsFromMobile.count { it != null }

        while (numReceivedPackets < request.expectedNumPackets) {
            val pkt = withTimeoutOrNull(TIMEOUT_MS_FOR_RECEIVING_FROM_MOBILE) {
                channel.receive()
            }
            Log.d(TAG, "Received rest packet from mobile: $pkt")

            when {
                pkt is RelayPacket.FirstPacket -> {
                    if (pkt.requestId == request.requestId) {
                        Log.d(TAG, "Received duplicate first packet, sending ACK again")
                        smsFormatter.sendAckMessage(request, packetNumber = 0)
                    } else {
                        Log.d(TAG, "Discarding because another request is in progress")
                    }
                }
                // It is possible for us to receive a rest packet that we received before. For example, our
                // ACK message arrived too late to CRADLE-Mobile and it resend that packet. We choose to
                // overwrite data in this scenario.
                pkt is RelayPacket.RestPacket && (pkt.packetNumber < request.expectedNumPackets) -> {
                    val currTime = System.currentTimeMillis()

                    request.dataPacketsFromMobile[pkt.packetNumber] = RelayRequestData(
                        data = pkt.data,
                        timeMsReceived = currTime,
                    )
                    request.timeMsLastReceived = currTime
                    smsRelayRepository.updateRelayRequest(request)

                    smsFormatter.sendAckMessage(request, pkt.packetNumber)
                }

                pkt == null -> {
                    throw RelayRequestFailedException("Timed out waiting for packets")
                }

                else -> {
                    Log.d(TAG, "Discarding unexpected packet: $pkt")
                }
            }
            numReceivedPackets = request.dataPacketsFromMobile.count { it != null }
        }
    }

    @Throws(RelayRequestFailedException::class)
    private suspend fun relayToServer(relayRequest: RelayRequest): NetworkResult<String> {
        assert(relayRequest.dataPacketsFromMobile.all { it != null })

        smsRelayRepository.updateRequestPhase(relayRequest, RelayRequestPhase.RELAYING_TO_SERVER)

        // Step 1: Concat all fragments into base64 string
        // we want to concat before decrypting due to base64 length requirements for full bytes
        val base64EncodedMessage =
            relayRequest.dataPacketsFromMobile.joinToString(separator = "") { it!!.data }

        // Step 2: Decode Base64 to retrieve the AES-encrypted payload
        // similar payload that was originally being sent to server is now extracted
        val aesEncryptedData = Base64.getDecoder().decode(base64EncodedMessage)

        smsRelayRepository.updateRequestPhase(relayRequest, RelayRequestPhase.RECEIVING_FROM_SERVER)

        val relayRequestBody = HttpRelayRequestBody(
            phoneNumber = relayRequest.phoneNumber,
            encryptedData = String(aesEncryptedData, Charsets.UTF_8)
        )

        // Step 3: Send data to the server
        return restApi.relaySmsRequest(relayRequestBody)
    }

    @Throws(RelayRequestFailedException::class)
    @Suppress("MagicNumber")
    private suspend fun relayToMobile(
        request: RelayRequest,
        channel: Channel<RelayPacket>,
        serverNetworkResult: NetworkResult<String>
    ) {
        val dataPacketsToMobile = getDataPacketsToMobile(request, serverNetworkResult)

        // Note: We don't really need to store this in DB, but the UI uses it in the Details
        // activity. It's easier to let the UI use this data if we store it in the DB
        request.dataPacketsToMobile.addAll(dataPacketsToMobile.map {
            RelayResponseData(data = it, ackedCount = 0)
        })

        // Phase change should be after dataPacketsToMobile is set or the UI could think we have no
        // packets to send for a split moment
        smsRelayRepository.updateRequestPhase(request, RelayRequestPhase.RELAYING_TO_MOBILE)

        // Send first packet to mobile which we assert to always exist. Then we go into the loop
        // which facilitates waiting for the first packet's ACK and then doing the same for the
        // rest of the packets.
        assert(request.dataPacketsToMobile.isNotEmpty())
        smsFormatter.sendMessage(request.phoneNumber, request.dataPacketsToMobile[0].data)

        var retriesForPacket = 0
        var currPacketToMobileIdx = 0
        while (currPacketToMobileIdx != -1) {
            val pkt = withTimeoutOrNull(TIMEOUT_MS_FOR_RECEIVING_ACK_FROM_MOBILE) {
                channel.receive()
            }
            Log.d(TAG, "Received ack packet from mobile (null is timeout): $pkt")
            when {
                // CASE: We received a relevant ACK packet from mobile
                pkt is RelayPacket.AckPacket && (pkt.packetNumber < request.dataPacketsToMobile.size) -> {
                    val acknowledgedPacket = request.dataPacketsToMobile[pkt.packetNumber]
                    acknowledgedPacket.ackedCount++
                    request.timeMsLastReceived = System.currentTimeMillis()
                    smsRelayRepository.updateRelayRequest(request)
                }
                // CASE: We timed out waiting for ACK from mobile
                pkt == null -> {
                    retriesForPacket++
                    if (retriesForPacket >= MAX_RETRIES_FOR_DATA_PACKETS_TO_MOBILE) {
                        throw RelayRequestFailedException("Retries exceeded for relaying to mobile")
                    } else {
                        Log.d(TAG, "Timed out waiting for ACK, retrying")
                        smsFormatter.sendMessage(
                            phoneNumber = request.phoneNumber,
                            smsMessage = request.dataPacketsToMobile[currPacketToMobileIdx].data
                        )
                    }
                }
                // CASE: We received some random packet
                else -> Log.d(TAG, "Discarding unexpected packet: $pkt")
            }
            // Check if there is a change in total acks received (non-duplicate). If true, next packet
            // to mobile would be different from current, indicating that we are ready to send the
            // next packet to mobile
            val nextPacketToMobileIdx =
                request.dataPacketsToMobile.indexOfFirst { it.ackedCount == 0 }
            if (nextPacketToMobileIdx != -1 && nextPacketToMobileIdx != currPacketToMobileIdx) {
                smsFormatter.sendMessage(
                    phoneNumber = request.phoneNumber,
                    smsMessage = request.dataPacketsToMobile[nextPacketToMobileIdx].data
                )
                retriesForPacket = 0 // Reset retries since its a fresh packet
            }
            currPacketToMobileIdx = nextPacketToMobileIdx
        }
    }

    private fun getDataPacketsToMobile(
        request: RelayRequest,
        serverNetworkResult: NetworkResult<String>
    ): MutableList<String> =
        when (serverNetworkResult) {
            is NetworkResult.Success -> {
                val serverResponse = serverNetworkResult.value
                // Prepare the data to be sent to mobile as multiple SMS messages/packets
                val base64EncodedResponse =
                    Base64.getEncoder().encodeToString(serverResponse.toByteArray(Charsets.UTF_8))

                smsFormatter.formatSMS(
                    msg = base64EncodedResponse,
                    currentRequestCounter = request.requestId,
                    isSuccessful = true,
                    statusCode = 200
                )
            }

            is NetworkResult.Failure -> {
                val isEncrypted =
                    isHttpRelayResponseErrorBodyEncrypted(serverNetworkResult.statusCode)
                val errorBody = serverNetworkResult.body.decodeToString()

                val errorMessage =
                    if (isEncrypted) {
                        Base64.getEncoder().encodeToString(errorBody.toByteArray(Charsets.UTF_8))
                    } else {
                        processHttpRelayResponseErrorBody(errorBody)
                    }
                Log.e(TAG, "Failure: $errorMessage")

                smsFormatter.formatSMS(
                    msg = errorMessage,
                    currentRequestCounter = request.requestId,
                    isSuccessful = false,
                    statusCode = serverNetworkResult.statusCode
                )
            }

            is NetworkResult.NetworkException ->
                smsFormatter.formatSMS(
                    msg = serverNetworkResult.getStatusMessage() ?: "An Exception Occurred!",
                    currentRequestCounter = request.requestId,
                    isSuccessful = false,
                    statusCode = 500
                )
        }

    private fun processHttpRelayResponseErrorBody(errorBody: String?): String {
        if (errorBody == null) return "Unknown error"

        try {
            return JSONObject(errorBody).run {
                when {
                    has("description") -> getString("description")
                    has("body") -> getString("body")
                    else -> errorBody
                }
            }
        } catch (e: org.json.JSONException) {
            Log.d(TAG, "Error message is not valid JSON: $errorBody")
            throw JsonProcessingException("Failed to process error body", e)
        }
    }

    private fun isHttpRelayResponseErrorBodyEncrypted(errCode: Int): Boolean {
        val encryptedErrorCodes = listOf(REQUEST_NUMBER_MISMATCH)
        return errCode in encryptedErrorCodes
    }

    companion object {
        private const val TAG = "MessageReceiver"

        private const val LAST_RUN_PREF = "sharedPrefLastTimeServiceRun"
        private const val LAST_RUN_TIME = "lastTimeServiceRun"

        private const val MAX_RETRIES_FOR_DATA_PACKETS_TO_MOBILE = 5
        private const val TIMEOUT_MS_FOR_RECEIVING_FROM_MOBILE = 20_000L
        private const val TIMEOUT_MS_FOR_RECEIVING_ACK_FROM_MOBILE = 8000L

        private const val REQUEST_NUMBER_MISMATCH = 425
    }
}

