package de.ole101.lodestone.reactive

import de.ole101.lodestone.schedulerManager
import net.minestom.server.MinecraftServer
import net.minestom.server.thread.TickSchedulerThread
import net.minestom.server.thread.TickThread

public fun interface FlushScheduler {
    /** Schedules [flush] to run later, never inside this call. */
    public fun schedule(flush: Runnable)

    /**
     * Checks each write reaching this scheduler's effects and each enqueue. Throw to reject a write without
     * changing state or graph. Does nothing by default.
     */
    public fun checkWrite() {}

    public companion object {
        /**
         * Flushes at tick end, after input handling and before outgoing packets.
         *
         * After server start, writes reaching its effects outside tick scheduler or tick threads throw
         * [IllegalStateException]. States have no locks: with `minestom.dispatcher-threads` above 1,
         * do not share state between players in different chunks.
         */
        public val EndOfTick: FlushScheduler = object : FlushScheduler {
            override fun schedule(flush: Runnable) {
                schedulerManager.scheduleEndOfTick(flush)
            }

            override fun checkWrite() {
                val thread = Thread.currentThread()
                check(!MinecraftServer.isStarted() || thread is TickSchedulerThread || thread is TickThread) {
                    "A state write reached an effect on thread ${thread.name}. Write states only on the tick thread, " +
                            "e.g. from an event listener or the Minestom coroutine dispatcher."
                }
            }
        }
    }
}
