package org.kingdoms.services

import org.bukkit.OfflinePlayer
import org.kingdoms.constants.economy.AbstractEconomy
import org.kingdoms.constants.economy.Balance
import org.kingdoms.constants.namespace.Namespace

object VaultEconomy : AbstractEconomy(Namespace("VAULT", "MAIN")) {
    override fun isAvailable() = ServiceVault.isAvailable(ServiceVault.Component.ECO)
}

class VaultBalance(private val account: OfflinePlayer) : Balance {
    override fun get(): Double = ServiceVault.getMoney(account)
    override fun set(value: Number): Double {
        val bal = get()
        val dbVal = value.toDouble()
        if (bal > dbVal) {
            ServiceVault.withdraw(account, bal - dbVal)
        } else if (bal < dbVal) {
            ServiceVault.deposit(account, dbVal - bal)
        }
        return dbVal
    }

    override fun getEconomy() = VaultEconomy
}