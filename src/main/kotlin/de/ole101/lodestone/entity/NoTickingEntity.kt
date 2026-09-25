package de.ole101.lodestone.entity

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

/** Entity of [type] that does literally nothing on each tick. */
public open class NoTickingEntity(type: EntityType) : Entity(type) {

    init {
        this.hasPhysics = false
        setNoGravity(true)
    }

    override fun tick(time: Long) {
        // no-op
    }
}
