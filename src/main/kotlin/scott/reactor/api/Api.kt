package scott.reactor.api

import org.reactivestreams.Publisher
import scott.reactor.core.CorePublisher
import scott.reactor.operations.*

/**
 * Fluxes and Monos are not technically required, you can do everything at the  Publisher level
 * But they provide nice types for an API so a Mono<String> will clearly return just one value....
 *
 * So we just create the API by wrapping publishers in Flux or Mono wrappers
 * It allows us to have the same implementations for both, as usually the code which works for emitting
 * N events is also correct for emitting only 1 event.
 */


open class Flux<T>(val wrapping: Publisher<T>) : Publisher<T> by wrapping {
    /**
     * buffer can be used to buffer a single event or multiple events
     */
    fun buffer() = BufferedPublisher(this).presentAsFlux()

    /**
     * Collecting a list must be from a Flux (many) and returns a Mono (single result of the whole list)
     */
    fun collectList() = CollectPublisher(this).presentAsMono()

    /**
     * Fliltering a Flux returns a Flux
     */
    fun filter(predicate: (T) -> Boolean) = FilteredPublisher(predicate, this).presentAsFlux()

    /**
     * Mapper
     */
    fun <DEST> map(mapper: (T) -> DEST) = MappedPublisher(mapper, this).presentAsFlux()


    /**
     * Flatmap for 1 event emitting N events, so both Mono and Flux can  this, the output in both cases is a Flux
     */
    fun <DEST> flatMap(mapper: (T) -> Publisher<DEST>): Publisher<DEST> =
        FlatMapPublisher(mapper, this).presentAsFlux()

    /**
     * Only Flux can have a next
     */
    fun next() = NextPublisher(this).presentAsMono()


}

open class Mono<T>(val wrapping: Publisher<T>) : Publisher<T> by wrapping{

    /**
     * buffer can be used to buffer a single event or multiple events
     */
    fun buffer() = BufferedPublisher(this).presentAsMono()

    /**
     * A filtered Mono, when the predicate is false, it will just complete without emitting anything
     */
    fun filter(predicate: (T) -> Boolean) = FilteredPublisher(predicate, this).presentAsMono()

    /**
     * Mapper
     */
    fun <DEST> map(mapper: (T) -> DEST) = MappedPublisher(mapper, this).presentAsMono()

    /**
     * Flatmap for 1 event emitting N events, so both Mono and Flux can  this, the output in both cases is a Flux
     */
    fun <DEST> flatMap(mapper: (T) -> Publisher<DEST>): Publisher<DEST> =
        FlatMapPublisher(mapper, this).presentAsFlux()

}

fun <T> Publisher<T>.presentAsMono() : Mono<T> = Mono(this)
fun <T> Publisher<T>.presentAsFlux() : Flux<T> = Flux(this)

/**
 * Concat can equally concat Monos and Fluxes and returns a Flux
 */
fun <T> List<Publisher<T>>.concat() = ConcatPublisher(this).presentAsFlux()



class FluxSink<T>(val corePublisher: CorePublisher<T>) : Flux<T>(corePublisher){
    fun emitNext(vararg events: T) = corePublisher.emitNext(*events)
    fun complete() = corePublisher.complete()
}

class MonoSink<T>(val corePublisher: CorePublisher<T>) : Mono<T>(corePublisher) {
    fun emitNext(vararg events: T) = corePublisher.emitNext(*events)
    fun complete() = corePublisher.complete()
}

class Sinks {
    companion object {
        fun <T> many(): FluxSink<T> = FluxSink(CorePublisher())
        fun <T> one(): MonoSink<T> = MonoSink(CorePublisher())
    }
}

