package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import scott.reactor.core.subscribe

/**
 * Subscriptions to FlatMapPublisher receive events emitted from each Publisher<DEST> provided by the flatMapper(event) function
 */
class FlatMapPublisher<SOURCE, DEST>(val flatMapper: (SOURCE) -> Publisher<DEST>, val publisher: Publisher<SOURCE>) : Publisher<DEST> {
    override fun subscribe(subscriber: Subscriber<in DEST>) {
        publisher.subscribe(FlatMapSubscriber(flatMapper, subscriber))
    }
}

/**
 * Decorates the given Subscriber<DEST> to present a Subscriber<SOURCE> which can subscribe to Publisher<SOURCE>
 * Each event emitted is transformed into a Publisher<DEST> which is dynamically subscribed to, allowing the subscribed events to go to Subscriber<DEST>
 */
class FlatMapSubscriber<SOURCE, DEST>(val flatMapper: (SOURCE) -> Publisher<DEST>, val subscriber: Subscriber<in DEST>) : Subscriber<SOURCE> {
    override fun onNext(event: SOURCE) {
        //TODO: I guess we never need to cancel this dynamic subscription,
        //TODO: I guess we expect the underlying Publisher (which receives the subscription) to complete it normally when there is no more data, sounds reasonable
        flatMapper(event).subscribe { ev -> subscriber.onNext(ev) }
        //TODO:hmm, perhaps when onNext is called again we can cancel the previous subscription?????? - is it a good idea? maybe not...
    }

    override fun onSubscribe(subscription: Subscription) {
        subscriber.onSubscribe(subscription)
    }

    override fun onError(t: Throwable) = subscriber.onError(t)
    override fun onComplete() = subscriber.onComplete()
}

/**
 * TODO: we should have our own FlatMapSubscription, so that we know when the Subscriber to  FlatMapPublisher cancels, and we can cancel the subscriptions we have made to all those Publisher<DEST> to prevent further
 * events being emitted
 */

