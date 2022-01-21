package ch.guengel.apod.crawler

import java.util.*

private const val NOT_AVAILABLE = "n/a"

class GitInfo {
    var buildTime: String = NOT_AVAILABLE
        private set
    var version: String = NOT_AVAILABLE
        private set
    var commitShort: String = NOT_AVAILABLE
        private set
    var commit: String = NOT_AVAILABLE
        private set

    init {
        GitInfo::class.java.getResourceAsStream("/git.properties")?.use {
            val properties = Properties()
            properties.load(it)
            buildTime = properties.getProperty("git.build.time", NOT_AVAILABLE)
            version = properties.getProperty("git.build.version", NOT_AVAILABLE)
            commitShort = properties.getProperty("git.commit.id.abbrev", NOT_AVAILABLE)
            commit = properties.getProperty("git.commit.id.full", NOT_AVAILABLE)
        }
    }
}
