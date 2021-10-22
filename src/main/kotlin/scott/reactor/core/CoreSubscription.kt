package scott.reactor.core

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 *  A Subscription between a publisher and a subscriber
 *  We have the queuing logic here to manage publisher and subscriber supply & demand
 */
class CoreSubscription<T>(val cancelSub : (Subscription) -> Unit, val subscriber : Subscriber<in T>) : Subscription {
    /**
     * keep count of what the subscriber says he can take
     */
    private var subscribersCapacity = AtomicLong(0)
    private val queue = mutableListOf<T>()
    private val publisherCompleted = AtomicBoolean(false)
    private val terminated = AtomicBoolean(false)

    /**
     *  used by the publisher to publish data towards a Subscriber
     *  the data enters the FIFO queue to be drained
     */
    fun publish(event : T) {
        if (!terminated.get()) queue.syncAdd(event)
        drain()
    }

    fun drain() {
        queue.syncExtractMax(subscribersCapacity.get())
            .forEach { subscriber.onNext(it).also { subscribersCapacity.decrementAndGet() } }
        /*
         * if we have sent everything we have and the publisher is complete then tell the subcriber that we are done
         */
        if (queue.isEmpty() && publisherCompleted.get()) {
            subscriber.onComplete()
        }
    }

    /**
     * The subscriber is requesting the publisher to send some data if it can
     */
    override fun request(numberOfEventsRequested: Long) {
        if (!terminated.get()) {
            subscribersCapacity.updateAndGet { c ->
                if (numberOfEventsRequested > Long.MAX_VALUE - c) Long.MAX_VALUE
                else c + numberOfEventsRequested
            }
            drain()
        }
    }

    /**
     * Called by the publisher when it completes
     */
    fun complete() {
        publisherCompleted.set(true)
    }

    /**
     * Called to cancel the subscription
     */
    override fun cancel() {
        if (!terminated.compareAndExchange(false, true)) {
            subscriber.onComplete()
            cancelSub(this)
        }
    }
}
