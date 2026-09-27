package de.ole101.lodestone.reactive

import de.ole101.lodestone.exceptionManager
import de.ole101.lodestone.schedulerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.minestom.server.MinecraftServer

private class ManualScheduler : FlushScheduler {
    var pending: Runnable? = null
    var scheduled = 0
    var checks = 0
    var rejectWrites = false

    override fun schedule(flush: Runnable) {
        pending = flush
        scheduled++
    }

    override fun checkWrite() {
        checks++
        check(!rejectWrites) { "write rejected" }
    }

    fun run() {
        val flush = checkNotNull(pending) { "no flush scheduled" }
        pending = null
        flush.run()
    }
}

class ReactiveTest : FunSpec({

    beforeSpec { MinecraftServer.init() }

    lateinit var scheduler: ManualScheduler
    lateinit var scope: ReactiveScope

    beforeTest {
        scheduler = ManualScheduler()
        scope = ReactiveScope(scheduler)
    }

    afterTest { scope.dispose() }

    test("an effect reruns once per flush and sees the last value") {
        val count = state(0)
        val seen = mutableListOf<Int>()
        scope.effect { seen += count.value }
        scheduler.run()
        scheduler.scheduled shouldBe 1

        count.value = 1
        count.value = 2
        scheduler.scheduled shouldBe 2
        scheduler.run()

        seen shouldContainExactly listOf(0, 2)
        scheduler.scheduled shouldBe 2
    }

    test("writing an equal value schedules no flush") {
        val count = state(0)
        scope.effect { count.value }
        scheduler.run()

        count.value = 0
        scheduler.pending shouldBe null
    }

    test("a derived read twice without changes is computed once") {
        val count = state(1)
        var computed = 0
        val double = scope.derived {
            computed++
            count.value * 2
        }

        double.value shouldBe 2
        double.value shouldBe 2
        computed shouldBe 1

        count.value = 2
        double.value shouldBe 4
        computed shouldBe 2
    }

    test("an effect behind a diamond runs once per change and never sees it out of sync") {
        val a = state(1)
        val b = scope.derived { a.value + 1 }
        val c = scope.derived { a.value * 10 }
        val seen = mutableListOf<Pair<Int, Int>>()
        scope.effect { seen += b.value to c.value }
        scheduler.run()

        a.value = 2
        scheduler.run()

        seen shouldContainExactly listOf(2 to 10, 3 to 20)
    }

    test("an effect reached indirectly before directly still sees the changed state") {
        val count = state(1)
        val isPositive = scope.derived { count.value > 0 }
        val seen = mutableListOf<Pair<Boolean, Int>>()
        scope.effect { seen += isPositive.value to count.value }
        scheduler.run()

        count.value = 2
        scheduler.run()

        seen shouldContainExactly listOf(true to 1, true to 2)
    }

    test("effects are skipped when a derived recomputes to the same value") {
        val count = state(1)
        val isPositive = scope.derived { count.value > 0 }
        var runs = 0
        scope.effect {
            isPositive.value
            runs++
        }
        scheduler.run()

        count.value = 5
        scheduler.run()

        runs shouldBe 1
    }

    test("writes to a branch that is no longer read do not rerun the effect") {
        val flag = state(true)
        val a = state("a")
        val b = state("b")
        val seen = mutableListOf<String>()
        scope.effect { seen += if (flag.value) a.value else b.value }
        scheduler.run()

        flag.value = false
        scheduler.run()
        a.value = "a2"
        scheduler.pending shouldBe null
        b.value = "b2"
        scheduler.run()

        seen shouldContainExactly listOf("a", "b", "b2")
    }

    test("onCleanup runs before each rerun and on dispose") {
        val count = state(0)
        val log = mutableListOf<String>()
        scope.effect {
            val value = count.value
            log += "run $value"
            onCleanup { log += "cleanup $value" }
        }
        scheduler.run()
        count.value = 1
        scheduler.run()
        scope.dispose()

        log shouldContainExactly listOf("run 0", "cleanup 0", "run 1", "cleanup 1")
    }

    test("cleanups of an effect that disposes its own scope still run") {
        val close = state(false)
        val log = mutableListOf<String>()
        scope.effect {
            onCleanup { log += "cleanup" }
            if (close.value) scope.dispose()
        }
        scheduler.run()
        close.value = true
        scheduler.run()

        log shouldContainExactly listOf("cleanup", "cleanup")
    }

    test("a rejected write changes neither the state nor the graph") {
        val count = state(0)
        val double = scope.derived { count.value * 2 }
        scope.effect { double.value }
        scheduler.run()

        scheduler.rejectWrites = true
        shouldThrow<IllegalStateException> { count.value = 1 }
        scheduler.rejectWrites = false

        count.value shouldBe 0
        double.value shouldBe 0
        count.value = 2
        scheduler.run()
        double.value shouldBe 4
    }

    test("every write that reaches an effect is checked, not only the first of a flush") {
        val count = state(0)
        scope.effect { count.value }
        scheduler.run()
        count.value = 1
        scheduler.checks = 0

        count.value = 2
        scheduler.checks shouldBe 1
    }

    test("pre-effects run before effects queued in the same flush") {
        val log = mutableListOf<String>()
        scope.effect { log += "effect" }
        scope.preEffect { log += "pre" }
        scheduler.run()

        log shouldContainExactly listOf("pre", "effect")
    }

    test("an effect writing a state another effect reads settles in one flush") {
        val source = state(1)
        val copy = state(0)
        val seen = mutableListOf<Int>()
        scope.effect { seen += copy.value }
        scope.effect { copy.value = source.value * 2 }
        scheduler.run()

        source.value = 5
        scheduler.run()

        seen shouldContainExactly listOf(0, 2, 10)
        scheduler.pending shouldBe null
    }

    test("a write to an effect waiting in the current batch does not queue it again") {
        val source = state(0)
        val copy = state(0)
        val seen = mutableListOf<Pair<Int, Int>>()
        scope.effect { copy.value = source.value }
        scope.effect { seen += source.value to copy.value }
        scheduler.run()

        source.value = 1
        scheduler.checks = 0
        scheduler.run()

        seen shouldContainExactly listOf(0 to 0, 1 to 1)
        // Only the write is checked; the effect is already queued
        scheduler.checks shouldBe 1
        scheduler.pending shouldBe null
        scheduler.scheduled shouldBe 2
    }

    test("two effects writing each other forever throw after 100 rounds") {
        val a = state(0)
        val b = state(0)
        scope.effect { a.value = b.value + 1 }
        scope.effect { b.value = a.value + 1 }

        val error = shouldThrow<IllegalStateException> { scheduler.run() }
        error.message shouldContain "100 flush rounds"
        error.message shouldContain "effect("
    }

    test("disposing the scope unsubscribes it from global states") {
        val global = state(0) as StateImpl<Int>
        val double = scope.derived { global.value * 2 }
        scope.effect { double.value }
        scheduler.run()
        global.observers.size shouldBe 1

        scope.dispose()
        global.observers.shouldBeEmpty()
        global.value = 1
        scheduler.pending shouldBe null
    }

    test("untrack reads without subscribing") {
        val count = state(0)
        var runs = 0
        scope.effect {
            untrack { count.value }
            runs++
        }
        scheduler.run()

        count.value = 1
        scheduler.pending shouldBe null
        runs shouldBe 1
    }

    test("a throwing effect goes to the exception manager and the others still run") {
        val caught = mutableListOf<Throwable>()
        val previous = exceptionManager.exceptionHandler
        exceptionManager.setExceptionHandler { caught += it }
        try {
            val failure = IllegalStateException("boom")
            var ran = false
            scope.effect { throw failure }
            scope.effect { ran = true }
            scheduler.run()

            caught shouldContainExactly listOf(failure)
            ran shouldBe true
        } finally {
            exceptionManager.exceptionHandler = previous
        }
    }

    test("EndOfTick flushes in the tick end, also when scheduled from a tick end task") {
        val endOfTick = ReactiveScope()
        val count = state(0)
        val seen = mutableListOf<Int>()
        endOfTick.effect { seen += count.value }
        schedulerManager.scheduleEndOfTick { count.value = 1 }

        // The write schedules another flush within the same tick end
        schedulerManager.processTickEnd()

        seen shouldContainExactly listOf(0, 1)
        endOfTick.dispose()
    }
})
