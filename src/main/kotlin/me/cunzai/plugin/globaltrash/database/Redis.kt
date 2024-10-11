package me.cunzai.plugin.globaltrash.database

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.inventory.ItemStack
import redis.clients.jedis.JedisPool
import taboolib.common5.util.decodeBase64
import taboolib.common5.util.encodeBase64
import taboolib.expansion.AlkaidRedis
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.fromConfig
import taboolib.expansion.lock.Lock
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.deserializeToItemStack
import taboolib.platform.util.serializeToByteArray
import java.util.UUID

@Config("database.yml")
lateinit var databaseConfig: Configuration

val redis by lazy {
    AlkaidRedis.create()
        .fromConfig(databaseConfig.getConfigurationSection("redis")!!)
        .connect()
        .connection()
}

val gson = Gson()

const val redisInventoryMarker = "global_trash:global_trash"

private val redisWriteLock by lazy {
    redis.getLock("global_trash:write_lock")
}

private val pool by lazy {
    redis.getProperty<JedisPool>("pool")!!
}

suspend fun getGlobalItems(): List<Pair<UUID, ItemStack>> = withContext(AsyncDispatcher) {
    pool.resource.use { jedis ->
        jedis.hgetAll(redisInventoryMarker).map { (key, value) -> UUID.fromString(key) to value.decodeBase64().deserializeToItemStack() }
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
    pool.resource.use { jedis ->
        jedis.hset(redisInventoryMarker, UUID.randomUUID().toString(), itemStack.serializeToByteArray().encodeBase64())
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
        } finally {
            if (writeLocked) redisWriteLock.unlock()
        }
        delay(50L)
        triedTimes++
    }

    throw IllegalStateException("无法获取读写锁")
}