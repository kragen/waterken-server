// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * An {@link N2V} archive creator.
 */
public class
N2VOutput {

    private final OutputStream out;
    private       long total = 0;
    private final ByteArrayBuilder meta = new ByteArrayBuilder(2048);
    private       Integer[] offsets = new Integer[32];
    private       int offsetCount = 0;
    
    /**
     * Constructs an instance.
     * @param out   output stream
     * @throws IOException  any I/O problem
     */
    public
    N2VOutput(final OutputStream out) throws IOException {
        this.out = out;
        
        writeFixedLong(out, N2V.magicSize, N2V.beginMagic);
        total += N2V.magicSize;
    }
    
    /**
     * Creates an archive in a new file.
     * @param file  file to create
     * @throws IOException  any I/O problem
     */
    static public N2VOutput
    create(final File file) throws IOException {
        if (!file.createNewFile()) { throw new IOException(); }
        return new N2VOutput(new FileOutputStream(file));
    }
    
    // org.waterken.archive.n2v.N2VOutput interface
    
    private       OutputStream current = null;
    
    /**
     * Appends an entry to the archive.
     * @param path  path of file to append
     * @return output stream for file data
     */
    public OutputStream
    append(final String path) {
        if (null != current) { throw new RuntimeException(); }
        return current = new OutputStream() {
            
            private long length = 0;
            
            public void
            write(final int b) throws IOException {
                if (this != current) { throw new RuntimeException(); }
                out.write(b);
                ++length;
            }

            public void
            write(final byte[] b) throws IOException {
                if (this != current) { throw new RuntimeException(); }
                out.write(b);
                length += b.length;
            }

            public void
            write(final byte[] b,final int off,final int len)throws IOException{
                if (this != current) { throw new RuntimeException(); }
                out.write(b, off, len);
                length += len;
            }

            public void
            flush() {}

            public void
            close() throws UnsupportedEncodingException {
                if (this == current) {
                    current = null;
                    if (offsets.length == offsetCount) {
                        System.arraycopy(offsets, 0,
                            offsets=new Integer[2*offsetCount], 0, offsetCount);
                    }
                    offsets[offsetCount++] = Integer.valueOf(meta.size());
                    appendSummary(path, length,
                                  0 != length ? total : N2V.emptyFileAddress);
                    total += length;
                }
            }
        };
    }
    
    private void
    appendSummary(final String path, final long length,
                  final long address) throws UnsupportedEncodingException {
        meta.write(path.getBytes("UTF-8"));
        meta.write(0);
        writeExtensionLong(meta, length);   // valueLength
        writeExtensionLong(meta, address);  // valueStart
        writeExtensionLong(meta, 0);        // commentLength
    }
    
    public void
    appendDirectory(final String path) throws IOException {
        if (null != current) { throw new RuntimeException(); }
        appendSummary(path, 0, N2V.directoryAddress);
    }
    
    /**
     * Finish the archive.
     * @throws IOException  any I/O problem
     */
    public void
    finish() throws IOException {
        if (null != current) { throw new RuntimeException(); }
        current = new ByteArrayOutputStream(0);
        
        final long summaryAddress = total;
        final long indexAddress = total + meta.size();
        
        Arrays.sort(offsets, 0, offsetCount, new Comparator<Integer>() {
            public int compare(final Integer a, final Integer b) {
                int iv = 1;
                int jv = 1;
                for (int i = a, j = b; iv == jv && 0 != iv; ++i, ++j) {
                    iv = 0xFF & meta.get(i);
                    jv = 0xFF & meta.get(j);
                }
                return iv - jv;
            }
        });
        final int offsetSize = N2V.sizeof(offsetCount);
        for (int i = 0; i != offsetCount; ++i) {
            writeFixedLong(meta, offsetSize, offsets[i]);
        }
        
        total += meta.size();
        final int addressSize =
            N2V.sizeof(total + 3 * N2V.sizeof(total) + N2V.magicSize);
        writeFixedLong(meta, addressSize, offsetCount);
        writeFixedLong(meta, addressSize, indexAddress);
        writeFixedLong(meta, addressSize, summaryAddress);
        writeFixedLong(meta, N2V.magicSize, N2V.endMagic);

        meta.writeTo(out);
    }
    
    static private void
    writeExtensionLong(final ByteArrayBuilder out, final long n) {
        int todo = 9 * 7;                   // number of bits to output
        long mask = 0x7FL << (todo -= 7);   // mask for next output bits
        for (; 0L == (n & mask) && 0 != todo; todo -= 7, mask >>= 7) {}
        for (;                     0 != todo; todo -= 7, mask >>= 7) {
            out.write(0x80 | (int)((n & mask) >> todo));
        }
        out.write((int)(n & mask));
    }
    
    static private void
    writeFixedLong(final OutputStream out,
                   final int size, final long n) throws IOException {
        int todo = size * Byte.SIZE;                // number of bits to output
        long mask = 0xFFL << (todo -= Byte.SIZE);   // mask for next output bits
        for (; 0L != mask; todo -= Byte.SIZE, mask >>>= Byte.SIZE) {
            out.write((int)((n & mask) >>> todo));
        }
    }
}
