package com.hc.dat.di.module

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.hc.dat.di.ApplicationContext
import com.hc.dat.model.database.AppDatabase
import com.hc.dat.model.repository.Repository
import com.hc.dat.model.repository.RepositoryImpl
import com.hc.dat.service.ServiceDefinition
import com.hc.dat.service.api.DatService
import com.lws.device.Device
import com.lws.device.camera.CameraHandler
import com.lws.device.camerapreview.CameraPreviewHandler
import com.lws.device.gps.GPSHandler
import com.lws.device.network.NetworkConnectionHandler
import com.lws.device.nfc.NFCHandler
import com.omi.service.Service
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Singleton

@Module
class ApplicationModule(private val context: Context) {

    @Singleton
    @Provides
    @ApplicationContext
    fun provideContext(): Context = context.applicationContext

    @Singleton
    @Provides
    fun provideAppDatabase(): AppDatabase = AppDatabase.getInstance(context)

    @Singleton
    @Provides
    fun provideSharePreference(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Singleton
    @Provides
    fun provideDatService(): DatService = Service.buildService(
        DatService::class.java,
        ServiceDefinition.HOST_BASE_URL,
        ServiceDefinition.CONNECTING_TIMEOUTS_DEFAULT
    )

    @Singleton
    @Inject
    @Provides
    fun provideRepository(
        appDatabase: AppDatabase,
        sharedPreferences: SharedPreferences,
        datService: DatService
    ): Repository =
        RepositoryImpl(appDatabase, sharedPreferences, datService)

    @Singleton
    @Provides
    fun provideDevice(): Device = Device(
        CameraHandler(),
        NFCHandler(),
        GPSHandler(),
        NetworkConnectionHandler(),
        CameraPreviewHandler()
    )
}
