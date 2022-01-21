package ch.guengel.apod.crawler

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val APOD_PREFIX = "https://apod.nasa.gov/apod"

class ApodCrawler(
    private val downloadDirectory: Path,
    private val startDate: LocalDate,
    private val endDate: LocalDate = LocalDate.now(),
    private val concurrency: Int = 2,
) : AutoCloseable {
    @OptIn(DelicateCoroutinesApi::class)
    private val threadPool = newFixedThreadPoolContext(concurrency, "Crawler-Context")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyMMdd")
    private val client = HttpClient(Apache) {
        engine {
            customizeClient {
                setMaxConnTotal(concurrency)
            }
            customizeRequest {
                setContentCompressionEnabled(true)
                connectionRequestTimeout = Int.MAX_VALUE
            }
        }
    }

    init {
        check(endDate >= startDate) { "endDate must be after startDate" }
    }

    fun crawl() = runBlocking(threadPool) {
        val dates = generateDates()
        val imageUris = dates
            .map { date ->
                logger.info("Processing date {}", date)
                async {
                    getPictureURI(date)
                }
            }
            .awaitAll()
            .filterNotNull()

        logger.info("Found {} images", imageUris.size)

        imageUris.map { async { fetchImage(it) } }.awaitAll()
    }

    private fun generateDates() = buildList {
        var date = startDate
        while (date <= endDate) {
            add(date)
            date = date.plusDays(1)
        }
    }

    private suspend fun getPictureURI(date: LocalDate): URI? {
        val htmlUrl = "$APOD_PREFIX/ap${date.format(dateFormatter)}.html"
        try {
            val response: HttpResponse = client.get(htmlUrl)
            if (response.status != HttpStatusCode.OK) {
                logger.error("Got HTTP Status {} on {}", response.status, htmlUrl)
                return null
            }

            return response.receive<String>().let { text ->
                createImageURI(text, htmlUrl)
            }
        } catch (e: Exception) {
            logger.error("Got exception on {}", htmlUrl, e)
            return null
        }
    }

    private suspend fun fetchImage(imageURI: URI) {
        val filename = Path.of(imageURI.path).fileName
        val file = downloadDirectory.resolve(filename).toFile()
        try {
            client.get<HttpStatement>(imageURI.toASCIIString()).execute { httpResponse ->
                if (httpResponse.status != HttpStatusCode.OK) {
                    logger.error("Got HTTP Status {} on {}", httpResponse.status, imageURI)
                    return@execute
                }

                downloadImage(httpResponse, file)
            }
        } catch (ex: Exception) {
            logger.error("Got exception on {}", imageURI, ex)
        }
    }

    private suspend fun downloadImage(
        httpResponse: HttpResponse,
        file: File,
    ) {
        val channel: ByteReadChannel = httpResponse.receive()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                file.appendBytes(bytes)
            }
        }
        logger.info("Downloaded {} from {}", file, httpResponse.request.url.toString())
    }

    private fun createImageURI(html: String, htmlUrl: String): URI? = imagePathRegex.find(html)?.let {
        val imagePath = it.groups["imagePath"]!!.value
        logger.info("Found image {}", imagePath)
        URI.create("$APOD_PREFIX/$imagePath")
    } ?: run {
        logger.error("No image found on {}", htmlUrl)
        null
    }

    override fun close() {
        logger.debug("Shutting down thread pool")
        threadPool.close()
        logger.debug("Shutting down http client")
        client.close()
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ApodCrawler::class.java)
        val imagePathRegex = Regex("<a href=\"(?<imagePath>image/[\\w/._-]+)\"")
    }
}
