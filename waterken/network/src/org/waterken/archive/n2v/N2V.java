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
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
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
    private final int summaryAddress;
    private final int summaryOffsetSize;
    private final int dataOffsetSize;
    private final ByteBuffer data;
    private final ByteBuffer summary;
    private final ByteBuffer index;
    
    /*
     * archive ::= beginMagic
     *             data summary index
     *             entryCount indexAddress summaryAddress
     *             endMagic
     * data ::= value*
     * value ::= byte*
     * summary ::= entry*
     * entry ::= name NULL valueLength commentLength commentByte*
     * index ::= (summaryOffset dataOffset)*
     */
    
    /**
     * Constructs an instance.
     * @param etag  {@link #etag}
     * @param raw   {@link #raw}
     */
    public
    N2V(final String etag, final ByteBuffer raw) throws EOFException {
        this.etag = etag;
        this.raw = raw;
        
        final int rawLength = raw.limit();
        final int rawOffsetSize = sizeof(rawLength);
        final int totalsAddress = rawLength - magicSize - 3 * rawOffsetSize;
        raw.position(totalsAddress);
        entryCount = readFixedInt(raw, rawOffsetSize);
        final int indexAddress = readFixedInt(raw, rawOffsetSize);
        summaryAddress = readFixedInt(raw, rawOffsetSize);
        if (endMagic != readFixedInt(raw,magicSize)) {throw new EOFException();}
        
        final int summaryLength = indexAddress - summaryAddress;
        summaryOffsetSize = sizeof(summaryLength);
        dataOffsetSize = sizeof(summaryAddress);

        raw.position(0);
        data = raw.slice();
        data.limit(summaryAddress);
        if (beginMagic!=readFixedInt(data,magicSize)){throw new EOFException();}
        
        raw.position(summaryAddress);
        summary = raw.slice();
        summary.limit(summaryLength);
        
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
    
    static public void
    merge(final WritableByteChannel out,
          final N2V... versions) throws IOException {
        long total = 0;
        versions[versions.length - 1].data.rewind();
        out.write(versions[versions.length - 1].data);
        total += versions[versions.length - 1].data.limit();
        for (int i = versions.length; --i != 0;) {
            // TODO
        }
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
    
    static protected void
    writeFixedLong(final OutputStream out,
                   final int size, final long n) throws IOException {
        int todo = size * Byte.SIZE;                // number of bits to output
        long mask = 0xFFL << (todo -= Byte.SIZE);   // mask for next output bits
        for (; 0L != mask; todo -= Byte.SIZE, mask >>>= Byte.SIZE) {
            out.write((int)((n & mask) >>> todo));
        }
    }
    
    // java.lang.Iterable interface

    public Iterator<Archive.Entry>
    iterator() {
        final ByteBuffer summary = this.summary.duplicate();
        summary.position(0);
        return new Iterator<Archive.Entry>() {
            
            private int address = magicSize;

            public boolean
            hasNext() { return summaryAddress != address; }

            public Archive.Entry
            next() {
                final ByteArrayOutputStream path = new ByteArrayOutputStream();
                try {
                    while (true) {
                        final byte b = summary.get();
                        if (0 == b) { break; }
                        path.write(b);
                    }
                    final Entry r =
                        new Entry(address, path.toString("UTF-8"), summary);
                    address += r.length;
                    return r;
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

        private final int address;
        private final String path;
        private final int length;
        
        Entry(final int address, final String path, final ByteBuffer summary) {
            this.address = address;
            this.path = path;
            length = readExtensionInt(summary);
            final int commentLength = readExtensionInt(summary);
            if (0 != commentLength) {
                summary.position(summary.position() + commentLength);
            }
        }

        public String
        getPath() { return path; }

        public String
        getETag() { return etag; }

        public long
        getLength() { return length; }
        
        public InputStream
        open() {
            if (0 == length) { return new ByteArrayInputStream(new byte[0]); }
            data.position(address);
            final ByteBuffer r = data.slice();
            r.limit(length);
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
    
    static protected void
    writeExtensionLong(final ByteArrayBuilder out, final long n) {
        int todo = 9 * 7;                   // number of bits to output
        long mask = 0x7FL << (todo -= 7);   // mask for next output bits
        for (; 0L == (n & mask) && 0 != todo; todo -= 7, mask >>= 7) {}
        for (;                     0 != todo; todo -= 7, mask >>= 7) {
            out.write(0x80 | (int)((n & mask) >> todo));
        }
        out.write((int)(n & mask));
    }
    
    // org.waterken.archive.Archive interface
    
    public Archive.Entry
    find(final String path) throws UnsupportedEncodingException {
        final byte[] key = path.getBytes("UTF-8");
        int low = 0;
        int high = entryCount - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            index.position(mid * (summaryOffsetSize + dataOffsetSize));
            summary.position(readFixedInt(index, summaryOffsetSize));
            int d = 0;
            for (int i = 0; 0 == d; ++i) {
                d = 0xFF & summary.get();
                if (i == key.length) {
                    if (0 == d) {
                        return new Entry(readFixedInt(index, dataOffsetSize),
                                         path, summary);
                    }
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
