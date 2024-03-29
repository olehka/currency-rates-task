package com.olehka.currencyrates.di

import android.content.Context
import com.olehka.currencyrates.RatesApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ApplicationModule::class,
        ViewModelModule::class,
        FragmentModule::class
    ]
)
interface ApplicationComponent : AndroidInjector<RatesApplication> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): ApplicationComponent
    }
}