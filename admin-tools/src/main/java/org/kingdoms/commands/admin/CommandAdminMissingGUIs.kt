package org.kingdoms.commands.admin

import org.bukkit.permissions.PermissionDefault
import org.kingdoms.admintools.AdminToolsLang
import org.kingdoms.commands.CommandContext
import org.kingdoms.commands.CommandResult
import org.kingdoms.commands.KingdomsCommand
import org.kingdoms.commands.KingdomsParentCommand
import org.kingdoms.commands.annotations.Cmd
import org.kingdoms.commands.annotations.CmdParent
import org.kingdoms.commands.annotations.CmdPerm
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.Language
import org.kingdoms.locale.SupportedLanguage
import org.kingdoms.managers.TempKingdomsFolder
import org.kingdoms.utils.string.tree.StringPathBuilder
import org.kingdoms.utils.string.tree.StringTree
import org.kingdoms.utils.string.tree.TreeStyle
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Locale

@Cmd("missingGUIs")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefault.OP)
class CommandAdminMissingGUIs : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        val output = TempKingdomsFolder.getOrCreateFolder("GUIS").resolve("Missing GUIs.txt")
        context.`var`("output", output)
        context.`var`("sanitized_output", CommandAdminOpenFile.sanitize(output))

        val files = arrayListOf<String>()
        val installed = SupportedLanguage.getInstalled()

        if (installed.isEmpty()) {
            context.sendError(AdminToolsLang.COMMAND_ADMIN_MISSINGGUIS_NO_LANGUAGE_PACKS_INSTALLED)
            return CommandResult.FAILED
        }

        for (lang in installed) {
            if (lang == Language.Companion.default) continue
            for (gui in Language.Companion.default.GUIs.keys) {
                val exists = lang.getGUI(gui) != null
                if (!exists) files.add("${lang.name.lowercase(Locale.ENGLISH)} (${lang.nativeName})/$gui")
            }
        }

        if (files.isEmpty()) {
            return context.fail(AdminToolsLang.COMMAND_ADMIN_MISSINGGUIS_NO_MISSING_GUIS)
        }

        val style = TreeStyle(StringTree.Companion.UTF_CHARACTER_SET, emptyMap()).apply {
            flatten = true
            indentation = 2
        }

        val pathBuilder = StringPathBuilder(files).toStringTree(style).print()
        Files.write(
            output, pathBuilder.lines.map { x -> x.toString() },
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        )

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_MISSINGGUIS_DONE)
        return CommandResult.SUCCESS
    }
}