package org.kingdoms.commands.admin;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.kingdoms.admintools.AdminToolsLang;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.annotations.Cmd;
import org.kingdoms.commands.annotations.CmdParent;
import org.kingdoms.commands.annotations.CmdPerm;
import org.kingdoms.config.KingdomsConfig;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.Pair;
import org.kingdoms.locale.MessageHandler;
import org.kingdoms.locale.compiler.PlaceholderTranslationContext;
import org.kingdoms.locale.compiler.placeholders.Placeholder;
import org.kingdoms.locale.compiler.placeholders.PlaceholderParser;
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider;
import org.kingdoms.managers.network.socket.SocketHandler;
import org.kingdoms.managers.network.socket.SocketManager;
import org.kingdoms.server.permission.PermissionDefaultValue;
import org.kingdoms.utils.MathUtils;
import org.kingdoms.utils.compilers.ConditionalCompiler;
import org.kingdoms.utils.compilers.MathCompiler;
import org.kingdoms.utils.compilers.expressions.ConditionalExpression;
import org.kingdoms.utils.compilers.expressions.MathExpression;
import org.kingdoms.utils.string.Strings;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

@Cmd("condition")
@CmdParent(CommandAdmin.class)
@CmdPerm(PermissionDefaultValue.OP)
public class CommandAdminCondition extends KingdomsCommand {
    private static final String[] NAMES;

    public static final double DEFAULT_VAR_VAL = 10D;

    private static final List<Pair<String, String>> OPERATIONS = Arrays.asList(
            Pair.of("||", "Or"),
            Pair.of("&&", "And"),
            Pair.of("==", "Equals")
    );

    private static String getParams(String meth) {
        for (Method method : Math.class.getMethods()) {
            if (method.getName().equals(meth)) {
                return '(' + Strings.join(
                        Arrays.stream(method.getParameters())
                                .map(Parameter::getName)
                                .toArray(String[]::new), ",") + ')';
            }
        }

        return "()";
    }

    private static StringBuilder argsOf(int count) {
        StringBuilder args = new StringBuilder("(");

        for (int i = 0; i < count; i++) {
            args.append("arg").append(i);
            if (i < count - 1) args.append(',');
        }

        return args.append(')');
    }

    static {
        NAMES = new String[MathCompiler.getConstants().size() + MathCompiler.getFunctions().size()];

        int i = 0;
        for (String constant : MathCompiler.getConstants().keySet()) NAMES[i++] = constant;
        for (Map.Entry<String, MathCompiler.Function> func : MathCompiler.getFunctions().entrySet()) {
            String params = getParams(func.getKey());
            NAMES[i++] = func.getKey() + (params.equals("()") ? argsOf(func.getValue().getArgCount()) : params);
        }
    }

    static {
        if (KingdomsConfig.DEVELOPMENT_MODE.getBoolean()) {
            SocketManager.getMain().register(new SocketHandler(Namespace.kingdoms("EVAL_MATH"), true) {
                @Override
                public void onReceive(@NotNull SocketHandler.SocketSession session) {
                    MathExpression evaluated = MathCompiler.compile(session.getData().getAsString());
                    JsonObject result = new JsonObject();
                    result.addProperty("compiled", evaluated.toString());
                    result.addProperty("evaluated", evaluated.eval(x -> DEFAULT_VAR_VAL));
                    session.reply(result);
                }
            });
        }
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.requireArgs(1);
        String message = context.joinArgs();
        compile(context.getMessageReceiver(), message);
        return CommandResult.SUCCESS;
    }

    public static ConditionalExpression compile(CommandSender sender, String message) {
        ConditionalExpression result;
        try {
            result = ConditionalCompiler.compile(message).evaluate();
        } catch (Exception ex) {
            AdminToolsLang.COMMAND_ADMIN_CONDITION_FAILED.sendError(sender, new MessagePlaceholderProvider()
                    .raws("translated", message, "result", ex.getMessage()));

            if (sender instanceof Player) {
                MessageHandler.sendPluginMessage(sender, "&eWarning&8: &6Due to the nature of Minecraft's font typeface, " +
                        "some error pointers may point to inaccurate characters" +
                        "within the expression, to see the correct location, go to " +
                        "&2Options &7-> &2Language &7-> &2Force Unicode Font: ON\n" +
                        "&6or use this command from your console.");
            }

            // Matcher matcher = HEXADECIMAL.matcher(message);
            // if (matcher.find())
            //     MessageHandler.sendMessage(sender, "&eWarning&8: &6Hexadecimal numbers &2" + matcher.group() + "&2 &6are not supported.");
            //
            // matcher = SHORT_DECIMAL.matcher(message);
            // if (matcher.find())
            //     MessageHandler.sendMessage(sender, "&eWarning&8: &6Short decimal notations &2'" + matcher.group(1) + "&2' &6are not supported.");
            //
            // matcher = EXPLICIT_POS_SIGN.matcher(message);
            // if (matcher.find())
            //     MessageHandler.sendMessage(sender, "&eWarning&8: &6Explicit positive signs &2'" + matcher.group() + "&2' &6are not supported.");
            //
            // matcher = VARIABLE_BINDINGS.matcher(message);
            // if (matcher.find())
            //     MessageHandler.sendMessage(sender, "&eWarning&8: &6Variable bindings &2'" + matcher.group() + "&2' &6are not supported.");
            // return null;
            return null;
        }

        Map<String, Object> evaluatedPlaceholders = new HashMap<>(10);
        boolean evaluated = result.eval(x -> {
            if (sender instanceof Player) {
                Placeholder parsed = PlaceholderParser.parse(x);

                Player anotherPlayer = Bukkit.getOnlinePlayers().stream().filter(p -> p != sender).findFirst().orElse(null);
                MessagePlaceholderProvider context = new MessagePlaceholderProvider().withContext(sender).other(anotherPlayer);
                Object res = parsed.request(context);
                if (res instanceof PlaceholderTranslationContext) {
                    PlaceholderTranslationContext phCtx = (PlaceholderTranslationContext) res;
                    res = PlaceholderTranslationContext.unwrapContextualPlaceholder(
                            phCtx.getValue(),
                            context
                    );
                }

                if (res == null) {
                    MessageHandler.sendMessage(sender, "&cError&8: &6Failed to parse &e'" + x + "&e' " +
                            "&6placeholder, defaulting to &e10");
                    return DEFAULT_VAR_VAL;
                } else {
                    evaluatedPlaceholders.put(x, res);
                }
                return res;
            }

            Object global = new MessagePlaceholderProvider().withContext(sender).providePlaceholder(x);
            if (global != null) return MathUtils.expectDouble(x, global);

            MessageHandler.sendMessage(sender, "&eWarning&8: &6Variable &e'" + x + "' &6is unrecognized. " +
                    "Defaulting to &210.0 &6for evaluation to succeed.");
            return DEFAULT_VAR_VAL;
        });

        for (Map.Entry<String, Object> pl : evaluatedPlaceholders.entrySet()) {
            MessageHandler.sendMessage(sender, "&2" + pl.getKey() + " &8= &6" + Strings.toDetailedString(pl.getValue()));
        }

        AdminToolsLang.COMMAND_ADMIN_CONDITION_EVALUATED.sendMessage(sender,
                "translated", message, "result", evaluated, "object-code", result);

        return result;
    }

    @Override
    public @NonNull List<String> tabComplete(CommandTabContext context) {
        if (!context.hasArgs(1)) return Arrays.asList(NAMES);
        String arg = context.currentArg().toLowerCase(Locale.ENGLISH);
        if (context.isNumber(context.args.length - 1)) return context.emptyTab();

        if (OPERATIONS.stream().anyMatch(x -> x.getKey().equals(arg)))
            return OPERATIONS.stream().map(x -> x.getKey() + ' ' + x.getValue()).collect(Collectors.toList());

        return context.suggest(context.args.length - 1, NAMES);
    }
}
