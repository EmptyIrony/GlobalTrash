package me.cunzai.plugin.globaltrash.database

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.cunzai.plugin.globaltrash.ui.TrashUI.cache
import org.bukkit.inventory.ItemStack
import redis.clients.jedis.JedisPool
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common5.util.decodeBase64
import taboolib.common5.util.encodeBase64
import taboolib.expansion.AlkaidRedis
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.fromConfig
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.deserializeToItemStack
import taboolib.platform.util.serializeToByteArray
import java.util.*

@Config("database.yml")
lateinit var databaseConfig: Configuration

val redis by lazy {
    AlkaidRedis.create()
        .fromConfig(databaseConfig.getConfigurationSection("redis")!!)
        .connect()
        .connection()
}

const val redisInventoryMarker = "global_trash:global_trash"

private val redisWriteLock by lazy {
    redis.getLock("global_trash:write_lock")
}

private val pool by lazy {
    redis.getProperty<JedisPool>("pool")!!
}

suspend fun getGlobalItems(): List<Pair<UUID, ItemStack>> = withContext(AsyncDispatcher) {
    return@withContext getGlobalItems0()
}

private fun getGlobalItems0(): List<Pair<UUID, ItemStack>> {
    return pool.resource.use { jedis ->
        jedis.hgetAll(redisInventoryMarker)
            .map { (key, value) -> UUID.fromString(key) to value.decodeBase64().deserializeToItemStack() }
    }
}

suspend fun removeItem(uuid: UUID): Boolean = withContext(AsyncDispatcher) {
    pool.resource.use { jedis ->
        jedis.hdel(redisInventoryMarker, uuid.toString()) == 1L
    }
}

suspend fun getItem(uuid: UUID): ItemStack? = withContext(AsyncDispatcher) {
    pool.resource.use { jedis ->
        jedis.hget(redisInventoryMarker, uuid.toString())?.decodeBase64()?.deserializeToItemStack()
    }
}

suspend fun addItem(itemStack: ItemStack) = withContext(AsyncDispatcher) {
    addItem0(itemStack)
}

private fun addItem0(itemStack: ItemStack) {
    for ((uuid, item) in getGlobalItems0()) {
        if (itemStack.amount <= 0) return
        if (item.amount >= item.type.maxStackSize) {
            continue
        }
        if (item.isSimilar(itemStack)) {
            val newItem = item.clone()
            // 取最大能存储的数量
            val coerceAtMost = (newItem.amount + itemStack.amount).coerceAtMost(item.type.maxStackSize)
            // 获取实际能添加进去的数量
            val added = item.type.maxStackSize - newItem.amount
            // 修正这个物品的数量
            newItem.amount = coerceAtMost
            pool.resource.use { jedis ->
                jedis.hset(redisInventoryMarker, uuid.toString(), newItem.serializeToByteArray().encodeBase64())
            }

            // 修正待添加的物品的数量
            itemStack.amount -= added
        }
    }

    if (itemStack.amount > 0) {
        pool.resource.use { jedis ->
            jedis.hset(
                redisInventoryMarker,
                UUID.randomUUID().toString(),
                itemStack.serializeToByteArray().encodeBase64()
            )
        }
    }
}

suspend fun getWriteLock(block: suspend () -> Unit) = withContext(AsyncDispatcher) {
    var triedTimes = 0
    while (triedTimes <= 10) {
        var writeLocked = false
        try {
            if (redisWriteLock.tryLock()) {
                writeLocked = true
                block()
                return@withContext
            }
            println("锁失败")
        } finally {
            if (writeLocked) redisWriteLock.unlock()
        }
        delay(50L)
        triedTimes++
    }

    throw IllegalStateException("无法获取读写锁")
}

private fun getWriteLock0(block: () -> Unit) {
    pool.resource.use { jedis ->
        var triedTimes = 0

        val lock = me.cunzai.plugin.globaltrash.util.Lock(jedis, "global_trash:write_lock")
        while (triedTimes <= 10) {
            var writeLocked = false
            try {
                if (lock.tryLock()) {
                    writeLocked = true
                    block()
                    return
                }
                println("锁失败")
            } finally {
                if (writeLocked) lock.unlock()
            }
            triedTimes++
        }

        throw IllegalStateException("无法获取读写锁")
    }
}

@Awake(LifeCycle.DISABLE)
fun disable() {
    getWriteLock0 {
        cache.forEach { (_, data) ->
            data.items.forEach { (_, item) ->
                addItem0(item.itemStack)
            }
        }
    }
}