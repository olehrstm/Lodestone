package de.ole101.lodestone.entity

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

public open class NoTickingEntity(type: EntityType) : Entity(type) {

    init {
        this.hasPhysics = false
        setNoGravity(true)
    }

    override fun tick(time: Long) {
        // no-op
    }
}
