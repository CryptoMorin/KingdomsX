package org.kingdoms.constants.economy

import com.google.common.util.concurrent.AtomicDouble
import org.kingdoms.constants.DataStringRepresentation
import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.constants.namespace.Namespaced
import org.kingdoms.constants.namespace.NamespacedRegistry
import org.kingdoms.utils.internal.numbers.AnyNumber

object EconomyRegistry : NamespacedRegistry<Economy>()

interface Economy : Namespaced {
    fun createBalance(value: Double): Balance = AbstractBalance(this, AtomicDouble(value))
    fun isAvailable(): Boolean
    fun allowsNegativeBalance(): Boolean
}

abstract class AbstractEconomy(private val ns: Namespace) : Economy {
    init {
        EconomyRegistry.register(this)
    }

    override fun getNamespace(): Namespace = ns
    override fun allowsNegativeBalance(): Boolean = false
}

interface BalanceListener : Balance

interface Balance : Comparable<Number>, DataStringRepresentation {
    fun get(): Double
    fun set(value: Number): Double
    fun getEconomy(): Economy

    fun transfer(amount: Number, to: Balance) {
        subtract(amount)
        to.add(amount)
    }
    fun has(amount: Number) = get() >= amount.toDouble()
    fun isEmpty() = !has(1)
    fun add(amount: Number): Double = set(get() + amount.toDouble())

    /**
     * Removes [amount] from this account.
     * @return the final amount remaining in this account of subtraction.
     */
    fun subtract(amount: Number): Double = set(get() - amount.toDouble())
    override fun compareTo(other: Number): Int = get().compareTo(other.toDouble())

    override fun asDataString(): String = get().toString()

    // @formatter:off
    // Increments and decrements not implemented because it's practically useless.
    @JvmSynthetic operator fun plus       (amt: Number) = get() + amt.toDouble()
    @JvmSynthetic operator fun minus      (amt: Number) = get() - amt.toDouble()
    @JvmSynthetic operator fun times      (amt: Number) = get() * amt.toDouble()
    @JvmSynthetic operator fun div        (amt: Number) = get() / amt.toDouble()
    @JvmSynthetic operator fun rem        (amt: Number) = get() % amt.toDouble()

    @JvmSynthetic operator fun unaryPlus () = +get()
    @JvmSynthetic operator fun unaryMinus() = -get()

    @JvmSynthetic operator fun plusAssign (amt: Number) { add(amt)         }
    @JvmSynthetic operator fun minusAssign(amt: Number) { subtract(amt)    }
    @JvmSynthetic operator fun timesAssign(amt: Number) { set(get() * amt.toDouble()) }
    @JvmSynthetic operator fun divAssign  (amt: Number) { set(get() / amt.toDouble()) }
    @JvmSynthetic operator fun remAssign  (amt: Number) { set(get() % amt.toDouble()) }
    // @formatter:on
}

class AbstractBalance(private var economy: Economy, private var value: AtomicDouble) : Balance {
    override fun get(): Double = value.get()
    override fun set(value: Number): Double {
        this.value.set(value.toDouble())
        return this.value.get()
    }

    override fun getEconomy(): Economy = economy
}