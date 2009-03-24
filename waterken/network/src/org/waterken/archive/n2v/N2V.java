// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.waterken.archive.Archive;

/**
 * An <code>.n2v</code> (name to value) archive file format. 
 */
public final class
N2V implements Archive, Serializable {
    static private final long serialVersionUID = 1L;

    static protected final Charset charset = Charset.forName("UTF-8");
    
    /**
     * 4 bytes at the end of every N2V archive file: {@link value}
     */
    static public    final int endMagic = 0x0A4E3256;
    
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
    private final int dataOffsetSize;
    private final int summaryOffsetSize;
    private final int indexEntrySize;
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
        final int summaryAddress = readFixedInt(raw, rawOffsetSize);
        if (endMagic != readFixedInt(raw,magicSize)) {throw new EOFException();}
        
        final int summaryLength = indexAddress - summaryAddress;
        dataOffsetSize = sizeof(summaryAddress);
        summaryOffsetSize = sizeof(summaryLength);
        indexEntrySize = summaryOffsetSize + dataOffsetSize;

        raw.position(0);
        data = raw.slice();
        data.limit(summaryAddress);
        
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
    
    /**
     * Merges the entries from multiple archives.
     * @param out   output channel for created archive
     * @param versions  archives to merge, ordered from oldest to newest
     * @throws IOException  any I/O problem
     */
    static public void
    merge(final WritableByteChannel out,
          final List<N2V> versions) throws IOException {
        final int versionCount = versions.size();
                
        // write out the most recent data values and reconstruct the indices
        final long[][] dataTable = new long[versionCount][];
        final long[][] summaryTable = new long[versionCount][];
        final ArrayList<ByteBuffer> summaryChunks =
            new ArrayList<ByteBuffer>(versionCount * 8);
        long dataOffset = 0;
        long summaryOffset = 0;
        long entryCount = 0;
        for (int i = versionCount; 0 != i--;) {
            final N2V m = versions.get(i);
            final long[] dataOffsets =  dataTable[i]   = new long[m.entryCount];
            final long[] summaryOffsets=summaryTable[i]= new long[m.entryCount];
            m.summary.position(0);
            final ByteBuffer names = m.summary.duplicate();
            int beginSummaryChunk = 0;
            int todo = 0;
            m.data.position(0);
            for (int entry = 0; m.entryCount != entry; ++entry) {
                boolean overridden = false;
                for (int j = i + 1; !overridden && j != versionCount; ++j) {
                    overridden = versions.get(j).contains(names);
                }
                if (!overridden && !m.contains(names)) { throw new Error(); }
                final int pos = m.index.position() / m.indexEntrySize;
                
                final int beginSummary = names.position();
                while (0 != names.get()) {}     // skip name and null
                final int valueLength = readExtensionInt(names);
                final int commentLength = readExtensionInt(names);
                if (0 != commentLength) {
                    names.position(names.position() + commentLength);
                }
                final int endSummary = names.position();
                
                if (overridden) {
                    if (0 != todo) {
                        final ByteBuffer chunk = m.data.slice();
                        chunk.limit(todo);
                        if (todo != out.write(chunk)) {throw new IOException();}
                        m.data.position(m.data.position() + todo);
                        todo = 0;
                    }
                    m.data.position(m.data.position() + valueLength);
                    if (beginSummary != beginSummaryChunk) {
                        m.summary.position(beginSummaryChunk);
                        final ByteBuffer chunk = m.summary.slice();
                        chunk.limit(beginSummary - beginSummaryChunk);
                        summaryChunks.add(chunk);
                    }
                    beginSummaryChunk = endSummary;
                } else {
                    dataOffsets[pos] = dataOffset;
                    summaryOffsets[pos] = summaryOffset;
                    dataOffset += valueLength;
                    summaryOffset += endSummary - beginSummary;
                    entryCount += 1;
                    todo += valueLength;
                }
            }
            if (todo != out.write(m.data)) { throw new IOException(); }
            if (names.limit() != beginSummaryChunk) {
                names.position(beginSummaryChunk);
                summaryChunks.add(names);
            }
        }
        final long summaryLength = summaryOffset;
        final long dataLength = dataOffset;
        for (final ByteBuffer chunk : summaryChunks) {
            if (chunk.remaining() != out.write(chunk)){throw new IOException();}
        }
        
        // produce an N-way merge of the indices
        final int dataOffsetSize = sizeof(dataLength);
        final int summaryOffsetSize = sizeof(summaryLength);
        final int indexOffsetSize = summaryOffsetSize + dataOffsetSize;
        final long indexLength = entryCount * indexOffsetSize;
        final long dsiLength = dataLength + summaryLength + indexLength;
        final int addressSize = sizeof(dsiLength+3*sizeof(dsiLength)+magicSize);
        final OutputStream sout = new BufferedOutputStream(
            new ChannelOutputStream(out),
            (int)Math.min(indexLength + 3 * addressSize + magicSize,
                          (1<<14) * indexOffsetSize));
        for (int j = versionCount; 0 != j--;) {
            versions.get(j).index.position(0).mark();
        }
        for (long i = entryCount; 0 != i--;) {
            int min = versionCount;
            ByteBuffer minKey = null;
            for (int j = min; 0 != j--;) {
                final N2V m = versions.get(j);
                int d = 0;
                while (m.index.hasRemaining() && 0 == d) {
                    m.index.mark();
                    m.summary.position(
                        readFixedInt(m.index, m.summaryOffsetSize)).mark();
                    readFixedInt(m.index, m.dataOffsetSize);
                    if (null != minKey) {
                        d = compare(m.summary, minKey);
                        m.summary.reset();
                        minKey.reset();
                    } else {
                        d = -1;
                    }
                }
                if (d < 0) {
                    min = j;
                    minKey = m.summary;
                }
            }
            final N2V m = versions.get(min);
            final int pos = m.index.position() / m.indexEntrySize;
            writeFixedLong(sout, summaryOffsetSize, summaryTable[min][pos-1]);
            writeFixedLong(sout, dataOffsetSize, dataTable[min][pos-1]);
            m.index.mark();
            for (int j=versionCount; 0 != j--;) {versions.get(j).index.reset();}
        }
        
        // append the totals
        writeFixedLong(sout, addressSize, entryCount);
        writeFixedLong(sout, addressSize, dataLength + summaryLength);
        writeFixedLong(sout, addressSize, dataLength);
        writeFixedLong(sout, magicSize,   endMagic);
        sout.flush();
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
            
            private int address = 0;

            public boolean
            hasNext() { return summary.hasRemaining(); }

            public Archive.Entry
            next() {
                try {
                    final ByteBuffer name = summary.slice();
                    final int begin = summary.position();
                    while (0 != summary.get()) {}
                    name.limit(summary.position() - begin - 1);
                    final Entry r = new Entry(address,
                        charset.decode(name).toString(), summary);
                    address += r.length;
                    return r;
                } catch (final BufferUnderflowException e) {
                    throw new NoSuchElementException();
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
        getName() { return path; }

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
    find(final String name) {
        return contains(charset.encode(name + '\0'))
            ? new Entry(readFixedInt(index, dataOffsetSize), name, summary)
        : null;
    }
    
    private boolean
    contains(final ByteBuffer name) {
        name.mark();
        for (int low = 0, high = entryCount - 1; low <= high;) {
            final int mid = (low + high) >>> 1;
            index.position(mid * indexEntrySize);
            summary.position(readFixedInt(index, summaryOffsetSize));
            final int d = compare(summary, name);
            name.reset();
            if (d < 0) {
                low = mid + 1;
            } else if (d > 0) {
                high = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }
    
    static private int
    compare(final ByteBuffer a, final ByteBuffer b) {
        while (true) {
            final int ab = 0xFF & a.get();
            final int bb = 0xFF & b.get();
            final int r = ab - bb;
            if (0 != r || 0 == ab) { return r; }
        }
    }
}
