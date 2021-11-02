package scott.reactor.operations

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Subscriptions to CollectPublisher will receive all events collected together (List<T>) when the parent Publisher completes.
 */
class CollectPublisher<T>(val parentPublisher: Publisher<T>) : Publisher<List<T>> {
    override fun subscribe(subscriber: Subscriber<in List<T>>) {
        parentPublisher.subscribe(CollectSubscriber(subscriber))
    }
}

/**
 * Decorates the Subscriber<List<T>> so that it can subscribe to a Publisher<T> and emit the collected events (List<T>) when Publisher<T> completes
 */
class CollectSubscriber<T>(val subscriber: Subscriber<in List<T>>) : Subscriber<T> {
    private val list = mutableListOf<T>()
    override fun onSubscribe(subscription: Subscription) {
        subscriber.onSubscribe(subscription)
    }

    override fun onNext(event: T) {
        list.add(event)
    }

    override fun onError(t: Throwable) = subscriber.onError(t)

    override fun onComplete() {
        subscriber.onNext(list)
        subscriber.onComplete()
    }
}


