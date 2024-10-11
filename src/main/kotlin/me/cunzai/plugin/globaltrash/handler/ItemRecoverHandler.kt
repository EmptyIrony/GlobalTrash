package me.cunzai.plugin.globaltrash.handler

import me.cunzai.plugin.globaltrash.database.addItem
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ItemDespawnEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.expansion.submitChain

object ItemRecoverHandler {

    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun e(e: ItemDespawnEvent) {
        submitChain {
            addItem(e.entity.itemStack)
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun e(e: EntityDeathEvent) {
        val item = e.entity
        if (item !is Item) return
        submitChain {
            addItem(item.itemStack)
        }
    }

}