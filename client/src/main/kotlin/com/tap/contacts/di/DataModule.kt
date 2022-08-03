package com.tap.contacts.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.tap.contacts.Database
import com.tap.contacts.network.ContactsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Defines all the classes that need to be provided in the scope of the app.
 *
 * Define here all objects that are shared throughout the app, like SharedPreferences, navigators or
 * others. If some of those objects are singletons, they should be annotated with `@Singleton`.
 */

@Module
@InstallIn(SingletonComponent::class)
class DataModule {


    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): Database {
        return Database(AndroidSqliteDriver(Database.Schema, context, "contacts.db"))
    }

    @Singleton
    @Provides
    fun provideContactsService(@ApplicationContext context: Context): ContactsService {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(Json.asConverterFactory(contentType))
            .build()

        return retrofit.create(ContactsService::class.java)
    }


}