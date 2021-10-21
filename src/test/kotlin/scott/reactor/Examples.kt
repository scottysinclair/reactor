package scott.reactor

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scott.reactor.core.CorePublisher
import scott.reactor.core.subscribe
import scott.reactor.operations.*

class Examples {

    @Test
    fun `direct subscription 100 numbers`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).toList())
    }

    @Test
    fun `direct subscription 100 strings`() {
        val numbersPublisher = CorePublisher<String>()
        val results = mutableListOf<String>()

        numbersPublisher.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext("number $i") }

        assertThat(results).isEqualTo((1..100).map { "number $it" }.toList())
    }

    @Test
    fun `simple map example`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<String>()

        numbersPublisher.map { "number $it" }.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).map { "number $it" }.toList())
    }

    @Test
    fun `simple filter example`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.filter { it % 2 == 0 }.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).filter { it % 2 == 0 }.toList())
    }

    @Test
    fun `map and filter example`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<String>()

        numbersPublisher.filter { it % 2 == 0 }.map { "number $it" }.subscribe {  results.add(it) }
        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo((1..100).filter { it % 2 == 0 }.map { "number $it" }.toList())
    }

    @Test
    fun `next example`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<Int>()

        numbersPublisher.filter { it == 5 }.next().subscribe {  results.add(it) }
        val source = listOf(1,2,3,4,5,5,5,5)
        source.forEach { i -> numbersPublisher.emitNext(i) }

        assertThat(results).isEqualTo(source.first { it == 5 }.let { listOf(it) })
    }

    @Test
    fun `100 single publisher-and-subscription example`() {
        val numbersPublisher = CorePublisher<Int>()
        //set up an array of 100 MutableLists for the upcoming 100 'next' subscriptions
        val results = (1..100).map { mutableListOf<Int>() }.toTypedArray()

        //create 100 publishers for numbers 1..100, each one has it's own subscription
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next().subscribe { results[i-1].add(it) }  }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //each array element should have it's single value
        (1..100).forEach {i -> assertThat(results[i-1]).isEqualTo(listOf(i)) }
    }

    @Test
    fun `100 single publishers concatenated into a single publisher - and subscribed to`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<Int>()

        //create 100 publishers for numbers 1..100, concat them together and subscribe
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().subscribe { results.add(it) }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //concat respects the order of concatenation...
        assertThat(results).isEqualTo((1..100).toList())
    }

    @Test
    fun `100 single publishers, concatenated into a single publisher, collected into a publisher which emits a single List event completion - and subscribed to`() {
        val numbersPublisher = CorePublisher<Int>()
        val results = mutableListOf<List<Int>>()

        //create 100 publishers for numbers 1..100, concat them together and subscribe
        (1..100).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().collectList().subscribe { listOfNumbers -> results.add(listOfNumbers) }
        //publish the numbers 1..100 twice
        (1..2).forEach { (1..100).forEach { i -> numbersPublisher.emitNext(i) } }

        //received 1 subscribe call containing the full 100 events
        assertThat(results).isEqualTo(listOf((1..100).toList()))
    }

    @Test
    fun `regardless of complexity - of a chain of decorated Publishers - the  Publisher 'facing the programmer' is always subscribed to  independently as much as we want - as they are only factories for decorated subscriptions`() {
        val numbersPublisher = CorePublisher<Int>()
        val results1 = mutableListOf<List<Int>>()
        val results2 = mutableListOf<List<Int>>()
        val results3 = mutableListOf<List<Int>>()

        //create 100 publishers for numbers 1..100, concat them together and subscribe
        val complexPublisher = (1..100).map { i ->  numbersPublisher.filter { it == i }.next()  }.concat().collectList()

        complexPublisher.map { list -> list.filter { it % 1 == 0 } }.subscribe { listOfNumbers -> results1.add(listOfNumbers) }
        complexPublisher.map { list -> list.filter { it % 2 == 0 } }.subscribe { listOfNumbers -> results2.add(listOfNumbers) }
        complexPublisher.map { list -> list.filter { it % 3 == 0 } }.subscribe { listOfNumbers -> results3.add(listOfNumbers) }

        (1..100).forEach { i -> numbersPublisher.emitNext(i) }

        //each result is correctly populated from the subscription
        assertThat(results1).isEqualTo(listOf((1..100).toList()))
        assertThat(results2).isEqualTo(listOf((1..100).filter { it % 2 == 0 }.toList()))
        assertThat(results3).isEqualTo(listOf((1..100).filter { it % 3 == 0 }.toList()))
    }

}

