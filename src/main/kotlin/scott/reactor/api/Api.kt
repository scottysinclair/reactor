package scott.reactor.api

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import scott.reactor.core.CorePublisher
import scott.reactor.operations.*


/**
Monos and Fluxes don't really exist in terms of separate implementations, we just have Publishers
So we only present Mono and Flux in the API for technical contract purposes (Mono<T> is contracted to asynchronously emit only 1 event)
 */
interface Flux<T> : Publisher<T>
interface Mono<T> : Publisher<T>

class FluxImpl<T>(val wrapping: Publisher<T>) : Flux<T> {
    override fun subscribe(subscriber: Subscriber<in T>?) {
        wrapping.subscribe(subscriber)
    }
}

class MonoImpl<T>(val wrapping: Publisher<T>) : Mono<T> {
    override fun subscribe(subscriber: Subscriber<in T>?) {
        wrapping.subscribe(subscriber)
    }
}

fun <T> Publisher<T>.presentAsMono() : Mono<T> = MonoImpl(this)
fun <T> Publisher<T>.presentAsFlux() : Flux<T> = FluxImpl(this)

/**
 * buffer can be used to buffer a single event or multiple events
 */
fun <T> Mono<T>.buffer() = BufferedPublisher(this).presentAsMono()
fun <T> Flux<T>.buffer() = BufferedPublisher(this).presentAsFlux()

/**
 * Collecting a list must be from a Flux (many) and returns a Mono (single result of the whole list)
 */
fun <T> Flux<T>.collectList() = CollectPublisher(this).presentAsMono()

/**
 * Concat can equally concat Monos and Fluxes and returns a Flux
 */
fun <T> List<Publisher<T>>.concat() = ConcatPublisher(this).presentAsFlux()

/**
 * Fliltering a Flux returns a Flux
 */
fun <T> Flux<T>.filter(predicate: (T) -> Boolean) = FilteredPublisher(predicate, this).presentAsFlux()

/**
 * A filtered Mono, when the predicate is false, it will just complete without emitting anything
 */
fun <T> Mono<T>.filter(predicate: (T) -> Boolean) = FilteredPublisher(predicate, this).presentAsMono()


/**
 * Flatmap for 1 event emitting N events, so both Mono and Flux can  this, the output in both cases is a Flux
 */

fun <T, DEST> Publisher<T>.flatMap(mapper: (T) -> Publisher<DEST>): Publisher<DEST> =
    FlatMapPublisher(mapper, this).presentAsFlux()

/*
* Flux and Mono mappers
*/
fun <SOURCE, DEST> Flux<SOURCE>.map(mapper: (SOURCE) -> DEST) = MappedPublisher(mapper, this).presentAsFlux()
fun <SOURCE, DEST> Mono<SOURCE>.map(mapper: (SOURCE) -> DEST) = MappedPublisher(mapper, this).presentAsMono()


/**
 * Only Flux can have a next
 */
fun <T> Flux<T>.next() = NextPublisher(this).presentAsMono()

class FluxSink<T>(val corePublisher: CorePublisher<T>) : Flux<T> {
    override fun subscribe(subscriber: Subscriber<in T>) = corePublisher.subscribe(subscriber)
    fun emitNext(vararg events: T) = corePublisher.emitNext(*events)
    fun complete() = corePublisher.complete()
}

class MonoSink<T>(val corePublisher: CorePublisher<T>) : Mono<T> {
    override fun subscribe(subscriber: Subscriber<in T>) = corePublisher.subscribe(subscriber)
    fun emitNext(vararg events: T) = corePublisher.emitNext(*events)
    fun complete() = corePublisher.complete()
}

class Sinks {
    companion object {
        fun <T> many(): FluxSink<T> = FluxSink(CorePublisher())
        fun <T> one(): MonoSink<T> = MonoSink(CorePublisher())
    }
}

