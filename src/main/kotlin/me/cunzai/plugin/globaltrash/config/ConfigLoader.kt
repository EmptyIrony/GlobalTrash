package me.cunzai.plugin.globaltrash.config

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object ConfigLoader {

    @Config("config.yml")
    lateinit var config: Configuration

    var clearTime = 60

    @Awake(LifeCycle.ENABLE)
    fun i() {
        clearTime = config.getInt("clear_time")
    }

}