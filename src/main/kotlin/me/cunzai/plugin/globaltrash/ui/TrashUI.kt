package me.cunzai.plugin.globaltrash.ui

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.cunzai.plugin.globaltrash.config.ConfigLoader
import me.cunzai.plugin.globaltrash.database.addItem
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.DispatcherType
import taboolib.expansion.submitChain
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.util.getStringColored
import taboolib.module.nms.getName
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.isAir
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

object TrashUI {

    val cache = HashMap<UUID, TrashData>()

    fun open(player: Player) {
        val trashData = cache.getOrPut(player.uniqueId) {
            TrashData()
        }
        player.openMenu<PageableChest<SingleItemData>>(uiConfig.getStringColored("title_personal")!!) {
            elements { trashData.items.values.toList() }
            map(*uiConfig.getStringList("map").toTypedArray())
            slots(getSlots('#'))
            onGenerate { _, (_, item), _, _ -> item }
            onClick { event, element ->
                event.isCancelled = true
                val clicker = event.clicker

                val itemData = trashData.items.remove(element.uuid) ?: run {
                    open(player)
                    return@onClick
                }

                clicker.inventory.addItem(itemData.itemStack)
                clicker.sendLang("get_success", itemData.itemStack.getName())
            }
            onClick { event ->
                event.isCancelled = true
                val clickEvent = event.clickEventOrNull() ?: return@onClick

                val inventory = clickEvent.clickedInventory ?: return@onClick
                if (inventory !is PlayerInventory) return@onClick
                if (!clickEvent.isLeftClick) {
                    return@onClick
                }

                val currentItem = clickEvent.currentItem
                if (currentItem.isAir()) return@onClick

                clickEvent.currentItem = null

                val uuid = UUID.randomUUID()
                trashData.items[uuid] = SingleItemData(
                    uuid, currentItem, System.currentTimeMillis()
                )

                player.sendLang("drop_success", currentItem.getName())
            }

            setupBasic(player)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        val data = cache.remove(e.player.uniqueId) ?: return
        submitChain {
            for ((_, item) in data.items) {
                addItem(item.itemStack)
            }
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun disable() {
        val awaitFuture = CompletableFuture<Unit>()
        submitChain {
            try {
                cache.forEach { (_, data) ->
                    data.items.forEach { (_, item) ->
                        addItem(item.itemStack)
                    }
                }
            } finally {
                awaitFuture.complete(Unit)
            }
        }

        awaitFuture.join()
    }

    @Schedule(period = 20L)
    fun uploadingToGlobal() {
        submitChain(DispatcherType.SYNC) {
            val now = System.currentTimeMillis()
            for ((_, data) in cache.toList()) {
                data.items.forEach { (_, item) ->
                    if (now - item.putTime >= ConfigLoader.clearTime * 1000L) {
                        data.items.remove(item.uuid)
                        withContext(AsyncDispatcher) {
                            launch {
                                addItem(item.itemStack)
                            }
                        }
                    }
                }
            }
        }
    }

    data class TrashData(
        val items: HashMap<UUID, SingleItemData> = HashMap()
    )

    data class SingleItemData(
        val uuid: UUID,
        val itemStack: ItemStack,
        val putTime: Long,
    )

}