package scott.reactor.diagram

import org.reactivestreams.Publisher
import scott.reactor.api.Flux
import scott.reactor.api.Mono
import scott.reactor.core.CorePublisher
import scott.reactor.operations.*


fun build(diagram: DependencyDiagram, publisher: Publisher<*>) {
    when(publisher) {
        is Mono<*> -> build(diagram, publisher.wrapping)
        is Flux<*> -> build(diagram, publisher.wrapping)
        is BufferedPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is MappedPublisher<*,*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is FilteredPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is FlatMapPublisher<*,*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is NextPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is CollectPublisher<*> -> {
            diagram.link(publisher.nodeName(), publisher.parentPublisher.nodeName(), "", LinkType.DEPENDENCY_DASHED)
            build(diagram, publisher.parentPublisher)
        }
        is ConcatPublisher<*> -> {
            publisher.parentPublishers.forEachIndexed { i, pub ->
                diagram.link(publisher.nodeName(), pub.nodeName(), "", LinkType.DEPENDENCY_DASHED)
                build(diagram, pub)
            }
        }
        is CorePublisher<*> -> {
            //NOOP
        }
        else -> throw IllegalStateException("Cannot draw publisher: " + publisher.nodeName())
    }
}

fun <T> Publisher<T>.nodeName() : String {
  return when(this) {
      is Flux<*> -> wrapping.nodeName()
      is Mono<*> -> wrapping.nodeName()
      else -> "${javaClass.simpleName}${System.identityHashCode(this)}"
  }
}


fun <T> Publisher<T>.toYumlString() = DependencyDiagram().also { build(it, this) }.toYumlString()