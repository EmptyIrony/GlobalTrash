package me.cunzai.plugin.globaltrash.nms

import org.bukkit.entity.Item

abstract class NMS {

    abstract fun willDead(item: Item, damage: Int): Boolean

}