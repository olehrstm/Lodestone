package de.ole101.lodestone.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType

class NoTickingEntityTest : FunSpec({

    test("has physics disabled") {
        NoTickingEntity(EntityType.ARMOR_STAND).hasPhysics().shouldBeFalse()
    }

    test("has gravity disabled") {
        NoTickingEntity(EntityType.ARMOR_STAND).hasNoGravity().shouldBeTrue()
    }

    test("ticking does nothing") {
        val entity = NoTickingEntity(EntityType.ARMOR_STAND)
        val before = entity.position

        repeat(20) { entity.tick(it.toLong()) }

        entity.position shouldBe before
        entity.aliveTicks shouldBe 0
    }

    test("stays at rest, since no tick applies gravity") {
        val entity = NoTickingEntity(EntityType.ARMOR_STAND)

        repeat(20) { entity.tick(it.toLong()) }

        entity.velocity shouldBe Vec.ZERO
    }
})
