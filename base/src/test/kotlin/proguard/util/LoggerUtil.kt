package proguard.util

import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.OutputStreamAppender
import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun withTestLogger(block: (OutputStream) -> Unit) {
    val context = LoggerContext.getContext(false)
    val config = context.configuration
    val loggerName = java.util.UUID.randomUUID().toString()
    try {
        val loggerOutput = ByteArrayOutputStream()
        // Grab the same config as the current appender.
        val layout = config.rootLogger.appenders.values.first().layout
        val appender = OutputStreamAppender.createAppender(layout, null, loggerOutput, loggerName, false, true)
        appender.start()
        config.addAppender(appender)
        config.rootLogger.addAppender(appender, null, null)
        block(loggerOutput)
    } finally {
        config.removeLogger(loggerName)
    }
}
