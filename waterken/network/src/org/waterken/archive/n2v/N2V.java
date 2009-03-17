// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.charset.UTF8;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.archive.Archive;
import org.waterken.archive.dir.FileMetadata;
import org.waterken.io.bounded.Bounded;

/**
 * An <code>.n2v</code> (name to value) archive file format. 
 */
public final class
N2V extends Struct implements Archive, Serializable {
    static private final long serialVersionUID = 1L;

    private final File file;
    private final String etag;
    private final byte[] summary;
    private final int[] index;
    
    /*
     * file ::= value* metaData* offset* entryCount indexAddress summaryAddress
     * value ::= byte*
     * metaData ::= name NULL
     *              valueLength valueOffset
     *              commentLength commentByte*
     */
    
    /**
     * Constructs an instance.
     * @param file  underlying file
     * @param meta  ETag generator
     * @throws IOException
     */
    public @deserializer
    N2V(@name("file") final File file,
        @name("meta") final FileMetadata meta) throws IOException {
        this.file = file;
        this.etag = meta.tag(file);
        
        final long summaryAddress; {
            final long archiveLength = Filesystem.length(file);
            final int addressSize = byteSize(archiveLength);
            final InputStream s =
                new BufferedInputStream(Filesystem.read(file), 3 * addressSize);
            skipFully(s, archiveLength - 3 * addressSize);
            final long entryCount = readLong(s, addressSize);
            final long indexAddress = readLong(s, addressSize);
            summaryAddress = readLong(s, addressSize);
            s.close();
            
            final long summaryLength = indexAddress - summaryAddress;
            if (summaryLength > Integer.MAX_VALUE) { throw new IOException(); }
            summary = new byte[(int)summaryLength];
            if (entryCount > Integer.MAX_VALUE) { throw new IOException(); }
            index = new int[(int)entryCount];
        }
        final InputStream s = new BufferedInputStream(Filesystem.read(file));
        skipFully(s, summaryAddress);
        readFully(s, summary);
        final int offsetSize = byteSize(index.length);
        for (int i = 0; i != index.length; ++i) {
            index[i] = readInt(s, offsetSize);
        }
        s.close();
    }
    
    static private long
    readLong(final InputStream s, final int actualSize) throws IOException {
        long r = 0;
        for (int n = actualSize; 0 != n--;) {
            final int b = s.read();
            if (-1 == b) { throw new EOFException(); }
            r <<= Byte.SIZE;
            r |= 0xFF & b;
        }
        return r;
    }
    
    static private int
    readInt(final InputStream s, final int actualSize) throws IOException {
        int r = 0;
        for (int n = actualSize; 0 != n--;) {
            final int b = s.read();
            if (-1 == b) { throw new EOFException(); }
            r <<= Byte.SIZE;
            r |= 0xFF & b;
        }
        return r;
    }
    
    static private void
    skipFully(final InputStream s, final long jump) throws IOException {
        for (long n = jump; 0 != n; n -= s.skip(n)) {}
    }
    
    static private void
    readFully(final InputStream s, final byte[] buffer) throws IOException {
        for (int n = buffer.length; 0 != n;) {
            final int d = s.read(buffer, buffer.length - n, n);
            if (-1 == d) { throw new EOFException(); }
            n -= d;
        }
    }
    
    static private int
    byteSize(final long max) {
        return max <= Byte.MAX_VALUE
            ? Byte.SIZE / Byte.SIZE
        : max <= Short.MAX_VALUE
            ? Short.SIZE / Byte.SIZE
        : max <= Integer.MAX_VALUE
            ? Integer.SIZE / Byte.SIZE
        : Long.SIZE / Byte.SIZE;
    }
    
    // org.waterken.archive.Archive interface

    public String
    tag(final String filename) throws IOException {
        return locate(UTF8.encode(filename)) < 0 ? null : etag;
    }

    public long
    measure(final String filename) throws IOException {
        final byte[] key = UTF8.encode(filename);
        final int i = locate(key);
        return i < 0 ? -1L : readExtensionLong(new ByteArrayInputStream(
                summary, index[i] + key.length + 1, 8));
    }

    public InputStream
    read(final String filename) throws FileNotFoundException, IOException {
        final byte[] key = UTF8.encode(filename);
        final int i = locate(key);
        if (i < 0) { throw new FileNotFoundException(); }
        final InputStream metadata =
            new ByteArrayInputStream(summary, index[i] + key.length + 1, 16);
        final long length = readExtensionLong(metadata);
        final long address = readExtensionLong(metadata);
        final InputStream r = Filesystem.read(file);
        skipFully(r, address);
        return Bounded.input(length, r);
    }
    
    private int
    locate(final byte[] key) {
        int low = 0;
        int high = index.length - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            int d = 0;
            for (int i = 0, j = index[mid]; 0 == d; ++i, ++j) {
                if (i == key.length) {
                    d = 0xFF & summary[j];
                    break;
                }
                d = (0xFF & summary[j]) - (0xFF & key[i]);
            }
            if (d < 0) {
                low = mid + 1;
            } else if (d > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found.
    }
    
    static private long
    readExtensionLong(final InputStream s) throws IOException {
        long r = 0;
        for (int i = Long.SIZE / Byte.SIZE; 0 != i--;) {
            final int b = s.read();
            if (-1 == b) { throw new EOFException(); }
            r <<= Byte.SIZE - 1;
            r |= 0x7F & b;
            if (0 == (0x80 & b)) { return r; }
        }
        throw new IOException();
    }
}
