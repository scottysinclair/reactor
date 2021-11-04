package scott.reactor.diagram

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import scott.reactor.api.Flux
import scott.reactor.api.Mono
import scott.reactor.core.CorePublisher
import scott.reactor.core.CoreSubscriber
import scott.reactor.operations.*


fun build(diagram: DependencyDiagram, publisher: Publisher<*>) {
    when(publisher) {
        is Mono<*> -> build(diagram, publisher.wrapping)
        is Flux<*> -> build(diagram, publisher.wrapping)
        is BufferedPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is MappedPublisher<*,*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is FilteredPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is FlatMapPublisher<*,*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is NextPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is CollectPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is ConcatPublisher<*> -> {
            publisher.parentPublishers.forEachIndexed { i, pub ->
                diagram.link(publisher.nodeName(), pub.nodeName(), "decor", LinkType.DEPENDENCY_DASHED)
                build(diagram, pub)
            }
        }
        is CorePublisher<*> -> {
            publisher.subs.forEach { sub ->
                diagram.link(publisher.nodeName(), sub.subscriber.nodeName(), "subscription", LinkType.DEPENDENCY)
                build(diagram, sub.subscriber)
            }
        }
        else -> throw IllegalStateException("Cannot draw publisher: " + publisher.nodeName())
    }
}

fun build(diagram: DependencyDiagram, subscriber: Subscriber<*>) {
    when(subscriber) {
        is BufferedSubscriber<*> -> {
            subscriber.subscriptions.forEach {
                diagram.link(subscriber.nodeName(), it.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
                build(diagram, it.subscriber)
            }
        }
        is CollectSubscriber<*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is ConcatSubscriber<*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is FilteredSubscriber<*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is MappedSubscriber<*,*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is FlatMapSubscriber<*,*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is NextSubscriber<*> -> {
            diagram.link(subscriber.nodeName(), subscriber.subscriber.nodeName(), "emit", LinkType.DEPENDENCY)
            build(diagram, subscriber.subscriber)
        }
        is CoreSubscriber<*> -> {
            //NOOP
        }
        else -> throw IllegalStateException("Cannot draw subscriber: " + subscriber.nodeName())
    }

}

fun <T> Publisher<T>.nodeName() : String {
  return when(this) {
      is Flux<*> -> wrapping.nodeName()
      is Mono<*> -> wrapping.nodeName()
      else -> "${javaClass.simpleName}${System.identityHashCode(this)}"
  }
}

fun <T> Subscriber<T>.nodeName() : String = "${javaClass.simpleName}${System.identityHashCode(this)}"


fun <T> Publisher<T>.toYumlString() = DependencyDiagram().also { build(it, this) }.toYumlString()
fun <T> Subscriber<T>.toYumlString() = DependencyDiagram().also { build(it, this) }.toYumlString()