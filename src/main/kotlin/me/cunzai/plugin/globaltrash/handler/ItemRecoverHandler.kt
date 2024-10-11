package me.cunzai.plugin.globaltrash.handler

import me.cunzai.plugin.globaltrash.database.addItem
import me.cunzai.plugin.globaltrash.database.getWriteLock
import me.cunzai.plugin.globaltrash.nms.NMS
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.expansion.submitChain
import taboolib.module.nms.nmsProxy

object ItemRecoverHandler {

    @SubscribeEvent(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun e(e: ItemDespawnEvent) {
        submitChain {
            kotlin.runCatching {
                getWriteLock {
                    submitChain {
                        addItem(e.entity.itemStack)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun e(e: EntityDamageEvent) {
        val item = e.entity
        if (item !is Item) return

        val nms = nmsProxy<NMS>()
        val willDead = nms.willDead(item, e.finalDamage.toInt())

        if (willDead) {
            submitChain {
                kotlin.runCatching {
                    getWriteLock {
                        submitChain {
                            addItem(item.itemStack)
                        }
                    }
                }
            }
        }
    }

}