plugins {
    id("java-library")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

spotless {
    kotlin {
        ktlint()
    }
}

spotbugs {
    showProgress = true
}

tasks.spotbugsMain {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs.html")
        setStylesheet("fancy-hist.xsl")
    }
}