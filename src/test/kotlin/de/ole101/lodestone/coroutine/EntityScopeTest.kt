package de.ole101.lodestone.coroutine

import de.ole101.lodestone.schedulerManager
import de.ole101.lodestone.testing.TestServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.isActive
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import java.util.concurrent.atomic.AtomicBoolean

class EntityScopeTest : FunSpec({

    beforeSpec { TestServer.init() }

    test("coroutineScope is created once per entity") {
        val entity = Entity(EntityType.ZOMBIE)

        entity.coroutineScope shouldBeSameInstanceAs entity.coroutineScope
    }

    test("launch runs the body on the entity's scheduler, not the global one") {
        val entity = Entity(EntityType.ZOMBIE)
        val ran = AtomicBoolean(false)
        entity.launch { ran.set(true) }

        schedulerManager.processTick()
        ran.get() shouldBe false

        entity.scheduler().processTick()
        ran.get() shouldBe true
        entity.remove()
    }

    test("removing the entity cancels its scope") {
        val entity = Entity(EntityType.ZOMBIE)
        val ran = AtomicBoolean(false)
        entity.launch { ran.set(true) }

        entity.remove()
        entity.scheduler().processTick()

        entity.coroutineScope.isActive.shouldBeFalse()
        ran.get() shouldBe false
    }

    test("the scope of an already removed entity starts cancelled") {
        val entity = Entity(EntityType.ZOMBIE)
        entity.remove()

        entity.coroutineScope.isActive.shouldBeFalse()
    }
})
