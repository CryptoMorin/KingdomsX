package org.kingdoms.utils.internal.enumeration

object EnumsKt {
    inline fun <reified T : Enum<T>> nullableValueOf(name: String?): T? =
        name?.runCatching { enumValueOf<T>(this) }?.getOrNull()
}