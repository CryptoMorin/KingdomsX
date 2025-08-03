package org.kingdoms.commands.admin;

import org.bukkit.Material;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.*;
import org.kingdoms.commands.annotations.Cmd;
import org.kingdoms.commands.annotations.CmdParent;
import org.kingdoms.commands.annotations.CmdPerm;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.abstraction.data.KingdomItemBuilder;
import org.kingdoms.constants.land.location.SimpleLocation;
import org.kingdoms.constants.land.turrets.Turret;
import org.kingdoms.constants.land.turrets.TurretRegistry;
import org.kingdoms.constants.land.turrets.TurretStyle;
import org.kingdoms.locale.KingdomsLang;
import org.kingdoms.main.Kingdoms;

import java.util.List;
import java.util.stream.Collectors;

@Cmd("turret")
@CmdParent(CommandAdmin.class)
@CmdPerm(PermissionDefault.OP)
public class CommandAdminTurret extends KingdomsCommand {
    @Override
    public CommandResult execute(CommandContext context) {
        if (!context.hasArgs(2) || !context.argIsAny(0, "change", "remove") || (context.argIsAny(0, "change") && !context.hasArgs(3))) {
            throw context.wrongUsage();
        }

        TurretStyle turret = TurretRegistry.get().getStyles().get(context.arg(1));
        if (turret == null) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_TURRET_UNKNOWN_STYLE, "style", context.arg(1));
            return CommandResult.FAILED;
        }

        boolean change = context.argIsAny(0, "change");
        TurretStyle changeToTurret = null;
        if (change) {
            changeToTurret = TurretRegistry.get().getStyles().get(context.arg(2));
            if (changeToTurret == null) {
                context.sendError(AdminToolsLang.COMMAND_ADMIN_TURRET_UNKNOWN_STYLE, "style", context.arg(2));
                return CommandResult.FAILED;
            }
        }

        int amount = 0;
        for (Land land : Kingdoms.get().getDataCenter().getLandManager().peekAllData()) {
            for (Turret landTurret : land.getTurrets().values()) {
                if (!landTurret.getStyle().getName().equals(turret.getName())) continue;

                SimpleLocation location = landTurret.getOrigin();
                location.getBlock().setType(Material.AIR);
                landTurret.remove();

                if (change) {
                    Turret newTurret = changeToTurret.getType().build(new KingdomItemBuilder<>(changeToTurret, location));
                    newTurret.setLevel(1);
                    land.unsafeGetTurrets().put(location.toBlockVector(), newTurret);
                }

                amount++;
            }
        }

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_TURRET_SUCCESS, "amount", amount);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NonNull List<String> tabComplete(@NonNull CommandTabContext context) {
        if (context.isAtArg(0)) return context.tabComplete("change", "remove");
        else if (context.isAtArg(1)) return context.tabComplete(TurretRegistry.get().getStyles().keySet());
        else if (context.isAtArg(2) && context.args[0].equals("change"))
            return TurretRegistry.get().getStyles().keySet().stream().filter(x -> x.equals(context.args[1])).collect(Collectors.toList());
        return context.emptyTab();
    }
}
