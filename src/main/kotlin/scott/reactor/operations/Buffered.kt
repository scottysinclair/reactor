package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import scott.reactor.core.roundDownToMaxInt
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Subscriptions to BufferedPublisher will receive all emitted events from the time the BufferedPublisher was created.
 * The BufferedPublisher subscribes to the parentPublisher immediately (in order to get the data upfront even if there is noone subscribing to it)
 * The BufferedPublisher then cancels the subscription to the parentPublisher when it's last subscriber is removed
 */
class BufferedPublisher<T>(parentPublisher: Publisher<T>) : Publisher<T> {
    val bufferedSubscriber = BufferedSubscriber<T>()
    init {
        /*
         * subscribe to the parent publisher immediately so that we start to collect and buffer events
         */
        parentPublisher.subscribe(bufferedSubscriber)
    }

    override fun subscribe(subscriber: Subscriber<in T>) {
        bufferedSubscriber.add(subscriber)
    }
}

/**
 * Subscribes to the parent publisher and collects the emitted events into a buffer
 * Subscriptions to BufferedPublisher are dynamically added and start to receive events from the buffer in a FIFO manner.
 */
class BufferedSubscriber<T> : Subscriber<T> {
    /**
     * The subscription to the parent Publisher
     */
    lateinit var subscriptionToParent: Subscription

    /**
     * If the parent publisher has completed
     */
    private val parentPublisherCompleted = AtomicBoolean(false)

    /**
     *  BufferedPublisher subscriptions
     */
    val subscriptions = mutableListOf<BufferedSubscription<T>>()

    /**
     *  The buffer filled from the subscription to the parent Publisher
     */
    val buffer = mutableListOf<T>()

    /**
     * Called to add a BufferedPublisher Subscription
     */
    fun add(subscriber: Subscriber<in T>) {
        BufferedSubscription(this, subscriber).let {
            subscriptions.add(it)
            subscriber.onSubscribe(it)
        }
    }

    /**
     * Called when the underlying publisher provides data for us
     */
    override fun onNext(event: T) {
        buffer.add(event)
        subscriptions.forEach { it.drain() } //give each subscription a chance to emit from the buffer to it's subscriber
    }

    /**
     * Called when we are subscribed to the underlying publisher
     */
    override fun onSubscribe(subscription: Subscription) {
        this.subscriptionToParent = subscription
        subscriptionToParent.request(Long.MAX_VALUE)
    }

    override fun onError(t: Throwable) = subscriptions.forEach { it.subscriber.onError(t) }

    /**
     * Called when the underlying publisher completes, NOTE: it doesn't mean that WE should forward this... we need to make sure the buffer is drained
     */
    override fun onComplete() {
        parentPublisherCompleted.set(true)
        subscriptions.forEach { if (it.fullyDrained()) it.subscriber.onComplete() }
    }

    /**
     * Called to remove a buffered subscription
     */
    fun remove(bufferedSubscription: BufferedSubscription<T>) {
        subscriptions.remove(bufferedSubscription)
        if (subscriptions.isEmpty()) {
            subscriptionToParent.cancel()
        }
    }

    fun parentPublisherCompleted() = parentPublisherCompleted.get()
}

/**
 * A Subscription to the BufferedPublisher
 */
class BufferedSubscription<T>(val bufferedSubscriber: BufferedSubscriber<T>, val subscriber : Subscriber<in T>) : Subscription{
    /**
     * The subscriber's current capacity
     */
    private val subscribersCapacity = AtomicLong(0)

    /**
     * How much of the shared buffer we have already emitted to our Subscriber
     */
    val bufferConsumed = AtomicInteger(0)

    /**
     * If this subscription has fully emitted all events to our Subscriber
     */
    fun fullyDrained() = bufferConsumed.get() == bufferedSubscriber.buffer.size

    /**
     * Drain the next part of the buffer to the subscriber if it has capacity
     */
    @Synchronized
    fun drain() {
        val from = bufferConsumed.get()
        val to = bufferedSubscriber.buffer.size.coerceAtMost(subscribersCapacity.get().roundDownToMaxInt())
        bufferedSubscriber.buffer.subList(from, to).forEach { subscriber.onNext(it) }
        bufferConsumed.set(to)
    }

    /**
     * subscriber requests data, if we have data already buffered we can send it straight away
     */
    override fun request(numberOfEventsRequested: Long) {
        subscribersCapacity.updateAndGet { c ->
            if (numberOfEventsRequested > Long.MAX_VALUE - c) Long.MAX_VALUE
            else c + numberOfEventsRequested
        }
        if (!bufferedSubscriber.parentPublisherCompleted()) {
            //if the buffer cannot satisfy our subscriber's updated capacity, then request the difference from the parent Publisher
            val toRequestFromParent = subscribersCapacity.get() - (bufferedSubscriber.buffer.size - bufferConsumed.get())
            if (toRequestFromParent > 0) {
                bufferedSubscriber.subscriptionToParent.request(toRequestFromParent)
            }
        }
        /*
         / Use this oppertunity to drain to the Subscriber and even complete if possible
         */
        drain()
        if (bufferedSubscriber.parentPublisherCompleted() && fullyDrained()) subscriber.onComplete()
    }

    /**
     * Called to cancel the subscription
     */
    override fun cancel() {
        bufferedSubscriber.remove(this)
    }
}

