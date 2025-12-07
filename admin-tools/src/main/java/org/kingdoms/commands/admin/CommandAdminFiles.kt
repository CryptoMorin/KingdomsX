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
import org.kingdoms.main.Kingdoms
import org.kingdoms.server.permission.PermissionDefaultValue
import org.kingdoms.utils.internal.numbers.Numbers
import org.kingdoms.utils.string.tree.StringPathBuilder
import org.kingdoms.utils.string.tree.StringTree
import org.kingdoms.utils.string.tree.TreeStyle
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize

@Cmd("files")
@CmdParent(CommandAdmin::class)
@CmdPerm(PermissionDefaultValue.OP)
class CommandAdminFiles : KingdomsCommand() {
    override fun execute(context: CommandContext): CommandResult {
        val output = Kingdoms.getFolder().resolve("files.txt")
        context.`var`("output", output)
        context.`var`("sanitized_output", CommandAdminOpenFile.sanitize(output))

        val files = arrayListOf<String>()
        Files.walk(Kingdoms.getFolder()).forEach {
            val size =
                Numbers.toFancyNumber(it.fileSize().toDouble())
            val stringPath = Kingdoms.getFolder().relativize(it).toString().replace('\\', '/')
            files.add("$stringPath ($size)")
        }

        val style = TreeStyle(StringTree.UTF_CHARACTER_SET, emptyMap()).apply {
            flatten = true
            indentation = 2
        }

        val pathBuilder = StringPathBuilder(files).toStringTree(style).print()
        Files.write(
            output, pathBuilder.lines.map { x -> x.toString() },
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        )

        context.sendMessage(AdminToolsLang.COMMAND_ADMIN_FILES_DONE)
        return CommandResult.SUCCESS
    }
}