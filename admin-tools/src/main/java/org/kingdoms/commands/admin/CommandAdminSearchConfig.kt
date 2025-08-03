package org.kingdoms.commands.admin

import org.bukkit.ChatColor
import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.*
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.config.managers.ConfigManager
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.LangConstants
import org.kingdoms.locale.compiler.MessageCompiler
import org.kingdoms.locale.compiler.MessageCompilerSettings
import org.kingdoms.locale.compiler.MessageObject
import org.kingdoms.locale.messenger.StaticMessenger
import org.kingdoms.locale.placeholders.context.MessagePlaceholderProvider
import org.kingdoms.locale.provider.MessageProvider
import org.kingdoms.main.KLogger
import org.kingdoms.main.Kingdoms
import org.kingdoms.utils.config.CustomConfigValidators
import org.kingdoms.utils.config.adapters.YamlContainer
import org.kingdoms.utils.debugging.KingdomsDebug
import org.kingdoms.utils.internal.functional.SecondarySupplier
import org.snakeyaml.comments.CommentType
import org.snakeyaml.common.ScalarStyle
import org.snakeyaml.nodes.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.collections.iterator

@Cmd("searchConfig")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
class CommandAdminSearchConfig : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        context.requireArgs(1)
        Kingdoms.taskScheduler().async().execute { -> executeAsync(context) }
        return CommandResult.SUCCESS
    }

    override fun tabComplete(context: CommandTabContext): MutableList<String> {
        return context
            .completeNext(SecondarySupplier { listOf(context.tab("regex")) })
            .then(SecondarySupplier { listOf(context.tab("file-regex")) })
            .build()
    }

    private fun executeAsync(context: CommandContext) {
        val contentPattern: Pattern = try {
            Pattern.compile(context.arg(0))
        } catch (e: PatternSyntaxException) {
            KLogger.debug(KingdomsDebug.`INVALID$REGEX`, "Invalid pattern for /k admin searchConfig: " + e.description)
            wordListToRegex(context.args)
        }

        val filePattern: Pattern? = if (context.hasArgs(2)) {
            try {
                Pattern.compile(context.arg(1))
            } catch (e: PatternSyntaxException) {
                KLogger.debug(
                    KingdomsDebug.`INVALID$REGEX`,
                    "Invalid pattern for /k admin searchConfig: " + e.description
                )
                wordListToRegex(context.args)
            }
        } else null

        context.`var`("pattern", contentPattern.pattern())
        context.`var`("file-pattern", filePattern?.pattern() ?: KingdomsLang.NONE)

        val searcher = ConfigSearcher(ConfigManager.getMainConfigs(), filePattern, contentPattern)
        searcher.find()
        val result = searcher.result

        if (result.isEmpty()) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_SEARCHCONFIG_NO_RESULTS)
        } else {
            fun String.replaceMatched(pattern: Pattern, next: String): String {
                return pattern.toRegex()
                    .replace(this) { x: MatchResult -> // A console bug requires a space after &r or the underline wont reset...
                        "${"{#FF0000}".compileColors()}${ChatColor.UNDERLINE}${x.value}${ChatColor.RESET}${if (context.isPlayer()) "" else " "}$next"
                    }
            }

            val lineNumberPadding = searcher.largestLineNumber.toString().length
            fun lineNumber(line: Int) = "&7" + line.toString().padEnd(lineNumberPadding) + " | "

            context.sendMessage(AdminToolsLang.COMMAND_ADMIN_SEARCHCONFIG_FOUND)
            for (file in result) {
                val (adapter, result) = file

                context.sendMessage(AdminToolsLang.COMMAND_ADMIN_SEARCHCONFIG_ENTRY, "path", adapter.file?.path)

                for (entry in result) {
                    val line = entry.getLine()

                    entry.entryKey.comments?.let {
                        val commentColor = "{#0e7822}"
                        for ((index, blockComment) in it.dropWhile { x -> x.commentType == CommentType.BLANK_LINE }
                            .withIndex()) {
                            val startLine = line - it.size + index
                            StaticMessenger(lineNumber(startLine) + "$commentColor#%comment%").sendMessage(
                                context.getMessageReceiver(),
                                MessagePlaceholderProvider()
                                    .parse(
                                        "comment",
                                        blockComment.value.replaceMatched(contentPattern, commentColor.compileColors())
                                    )
                            )
                        }
                    }

                    fun ScalarNode.toSimpleString(): String {
                        val value = this.value
                        return when (this.scalarStyle) {
                            ScalarStyle.SINGLE_QUOTED -> "'${value}'"
                            ScalarStyle.DOUBLE_QUOTED -> "\"${value}\""
                            ScalarStyle.FOLDED -> ">\n${value}"
                            ScalarStyle.LITERAL -> "|\n${value}"
                            else -> value
                        }
                    }

                    fun nodeToString(node: Node): Pair<String, String> = when (node) {
                        is MappingNode -> Pair("${LangConstants.P_COLOR}", "{...}")
                        is SequenceNode -> Pair(
                            "${LangConstants.SEP}", "&8[ ${
                                node.value.joinToString("&7, ") { ele -> nodeToString(ele).let { it.first + it.second } }
                            } &8]".compileColors()
                        )

                        is ScalarNode -> Pair(
                            when (node.tag) {
                                Tag.BOOL -> if (node.parsed as Boolean) {
                                    "&2"
                                } else {
                                    "&c"
                                }

                                Tag.INT -> "&9"
                                Tag.NULL -> "&4"
                                Tag.STR -> "&6"
                                CustomConfigValidators.MATH -> "&5"
                                CustomConfigValidators.PERIOD -> "{#28869e}"
                                else -> "{#929e28}"
                            }, node.toSimpleString()
                        )

                        else -> Pair("${LangConstants.S_COLOR}", node.toString())
                    }

                    val value = nodeToString(entry.entryValue)
                    val keyColor = "{#0ca874}"
                    StaticMessenger(
                        (lineNumber(line) + keyColor + (entry.entryKey as ScalarNode).value.replaceMatched(
                            contentPattern,
                            keyColor.compileColors()
                        )
                                + "${LangConstants.SEP}: %value%").compileColorsToProvider()
                    ).sendMessage(
                        context.getMessageReceiver(), MessagePlaceholderProvider()
                            .raw(
                                "value",
                                value.first.compileColors() + value.second.replaceMatched(
                                    contentPattern,
                                    value.first.compileColors()
                                )
                            )
                    )
                    context.sendMessage(StaticMessenger("&7-------------------------------------"))
                }

                context.sendMessage(StaticMessenger("&8======================================================"))
            }
        }
    }

    private fun String.compileColorsToObj(): MessageObject =
        MessageCompiler(this, MessageCompilerSettings.none().translatePlaceholders().colorize()).compileObject()

    private fun String.compileColorsToProvider(): MessageProvider =
        MessageProvider(this.compileColorsToObj())

    private fun String.compileColors(): String = this.compileColorsToObj().buildPlain(MessagePlaceholderProvider())

    private fun wordListToRegex(keywords: Array<String>): Pattern =
        Pattern.compile(keywords.joinToString("|") { x -> Pattern.quote(x) })

    inner class ConfigSearcher(
        private val configs: Collection<YamlContainer>,
        filesPattern: Pattern?,
        pattern: Pattern,
    ) {
        private val filesPattern: Regex? = filesPattern?.toRegex()
        private val pattern: Regex = pattern.toRegex()
        val result: MutableList<Pair<YamlContainer, MutableList<ConfigSearchResult>>> = ArrayList()
        var largestLineNumber: Int = 0

        fun find() {
            val searchConfigs = if (filesPattern != null) {
                configs.filter {
                    val name =
                        Kingdoms.getFolder().toAbsolutePath().relativize(it.file!!.toPath().toAbsolutePath()).toString()
                    name.contains(filesPattern)
                }
            } else {
                configs
            }

            for (adapter in searchConfigs) {
                val innerResults: MutableList<ConfigSearchResult> = ArrayList()
                findInnerSections(adapter.config.node, innerResults)
                if (innerResults.isNotEmpty()) result.add(Pair(adapter, innerResults))
            }
        }

        private fun searchNode(node: Node): Boolean = when (node) {
            is ScalarNode -> node.value.contains(pattern)
            is SequenceNode -> node.value.any { ele -> searchNode(ele) }
            else -> false
        }

        private fun findInnerSections(section: MappingNode, results: MutableList<ConfigSearchResult>) {
            // Fucking Kotlin, support nested deconstruction already...
            for ((key: String, pair: NodePair) in section.pairs) {
                val node = pair.value
                val matches: Boolean = (key.contains(pattern))
                        || searchNode(node)
                        || (node.comments?.let { it.any { x -> x.value.contains(pattern) } } ?: false)

                if (matches) {
                    largestLineNumber = largestLineNumber.coerceAtLeast(node.startMark.line)
                    results.add(ConfigSearchResult(pair.key, node))
                }
                if (node is MappingNode) findInnerSections(node, results)
            }
        }
    }

    inner class ConfigSearchResult(val entryKey: Node, val entryValue: Node) {
        fun getLine(): Int = entryValue.startMark.line
    }
}