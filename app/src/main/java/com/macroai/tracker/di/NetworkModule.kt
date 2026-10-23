package com.macroai.tracker.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(180, TimeUnit.SECONDS)
                    writeTimeout(120, TimeUnit.SECONDS)
                    callTimeout(180, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                    followRedirects(true)
                    followSslRedirects(true)
                }
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 180_000
                socketTimeoutMillis = 180_000
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpRedirect) {
                checkHttpMethod = false
                allowHttpsDowngrade = false
            }
            install(Logging) {
                level = if (com.macroai.tracker.BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            }
        }
    }
}

