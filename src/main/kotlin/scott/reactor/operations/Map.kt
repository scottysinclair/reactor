package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Subscriptions to MappedPublisher receive DEST events transformed from SOURCE events emitted from the parent Publisher<SOURCE>
 */
class MappedPublisher<SOURCE, DEST>(val mapper: (SOURCE) -> DEST, val parentPublisher: Publisher<SOURCE>) : Publisher<DEST> {
    override fun subscribe(subscriber: Subscriber<in DEST>) {
        parentPublisher.subscribe(MappedSubscriber(mapper, subscriber))
    }
}

/**
 * Decorates the given Subscriber<DEST> to present a Subscriber<SOURCE> which can subscribe to Publisher<SOURCE>
 */
class MappedSubscriber<SOURCE, DEST>(val mapper: (SOURCE) -> DEST, val subscriber: Subscriber<in DEST>) : Subscriber<SOURCE> {
    override fun onNext(event: SOURCE) {
        subscriber.onNext(mapper(event)) //subscriber gets the transformed value
    }

    override fun onSubscribe(subscription: Subscription) {
        subscriber.onSubscribe(subscription)
    }

    override fun onError(t: Throwable) = subscriber.onError(t)
    override fun onComplete() = subscriber.onComplete()
}

