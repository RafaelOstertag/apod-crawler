package ch.guengel.apod.crawler

import org.slf4j.Logger
import org.slf4j.LoggerFactory


private val logger: Logger = LoggerFactory.getLogger("main")

fun main(args: Array<String>) {
    Arguments.parse(args)

    try {
        logger.info("Download APOD images to {} between {} and {} (concurrency: {})",
            Arguments.targetDirectory,
            Arguments.startDate.format(localDateFormatter),
            Arguments.endDate.format(localDateFormatter),
            Arguments.concurrency
        )

        ApodCrawler(
            Arguments.targetDirectory,
            Arguments.startDate,
            Arguments.endDate,
            Arguments.concurrency).use {
            it.crawl()
        }
    } catch (e: Exception) {
        logger.error("Error while crawling", e)
    }
}
