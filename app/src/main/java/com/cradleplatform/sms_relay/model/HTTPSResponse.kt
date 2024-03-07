package com.cradleplatform.sms_relay.model

import com.google.gson.annotations.SerializedName

data class HTTPSResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("body") val body: String
)
