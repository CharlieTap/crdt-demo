package com.tap.contacts.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.contacts.crdt.CRDTDelta
import com.contacts.crdt.network.request.SyncRequest
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.tap.contacts.Contact
import com.tap.contacts.ContactQueries
import com.tap.contacts.Database
import com.tap.contacts.data.ContactsDatastore
import com.tap.contacts.network.ContactsService
import com.tap.contacts.sync.syncClock
import com.tap.contacts.sync.syncData
import com.tap.hlc.HybridLogicalClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KProperty


@HiltWorker
class ContactsSync @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val database: Database,
    private val contactsService: ContactsService,
    private val contactsDatastore: ContactsDatastore,
    private val localClockStream: MutableStateFlow<HybridLogicalClock>
) : CoroutineWorker(context, parameters) {

    companion object {

        private const val WORK_QUEUE_NAME = "sync"
        private const val INPUT_DATA_DELTAS = "INPUT_DATA_DELTAS"

        fun schedule(manager: WorkManager, deltas: Set<CRDTDelta> = emptySet()) {
            val data = deltas.let {
                Json.encodeToString(SetSerializer(CRDTDelta.serializer()), it)
            }

            val request = OneTimeWorkRequestBuilder<ContactsSync>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(ContactsSync.INPUT_DATA_DELTAS to data))
                .build()

            manager.enqueueUniqueWork(ContactsSync.WORK_QUEUE_NAME, ExistingWorkPolicy.APPEND, request)
        }
    }


    override suspend fun doWork(): Result {

        val deltas = inputData.getString(INPUT_DATA_DELTAS)?.let {
            Json.decodeFromString(SetSerializer(CRDTDelta.serializer()), it)
        } ?: emptySet()

        return kotlin.runCatching {
            val lastSyncId = contactsDatastore.lastSyncId.first()
            val response = contactsService.sync(SyncRequest(deltas, lastSyncId))

            syncClock(localClockStream, response.events)
            syncData(database.contactQueries, response.events)
            contactsDatastore.saveLastSyncId(response.id)
        }.fold({Result.success()}, {Result.retry()})
    }
}