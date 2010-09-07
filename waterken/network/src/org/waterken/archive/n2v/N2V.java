// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.waterken.archive.Archive;
import org.waterken.io.bounded.Bounded;

/**
 * An <code>.n2v</code> (name to value) archive file format. 
 */
public final class
N2V implements Archive, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * 4 bytes at the end of every N2V archive file: {@link value}
     */
    static public    final int endMagic = 0x0A4E3256;
    
    static protected final int magicSize = Integer.SIZE / Byte.SIZE;

    private final String etag;      // corresponding ETag for all entries
    private final Cursor data;      // raw archive data
    
    private final int dataOffsetSize;
    private final long summaryAddress;
    private final long summaryLength;
    private final int summaryOffsetSize;
    private final long indexAddress;
    private final int indexEntrySize;
    private final long entryCount;
    private final long totalsAddress;
    
    /*
     * archive ::= data summary index indexAddress summaryAddress endMagic
     * data ::= value*
     * value ::= byte*
     * summary ::= entry*
     * entry ::= name 0 valueLength commentLength commentByte*
     * index ::= (dataOffset summaryOffset)*
     */
    
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
    
    /**
     * Constructs an instance.
     * @throws IOException  any I/O problem
     */
    private
    N2V(final String etag, final Cursor data) throws IOException {
        this.etag = etag;
        this.data = data;
        
        final long length = data.getLength();
        final int rawOffsetSize = sizeof(length);
        totalsAddress = length - magicSize - 2 * rawOffsetSize;
        data.jump(totalsAddress);
        indexAddress = data.readFixedLong(rawOffsetSize);
        summaryAddress = data.readFixedLong(rawOffsetSize);
        if (endMagic!=data.readFixedLong(magicSize)) {throw new EOFException();}
        
        dataOffsetSize = sizeof(summaryAddress);
        summaryLength = indexAddress - summaryAddress;
        summaryOffsetSize = sizeof(summaryLength);
        indexEntrySize = summaryOffsetSize + dataOffsetSize;
        entryCount = (totalsAddress - indexAddress) / indexEntrySize;
    }
    
    /**
     * Opens an archive.
     * @param file  archive file
     * @throws IOException  any I/O problem
     */
    static public N2V
    open(final File file) throws IOException {
        return new N2V('\"' + Long.toHexString(file.lastModified()) + '\"',
                       new Cursor(file));
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
        for (int i = versionCount; 0 != i--;) {
            final N2V m = versions.get(i);
            if (m.entryCount > Integer.MAX_VALUE) { throw new IOException(); }
            dataTable[i]    = new long[(int)m.entryCount];
            summaryTable[i] = new long[(int)m.entryCount];
        }
        class Chunk {
            final Cursor data;
            final long off;
            final long len;
            
            Chunk(final Cursor data, final long off, final long len) {
                this.data = data;
                this.off = off;
                this.len = len;
            }
        }
        final ArrayList<Chunk> summaryChunks =
            new ArrayList<Chunk>(versionCount * 8);
        long dataOffset = 0;
        long summaryOffset = 0;
        long entryCount = 0;
        for (int i = versionCount; 0 != i--;) {
            final long[] dataOffsets =    dataTable[i];
            final long[] summaryOffsets = summaryTable[i];
            final ArrayList<Chunk> dataChunks = new ArrayList<Chunk>(8);
            final N2V m = versions.get(i);
            m.data.jump(m.summaryAddress);
            final Cursor names = m.data.fork();
            long dataChunkOffset = 0;
            long dataChunkLength = 0;
            long summaryChunkOffset = m.summaryAddress;
            final long[] pos = new long[] { -1 };
            for (int entry = 0; m.entryCount != entry; ++entry) {
                boolean overridden = false;
                for (int j = i + 1; !overridden && j != versionCount; ++j) {
                    overridden = 0 <= versions.get(j).locate(pos, names);
                }
                pos[0] = -1;
                if (!overridden) {
                    if (0 > m.locate(pos, names)) { throw new Error(); }
                }
                
                final long beginSummary = names.getPosition();
                while (0 != names.read()) {}     // skip name and null
                final long valueLength = readExtensionLong(names);
                skip(names, readExtensionLong(names));
                final long endSummary = names.getPosition();
                
                if (overridden) {
                    if (0 != valueLength) {
                        if (0 != dataChunkLength) {
                            dataChunks.add(new Chunk(
                                m.data, dataChunkOffset, dataChunkLength));
                        }
                        dataChunkOffset += dataChunkLength + valueLength;
                        dataChunkLength = 0;
                    }
                    if (beginSummary != summaryChunkOffset) {
                        summaryChunks.add(new Chunk(m.data, summaryChunkOffset,
                            beginSummary - summaryChunkOffset));
                    }
                    summaryChunkOffset = endSummary;
                } else {
                    dataOffsets[(int)pos[0]] = dataOffset;
                    summaryOffsets[(int)pos[0]] = summaryOffset;
                    dataOffset += valueLength;
                    summaryOffset += endSummary - beginSummary;
                    entryCount += 1;
                    dataChunkLength += valueLength;
                }
            }
            if (0 != dataChunkLength) {
                dataChunks.add(new Chunk(
                    m.data, dataChunkOffset, dataChunkLength));
            }
            if (m.indexAddress != summaryChunkOffset) {
                summaryChunks.add(new Chunk(m.data, summaryChunkOffset,
                    m.indexAddress - summaryChunkOffset));
            }
            for (final Chunk chunk : dataChunks) {
                chunk.data.writeTo(chunk.off, chunk.len, out);
            }
        }
        final long dataLength = dataOffset;
        final long summaryLength = summaryOffset;
        for (final Chunk chunk : summaryChunks) {
            chunk.data.writeTo(chunk.off, chunk.len, out);
        }
        
        // produce an N-way merge of the indices
        final int dataOffsetSize = sizeof(dataLength);
        final int summaryOffsetSize = sizeof(summaryLength);
        final int indexOffsetSize = dataOffsetSize + summaryOffsetSize;
        final long indexLength = entryCount * indexOffsetSize;
        final long dsiLength = dataLength + summaryLength + indexLength;
        final int addressSize = sizeof(dsiLength+2*sizeof(dsiLength)+magicSize);
        final OutputStream sout = new BufferedOutputStream(
            new ChannelOutputStream(out),
            (int)Math.min(indexLength + 2 * addressSize + magicSize,
                          (1<<14) * indexOffsetSize));
        final int[] i = new int[versionCount];
        while (true) {
            int min = versionCount;
            Cursor minKey = null;
            for (int j = min; 0 != j--;) {
                final N2V m = versions.get(j);
                for (; i[j] != m.entryCount; i[j] += 1) {
                    m.data.jump(m.indexAddress + i[j] * m.indexEntrySize);
                    m.data.readFixedLong(m.dataOffsetSize);
                    m.data.jump(m.summaryAddress +
                                m.data.readFixedLong(m.summaryOffsetSize));
                    m.data.mark(0);
                    if (null != minKey) {
                        final int d = compare(m.data, minKey);
                        m.data.reset();
                        minKey.reset();
                        if (d > 0) { break; }
                        if (0 == d) { continue; }
                    }
                    min = j;
                    minKey = m.data;
                    break;
                }
            }
            if (null == minKey) { break; }
            writeFixedLong(sout, dataOffsetSize,    dataTable[min][i[min]]);
            writeFixedLong(sout, summaryOffsetSize, summaryTable[min][i[min]]);
            i[min] += 1;
        }
        
        // append the totals
        writeFixedLong(sout, addressSize, dataLength + summaryLength);
        writeFixedLong(sout, addressSize, dataLength);
        writeFixedLong(sout, magicSize,   endMagic);
        sout.flush();
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
    
    static protected long
    readExtensionLong(final InputStream buffer) throws IOException {
        long r = 0;
        for (int n = Long.SIZE / Byte.SIZE; 0 != n--;) {
            final int b = buffer.read();
            if (-1 == b) { throw new EOFException(); }
            r <<= Byte.SIZE - 1;
            r |= 0x7F & b;
            if (0 == (0x80 & b)) { return r; }
        }
        throw new RuntimeException();
    }
    
    static protected void
    writeExtensionLong(final OutputStream out, final long n) throws IOException{
        int todo = 9 * 7;                   // number of bits to output
        long mask = 0x7FL << (todo -= 7);   // mask for next output bits
        for (; 0L == (n & mask) && 0 != todo; todo -= 7, mask >>= 7) {}
        for (;                     0 != todo; todo -= 7, mask >>= 7) {
            out.write(0x80 | (int)((n & mask) >> todo));
        }
        out.write((int)(n & mask));
    }
    
    // java.lang.Iterable interface

    public Iterator<Archive.Entry>
    iterator() {
        final InputStream s;
        final int first;
        try {
            data.jump(summaryAddress);
            s = Bounded.input(summaryLength, data.fork());
            first = s.read();
        } catch (final IOException e) { throw new RuntimeException(e); }
        return new Iterator<Archive.Entry>() {
            
            private long address = 0;
            private int next = first;

            public boolean
            hasNext() { return -1 != next; }

            public Archive.Entry
            next() {
                if (-1 == next) { throw new NoSuchElementException(); }
                try {
                    final ByteArrayOutputStream name =
                        new ByteArrayOutputStream(32);
                    while (0 != next) {
                        name.write(next);
                        next = s.read();
                        if (-1 == next) { throw new EOFException(); }
                    } 
                    final Entry r = new Entry(address,name.toString("UTF-8"),s);
                    address += r.length;
                    next = s.read();
                    return r;
                } catch (final EOFException e) {
                    throw new NoSuchElementException();
                } catch (final IOException e) { throw new RuntimeException(e); }
            }

            public void
            remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    private final class
    Entry implements Archive.Entry {

        private final long address;
        private final String name;
        private final long length;
        
        Entry(final long address,
              final String name, final InputStream meta) throws IOException {
            this.address = address;
            this.name = name;
            length = readExtensionLong(meta);
            skip(meta, readExtensionLong(meta));
        }

        public String
        getName() { return name; }

        public String
        getETag() { return etag; }

        public long
        getLength() { return length; }
        
        public InputStream
        open() throws IOException {
            if (0 == length) { return new ByteArrayInputStream(new byte[0]); }
            data.jump(address);
            return Bounded.input(length, data.fork());
        }
    }
    
    static protected void
    skip(final InputStream buffer, final long n) throws IOException {
        for (long r = n; r > 0;) {
            final long d = buffer.skip(r);
            if (0 < d) { r -= d; }
        }
    }
    
    // org.waterken.archive.Archive interface
    
    public void
    close() throws IOException { data.close(); }
    
    public Archive.Entry
    find(final String name) throws IOException {
        final long address = locate(new long[] { -1 },
            new ByteArrayInputStream((name + '\0').getBytes("UTF-8")));
        return address >= 0 ? new Entry(address, name, data) : null;
    }
    
    private long
    locate(final long[] pos, final InputStream name) throws IOException {
        name.mark(0);
        for (long low = 0, high = entryCount - 1; low <= high;) {
            final long mid = (low + high) >>> 1;
            data.jump(indexAddress + mid * indexEntrySize);
            final long r = data.readFixedLong(dataOffsetSize);
            data.jump(summaryAddress + data.readFixedLong(summaryOffsetSize));
            final int d = compare(data, name);
            name.reset();
            if (d < 0) {
                low = mid + 1;
            } else if (d > 0) {
                high = mid - 1;
            } else {
                pos[0] = mid;
                return r;
            }
        }
        return -1;
    }
    
    static private int
    compare(final InputStream a, final InputStream b) throws IOException {
        while (true) {
            final int ab = a.read();
            final int bb = b.read();
            final int r = ab - bb;
            if (0 != r || 0 == ab) { return r; }
        }
    }
}
