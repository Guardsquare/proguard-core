package proguard.testutils.io

import io.mockk.every
import io.mockk.mockk
import proguard.io.DataEntry
import java.io.InputStream

class DataEntryUtil {
    class MockDataEntry {
        private val dataEntry = mockk<DataEntry>(relaxed = true)

        fun build(): DataEntry = dataEntry

        fun withName(name: String): MockDataEntry {
            every { dataEntry.name } returns name
            return this
        }

        fun withOriginalName(name: String): MockDataEntry {
            every { dataEntry.originalName } returns name
            return this
        }

        fun withInputStream(inputStream: InputStream): MockDataEntry {
            every { dataEntry.inputStream } returns inputStream
            every { dataEntry.closeInputStream() } returns inputStream.close()
            return this
        }

        fun withParent(parent: DataEntry): MockDataEntry {
            every { dataEntry.parent } returns parent
            return this
        }

        fun withSize(size: Long): MockDataEntry {
            every { dataEntry.size } returns size
            return this
        }

        fun withIsDirectory(isDirectory: Boolean): MockDataEntry {
            every { dataEntry.isDirectory } returns isDirectory
            return this
        }
    }
}
