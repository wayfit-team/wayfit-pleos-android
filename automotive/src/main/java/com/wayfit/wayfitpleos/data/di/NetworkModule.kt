package com.wayfit.wayfitpleos.data.di

import com.wayfit.wayfitpleos.BuildConfig
import com.wayfit.wayfitpleos.data.remote.DeviceAuthInterceptor
import com.wayfit.wayfitpleos.data.remote.DeviceTokenStore
import com.wayfit.wayfitpleos.data.remote.PrefsDeviceTokenStore
import com.wayfit.wayfitpleos.data.remote.WayfitApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindDeviceTokenStore(store: PrefsDeviceTokenStore): DeviceTokenStore

    companion object {

        @Provides
        @Singleton
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        @Provides
        @Singleton
        fun provideOkHttpClient(authInterceptor: DeviceAuthInterceptor): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun provideWayfitApi(client: OkHttpClient, json: Json): WayfitApi = Retrofit.Builder()
            .baseUrl(BuildConfig.WAYFIT_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WayfitApi::class.java)
    }
}
