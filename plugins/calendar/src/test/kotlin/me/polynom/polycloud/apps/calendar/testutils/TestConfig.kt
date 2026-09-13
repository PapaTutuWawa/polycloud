package me.polynom.polycloud.apps.calendar.testutils

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EntityScan("me.polynom.polycloud.apps.calendar")
open class TestConfig {
}