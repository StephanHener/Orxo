package com.tractive.android.orxo

import android.util.Log
import com.jakewharton.rxrelay.*
import rx.Scheduler
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.subscriptions.CompositeSubscription
import java.util.*


abstract class OrxoBus(val isSerialized: Boolean = false) {


    val relay: Relay<Any, Any> by lazy {
        if (isSerialized) SerializedRelay(provideRelay()) else provideRelay()
    }

    private val subscriptions: HashMap<Any, CompositeSubscription> by lazy {
        HashMap<Any, CompositeSubscription>()
    }

    protected abstract fun provideRelay(): Relay<Any, Any>

    fun unregister(_subscriber: Any) {
        subscriptions[_subscriber]?.apply {
            clear()
            subscriptions.remove(_subscriber)
        }
    }

    @JvmOverloads
    fun <T> getEvent(_subscriber: Any, _event: Class<T>, _scheduler: Scheduler = AndroidSchedulers.mainThread(), _action: Action1<T>) = relay
            .ofType(_event)
            .observeOn(_scheduler)
            .onBackpressureDrop()
            .subscribe(_action, Action1 { Log.i("Orxo", "Exception in OrxoBus: " + it.message) })
            .register(_subscriber)

    fun post(_event: Any) = relay.call(_event)

    private fun register(_object: Any, subscription: Subscription) {
        subscriptions[_object] = (subscriptions[_object] ?: CompositeSubscription())
                .apply {
                    add(subscription)
                }
    }

    private fun Subscription.register(_subscriber: Any) = register(_subscriber, this)

    class Behaviour(isSerialized: Boolean = false) : OrxoBus(isSerialized) {
        override fun provideRelay(): Relay<Any, Any> = BehaviorRelay.create()
    }

    class Publish(isSerialized: Boolean = false) : OrxoBus(isSerialized) {
        override fun provideRelay(): Relay<Any, Any> = PublishRelay.create()
    }

    class Replay(isSerialized: Boolean = false) : OrxoBus(isSerialized) {
        override fun provideRelay(): Relay<Any, Any> = ReplayRelay.create()
    }
}

class Orxo(private val subscriber: Any, val bus: OrxoBus, private val scheduler: Scheduler) {

    @JvmOverloads
    fun <T> subscribe(_event: Class<T>, _schedule: Scheduler = scheduler, _action1: Action1<T>) {
        bus.getEvent(subscriber, _event, _schedule, _action1)
    }

    fun unregister() {
        bus.unregister(subscriber)
    }
}






