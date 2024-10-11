package me.cunzai.plugin.globaltrash.config

import org.bukkit.Material
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.nms.getI18nName
import taboolib.platform.util.buildItem

object ConfigLoader {

    @Config("config.yml")
    lateinit var config: Configuration

    var clearTime = 60

    val nameToMaterial = HashMap<String, Material>()

    @Awake(LifeCycle.ENABLE)
    fun i() {
        clearTime = config.getInt("clear_time")

        for (material in Material.values()) {
            nameToMaterial[buildItem(material).getI18nName()] = material
        }
    }

}