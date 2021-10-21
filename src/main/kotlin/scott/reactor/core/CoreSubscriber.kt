package scott.reactor.core

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * A simple subscriber with a provided consumer function
 */
class CoreSubscriber<T>(val consumer: (T) -> Unit) : Subscriber<T> {
    /**
     * When we are subscribed, request as much data as possible
     */
    override fun onSubscribe(subscription: Subscription) {
        subscription.request(Long.MAX_VALUE)
    }

    /**
     * When we receive an event consume it with our consuming function to do some logic
     */
    override fun onNext(event: T) = consumer(event)

    /**
     * Called when an error occurred during processing of events
     */
    override fun onError(t: Throwable?) {}

    /**
     * Called when the publisher has no more data for us
     */
    override fun onComplete() {}
}
