package de.ole101.lodestone

/**
 * Marks every Lodestone DSL scope, so a nested block cannot call members of an outer scope by accident.
 */
@DslMarker
public annotation class LodestoneDsl
