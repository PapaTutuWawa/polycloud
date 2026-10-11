package me.polynom.polycloud.apps.webdav.auth

import me.polynom.polycloud.apps.webdav.persistence.entities.AppPasswordEntity
import me.polynom.polycloud.apps.webdav.persistence.repository.AppPasswordRepository
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Minimal in-memory [AppPasswordRepository] fake, so [AppPasswordService] tests don't need a real database. */
class FakeAppPasswordRepository : AppPasswordRepository {
    private val byHash = ConcurrentHashMap<String, AppPasswordEntity>()

    override fun <S : AppPasswordEntity> save(entity: S): S {
        byHash[requireNotNull(entity.secretHash)] = entity
        return entity
    }

    override fun <S : AppPasswordEntity> saveAll(entities: Iterable<S>): Iterable<S> = entities.map { save(it) }

    override fun findById(id: String): Optional<AppPasswordEntity> = Optional.ofNullable(byHash[id])

    override fun existsById(id: String): Boolean = byHash.containsKey(id)

    override fun findAll(): Iterable<AppPasswordEntity> = byHash.values.toList()

    override fun findAllById(ids: Iterable<String>): Iterable<AppPasswordEntity> = ids.mapNotNull { byHash[it] }

    override fun count(): Long = byHash.size.toLong()

    override fun deleteById(id: String) {
        byHash.remove(id)
    }

    override fun delete(entity: AppPasswordEntity) {
        byHash.remove(entity.secretHash)
    }

    override fun deleteAllById(ids: Iterable<String>) {
        ids.forEach { byHash.remove(it) }
    }

    override fun deleteAll(entities: Iterable<AppPasswordEntity>) {
        entities.forEach { delete(it) }
    }

    override fun deleteAll() {
        byHash.clear()
    }

    override fun findAllByUsername(username: String): List<AppPasswordEntity> = byHash.values.filter { it.username == username }

    override fun findByAppPasswordIdAndUsername(
        appPasswordId: UUID,
        username: String,
    ): AppPasswordEntity? = byHash.values.find { it.appPasswordId == appPasswordId && it.username == username }
}
