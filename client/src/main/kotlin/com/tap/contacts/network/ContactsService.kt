package com.tap.contacts.network

import com.contacts.crdt.network.request.SyncRequest
import com.contacts.crdt.network.response.SyncResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface ContactsService {
    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): SyncResponse
}