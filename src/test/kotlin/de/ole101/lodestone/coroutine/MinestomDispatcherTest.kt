package de.ole101.lodestone.coroutine

import de.ole101.lodestone.exceptionManager
import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.TestServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

/** Calls processTick() on this thread, like the tick thread would, until [done] holds or 2 seconds pass. */
private fun tickUntil(done: () -> Boolean) {
    val deadline = System.nanoTime() + 2_000_000_000
    while (!done()) {
        check(System.nanoTime() < deadline) { "condition not met within 2 seconds" }
        schedulerManager.processTick()
        Thread.sleep(5)
    }
}

class MinestomDispatcherTest : FunSpec({

    beforeSpec { TestServer.init() }

    test("launch runs the body only when a tick is processed") {
        val scope = tickScope()
        val threads = CopyOnWriteArrayList<Thread>()
        scope.launch { threads += Thread.currentThread() }

        Thread.sleep(50)
        threads.size shouldBe 0

        schedulerManager.processTick()
        threads shouldContainExactly listOf(Thread.currentThread())
        scope.cancel()
    }

    test("withContext(Dispatchers.IO) resumes inside a later tick") {
        val scope = tickScope()
        val steps = CopyOnWriteArrayList<String>()
        val testThread = Thread.currentThread()
        scope.launch {
            withContext(Dispatchers.IO) { steps += "io" }
            steps += "tick:${Thread.currentThread() == testThread}"
        }

        schedulerManager.processTick()
        tickUntil { "io" in steps }
        Thread.sleep(50)
        steps shouldContainExactly listOf("io")

        schedulerManager.processTick()
        steps shouldContainExactly listOf("io", "tick:true")
        scope.cancel()
    }

    test("delay resumes on the first tick after it ends") {
        val scope = tickScope()
        val resumedAfter = AtomicLong(-1)
        val start = System.nanoTime()
        scope.launch {
            delay(100.milliseconds)
            resumedAfter.set((System.nanoTime() - start) / 1_000_000)
        }

        tickUntil { resumedAfter.get() >= 0 }
        resumedAfter.get() shouldBeGreaterThanOrEqual 100
        scope.cancel()
    }

    test("a failing child goes to the exception manager and its sibling keeps running") {
        val caught = CopyOnWriteArrayList<Throwable>()
        val previous = exceptionManager.exceptionHandler
        exceptionManager.setExceptionHandler { caught += it }
        val scope = tickScope()
        val siblingDone = AtomicBoolean(false)
        try {
            val failure = IllegalStateException("boom")
            scope.launch { throw failure }
            scope.launch {
                delay(60.milliseconds)
                siblingDone.set(true)
            }

            tickUntil { siblingDone.get() }
            caught shouldContainExactly listOf(failure)
        } finally {
            exceptionManager.exceptionHandler = previous
            scope.cancel()
        }
    }

    test("cancelling the scope before the tick skips the body") {
        val scope = tickScope()
        val ran = AtomicBoolean(false)
        scope.launch { ran.set(true) }

        scope.cancel()
        schedulerManager.processTick()
        schedulerManager.processTick()
        ran.get() shouldBe false
    }
})
