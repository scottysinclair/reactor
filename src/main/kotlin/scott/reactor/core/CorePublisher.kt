package scott.reactor.core

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Publisher to which Subscribers subscribe
 */
class CorePublisher<T> : Publisher<T> {
    val subs = mutableListOf<CoreSubscription<T>>()
    private val completed = AtomicBoolean(false)

    /**
     * Register the subscriber with the publisher
     */
    override fun subscribe(subscriber: Subscriber<in T>) {
        synchronized(subs) { CoreSubscription(this::cancelSub, subscriber).also { subs.add(it) } }
            .also { subscriber.onSubscribe(it) }
    }

    /**
     * programmer friendly way to get the publisher to emit something
     */
    fun emitNext(vararg events : T) {
        if (!completed.get()) {
            synchronized(subs) { subs.toList() }.forEach { events.forEach { e ->it.publish(e) } }
        }
    }

    /**
     * programmer friendly way to get the Publisher to complete
     */
    fun complete() {
        synchronized(subs) { subs.toList() }.forEach { it.complete() }
    }

    /**
     * Called by subscriptions when they are cancelled
     */
    private fun cancelSub(sub : Subscription) {
        synchronized(subs) { subs.remove(sub) }
    }
}
