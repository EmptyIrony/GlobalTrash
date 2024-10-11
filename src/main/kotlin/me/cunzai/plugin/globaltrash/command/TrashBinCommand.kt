package me.cunzai.plugin.globaltrash.command

import me.cunzai.plugin.globaltrash.config.ConfigLoader
import me.cunzai.plugin.globaltrash.ui.GlobalTrashUI
import me.cunzai.plugin.globaltrash.ui.TrashUI
import me.cunzai.plugin.globaltrash.ui.uiConfig
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.*

@CommandHeader(name = "trashBin", aliases = ["tb", "trash"], permissionDefault = PermissionDefault.TRUE)
object TrashBinCommand {

    @CommandBody(permissionDefault = PermissionDefault.TRUE)
    val main = mainCommand {
        execute<Player> { sender, _, _ ->
            TrashUI.open(sender)
        }
    }

    @CommandBody(permissionDefault = PermissionDefault.TRUE)
    val global = subCommand {
        execute<Player> { sender, _, _ ->
            GlobalTrashUI.open(sender)
        }
    }

    @CommandBody(permission = "trans.admin")
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            ConfigLoader.config.reload()
            uiConfig.reload()

            ConfigLoader.i()
            sender.sendMessage("ok")
        }
    }

}