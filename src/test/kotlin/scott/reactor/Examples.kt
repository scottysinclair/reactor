package scott.reactor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scott.reactor.core.subscribe
import scott.reactor.api.*
import scott.reactor.diagram.toYumlString

class Examples {

    @Test
    fun `direct subscription 100 numbers`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).toList())
    }

    @Test
    fun `direct subscription 100 strings`() {
        val numbersPublisher = Sinks.many<String>()
        val results = mutableListOf<String>()

        numbersPublisher.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext("number $i") }

        assertThat(results).isEqualTo((1..100).map { "number $it" }.toList())
    }

    @Test
    fun `simple map example`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<String>()

        numbersPublisher.map { "number $it" }.apply { subscribe {  results.add(it) }; println(toYumlString()) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).map { "number $it" }.toList())
    }

    @Test
    fun `simple filter example`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.filter { it % 2 == 0 }.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).filter { it % 2 == 0 }.toList())
    }

    @Test
    fun `map and filter example`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<String>()

        numbersPublisher.filter { it % 2 == 0 }.map { "number $it" }.also { println(it.toYumlString()) }.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        println(numbersPublisher.toYumlString())

        assertThat(results).isEqualTo((1..100).filter { it % 2 == 0 }.map { "number $it" }.toList())
    }

    @Test
    fun `next example`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.filter { it == 5 }.next().subscribe {  results.add(it) }
        val source = listOf(1,2,3,4,5,5,5,5)
        source.forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo(listOf(5))
    }

    @Test
    fun `1 publisher decorated by 100 child filer+next publishers publisher-and-subscription example`() {
        val numbersPublisher = Sinks.many<Int>()
        //set up an array of 100 MutableLists for the upcoming 100 'next' subscriptions
        val results = (1..100).map { mutableListOf<Int>() }.toTypedArray()

        //create 100 publishers from the numbersPublisher for numbers 1..100, each one has it's own subscription
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next().subscribe { results[i-1].add(it) }  }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //each array element should have it's single value
        (1..100).forEach {i -> assertThat(results[i-1]).isEqualTo(listOf(i)) }
    }

    @Test
    fun `1 publisher decorated by 100 child filter+next publishers which are all then concatenated back together into a single publisher - pointless but fun`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<Int>()

        //create 100 publishers from the numbersPublisher for numbers 1..100, concat them together and subscribe
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().subscribe { results.add(it) }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //concat respects the order of concatenation...
        assertThat(results).isEqualTo((1..100).toList())
    }

    @Test
    fun `1 publisher decorated by 100 single filter+next publishers which are then concatenated back into a single publisher and then collected into a single List which is emitted upon completion`() {
        val numbersPublisher = Sinks.many<Int>()
        val results = mutableListOf<List<Int>>()

        //create 100 publishers from the numbersPublisher for numbers 1..100, concat them together and collect all into a list  and subscribe
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().collectList().subscribe { listOfNumbers -> results.add(listOfNumbers) }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //received 1 subscribe call containing the full 100 events
        assertThat(results).isEqualTo(listOf((1..100).toList()))
    }

    @Test
    fun `regardless of complexity - of a chain of decorated Publishers - the  Publisher 'facing the programmer' is always subscribed to  independently as much as we want - as they are only factories for decorated subscriptions`() {
        val numbersPublisher = Sinks.many<Int>()
        val results1 = mutableListOf<List<Int>>()
        val results2 = mutableListOf<List<Int>>()
        val results3 = mutableListOf<List<Int>>()

        //create 100 publishers from the numbersPublisher for numbers 1..100, concat them together and collect all into a list
        val complexPublisher = (1..10).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().collectList()

        //println(complexPublisher.toYumlString())

        //subscribe to that in 3 different ways, to see that the subscriptions are completely independent
        complexPublisher.subscribe { listOfNumbers -> results1.add(listOfNumbers) }.also { println(it.toYumlString()) }

        complexPublisher.map { list -> list.filter { it % 2 == 0 } }.subscribe { listOfNumbers -> results2.add(listOfNumbers) }
        complexPublisher.map { list -> list.filter { it % 3 == 0 } }.subscribe { listOfNumbers -> results3.add(listOfNumbers) }

        println(complexPublisher.toYumlString())

        (1..10).forEach { i -> numbersPublisher.emitNext(i) }

        //each result is correctly (and independently) populated from the subscription
        assertThat(results1).isEqualTo(listOf((1..10).toList()))
        assertThat(results2).isEqualTo(listOf((1..10).filter { it % 2 == 0 }.toList()))
        assertThat(results3).isEqualTo(listOf((1..10).filter { it % 3 == 0 }.toList()))
    }

    @Test
    fun `flatmap to subscribe to the combination of two different Publishers`() {
        val agePublisher = Sinks.many<Int>()
        val namePublisher = Sinks.many<String>()
        val result = mutableListOf<String>()

        /*
         * subscribe to the publication of age '10' and name 'John' and combine both in an output which we subscribe to.
         */
        agePublisher.filter { it == 10 }.flatMap { number10 -> namePublisher.filter { it == "John" }.map { name -> "$name is $number10 years old" } }.also { println(it.toYumlString()) }.subscribe {  result.add(it) }

        (1..10).forEach { agePublisher.emitNext(it) }
        listOf("Fred", "Ian", "John").forEach { namePublisher.emitNext(it) }

        assertThat(result).isEqualTo(listOf(("John is 10 years old")))
    }

    @Test
    fun `flatMap conversion from Publisher List of item to Publisher item  Publishers - flatmap is good for splitting things up`() {
        val listPublisher = Sinks.many<List<Int>>()
        val result = mutableListOf<Int>()

        //classic flatmap List<List<T>> to List<T> or in reactor world Publisher<List<T>> Publisher<T>
        listPublisher.flatMap { list ->
            /*
             * Each time a List<Int> is received, create a new BufferedPublisher<Int> and publish all values into it
             * The downstream  Subscriber<Int> will sequentially receive all Int values via these BufferedPublisher<Int>
             */
            val singlePublisher = Sinks.many<Int>()
            singlePublisher.buffer().also {
                list.forEach { singlePublisher.emitNext(it) } // then emit each list element into it
                singlePublisher.complete()  //complete the publisher, allowing the buffer to be reclaimed (the subscriber which is notified of completion (onComplete()) is an internal subscriber created by the flatmap, NOT the end susbscriber.
            }
        }.subscribe { result.add(it) }  //the subscriber is getting the individual elements
        listPublisher.emitNext((1..100).toList())
        listPublisher.complete()
        assertThat(result).isEqualTo((1..100).toList())
    }

}

