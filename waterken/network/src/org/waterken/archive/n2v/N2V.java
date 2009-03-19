// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.waterken.archive.Archive;

/**
 * An <code>.n2v</code> (name to value) archive file format. 
 */
public final class
N2V implements Archive, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * 4 bytes at the start of every N2V archive file: {@link value}
     */
    static public    final int beginMagic = 0x4E32560A;
    
    /**
     * 4 bytes at the end of every N2V archive file: {@link value}
     */
    static public    final int endMagic = 0x0A56324E;
    
    static protected final int magicSize = Integer.SIZE / Byte.SIZE;

    /**
     * corresponding ETag for all entries
     */
    public final String etag;
    
    /**
     * raw archive data
     */
    public final ByteBuffer raw;
    
    /**
     * number of entries
     */
    public  final int entryCount;
    private final int indexOffsetSize;
    private final ByteBuffer data;
    private final ByteBuffer summary;
    private final ByteBuffer index;
    
    /*
     * archive ::= beginMagic
     *             value* summary index
     *             entryCount indexAddress summaryAddress
     *             endMagic
     * value ::= byte*
     * summary ::= entry*
     * index ::= offset*
     * entry ::= name NULL
     *           valueLength valueStart
     *           commentLength commentByte*
     */
    
    // special values for the valueStart
    static protected final int emptyFileAddress = 0;
    static protected final int directoryAddress = 1;
    
    /**
     * Constructs an instance.
     * @param etag  {@link #etag}
     * @param raw   {@link #raw}
     */
    public
    N2V(final String etag, final ByteBuffer raw) throws EOFException {
        this.etag = etag;
        this.raw = raw;
        
        final int archiveLength = raw.limit();
        final int addressSize = sizeof(archiveLength);
        final int totalsAddress = archiveLength - magicSize - 3 * addressSize;
        raw.position(totalsAddress);
        entryCount = readFixedInt(raw, addressSize);
        indexOffsetSize = sizeof(entryCount);
        final int indexAddress = readFixedInt(raw, addressSize);
        final int summaryAddress = readFixedInt(raw, addressSize);
        if (endMagic != readFixedInt(raw,magicSize)) {throw new EOFException();}

        raw.position(0);
        data = raw.slice();
        data.limit(summaryAddress);
        if (beginMagic!=readFixedInt(data,magicSize)){throw new EOFException();}
        
        raw.position(summaryAddress);
        summary = raw.slice();
        summary.limit(indexAddress - summaryAddress);
        
        raw.position(indexAddress);
        index = raw.slice();
        index.limit(totalsAddress - indexAddress);
    }
    
    /**
     * Opens an existing archive.
     * @param file  underlying file
     * @throws IOException  any I/O problem
     */
    static public N2V
    open(final File file) throws IOException {
        final FileChannel ch = new FileInputStream(file).getChannel();
        final long archiveLength = ch.size();
        if (archiveLength > Integer.MAX_VALUE) { throw new IOException(); }
        return new N2V('\"' + Long.toHexString(file.lastModified()) + '\"',
            ch.map(FileChannel.MapMode.READ_ONLY, 0, (int)archiveLength));
    }
    
    static protected int
    sizeof(final long max) {
        return max <= Byte.MAX_VALUE
            ? Byte.SIZE / Byte.SIZE
        : max <= Short.MAX_VALUE
            ? Short.SIZE / Byte.SIZE
        : max <= Integer.MAX_VALUE
            ? Integer.SIZE / Byte.SIZE
        : Long.SIZE / Byte.SIZE;
    }
    
    static private int
    readFixedInt(ByteBuffer buffer, final int size) {
        return Byte.SIZE / Byte.SIZE == size
            ? buffer.get()
        : Short.SIZE / Byte.SIZE == size
            ? buffer.getShort()
        : buffer.getInt();
    }
    
    // java.lang.Iterable interface

    public Iterator<Archive.Entry>
    iterator() {
        final ByteBuffer summary = this.summary.duplicate();
        summary.position(0);
        return new Iterator<Archive.Entry>() {

            public boolean
            hasNext() { return summary.position() != summary.limit(); }

            public Archive.Entry
            next() {
                final ByteArrayOutputStream path = new ByteArrayOutputStream();
                try {
                    while (true) {
                        final byte b = summary.get();
                        if (0 == b) { break; }
                        path.write(b);
                    }
                    return new Entry(path.toString("UTF-8"), summary);
                } catch (final BufferUnderflowException e) {
                    throw new NoSuchElementException();
                } catch (final UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            public void
            remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    private final class
    Entry implements Archive.Entry {
        
        private final String path;
        private final int length;
        private final int address;
        
        Entry(final String path, final ByteBuffer summary) {
            this.path = path;
            length = readExtensionInt(summary);
            address = readExtensionInt(summary);
            final int commentLength = readExtensionInt(summary);
            summary.position(summary.position() + commentLength);
        }

        public String
        getPath() { return path; }

        public boolean
        isDirectory() { return directoryAddress == address; }

        public String
        getETag() { return etag; }

        public long
        getLength() { return length; }
        
        public InputStream
        open() {
            if (0 == length) { return new ByteArrayInputStream(new byte[0]); }
            data.position(address);
            final ByteBuffer r = data.slice();
            r.limit((int)length);
            return new ByteBufferInputStream(r);
        }
    }
    
    static private int
    readExtensionInt(final ByteBuffer buffer) {
        int r = 0;
        for (int n = Integer.SIZE / Byte.SIZE; 0 != n--;) {
            final int b = buffer.get();
            r <<= Byte.SIZE - 1;
            r |= 0x7F & b;
            if (0 == (0x80 & b)) { return r; }
        }
        throw new RuntimeException();
    }
    
    // org.waterken.archive.Archive interface
    
    public Archive.Entry
    find(final String path) throws UnsupportedEncodingException {
        final byte[] key = path.getBytes("UTF-8");
        int low = 0;
        int high = entryCount - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            index.position(mid * indexOffsetSize);
            summary.position(readFixedInt(index, indexOffsetSize));
            int d = 0;
            for (int i = 0; 0 == d; ++i) {
                d = 0xFF & summary.get();
                if (i == key.length) {
                    if (0 == d) { return new Entry(path, summary); }
                } else {
                    d -= 0xFF & key[i];
                }
            }
            if (d < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }
}
