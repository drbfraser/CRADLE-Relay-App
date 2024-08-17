package com.cradleplatform.cradle_vsa_sms_relay.broadcast_receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.HttpRelayResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestData
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestResult
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequestPhase
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayResponseData
import com.cradleplatform.cradle_vsa_sms_relay.repository.HttpsRequestRepository
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Throws


class RelayRequestFailedException(message: String) : Exception(message)

class MessageReceiver(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) :
    BroadcastReceiver() {

    @Inject
    lateinit var smsFormatter: SMSFormatter

    @Inject
    lateinit var smsRelayRepository: SmsRelayRepository

    @Inject
    lateinit var httpsRequestRepository: HttpsRequestRepository

    private val requestChannels: ConcurrentHashMap<String, Channel<RelayPacket>> =
        ConcurrentHashMap()

    init {
        (context.applicationContext as MyApp).component.inject(this)
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                smsRelayRepository.terminateAllActiveRequests()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // getMessagesFromIntent returns an array because long SMS Messages (above 160 characters)
        // will be split into multiple messages. However, in the context of our SMS Tunnelling
        // protocol, we always stay under that limit. So, we can consider each element in this array
        // to be a standalone packet. This is different from our old code which assumed packets
        // maybe split across multiple array elements.
        val relayPackets = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            .mapNotNull { smsFormatter.smsMessageToRelayPacket(it) }

        // Its important to note that in the 99.99% of cases, relayPackets will be an array of length 1.
        for (pkt in relayPackets) {
            when {
                pkt is RelayPacket.AckPacket && requestChannels.containsKey(pkt.phoneNumber) -> {
                    coroutineScope.launch {
                        requestChannels[pkt.phoneNumber]!!.send(pkt)
                    }
                }

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

                pkt is RelayPacket.RestPacket && requestChannels.containsKey(pkt.phoneNumber) -> {
                    coroutineScope.launch {
                        requestChannels[pkt.phoneNumber]!!.send(pkt)
                    }
                }

                else -> {
                    Log.d(TAG, "Discarding unexpected packet")
                }
            }
        }
    }

    private suspend fun handleRequest(relayPacket: RelayPacket.FirstPacket) {
        val (request, channel) = startNewRequest(relayPacket)
        try {
            // PHASE 1: Receive HTTP Request from CRADLE-Mobile via SMS
            receiveFromMobile(request, channel)

            // PHASE 2: Relay HTTP Request from CRADLE-Mobile to server &
            // PHASE 3: Receive HTTP Response from server (TODO: Useless phase)
            val response = relayToServer(request)

            // PHASE 3: Relay the HTTP Response from server to CRADLE-Mobile
            relayToMobile(request, channel, response)

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

    private fun startNewRequest(relayPacket: RelayPacket.FirstPacket): Pair<RelayRequest, Channel<RelayPacket>> {
        requestChannels[relayPacket.phoneNumber]?.cancel()

        val newChannel = Channel<RelayPacket>()
        requestChannels[relayPacket.phoneNumber] = newChannel

        val currTimeMs = System.currentTimeMillis()

        val dataPacketsFromMobile: MutableList<RelayRequestData?> =
            MutableList(relayPacket.expectedNumPackets) { null }

        dataPacketsFromMobile[0] = RelayRequestData(
            data = relayPacket.data,
            timeMsReceived = currTimeMs
        )

        val newRelayRequest = RelayRequest(
            requestId = relayPacket.requestId,
            requestPhase = RelayRequestPhase.RECEIVING_FROM_MOBILE,
            expectedNumPackets = relayPacket.expectedNumPackets,
            timeMsInitiated = currTimeMs,
            timeMsLastReceived = currTimeMs,
            phoneNumber = relayPacket.phoneNumber,
            requestResult = RelayRequestResult.PENDING,
            dataPacketsFromMobile = dataPacketsFromMobile,
            dataPacketsToMobile = mutableListOf()
        )

        smsRelayRepository.insertRelayRequest(newRelayRequest)
        smsFormatter.sendAckMessage(newRelayRequest, relayPacket.packetNumber)

        return Pair(newRelayRequest, newChannel)
    }

    private suspend fun receiveFromMobile(request: RelayRequest, channel: Channel<RelayPacket>) {
        // NOTE: Cannot use a simple counter because there could be duplicates of each packet
        var numReceivedPackets = request.dataPacketsFromMobile.count { it != null }

        while (numReceivedPackets < request.expectedNumPackets) {
            val pkt = withTimeoutOrNull(TIMEOUT_MS_FOR_RECEIVING_FROM_MOBILE) {
                channel.receive()
            }
            when (pkt) {
                is RelayPacket.FirstPacket -> {
                    if (pkt.requestId == request.requestId) {
                        Log.d(TAG, "Received duplicate first packet, sending ACK again")
                        smsFormatter.sendAckMessage(request, packetNumber = 0)
                    } else {
                        Log.d(TAG, "Discarding because another request is in progress")
                    }
                }

                is RelayPacket.AckPacket -> Log.d(TAG, "Discarding unexpected AckPacket")

                is RelayPacket.RestPacket -> {
                    if (pkt.packetNumber >= request.expectedNumPackets) {
                        Log.d(TAG, "Discarding because another request in progress")
                        continue
                    } else {
                        // It is possible for us to receive a rest packet that we received before. For example, our
                        // ACK message arrived too late to CRADLE-Mobile and it resend that packet. We choose to
                        // overwrite data in this scenario.
                        val currTime = System.currentTimeMillis()

                        request.dataPacketsFromMobile[pkt.packetNumber] = RelayRequestData(
                            data = pkt.data,
                            timeMsReceived = currTime,
                        )
                        request.timeMsLastReceived = currTime
                        smsRelayRepository.updateRelayRequest(request)

                        smsFormatter.sendAckMessage(request, pkt.packetNumber)
                    }
                }

                null -> {
                    throw RelayRequestFailedException("Timed out waiting for packets")
                }
            }
            numReceivedPackets = request.dataPacketsFromMobile.count { it != null }
        }
    }

    private suspend fun relayToServer(relayRequest: RelayRequest): retrofit2.Response<HttpRelayResponse> {
        // This method should only be called when we have the entire request
        assert(relayRequest.dataPacketsFromMobile.all { it != null })

        smsRelayRepository.updateRequestPhase(relayRequest, RelayRequestPhase.RELAYING_TO_SERVER)
        val data = relayRequest.dataPacketsFromMobile.joinToString(separator = "") { it!!.data }

        val response = httpsRequestRepository.relayRequestToServer(
            phoneNumber = relayRequest.phoneNumber,
            data = data
        )

        // TODO: This phase change is basically useless because it will be changed immediately to
        // TODO: RELAYING_TO_MOBILE, but removing this requires UI design changes
        smsRelayRepository.updateRequestPhase(relayRequest, RelayRequestPhase.RECEIVING_FROM_SERVER)

        return response
    }

    @Throws(RelayRequestFailedException::class)
    private suspend fun relayToMobile(
        request: RelayRequest,
        channel: Channel<RelayPacket>,
        response: retrofit2.Response<HttpRelayResponse>
    ) {
        smsRelayRepository.updateRequestPhase(request, RelayRequestPhase.RELAYING_TO_MOBILE)

        val dataPacketsToMobile = if (response.isSuccessful && response.body() != null) {
            smsFormatter.formatSMS(
                msg = response.body()!!.body,
                currentRequestCounter = request.requestId,
                isSuccessful = true,
                statusCode = response.code()
            )
        } else {
            smsFormatter.formatSMS(
                msg = processHttpRelayResponseErrorBody(response.errorBody()?.string()),
                currentRequestCounter = request.requestId,
                isSuccessful = false,
                statusCode = response.code()
            )
        }

        request.dataPacketsToMobile.addAll(dataPacketsToMobile.map {
            RelayResponseData(
                data = it,
                isAcked = false
            )
        })

        // Send first packet to mobile
        var nextPacketToMobile = request.dataPacketsToMobile.find { !it.isAcked }
        smsFormatter.sendMessage(request.phoneNumber, nextPacketToMobile!!.data)

        var retriesForPacket = 0
        while (nextPacketToMobile != null) {
            val packet = withTimeoutOrNull(TIMEOUT_MS_FOR_RECEIVING_FROM_MOBILE) {
                channel.receive()
            }

            when (packet) {
                is RelayPacket.FirstPacket -> Log.d(TAG, "Discarding unexpected FirstPacket")
                is RelayPacket.RestPacket -> Log.d(TAG, "Discarding unexpected RestPacket")
                is RelayPacket.AckPacket -> {
                    if (packet.packetNumber < request.dataPacketsToMobile.size) {
                        val wasAckedBefore =
                            request.dataPacketsToMobile[packet.packetNumber].isAcked
                        request.dataPacketsToMobile[packet.packetNumber].isAcked = true

                        request.timeMsLastReceived = System.currentTimeMillis()
                        smsRelayRepository.updateRelayRequest(request)

                        nextPacketToMobile = request.dataPacketsToMobile.find { !it.isAcked }
                        if (!wasAckedBefore && nextPacketToMobile != null) {
                            smsFormatter.sendMessage(request.phoneNumber, nextPacketToMobile.data)
                            retriesForPacket = 0
                        }
                    } else {
                        Log.d(TAG, "Discarding unexpected RestPacket")
                    }
                }

                null -> {
                    Log.d(TAG, "Timed out waiting for ACK, retrying")
                    retriesForPacket++
                    smsFormatter.sendMessage(request.phoneNumber, nextPacketToMobile.data)

                    if (retriesForPacket > MAX_RETRIES_FOR_DATA_PACKETS_TO_MOBILE) {
                        throw RelayRequestFailedException("Retries exceeded for relaying to mobile")
                    }
                }
            }

            nextPacketToMobile = request.dataPacketsToMobile.find { !it.isAcked }
        }
    }

    private fun processHttpRelayResponseErrorBody(errorBody: String?): String {
        if (errorBody == null) return "Unknown error"

        try {
            return JSONObject(errorBody).run {
                when {
                    has("msg") -> getString("msg")
                    has("message") -> getString("message")
                    else -> errorBody
                }
            }
        } catch (e: org.json.JSONException) {
            Log.d(TAG, "Error message is not valid JSON: $errorBody")
            return "Unknown error"
        }
    }

    companion object {
        private const val TAG = "BetterMessageReceiver"
        private const val MAX_RETRIES_FOR_DATA_PACKETS_TO_MOBILE = 5

        // We will use the same time out for receiving normal packets and ACK packets from mobile
        private const val TIMEOUT_MS_FOR_RECEIVING_FROM_MOBILE = 20_000L
    }
}

