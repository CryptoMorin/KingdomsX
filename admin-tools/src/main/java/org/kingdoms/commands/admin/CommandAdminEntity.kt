package org.kingdoms.commands.admin

import com.cryptomorin.xseries.particles.ParticleDisplay
import com.cryptomorin.xseries.particles.Particles
import com.cryptomorin.xseries.particles.XParticle
import com.cryptomorin.xseries.reflection.XReflection
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.TextDisplay
import org.bukkit.entity.Zombie
import org.bukkit.permissions.PermissionDefault
import org.bukkit.scheduler.BukkitRunnable
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.CommandTabContext
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.data.Pair
import org.kingdoms.locale.MessageHandler
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.LocationUtils
import org.kingdoms.utils.internal.reflection.Reflect
import org.kingdoms.utils.time.TimeFormatter
import java.util.concurrent.atomic.AtomicInteger

@Cmd("entity")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminEntity : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.assertPlayer()
        val radius: Double = if (context.hasArgs(1)) context.getDouble(0) else 10.0
        val stay = if (context.hasArgs(2)) context.getInt(1) else 1
        val showDetails = context.hasArgs(3) && context.arg(2).equals("true", ignoreCase = true)
        context.`var`("radius", radius)
        context.`var`("stay", stay)

        if (radius <= 1) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_ENTITY_NEGATIVE_RADIUS)
        }
        if (stay <= 0) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_ENTITY_NEGATIVE_STAY)
        }

        val total = AtomicInteger()
        val player = context.senderAsPlayer()
        object : BukkitRunnable() {
            var times = stay
            var looping = false
            override fun run() {
                for (entity in player.getNearbyEntities(radius, radius, radius)) {
                    if (!looping) {
                        val loc = entity.location
                        var metadata: String = if (!entity.customName.isNullOrEmpty()) {
                            entity.customName!!
                        } else if (Reflect.classExists("org.bukkit.entity.TextDisplay") && entity is TextDisplay) {
                            entity.text ?: ""
                        } else {
                            ""
                        }

                        metadata = metadata.replace("\n", "\\n")
                        if (metadata.length > 30) {
                            metadata = metadata.substring(0, 30) + "..."
                        }

                        MessageHandler.sendMessage(
                            player, "&8⚫ &2${entity.type}${if (metadata.isEmpty()) "" else " &8(&3$metadata&8)"}" +
                                    " &6hover:{${LocationUtils.toReadableLoc(loc)};&9Click to teleport;/minecraft:tp ${loc.x} ${loc.y} ${loc.z}}"
                                    + if (showDetails) details(entity) else ""
                        )
                    }
                    ParticleDisplay.of(XParticle.FLAME).withLocation(entity.location).withCount(20).offset(0.3).spawn()
                    Particles.line(player.location, entity.location, 0.5, ParticleDisplay.of(XParticle.WITCH))
                    if (!looping) total.incrementAndGet()
                }
                if (!looping) {
                    looping = true
                    context.`var`("total", total)
                    context.sendMessage(AdminToolsLang.COMMAND_ADMIN_ENTITY_FOUND)
                }
                if (--times <= 0) cancel()
            }
        }.runTaskTimer(plugin, 0L, 5L)

        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): List<String> {
        if (context.isAtArg(0)) return context.tabComplete("[radius]")
        if (context.isAtArg(1)) return context.tabComplete("[stay]")
        return if (context.isAtArg(2)) context.tabComplete("[show details?]") else context.emptyTab()
    }

    companion object {
        private fun details(entity: Entity): String {
            val details: MutableList<Pair<String, Any>> = ArrayList()
            val flags: MutableList<String> = ArrayList()

            if (entity.isFrozen) flags.add("Frozen")
            if (entity.isInvulnerable) flags.add("Invulnerable")
            if (entity.isSilent) flags.add("Silent")
            if (entity is LivingEntity) {
                details.add(Pair.of("No Damage Ticks", entity.noDamageTicks))
                if (entity.isCollidable) flags.add("Collidable")
                if (entity.isInvisible) flags.add("Invisible")
                if (XReflection.supports(13) && entity is Zombie) {
                    val zom = entity
                    if (zom.isConverting) details.add(
                        Pair.of(
                            "Convertion Time",
                            TimeFormatter.of(zom.conversionTime.toLong())
                        )
                    )
                }
            }
            val detailsStr = StringBuilder()
            if (flags.isNotEmpty()) {
                detailsStr.append("\n   &8| &9")
                detailsStr.append(java.lang.String.join("&7, &9", flags))
            }
            for ((key, value) in details) {
                if (value is Number && value.toDouble() == 0.0) continue
                detailsStr.append("\n   &8| &2").append(key).append("&7: &9").append(value)
            }
            return detailsStr.toString()
        }
    }
}