package scott.reactor.core

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import kotlin.math.min

fun <T> Publisher<T>.subscribe(consumer : (T) -> Unit) :Subscriber<T> = CoreSubscriber(consumer).also { subscribe(it) }

fun <T> MutableList<T>.syncAdd(value : T) = synchronized(this) { add(value) }

fun <T> MutableList<T>.syncExtractMax(max : Long) : List<T> = synchronized(this) {
    return subList(0, min(size, max.roundDownToMaxInt())).toList().also { removeAll(it) }
}

fun Long.roundDownToMaxInt() = min(Int.MAX_VALUE.toLong(), this).toInt()