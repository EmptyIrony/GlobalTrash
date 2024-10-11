package me.cunzai.plugin.globaltrash.ui

import kotlinx.coroutines.withContext
import me.cunzai.plugin.globaltrash.config.ConfigLoader
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
import taboolib.module.nms.inputSign
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.replaceLore
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
    fun open(player: Player, search: String? = null) {
        submitChain {
            val items = getGlobalItems()
            println("all item: ${items.size}")
            open(player, items, search)
        }
    }

    private suspend fun open(player: Player, items: List<Pair<UUID, ItemStack>>, search: String? = null) = withContext(SyncDispatcher) {
        player.openMenu<PageableChest<Pair<UUID, ItemStack>>>(uiConfig.getStringColored("title")!!) {
            elements {
                if (search.isNullOrBlank()) {
                    items
                } else {
                    val keyWorlds = search.replace(" ", "")
                        .replace("\"", "")

                    val matchedMaterials = ConfigLoader.nameToMaterial.filter { (chineseName, material) ->
                        chineseName.contains(keyWorlds, ignoreCase = true)
                    }.values.toHashSet()

                    items.filter {
                        matchedMaterials.contains(it.second.type)
                    }
                }
            }
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
                        sync {
                            clicker.closeInventory()
                        }
                        return@submitChain
                    }
                }
            }
            onClick { event -> event.isCancelled = true}


            if (player.hasPermission("trash.search")) {
                set('$', uiConfig.getItemStack("search")!!.replaceLore(
                    mapOf("%search%" to (search ?: ""))
                )) {
                    isCancelled = true
                    player.closeInventory()
                    submit(delay = 1L) {
                        player.inputSign(
                            arrayOf(
                                "",
                                "~~~~~~~~~~",
                                "在上方输入",
                                "搜索内容"
                            )
                        ) {
                            open(player, it.getOrNull(0))
                        }
                    }
                }
            }

            setupBasic(player)
        }
    }

    private suspend fun giveItem(player: Player, item: ItemStack) = withContext(SyncDispatcher) {
        player.inventory.addItem(item)
        player.sendLang("get_success", item.getName())
    }

}