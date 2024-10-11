package me.cunzai.plugin.globaltrash.nms

import org.bukkit.craftbukkit.v1_20_R3.entity.CraftItem
import org.bukkit.entity.Item
import taboolib.library.reflex.Reflex.Companion.getProperty

class NMSImpl: NMS() {
    override fun willDead(item: Item, damage: Int): Boolean {
        val entityItem = (item as CraftItem).handle
        val health = entityItem.getProperty<Int>("health") ?: 1000
        return health - damage <= 0
    }
}