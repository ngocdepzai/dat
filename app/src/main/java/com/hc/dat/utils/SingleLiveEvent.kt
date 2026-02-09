package com.hc.dat.utils

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.lws.type.Logger
import java.util.concurrent.atomic.AtomicBoolean

class SingleLiveEvent<T> : MutableLiveData<T>() {
    private var pendding = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        if (hasActiveObservers()) {
            Logger.w("Multiple observers registered but only one will be notified of changes.")
        }
        super.observe(
            owner,
            Observer {
                if (pendding.compareAndSet(true, false)) {
                    observer.onChanged(it)
                }
            }
        )
    }

    @MainThread
    override fun setValue(value: T?) {
        pendding.set(true)
        super.setValue(value)
    }

    override fun postValue(value: T) {
        pendding.set(true)
        super.postValue(value)
    }
}
