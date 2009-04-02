// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.joe_e.file.InvalidFilenameException;
import org.waterken.archive.ArchiveOutput;

/**
 * An {@link N2V} archive creator.
 */
public final class
N2VOutput implements ArchiveOutput {

    private final OutputStream out;
    private       long total = 0;
    private final ByteArrayBuilder meta = new ByteArrayBuilder(2048);
    private       Offset[] offsets = new Offset[32];
    private       int offsetCount = 0;
    
    static private final class
    Offset {
        protected final long data;
        protected final int summary;
        
        Offset(final long data, final int summary) {
            this.data = data;
            this.summary = summary;
        }
    }
    
    /**
     * Constructs an instance.
     * @param out   output stream
     */
    public
    N2VOutput(final OutputStream out) {
        this.out = out;
    }
    
    // org.waterken.archive.n2v.N2VOutput interface
    
    private       OutputStream current = null;
    
    public OutputStream
    append(final String name) {
        if (-1 != name.indexOf('\0')) { throw new InvalidFilenameException(); }
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
            close() throws IOException {
                if (this == current) {
                    current = null;
                    if (offsets.length == offsetCount) {
                        System.arraycopy(offsets, 0,
                            offsets= new Offset[2*offsetCount], 0, offsetCount);
                    }
                    offsets[offsetCount++] = new Offset(total, meta.size());
                    meta.write(name.getBytes("UTF-8"));
                    meta.write(0);
                    N2V.writeExtensionLong(meta, length);   // valueLength
                    N2V.writeExtensionLong(meta, 0);        // commentLength
                    total += length;
                }
            }
        };
    }
    
    public void
    finish() throws IOException {
        if (null != current) { throw new RuntimeException(); }
        current = new ByteArrayOutputStream(0);
        
        final long summaryAddress = total;
        final long indexAddress = total + meta.size();
        final int summaryOffsetSize = N2V.sizeof(meta.size());
        final int dataOffsetSize = N2V.sizeof(summaryAddress);
        
        Arrays.sort(offsets, 0, offsetCount, new Comparator<Offset>() {
            public int
            compare(final Offset a, final Offset b) {
                int iv = 1;
                int jv = 1;
                for (int i=a.summary, j=b.summary; iv==jv && 0!=iv; ++i, ++j) {
                    iv = 0xFF & meta.get(i);
                    jv = 0xFF & meta.get(j);
                }
                return iv - jv;
            }
        });
        for (int i = 0; i != offsetCount; ++i) {
            N2V.writeFixedLong(meta, dataOffsetSize, offsets[i].data);
            N2V.writeFixedLong(meta, summaryOffsetSize, offsets[i].summary);
        }
        
        total += meta.size();
        final int addressSize =
            N2V.sizeof(total + 2 * N2V.sizeof(total) + N2V.magicSize);
        N2V.writeFixedLong(meta, addressSize, indexAddress);
        N2V.writeFixedLong(meta, addressSize, summaryAddress);
        N2V.writeFixedLong(meta, N2V.magicSize, N2V.endMagic);

        meta.writeTo(out);
        out.flush();
        out.close();
    }
}
