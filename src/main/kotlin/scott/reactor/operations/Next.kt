package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import scott.reactor.api.presentAsMono
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Subscriptions to NextPublisher will only receive the single next value from the parent publisher
 */
class NextPublisher<T>(val parentPublisher: Publisher<T>) : Publisher<T> {
    override fun subscribe(subscriber: Subscriber<in T>) {
        parentPublisher.subscribe(NextSubscriber(subscriber))
    }
}

/**
 * Decorates the given Subscriber<T> so that it only receives the next emitted event and then receives onComplete()
 */
class NextSubscriber<T>(val subscriber: Subscriber<in T>) : Subscriber<T> {
    private lateinit var subscription: Subscription
    private val terminated = AtomicBoolean(false)

    /**
     * When the first event is emitted we tell the subscriber we are complete and we cancel our Subscription to the parent Publisher
     */
    override fun onNext(event: T) {
        if (!terminated.compareAndExchange(false, true)) {
            subscriber.onNext(event)
            subscriber.onComplete()
            subscription.cancel()
        }
    }

    override fun onSubscribe(subscription: Subscription) {
        this.subscription = subscription
        subscriber.onSubscribe(subscription)
    }

    override fun onError(t: Throwable) {
        if (!terminated.compareAndExchange(false, true)) {
            subscriber.onError(t)
        }
    }

    override fun onComplete() {
        if (!terminated.compareAndExchange(false, true)) {
            subscriber.onComplete()
        }
    }
}

