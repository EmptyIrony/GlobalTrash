package me.cunzai.plugin.globaltrash.ui

import kotlinx.coroutines.withContext
import me.cunzai.plugin.globaltrash.database.getGlobalItems
import me.cunzai.plugin.globaltrash.database.getItem
import me.cunzai.plugin.globaltrash.database.getWriteLock
import me.cunzai.plugin.globaltrash.database.removeItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.expansion.SyncDispatcher
import taboolib.expansion.submitChain
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.getStringColored
import taboolib.module.nms.getName
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.sendLang
import java.util.UUID

@Config("ui.yml")
lateinit var uiConfig: Configuration

fun PageableChest<*>.setupBasic(player: Player) {
    setPreviousPage(getSlots('<').first()) { _, _ ->
        uiConfig.getItemStack("previous")!!
    }
    setNextPage(getSlots('>').first()) { _, _ ->
        uiConfig.getItemStack("next")!!
    }
    set('!', uiConfig.getItemStack("back")!!) {
        isCancelled = true
        submit(delay = 1L) {
            uiConfig.getStringColored("back.command")!!.replace(
                "%player%", player.name
            ).apply {
                Bukkit.dispatchCommand(player, this)
            }
        }
    }
}

object GlobalTrashUI {
    fun open(player: Player) {
        submitChain {
            val items = getGlobalItems()
            open(player, items)
        }
    }

    private suspend fun open(player: Player, items: List<Pair<UUID, ItemStack>>) = withContext(SyncDispatcher) {
        player.openMenu<PageableChest<Pair<UUID, ItemStack>>>(uiConfig.getStringColored("title")!!) {
            elements { items }
            map(*uiConfig.getStringList("map").toTypedArray())
            slots(getSlots('#'))
            onGenerate { _, (_, item), _, _ -> item }
            onClick { event, (uuid, _) ->
                event.isCancelled = true
                val clicker = event.clicker

                submitChain {
                    try {
                        getWriteLock {
                            val latestItem = getItem(uuid)
                            if (latestItem == null) {
                                sync {
                                    open(player)
                                }
                                clicker.sendLang("item_not_exist")
                                return@getWriteLock
                            }

                            val success = removeItem(uuid)
                            if (!success) {
                                sync {
                                    open(player)
                                }
                                clicker.sendLang("item_not_exist")
                                return@getWriteLock
                            }

                            giveItem(player, latestItem)
                            sync {
                                open(player)
                            }
                        }
                    } catch (e: IllegalStateException) {
                        clicker.sendLang("get_lock_failed")
                        clicker.closeInventory()
                        return@submitChain
                    }
                }
            }
            onClick { event -> event.isCancelled = true}

            setupBasic(player)
        }
    }

    suspend fun giveItem(player: Player, item: ItemStack) = withContext(SyncDispatcher) {
        player.inventory.addItem(item)
        player.sendLang("get_success", item.getName())
    }

}