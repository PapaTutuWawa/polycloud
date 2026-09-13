package me.polynom.polycloud.database.exceptions

/**
 * Exception for when the initial database setup failed.
 */
class MigrationDatabaseSetupException : RuntimeException("Failed to create initial database table")
