package ch.guengel.apod.crawler

import org.slf4j.Logger
import org.slf4j.LoggerFactory


private val logger: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    val gitInfo = GitInfo()
    logger.info("apod-crawler ${gitInfo.version} built from commit ${gitInfo.commitShort} @ ${gitInfo.buildTime}")

    val arguments = Arguments()
    arguments.parse(args)

    try {
        logger.info("Download APOD images to {} between {} and {} (concurrency: {})",
            arguments.targetDirectory,
            arguments.startDate.format(localDateFormatter),
            arguments.endDate.format(localDateFormatter),
            arguments.concurrency
        )

        ApodCrawler(
            arguments.targetDirectory,
            arguments.startDate,
            arguments.endDate,
            arguments.concurrency).use {
            it.crawl()
        }
    } catch (e: Exception) {
        logger.error("Error while crawling", e)
    }
}
