package org.kingdoms.commands.admin.debugging;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kingdoms.commands.*;
import org.kingdoms.ide.Bookmark;
import org.kingdoms.ide.BookmarkType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

@Bookmark(BookmarkType.EXPERIMENTAL)
@Bookmark(BookmarkType.EXPLOITABLE)
public class CommandAdminRepl extends KingdomsCommand implements Listener {
    private static final Object JSHELL;
    private static final Set<UUID> TOGGLED = new HashSet<>();

    static {
        Object jshell = null;
        try {
            Class<?> jshellClass = Class.forName("jdk.jshell.JShell");
            jshell = jshellClass.getMethod("create").invoke(null);
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
        JSHELL = jshell;
    }

    public CommandAdminRepl(KingdomsParentCommand parent) {
        super("repl", parent);
    }

    private static String tinyEval(Player player, String str) {
        int index = str.indexOf(' ');
        String fn = str.substring(0, index);
        String[] params = str.substring(index + 1).split(" ");
        Object[] parameters = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            try {
                parameters[i] = Integer.parseInt(param);
                continue;
            } catch (NumberFormatException ex) {
                if (param.equals("true") || param.equals("false")) {
                    parameters[i] = param.equals("true");
                    continue;
                }
                if (param.equals("nil")) {
                    parameters[i] = null;
                    continue;
                }
            }
            throw new IllegalArgumentException("unknown type " + param);
        }

        for (Method meth : player.getClass().getMethods()) {
            if (meth.getName().equals(fn)) {
                try {
                    return String.valueOf(meth.invoke(player, parameters));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        throw new IllegalArgumentException("Unknown function: " + fn);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Player player = context.senderAsPlayer();
        if (TOGGLED.add(player.getUniqueId())) {
            player.sendMessage("Activated;");
        } else {
            TOGGLED.remove(player.getUniqueId());
            player.sendMessage("Deactivated;");
        }
        return CommandResult.SUCCESS;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onREPL(AsyncPlayerChatEvent event) {
        if (!TOGGLED.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);

        try {
            event.getPlayer().sendMessage("λ> " + event.getMessage());
            String response = tinyEval(event.getPlayer(), event.getMessage());
            event.getPlayer().sendMessage("➜ " + response);

            List<?> snips = (List<?>) Class.forName("jdk.jshell.JShell").getMethod("eval", String.class).invoke(JSHELL, event.getMessage());
            Class<?> jshellClazz = Class.forName("jdk.jshell.SnippetEvent");
            for (Object snip : snips) {
                Enum<?> status = (Enum<?>) jshellClazz.getMethod("status").invoke(null);
                String res = (String) jshellClazz.getMethod("value").invoke(snip);
                String finalRes = response + " (" + res + ')';

                if (status.name().equals("REJECTED")) {
                    event.getPlayer().sendMessage("!➜ " + finalRes);
                } else {
                    event.getPlayer().sendMessage("➜ " + finalRes);
                }
            }
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0))
            return context.filter(Arrays.stream(Player.class.getMethods()).map(Method::getName), 0).collect(Collectors.toList());
        Optional<Method> meth = Arrays.stream(Player.class.getMethods()).filter(x -> x.getName().equals(context.arg(0))).findFirst();
        if (!meth.isPresent()) return context.tabComplete("Unknown method");
        Parameter[] params = meth.get().getParameters();
        if (params.length < context.args.length) return context.tabComplete("Too many arguments");
        Parameter param = params[context.args.length - 1];
        return context.tabComplete(param.getName() + ": " + param.getType().getName());
    }
}