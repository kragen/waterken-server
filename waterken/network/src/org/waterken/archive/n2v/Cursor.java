// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.WritableByteChannel;

/**
 * A random access file cursor.
 */
/* package */ final class
Cursor extends InputStream {
    
    private final boolean top;
    private final Cursor[] current;
    private final RandomAccessFile main;
    
    private       long saved;   // saved position
    private       long marked;  // marked position
    
    private
    Cursor(final Cursor[] current, final RandomAccessFile main,
           final long saved, final long marked) {
        top = false;
        this.current = current;
        this.main = main;
        
        this.saved = saved;
        this.marked = marked;
    }
    
    private
    Cursor(final RandomAccessFile main) {
        top = true;
        this.current = new Cursor[] { this };   
        this.main = main;
        
        saved = -1;
        marked = -1;
    }
    
    public
    Cursor(final File file) throws IOException {
        this(new RandomAccessFile(file, "r"));
    }
    
    private void
    restore() throws IOException {
        if (this != current[0]) {
            current[0].saved = main.getFilePointer();
            current[0] = this;
            main.seek(saved);
            saved = -1;
        }
    }
    
    // java.io.InputStream interface

    public int
    read() throws IOException { restore(); return main.read(); }

    public int
    read(final byte[] b, final int off, final int len) throws IOException {
        restore(); return main.read(b, off, len);
    }

    public long
    skip(final long n) throws IOException {
        restore(); return main.skipBytes((int)Math.min(n, Integer.MAX_VALUE));
    }

    public int
    available() { return 0; }

    public void
    close() throws IOException { if (top) { main.close(); } }

    public boolean
    markSupported() { return true; }

    public void
    mark(final int readlimit) {
        if (this == current[0]) {
            try {
                marked = main.getFilePointer();
            } catch (final IOException e) {
                marked = -1;
            }
        } else {
            marked = saved;
        }
    }

    public void
    reset() throws IOException {
        if (-1 == marked) { throw new IOException(); }
        if (this == current[0]) {
            main.seek(marked);
        } else {
            saved = marked;
        }
    }
    
    // org.waterken.archive.n2v.Cursor interface
    
    public long
    getLength() throws IOException { return main.length(); }
    
    public long
    getPosition() throws IOException {
        return this == current[0] ? main.getFilePointer() : saved;
    }
    
    public void
    writeTo(final long off, final long len,
            final WritableByteChannel out) throws IOException {
        if (len != main.getChannel().transferTo(off, len, out)) {
            throw new IOException();
        }
    }
    
    public void
    jump(final long address) throws IOException {
        if (this == current[0]) {
            main.seek(address);
        } else {
            saved = address;
        }
    }
    
    public Cursor
    fork() throws IOException {
        if (this == current[0]) {
            saved = main.getFilePointer();
            return current[0] = new Cursor(current, main, -1, marked);
        }
        return new Cursor(current, main, saved, marked);
    }
    
    public long
    readFixedLong(final int size) throws IOException {
        restore();
        return Byte.SIZE / Byte.SIZE == size
            ? main.readByte()
        : Short.SIZE / Byte.SIZE == size
            ? main.readShort()
        : Integer.SIZE / Byte.SIZE == size
            ? main.readInt()
        : Long.SIZE / Byte.SIZE == size
            ? main.readLong()
        : 1 / 0;
    }
}
