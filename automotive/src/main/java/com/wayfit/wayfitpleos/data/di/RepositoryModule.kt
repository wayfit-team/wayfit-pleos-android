package com.wayfit.wayfitpleos.data.di

import com.wayfit.wayfitpleos.data.repository.CouponRepository
import com.wayfit.wayfitpleos.data.repository.DefaultCouponRepository
import com.wayfit.wayfitpleos.data.repository.DefaultLocationRepository
import com.wayfit.wayfitpleos.data.repository.DefaultNaviRepository
import com.wayfit.wayfitpleos.data.repository.DefaultPairingRepository
import com.wayfit.wayfitpleos.data.repository.DefaultSettingsRepository
import com.wayfit.wayfitpleos.data.repository.SettingsRepository
import com.wayfit.wayfitpleos.data.repository.DefaultStoreRepository
import com.wayfit.wayfitpleos.data.repository.DefaultVehicleRepository
import com.wayfit.wayfitpleos.data.repository.DefaultVoiceRepository
import com.wayfit.wayfitpleos.data.repository.LocationRepository
import com.wayfit.wayfitpleos.data.repository.NaviRepository
import com.wayfit.wayfitpleos.data.repository.PairingRepository
import com.wayfit.wayfitpleos.data.repository.StoreRepository
import com.wayfit.wayfitpleos.data.repository.VehicleRepository
import com.wayfit.wayfitpleos.data.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(repository: DefaultVehicleRepository): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindNaviRepository(repository: DefaultNaviRepository): NaviRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(repository: DefaultLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(repository: DefaultVoiceRepository): VoiceRepository

    @Binds
    @Singleton
    abstract fun bindStoreRepository(repository: DefaultStoreRepository): StoreRepository

    @Binds
    @Singleton
    abstract fun bindCouponRepository(repository: DefaultCouponRepository): CouponRepository

    @Binds
    @Singleton
    abstract fun bindPairingRepository(repository: DefaultPairingRepository): PairingRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository
}
