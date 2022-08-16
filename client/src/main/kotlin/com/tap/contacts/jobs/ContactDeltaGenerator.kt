package com.tap.contacts.jobs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.contacts.crdt.CRDTDelta
import com.github.michaelbull.result.getOrThrow
import com.tap.contacts.Contact
import com.tap.contacts.ContactQueries
import com.tap.contacts.Database
import com.tap.contacts.R
import com.tap.contacts.sync.syncData
import com.tap.hlc.HybridLogicalClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.serpro69.kfaker.Faker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

import kotlin.random.Random

@HiltWorker
class ContactDeltaGenerator @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val manager: WorkManager,
    private val database: Database,
    private val localClockStream: MutableStateFlow<HybridLogicalClock>
) : CoroutineWorker(context, parameters) {

    companion object {
        private const val WORK_QUEUE_NAME = "delta_gen"
        private const val OP_CODE_CREATE = 0
        private const val OP_CODE_UPDATE = 1
        private const val OP_CODE_DELETE = 2

        fun schedule(manager: WorkManager) {

            val request = OneTimeWorkRequestBuilder<ContactDeltaGenerator>()
                .build()

            manager.enqueueUniqueWork(WORK_QUEUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(manager: WorkManager) {
            manager.cancelUniqueWork(WORK_QUEUE_NAME)
        }
    }

    private val faker = Faker()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager


    private suspend fun createContact(contactQueries: ContactQueries) : Set<CRDTDelta> {

        val id = uuid4()
        val name = faker.name.name()

        return mapOf(
            Contact::id.name to id.toString(),
            Contact::name.name to name,
            Contact::phone.name to faker.phoneNumber.phoneNumber(),
            Contact::email.name to "$name@google.com".lowercase().replace(" ", "_"),
            Contact::tombstone.name to "0"
        ).map { entry ->
            val timestamp = HybridLogicalClock.localTick(localClockStream.value).getOrThrow { IllegalStateException() }
            localClockStream.emit(timestamp)
            CRDTDelta(
                "contacts",
                id,
                timestamp,
                entry.key,
                entry.value
            )
        }.toSet()
    }


    private suspend fun updateContact(contactQueries: ContactQueries) : Set<CRDTDelta> {

        val name = faker.name.name()
        val contact = contactQueries.findRandom().executeAsOneOrNull() ?: return emptySet()

        val mutationMap = mapOf(
            Contact::name.name to name,
            Contact::phone.name to faker.phoneNumber.phoneNumber(),
            Contact::email.name to "$name@google.com".lowercase().replace(" ", "_"),
        )

        val randomEntry = mutationMap.entries.elementAt(Random.nextInt(mutationMap.size))

        return randomEntry.let { entry ->
            val timestamp = HybridLogicalClock.localTick(localClockStream.value).getOrThrow { IllegalStateException() }
            localClockStream.emit(timestamp)
            setOf( CRDTDelta(
                "contacts",
                Uuid.fromString(contact.id),
                timestamp,
                entry.key,
                entry.value
            ))
        }
    }

    private suspend fun deleteContact(contactQueries: ContactQueries) : Set<CRDTDelta> {
        val contact = contactQueries.findRandom().executeAsOneOrNull() ?: return emptySet()

        return mapOf(
            Contact::tombstone.name to "1"
        ).map { entry ->
            val timestamp = HybridLogicalClock.localTick(localClockStream.value).getOrThrow { IllegalStateException() }
            localClockStream.emit(timestamp)
            CRDTDelta(
                "contacts",
                Uuid.fromString(contact.id),
                timestamp,
                entry.key,
                entry.value
            )
        }.toSet()
    }

    override suspend fun doWork(): Result {

        val progress = "Generating events"
        setForeground(createForegroundInfo(progress))

        createContact(database.contactQueries)

        while (true) {
            val random = Random.nextInt(0, 100)
            val operation = if(random < 60) { // bias creation and updates else everything would be deleted
                OP_CODE_CREATE
            } else if(random < 95) {
                OP_CODE_UPDATE
            } else {
                OP_CODE_DELETE
            }

            val deltas = when(operation) {
                OP_CODE_CREATE -> {
                     createContact(database.contactQueries)
                }
                OP_CODE_UPDATE -> {
                    updateContact(database.contactQueries)
                }
                OP_CODE_DELETE -> {
                    deleteContact(database.contactQueries)
                }
                else -> {
                    createContact(database.contactQueries)
                }
            }
            syncData(database.contactQueries, deltas)
            ContactsSync.schedule(manager, deltas)
            delay(1000) // one generation per second
        }
    }


    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val id = "Event Generator"
        val title = "Event Generator"
        val cancel = "Cancel Event Generation"

        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setChannelId("event")
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(117, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val name = "Event Generator"
        val descriptionText = "Reports when events are generated"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("event", name, importance)
        mChannel.description = descriptionText

        notificationManager.createNotificationChannel(mChannel)
    }

}