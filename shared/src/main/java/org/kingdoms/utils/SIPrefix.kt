package org.kingdoms.utils

/**
 * https://www.nist.gov/pml/owm/metric-si-prefixes
 */
enum class SIPrefix(val modifier: Long, val prefixSymbol: String) {
    KILO(1_000, "k"),
    MEGA(1_000_000, "M"),
    GIGA(1_000_000_000, "G"),
    TERA(1_000_000_000_000, "T")
    ;

    fun of(value: Long): Long = value * modifier

    companion object {
        @JvmStatic fun bestFor(value: Long): SIPrefix? = when {
            value < KILO.modifier -> null
            value < MEGA.modifier -> KILO
            value < GIGA.modifier -> MEGA
            value < TERA.modifier -> GIGA
            else -> TERA
        }
    }
}