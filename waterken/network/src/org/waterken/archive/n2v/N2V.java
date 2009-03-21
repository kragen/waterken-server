// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

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
import java.util.BitSet;
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
     * 4 bytes at the start of every N2V archive file: {@link value}
     */
    static public    final int beginMagic = 0x4E32560A;
    
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
          final List<N2V> versions) throws IOException {
        final int n = versions.size();
        if (0 == n) { return; }
        
        // write out all data values in the most recent version
        long dataLength;
        long count; {
            final N2V top = versions.get(n - 1);
            dataLength = top.data.limit();
            count = top.entryCount;
            top.data.position(0);
            if (dataLength != out.write(top.data)) { throw new IOException(); }
        }
        
        // write out the most recent data values from the preceding versions
        final BitSet[] lives = new BitSet[n - 1];
        for (int i = n - 1; 0 != i--;) {
            final N2V m = versions.get(i);
            final BitSet live = lives[i] = new BitSet(m.entryCount);
            m.data.position(magicSize);
            m.summary.position(0);
            int todo = 0;               // pending data bytes to be written
            for (int entry = 0; m.entryCount != entry; ++entry) {
                boolean override = false;
                for (int j = i + 1; !override && j != n; ++j) {
                    override = versions.get(j).contains(m.summary);
                }
                while (0 != m.summary.get()) {}     // skip name and null
                final int valueLength = readExtensionInt(m.summary);
                final int commentLength = readExtensionInt(m.summary);
                if (0 != commentLength) {
                    m.summary.position(m.summary.position() + commentLength);
                }
                if (override) {
                    if (0 != todo) {
                        final ByteBuffer chunk = m.data.slice();
                        chunk.limit(todo);
                        if (todo != out.write(chunk)) {throw new IOException();}
                        dataLength += todo;
                        m.data.position(m.data.position() + todo);
                        todo = 0;
                    }
                    m.data.position(m.data.position() + valueLength);
                } else {
                    count += 1;
                    live.set(entry);
                    todo += valueLength;
                }
            }
            if (0 != todo) {
                if (todo != out.write(m.data)) { throw new IOException(); }
                dataLength += todo;
            }
        }
        
        // write out the summary of the most recent version
        final long summaryLength;
        {
            final N2V top = versions.get(n - 1);
            top.summary.position(0);
            final int todo = top.summary.limit();
            if (todo != out.write(top.summary)) { throw new IOException(); }
            summaryLength = todo;
        }
        
        /*
        // write out the summary for entries from the preceding versions
        // and construct an N-way merge of the indices
        final int summaryOffsetSize = sizeof(indexAddress - summaryAddress);
        final int dataOffsetSize = sizeof(summaryAddress);
        final long indexLength = count * (summaryOffsetSize + dataOffsetSize);
        total += indexLength;
        final int addressSize = sizeof(total + 3 * sizeof(total) + magicSize);
        final int maxChunkSize = (1<<14) * (summaryOffsetSize + dataOffsetSize);
        final ByteArrayBuilder index = new ByteArrayBuilder((int)Math.min(
            indexLength + 3 * addressSize + magicSize, maxChunkSize));
        for (final N2V m : versions) {
            m.summary.position(0);
            m.index.position(0);
        }
        long dataOffset = magicSize;
        long summaryOffset = 0;
        for (int i = n - 1; 0 != i--;) {
            final N2V m = versions.get(i);
            final BitSet live = lives[i];
            m.summary.position(0);
            boolean pending = false;    // pending summary bytes to be written?
            for (int entry = 0; m.entryCount != entry; ++entry) {
                if (live.get(entry)) {
                    if (!pending) {
                        pending = true;
                        m.summary.mark();
                    }
                } else if (pending) {
                    final int here = m.summary.position();
                    m.summary.reset();
                    final int todo = here - m.summary.position();
                    final ByteBuffer chunk = m.summary.slice();
                    chunk.limit(todo);
                    if (todo != out.write(chunk)) { throw new IOException(); }
                    total += todo;
                    m.summary.position(here);
                    pending = false;
                }
                
                // skip over this summary entry
                while (0 != m.summary.get()) {}     // skip name and null
                readExtensionInt(m.summary);        // skip valueLength
                final int commentLength = readExtensionInt(m.summary);
                if (0 != commentLength) {
                    m.summary.position(m.summary.position() + commentLength);
                }
            }
            if (pending) {
                m.summary.reset();
                final int todo = m.summary.remaining();
                if (todo != out.write(m.summary)) { throw new IOException(); }
                total += todo;
            }
        }
        final long indexAddress = total;
        
        // append the totals to the index
        N2V.writeFixedLong(index, addressSize, count);
        N2V.writeFixedLong(index, addressSize, indexAddress);
        N2V.writeFixedLong(index, addressSize, summaryAddress);
        N2V.writeFixedLong(index, magicSize,   endMagic);
        
        index.writeTo(out);
        */
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
            hasNext() { return summary.hasRemaining(); }

            public Archive.Entry
            next() {
                try {
                    final ByteBuffer path = summary.slice();
                    final int begin = summary.position();
                    while (0 != summary.get()) {}
                    path.limit(summary.position() - begin - 1);
                    final Entry r = new Entry(address,
                        charset.decode(path).toString(), summary);
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
    find(final String path) {
        return contains(charset.encode(path + '\0'))
            ? new Entry(readFixedInt(index, dataOffsetSize), path, summary)
        : null;
    }
    
    private boolean
    contains(final ByteBuffer key) {
        key.mark();
        for (int low = 0, high = entryCount - 1; low <= high;) {
            final int mid = (low + high) >>> 1;
            index.position(mid * (summaryOffsetSize + dataOffsetSize));
            summary.position(readFixedInt(index, summaryOffsetSize));
            int a;
            int b;
            do {
                a = 0xFF & summary.get();
                b = 0xFF & key.get();
            } while (a == b && 0 != a);
            key.reset();
            if (a < b) {
                low = mid + 1;
            } else if (a > b) {
                high = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }
}
