package com.tap.contacts.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.tap.contacts.BuildConfig
import com.tap.contacts.data.ContactsDatastore
import com.tap.hlc.HybridLogicalClock

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Named("debug-mode")
    fun provideDebugMode(): Boolean = BuildConfig.DEBUG


    private val Context.settingsDatastore by preferencesDataStore("application_settings")

    @Singleton
    @Provides
    fun provideDatastore(@ApplicationContext context: Context): ContactsDatastore {
        return ContactsDatastore(context.settingsDatastore)
    }

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideLocalClock(datastore: ContactsDatastore): MutableStateFlow<HybridLogicalClock> {

        val clock = runBlocking {
            datastore.localClock.first()
        }

        return MutableStateFlow(clock)
    }

}