package com.hc.dat.di.component

import android.content.Context
import com.hc.dat.DatMainActivity
import com.hc.dat.di.module.ApplicationModule
import com.hc.dat.di.module.ViewModelModule
import com.hc.dat.di.scope.PerApplication
import com.hc.dat.view.DatBaseScreen
import dagger.Component
import javax.inject.Singleton

@PerApplication
@Singleton
@Component(
    modules = [ApplicationModule::class, ViewModelModule::class]
)
interface AppComponent {
    fun inject(mainActivity: DatMainActivity)
    fun inject(datBaseScreen: DatBaseScreen)

    companion object {
        private var INSTANCE: AppComponent? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = DaggerAppComponent.builder()
                    .applicationModule(ApplicationModule(context)).build()
            }
        }

        fun getInstance(): AppComponent {
            INSTANCE?.run {
                return this
            } ?: throw NullPointerException("ERROR: You must call init method first!")
        }
    }
}
