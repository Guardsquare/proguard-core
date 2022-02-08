/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.io;

import proguard.util.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * This class writes zip data to a given output stream. It returns a new
 * output stream for each zip entry that is opened. An entry can be compressed
 * or uncompressed. Uncompressed entries can be aligned to a multiple of a
 * given number of bytes.
 * <p/>
 * Multiple entries and output streams can be open at the same time. The entries
 * are added to the central directory in the order in which they are opened, but
 * the corresponding data are only written when their output streams are closed.
 * <p/>
 * The code automatically computes the CRC and lengths of the data, for
 * compressed and uncompressed data.
 *
 * @author Eric Lafortune
 */
public class ZipOutput
{
    private static final int MAGIC_LOCAL_FILE_HEADER                      = 0x04034b50;
    private static final int MAGIC_CENTRAL_DIRECTORY_FILE_HEADER          = 0x02014b50;
    private static final int MAGIC_END_OF_CENTRAL_DIRECTORY               = 0x06054b50;
    private static final int MAGIC_ZIP64_END_OF_CENTRAL_DIRECTORY         = 0x06064b50;
    private static final int MAGIC_ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR = 0x07064b50;
    private static final int MAGIC_ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD = 0x0001;


    private static final int VERSION              = 10;
    private static final int GENERAL_PURPOSE_FLAG =  0;
    private static final int METHOD_UNCOMPRESSED  =  0;
    private static final int METHOD_COMPRESSED    =  8;

    private static final int  ZIP64_MIN_VERSION                                  = 0x2d;
    private static final int  ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD_HEADER_SIZE = 4;
    private static final int  ZIP64_EXTENDED_SMALL_EXTRA_INFORMATION_FIELD_SIZE  = 16;
    private static final int  ZIP64_EXTENDED_LARGE_EXTRA_INFORMATION_FIELD_SIZE  = 24;
    private static final long ZIP64_FIELD_TOO_SMALL_32BIT                        = 0xFFFFFFFF;

    private static final boolean DEBUG = false;

    protected     LargeDataOutputStream outputStream;
    private final int                   uncompressedAlignment;

    private final String             comment;
    private final boolean            useZip64;

    private List<ZipEntry> zipEntries    = new ArrayList<>();
    private Set<String>    zipEntryNames = new HashSet<>();


    // Regular constructors.

    /**
     * Creates a new ZipOutput.
     * @param outputStream the output stream to which the zip data will be
     *                     written.
     */
    public ZipOutput(OutputStream outputStream)
    {
        this(outputStream, 1);
    }


    /**
     * Creates a new ZipOutput.
     * @param outputStream the output stream to which the zip data will be
     *                     written.
     * @param useZip64     Whether to write out the archive in zip64 format.
     */
    public ZipOutput(OutputStream outputStream, boolean useZip64)
    {
        this(outputStream, 1, useZip64, null);
    }


    /**
     * Creates a new ZipOutput that aligns uncompressed entries.
     * @param outputStream           the output stream to which the zip data will
     *                               be written.
     * @param uncompressedAlignment the default alignment of uncompressed data.
     */
    public ZipOutput(OutputStream outputStream,
                     int          uncompressedAlignment)
    {
        this(outputStream,
             uncompressedAlignment,
             false,
             null);
    }


    /**
     * Creates a new ZipOutput that aligns uncompressed entries.
     * @param outputStream          the output stream to which the zip data will
     *                              be written.
     * @param useZip64              Whether to write out the archive in zip64 format.
     * @param uncompressedAlignment the default alignment of uncompressed data.
     */
    public ZipOutput(OutputStream outputStream,
                     boolean      useZip64,
                     int          uncompressedAlignment)
    {
        this(outputStream,
             uncompressedAlignment,
             useZip64,
             null);
    }


    /**
     * Creates a new ZipOutput that aligns uncompressed entries and contains a comment.
     *
     * @param outputStream          the output stream to which the zip data will
     *                              be written.
     * @param uncompressedAlignment the default alignment of uncompressed data.
     * @param useZip64              Whether to write out the archive in zip64 format.
     * @param comment               optional comment for the entire zip file.
     */
    public ZipOutput(OutputStream outputStream,
                     int          uncompressedAlignment,
                     boolean      useZip64,
                     String       comment)
    {
        this.outputStream          = new LargeDataOutputStream(outputStream);
        this.uncompressedAlignment = uncompressedAlignment;
        this.useZip64              = useZip64;
        this.comment               = comment;
    }


    // These constructors write out a header immediately.

    /**
     * Creates a new ZipOutput that aligns uncompressed entries.
     * @param outputStream          the output stream to which the zip data will
     *                              be written.
     * @param header                an optional header for the zip file.
     * @param useZip64              Whether to write out the archive in zip64 format.
     * @param uncompressedAlignment the requested alignment of uncompressed data.
     */
    public ZipOutput(OutputStream outputStream,
                     byte[]       header,
                     int          uncompressedAlignment,
                     boolean      useZip64)
    throws IOException
    {
        this(outputStream,
             header,
             uncompressedAlignment,
             useZip64,
             null);
    }


    /**
     * Creates a new ZipOutput that aligns uncompressed entries and contains a comment.
     *
     * @param outputStream          the output stream to which the zip data will be written.
     * @param header                an optional header for the zip file.
     * @param uncompressedAlignment the requested alignment of uncompressed data.
     * @param useZip64              Whether to write out the archive in zip64 format.
     * @param comment               optional comment for the entire zip file.
     */
    public ZipOutput(OutputStream outputStream,
                     byte[]       header,
                     int          uncompressedAlignment,
                     boolean      useZip64,
                     String       comment)
    throws IOException
    {
        this.outputStream          = new LargeDataOutputStream(outputStream);
        this.uncompressedAlignment = uncompressedAlignment;
        this.useZip64              = useZip64;
        this.comment               = comment;
        if (header != null)
        {
            outputStream.write(header);
        }
    }


    /**
     * Creates a new zip entry, returning an output stream to write its data.
     * It is the caller's responsibility to close the output stream.
     * @param name             the name of the zip entry.
     * @param compress         specifies whether the entry should be
     *                         compressed with the default
     *                         alignment.
     * @param modificationTime the modification date and time of the zip
     *                         entry, in DOS format.
     * @return an output stream for writing the data of the zip entry.
     */
    public OutputStream createOutputStream(String  name,
                                           boolean compress,
                                           int     modificationTime)
    throws IOException
    {
        return createOutputStream(name,
                                  compress,
                                  uncompressedAlignment,
                                  modificationTime,
                                  null,
                                  null);
    }


    /**
     * Creates a new zip entry, returning an output stream to write its data.
     * It is the caller's responsibility to close the output stream.
     * @param name                  the name of the zip entry.
     * @param compress              specifies whether the entry should be
     *                              compressed.
     * @param uncompressedAlignment the requested alignment of uncompressed
     *                              data.
     * @param modificationTime      the modification date and time of the zip
     *                              entry, in DOS format.
     * @return an output stream for writing the data of the zip entry.
     */
    public OutputStream createOutputStream(String  name,
                                           boolean compress,
                                           int     uncompressedAlignment,
                                           int     modificationTime)
    throws IOException
    {
        return createOutputStream(name,
                                  compress,
                                  uncompressedAlignment,
                                  modificationTime,
                                  null,
                                  null);
    }


    /**
     * Creates a new zip entry, returning an output stream to write its data.
     * It is the caller's responsibility to close the output stream.
     * @param name                  the name of the zip entry.
     * @param compress              specifies whether the entry should be
     *                              compressed.
     * @param uncompressedAlignment the requested alignment of uncompressed
     *                              data.
     * @param modificationTime      the modification date and time of the zip
     *                              entry, in DOS format.
     * @param extraField            optional extra field data. These should
     *                              contain chunks, each with a short ID, a
     *                              short length (little endian), and their
     *                              corresponding data. The IDs 0-31 are
     *                              reserved for Pkware. Java's jar tool just
     *                              specifies an ID 0xcafe on its first entry.
     * @param comment               an optional comment.
     * @return an output stream for writing the data of the zip entry.
     */
    public OutputStream createOutputStream(String  name,
                                           boolean compress,
                                           int     uncompressedAlignment,
                                           int     modificationTime,
                                           byte[]  extraField,
                                           String  comment)
    throws IOException
    {
        // Check if the name hasn't been used yet.
        if (!zipEntryNames.add(name))
        {
            throw new IOException("Duplicate jar entry ["+name+"]");
        }

        ZipEntry entry = new ZipEntry(name,
                                      compress,
                                      uncompressedAlignment,
                                      modificationTime,
                                      extraField,
                                      comment);

        // Add the entry to the list that will be put in the central directory.
        zipEntries.add(entry);

        return entry.createOutputStream();
    }


    /**
     * Closes the zip archive, writing out its central directory and closing
     * the underlying output stream.
     */
    public void close() throws IOException
    {
        long centralDirectoryOffset = writeStartOfCentralDirectory();

        close(centralDirectoryOffset);
    }


    /**
     * Closes the zip archive, writing out its central directory and closing
     * the underlying output stream.
     */
    public void close(long centralDirectoryOffset) throws IOException
    {
        // Write the central directory.
        long centralDirectorySize = writeEntriesOfCentralDirectory();

        if (useZip64)
        {
            long zip64EndOfCentralDirectoryOffset = outputStream.getLongSize();;
            writeZip64EndOfCentralDirectory(centralDirectoryOffset, centralDirectorySize);
            writeZip64EndOfCentralDirectoryLocator(zip64EndOfCentralDirectoryOffset);
        }

        writeEndOfCentralDirectory(centralDirectoryOffset, centralDirectorySize);

        // Close the underlying output stream.
        outputStream.close();

        // Make sure the archive can't be used any further.
        outputStream  = null;
        zipEntries    = null;
        zipEntryNames = null;
    }


    /**
     * Returns the current size of the data written to the output stream.
     */
    protected long size()
    {
        return outputStream.getLongSize();
    }


    /**
     * Starts the central directory.
     * @return the current position in the output stream.
     */
    protected long writeStartOfCentralDirectory()
    {
        if (DEBUG)
        {
            System.out.println("ZipOutput.writeStartOfCentralDirectory");
        }

        // The central directory as such doesn't have a header.
        return outputStream.getLongSize();
    }


    /**
     * Writes the zip entries in the central directory.
     * @return the total size of the directory entries.
     */
    protected long writeEntriesOfCentralDirectory() throws IOException
    {
        if (DEBUG)
        {
            System.out.println("ZipOutput.writeEntriesOfCentralDirectory ("+zipEntries.size()+" entries)");
        }

        long offset = outputStream.getLongSize();

        for (int index = 0; index < zipEntries.size(); index++)
        {
            ZipEntry entry = zipEntries.get(index);

            entry.writeCentralDirectoryFileHeader();
        }

        return outputStream.getLongSize() - offset;
    }


    /**
     * Ends the central directory.
     * @param centralDirectoryOffset the offset of the central directory.
     * @param centralDirectorySize   the size of the central directory, not
     *                               counting the empty header and the trailer.
     */
    protected void writeEndOfCentralDirectory(long centralDirectoryOffset,
                                              long centralDirectorySize) throws IOException
    {
        if (DEBUG)
        {
            System.out.println("ZipOutput.writeEndOfCentralDirectory ("+zipEntries.size()+" entries)");
        }

        writeInt(MAGIC_END_OF_CENTRAL_DIRECTORY);
        writeShort(0);                                                             // Number of this disk.
        writeShort(0);                                                             // Number of disk with central directory.
        writeShort(zipEntries.size());                                             // Number of records on this disk.
        writeShort(zipEntries.size());                                             // Total number of records.
        writeInt(centralDirectorySize);                                            // Size of central directory, in bytes.
        writeInt(useZip64 ? ZIP64_FIELD_TOO_SMALL_32BIT : centralDirectoryOffset); // Offset of central directory.

        if (comment == null)
        {
            // No comment.
            writeShort(0);
        }
        else
        {
            // Comment length and comment.
            byte[] commentBytes = StringUtil.getModifiedUtf8Bytes(comment);
            writeShort(commentBytes.length);
            outputStream.write(commentBytes);
        }
    }


    /**
     * Writes out a ZIP64 end of central directory entry.
     * @param centralDirectoryOffset The offset from the start of the archive to the first central directory record.
     * @param centralDirectorySize   The total size of all central directory records.
     * @throws IOException In case a problem occurs during writing.
     */
    protected void writeZip64EndOfCentralDirectory(long centralDirectoryOffset,
                                                   long centralDirectorySize) throws IOException
    {
        if (DEBUG)
        {
            System.out.println("ZipOutput.writeZip64EndOfCentralDirectory");
        }

        writeInt(MAGIC_ZIP64_END_OF_CENTRAL_DIRECTORY);
        writeLong(44);                        // (Size = SizeOfFixedFields + SizeOfVariableData - 12.)
        writeShort(ZIP64_MIN_VERSION);        // Made by (Zip 4.5, Default OS value).
        writeShort(ZIP64_MIN_VERSION);        // Requires (Zip 4.5, Default OS value).
        writeInt(0);                          // Number of this disk.
        writeInt(0);                          // Number of disk with central directory.
        writeLong(zipEntries.size());         // Total number of entries in the central directory of this disk
        writeLong(zipEntries.size());         // Total number of entries in the central directory.
        writeLong(centralDirectorySize);
        writeLong(centralDirectoryOffset);
    }

    /**
     * Writes out a ZIP64 end of central directory locator.
     * @param  zip64CentralDirectoryOffset The offset to the zip64 end of central directory record.
     * @throws IOException In case a problem occurs during writing.
     */
    protected void writeZip64EndOfCentralDirectoryLocator(long zip64CentralDirectoryOffset) throws IOException
    {
        writeInt(MAGIC_ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR);
        writeInt(0); // Number of the disk with the start of the zip64 end of central directory
        writeLong(zip64CentralDirectoryOffset);
        writeInt(1); // Total number of disks
    }


    /**
     * This class represents a zip entry in its enclosing zip file. It can
     * provide an output stream and write its headers and its data to the main
     * zip output stream. In fact, it automatically writes its local header and
     * data when the output stream is closed.
     */
    private class ZipEntry
    {
        private boolean compressed;
        private int     uncompressedAlignment;
        private int     modificationTime;
        private int     crc;
        private long    compressedSize;
        private long    uncompressedSize;
        private long    offset;
        private String  name;
        private byte[]  extraField;
        private String  comment;


        /**
         * Creates a new zip entry, returning output stream to write its data.
         * It is the caller's responsibility to close the output stream.
         * @param name                  the name of the zip entry.
         * @param compressed            specifies whether the entry should be
         *                              compressed.
         * @param uncompressedAlignment the requested alignment of uncompressed
         *                              data.
         * @param modificationTime      the modification date and time of the
         *                              zip entry, in DOS format.
         * @param extraField            optional extra field data. These should
         *                              contain chunks, each with a short ID,
         *                              a short length (little endian), and
         *                              their corresponding data. The IDs 0-31
         *                              are reserved for Pkware. Java's jar tool
         *                              just specifies an ID 0xcafe on its first
         *                              entry.
         * @param comment               an optional comment.
         * @return an output stream for writing the zip data.
         */
        private ZipEntry(String  name,
                         boolean compressed,
                         int     uncompressedAlignment,
                         int     modificationTime,
                         byte[]  extraField,
                         String  comment)
        {
            this.name                  = name;
            this.compressed            = compressed;
            this.uncompressedAlignment = uncompressedAlignment;
            this.modificationTime      = modificationTime;
            this.extraField            = extraField;
            this.comment               = comment;
        }


        public OutputStream createOutputStream() throws IOException
        {
            return compressed ?
                new CompressedZipEntryOutputStream() :
                new UncompressedZipEntryOutputStream();
        }


        /**
         * Writes the local file header, which precedes the data, to the main
         * zip output stream.
         */
        private void writeLocalFileHeader() throws IOException
        {
            if (DEBUG)
            {
                System.out.println("ZipOutput.writeLocalFileHeader ["+name+"] (compressed = "+compressed+", offset = "+offset+", "+compressedSize+"/"+uncompressedSize+" bytes)");
            }
            // If any of these values do not fit, we have to create a zip64
            // extended extra information field to store them instead.
            boolean useZip64ExtraField = useZip64 && (
                (compressedSize   >> 32 > 0) ||
                (uncompressedSize >> 32 > 0));

            writeInt(MAGIC_LOCAL_FILE_HEADER);
            writeShort(useZip64 ? ZIP64_MIN_VERSION : VERSION);
            writeShort(GENERAL_PURPOSE_FLAG);
            writeShort(compressed ? METHOD_COMPRESSED : METHOD_UNCOMPRESSED);
            writeInt(modificationTime);
            writeInt(crc);
            writeInt(useZip64ExtraField ? ZIP64_FIELD_TOO_SMALL_32BIT : compressedSize);
            writeInt(useZip64ExtraField ? ZIP64_FIELD_TOO_SMALL_32BIT : uncompressedSize);

            byte[] nameBytes        = StringUtil.getModifiedUtf8Bytes(name);
            int    nameLength       = nameBytes.length;
            int    extraFieldLength = (useZip64ExtraField ? ZIP64_EXTENDED_SMALL_EXTRA_INFORMATION_FIELD_SIZE + ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD_HEADER_SIZE : 0) +
                                      (extraField != null ? extraField.length : 0);

            writeShort(nameLength);

            // Compute the required delta to get uncompressed data aligned.
            // These filler bytes will be appended to the extra field.
            int alignmentDelta = 0;
            if (!compressed)
            {
                long dataOffset = outputStream.getLongSize() + 2L + nameLength + extraFieldLength;
                alignmentDelta = (int)(dataOffset % uncompressedAlignment);
                if (alignmentDelta > 0)
                {
                    alignmentDelta = uncompressedAlignment - alignmentDelta;
                }
            }

            writeShort(extraFieldLength + alignmentDelta);

            outputStream.write(nameBytes);

            if (useZip64ExtraField)
            {
                // Write the Zip64 extended information field.
                writeShort(MAGIC_ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD);
                writeShort(ZIP64_EXTENDED_SMALL_EXTRA_INFORMATION_FIELD_SIZE);
                writeLong(uncompressedSize);
                writeLong(compressedSize);
            }

            if (extraField != null)
            {
                outputStream.write(extraField);
            }

            // Now append the filler bytes, as part of the extra field,
            // right before the data that will be added after the header.
            if (alignmentDelta > 0)
            {
                outputStream.write(new byte[alignmentDelta]);
            }
        }


        /**
         * Writes the file header for the central directory to the main zip
         * output stream.
         */
        public void writeCentralDirectoryFileHeader() throws IOException
        {
            if (DEBUG)
            {
                System.out.println("ZipOutput.writeCentralDirectoryFileHeader ["+name+"] (compressed = "+compressed+", offset = "+offset+", "+compressedSize+"/"+uncompressedSize+" bytes)");
            }

            // If any of these values do not fit, we have to create a zip64
            // extended extra information field to store them instead.
            boolean useZip64ExtraField = useZip64 && (
                (compressedSize   >> 32 > 0) ||
                (uncompressedSize >> 32 > 0) ||
                (offset           >> 32 > 0));

            writeInt(MAGIC_CENTRAL_DIRECTORY_FILE_HEADER);
            writeShort(useZip64 ? ZIP64_MIN_VERSION : VERSION); // Creation version.
            writeShort(useZip64 ? ZIP64_MIN_VERSION : VERSION); // Extraction Version.
            writeShort(GENERAL_PURPOSE_FLAG);
            writeShort(compressed ? METHOD_COMPRESSED : METHOD_UNCOMPRESSED);
            writeInt(modificationTime);
            writeInt(crc);
            writeInt(useZip64ExtraField ? ZIP64_FIELD_TOO_SMALL_32BIT : compressedSize);
            writeInt(useZip64ExtraField ? ZIP64_FIELD_TOO_SMALL_32BIT : uncompressedSize);

            byte[] nameBytes    = StringUtil.getModifiedUtf8Bytes(name);
            byte[] commentBytes = comment == null ? null :
                                  StringUtil.getModifiedUtf8Bytes(comment);

            writeShort(nameBytes.length);

            int extraFieldLength = (useZip64ExtraField ? ZIP64_EXTENDED_LARGE_EXTRA_INFORMATION_FIELD_SIZE + ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD_HEADER_SIZE : 0) +
                                   (extraField != null ? extraField.length : 0);

            writeShort(extraFieldLength);
            writeShort(commentBytes == null ? 0 : commentBytes.length);
            writeShort(0); // Disk number of file start.
            writeShort(0); // Internal file attributes.
            writeInt(0);   // External file attributes.
            writeInt(useZip64ExtraField ? ZIP64_FIELD_TOO_SMALL_32BIT : offset);
            outputStream.write(nameBytes);

            if (useZip64 && useZip64ExtraField)
            {
                // Write the Zip64 extended information field.
                writeShort(MAGIC_ZIP64_EXTENDED_EXTRA_INFORMATION_FIELD);
                writeShort(ZIP64_EXTENDED_LARGE_EXTRA_INFORMATION_FIELD_SIZE);
                writeLong(uncompressedSize);
                writeLong(compressedSize);
                writeLong(offset);
            }

            if (extraField != null)
            {
                outputStream.write(extraField);
            }

            if (commentBytes != null)
            {
                outputStream.write(commentBytes);
            }
        }


        /**
         * This OutputStream writes its uncompressed zip entry out to its zip
         * output stream when it is closed.
         */
        private class UncompressedZipEntryOutputStream extends ByteArrayOutputStream
        {
            private CRC32 crc32 = new CRC32();


            private UncompressedZipEntryOutputStream()
            {
                super(16 * 1024);
            }


            // Overridden methods for OutputStream.

            @Override
            public synchronized void write(int b)
            {
                super.write(b);

                crc32.update(b);
            }


            //public void write(byte[] b) throws IOException
            //{
            //    // The super implementation delegates to the method below.
            //    super.write(b);
            //}


            @Override
            public synchronized void write(byte[] b, int off, int len)
            {
                super.write(b, off, len);

                crc32.update(b, off, len);
            }


            @Override
            public void close() throws IOException
            {
                super.close();

                byte[] bytes = super.toByteArray();

                offset           = outputStream.getLongSize();
                crc              = (int)crc32.getValue();
                compressedSize   = bytes.length;
                uncompressedSize = bytes.length;

                writeLocalFileHeader();
                outputStream.write(bytes);
            }
        }


        /**
         * This OutputStream writes its compressed zip entry out to its zip
         * output stream when it is closed.
         */
        private class CompressedZipEntryOutputStream extends DeflaterOutputStream
        {
            private CRC32 crc32 = new CRC32();


            private CompressedZipEntryOutputStream()
            {
                super(new ByteArrayOutputStream(16 * 1024),
                      new Deflater(Deflater.BEST_COMPRESSION, true),
                      1024);
            }


            // Overridden methods for OutputStream.

            //public void write(int b) throws IOException
            //{
            //    // The super implementation delegates to the method below.
            //    super.write(b);
            //}
            //
            //
            //public void write(byte[] b) throws IOException
            //{
            //    // The super implementation delegates to the method below.
            //    super.write(b);
            //}


            @Override
            public void write(byte[] b, int off, int len) throws IOException
            {
                super.write(b, off, len);

                crc32.update(b, off, len);
                uncompressedSize += len;
            }


            @Override
            public void close() throws IOException
            {
                // Make sure the memory is freed. [JDK-4797189]
                super.finish();
                super.def.end();
                super.close();

                ByteArrayOutputStream byteArrayOutputStream =
                    (ByteArrayOutputStream)super.out;

                byte[] compressedBytes = byteArrayOutputStream.toByteArray();

                offset         = outputStream.getLongSize();
                crc            = (int)crc32.getValue();
                compressedSize = compressedBytes.length;

                writeLocalFileHeader();
                outputStream.write(compressedBytes);
            }
        }
    }


    /**
     * This output stream is mostly identical to DataOutputStream,
     * except it stores the amount of bytes written so far in a long
     * instead of an int. This makes sure the count is correct, even
     * when writing out more than 1 << 31 bytes.
     */
    public static class LargeDataOutputStream extends DataOutputStream
    {
        private long written;

        public LargeDataOutputStream(OutputStream out)
        {
            super(out);
            this.written = 0;
        }


        /**
         * @return the amount of bytes written to the output stream so far.
         */
        public long getLongSize()
        {
            return written;
        }


        // Implementations for DataOutputStream

        @Override
        public synchronized void write(int b) throws IOException
        {
            super.write(b);
            written++;
        }


        @Override
        public void write(byte[] b) throws IOException
        {
            super.write(b);
            written += b.length;
        }
    }


    // Small utility methods.

    /**
     * Writes out a little-endian short value to the zip output stream.
     */
    protected void writeShort(int value) throws IOException
    {
        outputStream.write(value);
        outputStream.write(value >>> 8);
    }


    /**
     * Writes out a little-endian int value to the zip output stream.
     */
    protected void writeInt(int value) throws IOException
    {
        outputStream.write(value);
        outputStream.write(value >>>  8);
        outputStream.write(value >>> 16);
        outputStream.write(value >>> 24);
    }


    /**
     * Writes out a little-endian int value to the zip output stream.
     */
    protected void writeInt(long value) throws IOException
    {
        outputStream.write((int)value);
        outputStream.write((int)(value >>>  8));
        outputStream.write((int)(value >>> 16));
        outputStream.write((int)(value >>> 24));
    }


    /**
     * Writes out a little-endian long value to the zip output stream.
     */
    protected void writeLong(long value) throws IOException
    {
        outputStream.write((int)value);
        outputStream.write((int)(value >>>  8));
        outputStream.write((int)(value >>> 16));
        outputStream.write((int)(value >>> 24));
        outputStream.write((int)(value >>> 32));
        outputStream.write((int)(value >>> 40));
        outputStream.write((int)(value >>> 48));
        outputStream.write((int)(value >>> 56));
    }


    /**
     * Provides a simple test for this class, creating a zip file with the
     * given name and a few aligned/compressed/uncompressed zip entries.
     */
    public static void main(String[] args)
    {
        try
        {
            ZipOutput output =
                new ZipOutput(new FileOutputStream(args[0]), null, 4, false, "Main file comment");

            PrintWriter printWriter1 =
                new PrintWriter(output.createOutputStream("file1.txt", false, 1, 0, new byte[] { 0x34, 0x12, 4, 0, 0x48, 0x65, 0x6c, 0x6c, 0x6f }, "Comment"));
            printWriter1.println("This is file 1.");
            printWriter1.println("Hello, world!");
            printWriter1.close();

            PrintWriter printWriter2 =
                new PrintWriter(output.createOutputStream("file2.txt", true, 1, 0, null, "Another comment"));
            printWriter2.println("This is file 2.");
            printWriter2.println("Hello, world!");
            printWriter2.close();

            PrintWriter printWriter3 =
                new PrintWriter(output.createOutputStream("file3.txt", false, 1, 0, null, "Last comment"));
            printWriter3.println("This is file 3.");
            printWriter3.println("Hello, world!");
            printWriter3.close();

            output.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
