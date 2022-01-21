package ch.guengel.apod.crawler

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

private const val minConcurrency = 1
private const val maxConcurrency = 20

class Arguments {
    private val logger: Logger = LoggerFactory.getLogger(Arguments::class.java)
    private val parser = ArgParser("apod-crawler")

    private var targetDirectoryArg by parser.argument(ArgType.String,
        fullName = "target_dir",
        description = "Directory to store images. Must be existing.")
    private var startDateArg by parser.argument(ArgType.String,
        fullName = "start_date",
        description = "Date to start download pictures from")
    private var endDateArg by parser.argument(ArgType.String,
        fullName = "end_date",
        description = "Date up to which downloading pictures")

    var concurrency by parser.option(ArgType.Int,
        fullName = "concurrency",
        shortName = "c",
        description = "Number of concurrent downloads. Min ${minConcurrency}, max $maxConcurrency").default(2)

    val startDate: LocalDate get() = LocalDate.parse(startDateArg, localDateFormatter)
    val endDate: LocalDate get() = LocalDate.parse(endDateArg, localDateFormatter)
    val targetDirectory: Path get() = Path(targetDirectoryArg)

    private fun validate() {
        if (!targetDirectory.exists()) {
            logger.error("'{}' does not exists", targetDirectory.toString())
            exitProcess(1)
        }
        if (!targetDirectory.isDirectory()) {
            logger.error("'{}' is not a directory", targetDirectory.toString())
            exitProcess(2)
        }

        if (!(concurrency in minConcurrency..maxConcurrency)) {
            logger.error("concurrency must be between {} and {} inclusive. Got {}", minConcurrency, maxConcurrency,
                concurrency)
            exitProcess(3)
        }

        if (startDate > endDate) {
            logger.error("start date ({}) must be earlier than or equal to end date ({})",
                startDate.format(localDateFormatter),
                endDate.format(localDateFormatter))
            exitProcess(3)
        }
    }

    fun parse(args: Array<String>) {
        parser.parse(args)
        validate()
    }
}
