package me.polynom.polycloud.database.exceptions

/**
 * Exception for failures while the migrations run.
 */
class MigrationFailureException(
    cause: Throwable? = null,
) : RuntimeException("Migration failed!", cause)
