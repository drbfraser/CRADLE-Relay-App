package com.cradleplatform.smsrelay.network

import android.app.Application
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

/**
 * class to hold one instance of the queue throughout the application.
 */
class VolleyRequestQueue(application: Application) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(application)

    fun <T> addRequest(request: Request<T>) {
        requestQueue.add(request)
    }
}
