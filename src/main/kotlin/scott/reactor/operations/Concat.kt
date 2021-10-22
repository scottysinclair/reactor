package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Subscriptions to ConcatPublisher will receive events from Publisher[0] until it completes, then Publisher[1], etc...
 * Subscriptions to ConcatPublisher complete when the last Publisher completes (Publisher[N-1])
 */
class ConcatPublisher<T>(val publishers: List<Publisher<T>>) : Publisher<T> {
    override fun subscribe(subscriber: Subscriber<in T>) {
        ConcatSubscriber(subscriber, publishers).start()
    }
}

/**
 * Iteratively subscribes to each parent Publisher forwarding the events to Subscriber<T> until the parent completes, then moves onto the next...
 * Provides Subscriber<T> with a single constant Subscription until final completion
 */
class ConcatSubscriber<T>(val subscriber: Subscriber<in T>, publishers: List<Publisher<T>>) : Subscriber<T>, Subscription {
    /**
     * our iterator across the parent publishers
     */
    private val iterator = publishers.iterator()

    /**
     * the current publisher subscribed to by us, which we are receiving events for
     */
    private lateinit var currentPublisher: Publisher<T>

    /**
     * the current subscription to the current Publisher
     */
    private lateinit var currentSubscription: Subscription

    /**
     * The last number of events requested, we simply ask the same amount from the 'next' publisher in the chain
     */
    private var lastNumberOfEventsRequested: Long = 0L

    /**
     * Kick off the first Publisher in the chain
     */
    fun start() {
        currentPublisher = iterator.next()
        currentPublisher.subscribe(this) //this as subscriber
        subscriber.onSubscribe(this)  //this as 'special concat' subscription
    }

    /**
     * Events go through to the real Subscriber
     */
    override fun onNext(event: T) {
        subscriber.onNext(event)
    }

    /**
     * Called when the current Publisher has accepted us
     */
    override fun onSubscribe(subscription: Subscription) {
        currentSubscription = subscription
        if (lastNumberOfEventsRequested > 0) {
            /*
             * request some data from him, so that we get something
             */
            currentSubscription.request(lastNumberOfEventsRequested)
        }
    }

    override fun onError(t: Throwable) {
        subscriber.onError(t)
    }

    /**
     * Called when the current Publisher completes, we will move onto the next Publisher or complete ourselves.
     */
    override fun onComplete() {
        if (iterator.hasNext()) {
            currentPublisher = iterator.next()
            currentPublisher.subscribe(this)
        } else {
            subscriber.onComplete()
        }
    }

    /**
     * The Subscriber to ConcatPublisher has requested data...
     */
    override fun request(numberOfEventsRequested: Long) {
        lastNumberOfEventsRequested = numberOfEventsRequested
        currentSubscription.request(numberOfEventsRequested)
    }

    /**
     * The Subscriber to ConcatPublisher is cancelling the subscription.
     */
    override fun cancel() {
        currentSubscription.cancel()
    }
}

