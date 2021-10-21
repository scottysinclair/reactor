package scott.reactor

import org.reactivestreams.Publisher
import scott.reactor.core.CorePublisher
import scott.reactor.core.subscribe
import scott.reactor.operations.*

fun <T> List<Publisher<T>>.toMonoOfList() : Publisher<List<T>> = concat().collectList()

fun main(args: Array<String>) {

    val numberPublisher = CorePublisher<Int>()

    /**
     * subscribe to all numbers between 1 and 10, converting thenm to strings and printing them
     */
    numberPublisher.filter { it >= 1 && it <= 10 }
        .map { "Matched $it"  }
        .subscribe { println(it) }

    /**
     * subscribe to the collection of numbers from 1..10
     */
    (1..10).map { i ->
        numberPublisher.filter { it == i } //filter on publications of number I
            .next() //then take the next one for a (mono) single result
    }
    .toMonoOfList() // convert the List<Publisher<T>> to Publisher<List<T>> which publishes all data when "all the (mono) publishers complete"
    .subscribe { println("All numbers from 1 to 10 have been found!!!") } //subscribe and print


    /*
     * Publisher for the collection of numbers from 20..30
     * The Publisher itself has no lifecycle/state and can be subscribed to / reused as many times as we want
     * To prove this we will use it many times
     */
    val mono20_to_30 = (20..30).map { i ->  numberPublisher.filter { it == i }.next() }.toMonoOfList().buffer()

    /*
     * once here to subscribe directly and print out when we complete
     */
    mono20_to_30.subscribe { numbers -> println("SUB1: All numbers from 20 to 30 have been found!!!: $numbers") }
    mono20_to_30.subscribe { numbers -> println("SUB2: All numbers from 20 to 30 have been found!!!: $numbers") }


    /**
     * subscribe to 3 specific numbers
     */
    listOf(10, 35, 90).map { i ->  numberPublisher.filter { it == i }.next() }.toMonoOfList().subscribe { println("10, 35 and 90 have been found!!! $it") }

    /*
     * flatmap example, the next cheeze has to be published and the numbers from 20 to 30 before the subscription will receive anything
     */
    val foodPublisher = CorePublisher<String>()
    val theNextCheesePublisher = foodPublisher.filter { it == "cheese" }.next()
    theNextCheesePublisher.flatMap {
        mono20_to_30.map {
                nums -> nums.map { n -> "num $n" }
        }
    }.subscribe { numbers ->  println("the next cheeze was published and we also got the numbers $numbers") }


    /**
     * now make the numberPublisher emit some numbers
     */
    listOf(10, 35, 90).forEach { i -> numberPublisher.emitNext(i) }
    /*
    (1..5000).forEach {
        numberPublisher.emitNext((1..500).random())
    }*/

    (1..100).forEach { i ->
        numberPublisher.emitNext(i.also { println("EMIT $it") })
    }


    /*
     * Publish the food, this should cause the message "the next cheeze was published and we also got the numbers" to be printed to the console
     */
    foodPublisher.emitNext("bread").emitNext("cheese").emitNext("sausages")


}