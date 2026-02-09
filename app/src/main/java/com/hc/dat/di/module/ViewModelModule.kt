package com.hc.dat.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hc.dat.di.ViewModelKey
import com.hc.dat.di.scope.PerApplication
import com.hc.dat.viewmodel.ApplicationViewModel
import com.hc.dat.viewmodel.FaceRecognitionViewModel
import com.hc.dat.viewmodel.RiderSessionViewModel
import com.hc.dat.viewmodel.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
abstract class ViewModelModule {
    @Binds
    @Singleton
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @Singleton
    @ViewModelKey(ApplicationViewModel::class)
    abstract fun applicationViewModel(viewModel: ApplicationViewModel): ViewModel

    @Binds
    @IntoMap
    @Singleton
    @ViewModelKey(FaceRecognitionViewModel::class)
    abstract fun faceRecognitionViewModel(viewModel: FaceRecognitionViewModel): ViewModel

    @Binds
    @IntoMap
    @Singleton
    @ViewModelKey(RiderSessionViewModel::class)
    abstract fun riderSessionViewModel(viewModel: RiderSessionViewModel): ViewModel
}
