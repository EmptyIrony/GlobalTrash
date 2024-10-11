package me.cunzai.plugin.globaltrash.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import java.util.HashMap
import java.util.UUID

object TrashUI {

    val cache = HashMap<UUID, TrashData>()

    fun open(player: Player) {
        val trashData = cache.remove(player.uniqueId) ?: return
        player.openMenu<PageableChest<ItemStack>>("1") {
//
        }
    }

    data class TrashData(
        val items: MutableList<Pair<ItemStack, Long>> = ArrayList()
    )

}