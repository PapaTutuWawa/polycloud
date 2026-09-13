package me.polynom.polycloud.database.exceptions

/**
 * Exception for failures while the migrations run.
 */
class MigrationFailureException : RuntimeException("Migration failed!")
