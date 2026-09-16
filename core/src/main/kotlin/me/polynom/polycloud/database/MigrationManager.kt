package me.polynom.polycloud.database

import jakarta.persistence.EntityManager
import me.polynom.polycloud.database.exceptions.MigrationDatabaseSetupException
import me.polynom.polycloud.database.exceptions.MigrationFailureException
import me.polynom.polycloud.persistence.entities.Migration
import me.polynom.polycloud.persistence.repository.MigrationRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.readText

/**
 * Extracts the version name of a migration from its filename.
 *
 * @param name  The file name of the migration.
 * @return The numerical version number.
 */
private fun versionFromMigrationName(name: String): Int = name.split("__")[0].substring(1).toInt()

/**
 * Handles the execution of database migration scripts.
 *
 * For a plugin to have migrations there are a couple of requirements:
 * - a plugin must have a "plugin" file in the META-INF directory that contains the plugin name.
 * - migrations (SQL scripts) must be in the db/migrations/ directory.
 * - a single migration file must be of the format "V<version>__<name>.sql"
 */
@Service
class MigrationManager(
    /** The JPA entity manager. */
    private val entityManager: EntityManager,
    /** The JPA transaction manager. */
    private val transactionManager: PlatformTransactionManager,
    /** The repository for migration records. */
    private val migrationRepository: MigrationRepository,
) {
    /** Logging. */
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * To initialize the database, i.e. create the table that contains the migration records, we
     * have to do it here since
     */
    fun initialiseDatabase() {
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            entityManager
                .createNativeQuery(
                    """
                    CREATE TABLE IF NOT EXISTS migration (
                        plugin  TEXT NOT NULL PRIMARY KEY,
                        version INTEGER NOT NULL,
                        name    TEXT NOT NULL
                    );
                    """.trimIndent(),
                ).executeUpdate()
            transactionManager.commit(status)
            logger.info("Ran initial database setup")
        } catch (e: Exception) {
            transactionManager.rollback(status)
            logger.error("Error initialising database ", e)
            throw MigrationDatabaseSetupException()
        }
    }

    /**
     * Extract the current version of the migrations for the plugin.
     *
     * @param pluginName    The name of the plugin.
     * @return The version of the migrations for the plugin or null, if migrations for that plugin have not yet run.
     */
    private fun getLatestMigration(pluginName: String): Int? =
        migrationRepository
            .findById(pluginName)
            .map { it.version }
            .orElse(null)

    /**
     * Upsert a migration record into the database.
     *
     * @param pluginName    The name of the plugin.
     * @param version       The version number of the migration.
     * @param migration     The name of the migration that was run.
     */
    private fun createMigrationRecord(
        pluginName: String,
        version: Int,
        migration: String,
    ) {
        val entity =
            Migration(
                plugin = pluginName,
                version = version,
                name = migration,
            )
        migrationRepository.save(entity)
    }

    /**
     * Runs migrations from the filesystem.
     *
     * @param url   The URL pointing to the directory where migrations lie.
     */
    private fun runFileMigrations(url: URL) {
        val migrationsPath = Paths.get(url.path)
        val pluginNamePath = Paths.get(migrationsPath.parent.parent.toString(), "META-INF", "plugin")

        val pluginName: String
        try {
            pluginName = pluginNamePath.readText()
        } catch (exception: IOException) {
            logger.error("Failed to read plugin name from $pluginNamePath", exception)
            throw MigrationFailureException()
        }

        val migrations = Files.list(migrationsPath).toList().toMutableList()
        migrations.sort()
        val latestMigration = getLatestMigration(pluginName)
        logger.debug("Discovered [{}] as latest version for [{}]", pluginName, latestMigration)
        val migrationsToRun =
            migrations.filter filter@{
                if (latestMigration == null) {
                    return@filter true
                }

                val version = versionFromMigrationName(it.fileName.toString())
                version > latestMigration
            }
        logger.debug("Discovered migrations for [{}]: [{}]", pluginName, migrations)
        if (migrationsToRun.isEmpty()) {
            logger.info("No migrations required for plugin [{}]", pluginName)
            return
        }

        for (migration in migrationsToRun) {
            logger.info("Running migration [{}] for [{}]", migration.fileName, pluginName)
            runMigration(pluginName, migration.fileName.toString(), migration.readText())
        }
    }

    /**
     * Runs migrations from a plugin JAR.
     *
     * @param url   The URL pointing to the JAR.
     */
    private fun runJarMigrations(url: URL) {
        val jarUrl = url.toString().split("!")[0].replace("jar:file:", "")
        val jar = JarFile(jarUrl)

        // Find out the plugin name
        val pluginName: String
        try {
            pluginName = URL("jar:file:$jarUrl!/META-INF/plugin").readText()
        } catch (exception: IOException) {
            logger.error("Failed to read plugin name from $jarUrl")
            throw MigrationFailureException()
        }

        val latestMigration = getLatestMigration(pluginName)
        logger.debug("Discovered [{}] as latest version for [{}]", pluginName, latestMigration)
        val migrations =
            jar
                .entries()
                .toList()
                .filter {
                    it.name.startsWith("db/migrations/") && it.name != "db/migrations/"
                }.map {
                    it.name.split("/").last()
                }.toMutableList()
        val migrationsToRun =
            migrations.filter filter@{
                if (latestMigration == null) {
                    return@filter true
                }

                val version = versionFromMigrationName(it)
                version > latestMigration
            }
        logger.debug("Discovered migrations for [{}]: [{}]", pluginName, migrations)
        if (migrationsToRun.isEmpty()) {
            logger.info("No migrations required for plugin [{}]", pluginName)
            return
        }

        for (migration in migrationsToRun) {
            logger.info("Running migration [{}] for [{}]", migration, pluginName)
            val content = URL("jar:file:$jarUrl!/db/migrations/$migration").readText()
            runMigration(pluginName, migration, content)
        }
    }

    /**
     * Runs a single migration from a string inside a transaction.
     *
     * @param pluginName    The name of the plugin.
     * @param name          The name of the migration that is run.
     * @param content       The SQL script to run as the migration.
     */
    private fun runMigration(
        pluginName: String,
        name: String,
        content: String,
    ) {
        val status = transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            entityManager.createNativeQuery(content).executeUpdate()
            transactionManager.commit(status)
            createMigrationRecord(
                pluginName,
                versionFromMigrationName(name),
                name,
            )
            logger.info("[{}] done.", name)
        } catch (e: Exception) {
            logger.error("Failed to run migration [{}]. Rolling back.", name, e)
            transactionManager.rollback(status)
            throw MigrationFailureException()
        }
    }

    /**
     * Discovers all migrations and runs them.
     */
    fun runMigrations() {
        initialiseDatabase()
        val targets = ClassLoader.getSystemClassLoader().getResources("db/migrations").toList()
        logger.debug("Found directories [{}]", targets)

        for (target in targets) {
            when (target.protocol) {
                "file" -> runFileMigrations(target)
                "jar" -> runJarMigrations(target)
            }
        }
    }
}
