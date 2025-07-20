package org.kingdoms.services.maps;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.commands.*;
import org.kingdoms.constants.land.Land;
import org.kingdoms.locale.messenger.StaticMessenger;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.utils.internal.numbers.AnyNumber;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class CommandAdminDynmap extends KingdomsCommand {
    public CommandAdminDynmap(KingdomsParentCommand parent) {
        super("dynmap", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.requireArgs(1);
        if (!MapViewerAddon.isAvailable()) {
            return context.fail(MapViewersLang.COMMAND_ADMIN_DYNMAP_NOT_AVAILABLE);
        }

        // TODO fix thess
        CommandSender sender = context.getMessageReceiver();
        String option = context.arg(0).toLowerCase(Locale.ENGLISH);

        if (option.equals("zoom")) {
            AnyNumber min = context.getNumber(1, true, false, new StaticMessenger("min"));
            AnyNumber max = context.getNumber(2, true, false, new StaticMessenger("max"));
            if (min == null || max == null) return CommandResult.FAILED;

            // LandMarkerSettings.zoomMin = min.getValue().intValue();
            // LandMarkerSettings.zoomMax = max.getValue().intValue();
            // context.sendMessage(new StaticMessenger("&2Forcing Zoom Values&8: &6"
            //         + LandMarkerSettings.zoomMin + "&7, &6" + LandMarkerSettings.zoomMax));
            ServiceMap.fullRender();
            ServiceMap.update();
            return CommandResult.SUCCESS;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (option.equals("fullrender")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    MapViewersLang.COMMAND_ADMIN_DYNMAP_RENDERING.sendMessage(sender);
                    int total = ServiceMap.fullRender();
                    ServiceMap.update();
                    MapViewersLang.COMMAND_ADMIN_DYNMAP_RENDERED.sendMessage(sender, "lands", total);
                });
            } else if (option.equals("remove") || option.equals("delete")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    MapViewersLang.COMMAND_ADMIN_DYNMAP_RENDERING.sendMessage(sender);
                    Collection<Land> lands = Kingdoms.get().getDataCenter().getLandManager().getLoadedData();
                    ServiceMap.removeEverything();
                    ServiceMap.update();
                    MapViewersLang.COMMAND_ADMIN_DYNMAP_REMOVED.sendMessage(sender, "lands", lands.size());
                });
            } else {
                MapViewersLang.COMMAND_ADMIN_DYNMAP_USAGE.sendMessage(sender);
            }
        });

        return CommandResult.SUCCESS;
    }

    @Override
    public @NotNull List<String> tabComplete(@NonNull CommandTabContext context) {
        return Arrays.asList("fullrender", "remove");
    }
}
