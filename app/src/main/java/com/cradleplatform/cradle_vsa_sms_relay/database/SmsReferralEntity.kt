package com.cradleplatform.cradle_vsa_sms_relay.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SmsReferralEntity(
    @PrimaryKey
    val id: String,
    // change to encryptedDataFromMobile
    val encryptedData: String?,
    // add encryptedDataFromServer
    // change to timeRequestInitiated
    // add timeLastDataMsgReceived
    // add timeLastDataMsgSent
    val timeReceived: Long,
    var isUploaded: Boolean,
    val phoneNumber: String?,
    // add totalFragmentsFromMobile
    // add numFragmentsReceivedFromMobile
    // add totalFragmentsToMobile
    // add numFragmentsSentToMobile
    var numberOfTriesUploaded: Int,
    var errorMessage: String,
    var deliveryReportSent: Boolean
//    @PrimaryKey
//    val id: String,
//    val encryptedDataFromMobile: String?,
//    //encrypted data or unencrypted message
//    val dataFromServer: String?,
//    val smsPackets: ArrayList<String>,
//    val timeLastDataMessageSent: Long?,
//    val timeLastDataMessageReceived: Long,
//    val timeRequestInitiated: Long,
//    var isUploaded: Boolean,
//    val phoneNumber: String?,
//    val totalFragmentsFromMobile: Int,
//    val numFragmentsReceivedFromMobile: Int,
//    val totalFragmentsFromServer: Int,
//    val numFragmentsSentToMobile: Int,
//    var numberOfTriesUploaded: Int,
//    var errorMessage: String,
//    var deliveryReportSent: Boolean
) : Serializable, Comparable<SmsReferralEntity> {

    override fun compareTo(other: SmsReferralEntity): Int {
        return (this.timeReceived - other.timeReceived).toInt()
    }
}
