/* $Id: TestRijndael.java,v 1.5 2000/07/28 20:06:11 gelderen Exp $
 *
 * Copyright (C) 1995-1999 The Cryptix Foundation Limited.
 * All rights reserved.
 *
 * Use, modification, copying and distribution of this software is subject
 * the terms and conditions of the Cryptix General Licence. You should have
 * received a copy of the Cryptix General Licence along with this library;
 * if not, you can download a copy from http://www.cryptix.org/ .
 */
package org.waterken.id.exports;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.IntArray;

final class
AESDecryptor extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    static private final ByteArray Si;
    static private final IntArray T5;
    static private final IntArray T6;
    static private final IntArray T7;
    static private final IntArray T8;

    static private final IntArray U1;
    static private final IntArray U2;
    static private final IntArray U3;
    static private final IntArray U4;
    static {
        final byte[] SiTmp = new byte[256];
        final int[] T5Tmp = new int[256];
        final int[] T6Tmp = new int[256];
        final int[] T7Tmp = new int[256];
        final int[] T8Tmp = new int[256];
        final int[] U1Tmp = new int[256];
        final int[] U2Tmp = new int[256];
        final int[] U3Tmp = new int[256];
        final int[] U4Tmp = new int[256];
        for (int i = 0; i < 256; i++) {
            final char c = AES.SS.charAt(i >>> 1);
            final int s = (((i & 1) == 0) ? c >>> 8 : c & 0xFF) & 0xFF;
            SiTmp[s] = (byte)i;
            int i2 = i << 1;
            if (i2 >= 0x100) {
                i2 ^= AES.ROOT;
            }
            int i4 = i2 << 1;
            if (i4 >= 0x100) {
                i4 ^= AES.ROOT;
            }
            int i8 = i4 << 1;
            if (i8 >= 0x100) {
                i8 ^= AES.ROOT;
            }
            final int i9 = i8 ^ i;
            final int ib = i9 ^ i2;
            final int id = i9 ^ i4;
            final int ie = i8 ^ i4 ^ i2;

            final int t = (ie << 24) | (i9 << 16) | (id << 8) | ib;
            T5Tmp[s] = U1Tmp[i] = t;
            T6Tmp[s] = U2Tmp[i] = (t >>>  8) | (t << 24);
            T7Tmp[s] = U3Tmp[i] = (t >>> 16) | (t << 16);
            T8Tmp[s] = U4Tmp[i] = (t >>> 24) | (t <<  8);
        }

        // Assign static constants.
        Si = ByteArray.array(SiTmp);
        T5 = IntArray.array(T5Tmp);
        T6 = IntArray.array(T6Tmp);
        T7 = IntArray.array(T7Tmp);
        T8 = IntArray.array(T8Tmp);

        U1 = IntArray.array(U1Tmp);
        U2 = IntArray.array(U2Tmp);
        U3 = IntArray.array(U3Tmp);
        U4 = IntArray.array(U4Tmp);
    }

    static private void
    invertKey(final int[] K) {

        for (int i = 0; i < K.length / 2 - 4; i += 4) {
            final int jj0 = K[i+0];
            final int jj1 = K[i+1];
            final int jj2 = K[i+2];
            final int jj3 = K[i+3];
            K[i+0] = K[K.length-i-4+0];
            K[i+1] = K[K.length-i-4+1];
            K[i+2] = K[K.length-i-4+2];
            K[i+3] = K[K.length-i-4+3];
            K[K.length-i-4+0] = jj0;
            K[K.length-i-4+1] = jj1;
            K[K.length-i-4+2] = jj2;
            K[K.length-i-4+3] = jj3;
        }
        
        for (int r = 4; r < K.length-4; r++) {
            final int tt = K[r];
            K[r] = U1.get((tt >>> 24) & 0xFF) ^
                   U2.get((tt >>> 16) & 0xFF) ^
                   U3.get((tt >>>  8) & 0xFF) ^
                   U4.get( tt         & 0xFF);
        }

        final int j0 = K[K.length-4];
        final int j1 = K[K.length-3];
        final int j2 = K[K.length-2];
        final int j3 = K[K.length-1];
        for (int i = K.length - 1; i > 3; i--) { K[i] = K[i-4]; }
        K[0] = j0;
        K[1] = j1;
        K[2] = j2;
        K[3] = j3;
    }

    // Per key state
    
    private final boolean rounds12, rounds14;

    /** Subkeys */
    private final IntArray K;
    
    AESDecryptor(final byte[] key) {
        final int len = key.length;
        if (len != 16 && len != 24 && len != 32 ) {
            throw new RuntimeException("Invalid user key length");
        }

        rounds12 = (len >= 24);
        rounds14 = (len == 32);
        final int[] KTmp = AESEncryptor.makeKey(key);
        invertKey(KTmp);
        K = IntArray.array(KTmp);
    }

    /**
     * Decrypt exactly one block of plaintext.
     */
    void
    run(final byte[] in, int inOffset, final byte[] out, int outOffset) {

        int keyOffset = 8;
        int t0, t1, t2, t3, a0, a1, a2;

        t0 = ((in[inOffset++]       ) << 24 |
              (in[inOffset++] & 0xFF) << 16 |
              (in[inOffset++] & 0xFF) <<  8 |
              (in[inOffset++] & 0xFF)        ) ^ K.get(4);
        t1 = ((in[inOffset++]       ) << 24 |
              (in[inOffset++] & 0xFF) << 16 |
              (in[inOffset++] & 0xFF) <<  8 |
              (in[inOffset++] & 0xFF)        ) ^ K.get(5);
        t2 = ((in[inOffset++]       ) << 24 |
              (in[inOffset++] & 0xFF) << 16 |
              (in[inOffset++] & 0xFF) <<  8 |
              (in[inOffset++] & 0xFF)        ) ^ K.get(6);
        t3 = ((in[inOffset++]       ) << 24 |
              (in[inOffset++] & 0xFF) << 16 |
              (in[inOffset++] & 0xFF) <<  8 |
              (in[inOffset  ] & 0xFF)        ) ^ K.get(7);

        if (rounds12) {
            a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
                 T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
                 K.get(keyOffset++);
            a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
                 T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
                 K.get(keyOffset++);
            a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
                 T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
                 K.get(keyOffset++);
            t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
                 T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
                 K.get(keyOffset++);
            t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
                 T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
                 K.get(keyOffset++);
            t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
                 T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
                 K.get(keyOffset++);
            t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
                 T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
                 K.get(keyOffset++);
            t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
                 T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
                 K.get(keyOffset++);

            if (rounds14) {
                a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
                     T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
                     K.get(keyOffset++);
                a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
                     T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
                     K.get(keyOffset++);
                a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
                     T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
                     K.get(keyOffset++);
                t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
                     T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
                     K.get(keyOffset++);
                t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
                     T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
                     K.get(keyOffset++);
                t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
                     T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
                     K.get(keyOffset++);
                t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
                     T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
                     K.get(keyOffset++);
                t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
                     T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
                     K.get(keyOffset++);
            }
        }
        a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
             K.get(keyOffset++);
        a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
             K.get(keyOffset++);
        a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
             T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
             T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
             K.get(keyOffset++);
        t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
             K.get(keyOffset++);
        t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
             K.get(keyOffset++);
        t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
             T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
             T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
             K.get(keyOffset++);
        a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
             K.get(keyOffset++);
        a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
             K.get(keyOffset++);
        a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
             T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
             T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
             K.get(keyOffset++);
        t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
             K.get(keyOffset++);
        t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
             K.get(keyOffset++);
        t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
             T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
             T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
             K.get(keyOffset++);
        a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
             K.get(keyOffset++);
        a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
             K.get(keyOffset++);
        a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
             T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
             T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
             K.get(keyOffset++);
        t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
             K.get(keyOffset++);
        t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
             K.get(keyOffset++);
        t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
             T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
             T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
             K.get(keyOffset++);
        a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
             K.get(keyOffset++);
        a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
             K.get(keyOffset++);
        a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
             T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
             T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
             K.get(keyOffset++);
        t0 = T5.get((a0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((a2>>> 8)&0xFF) ^ T8.get((a1     )&0xFF) ^
             K.get(keyOffset++);
        t1 = T5.get((a1>>>24)     ) ^ T6.get((a0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((a2     )&0xFF) ^
             K.get(keyOffset++);
        t2 = T5.get((a2>>>24)     ) ^ T6.get((a1>>>16)&0xFF) ^
             T7.get((a0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((a2>>>16)&0xFF) ^
             T7.get((a1>>> 8)&0xFF) ^ T8.get((a0     )&0xFF) ^
             K.get(keyOffset++);
        a0 = T5.get((t0>>>24)     ) ^ T6.get((t3>>>16)&0xFF) ^
             T7.get((t2>>> 8)&0xFF) ^ T8.get((t1     )&0xFF) ^
             K.get(keyOffset++);
        a1 = T5.get((t1>>>24)     ) ^ T6.get((t0>>>16)&0xFF) ^
             T7.get((t3>>> 8)&0xFF) ^ T8.get((t2     )&0xFF) ^
             K.get(keyOffset++);
        a2 = T5.get((t2>>>24)     ) ^ T6.get((t1>>>16)&0xFF) ^
             T7.get((t0>>> 8)&0xFF) ^ T8.get((t3     )&0xFF) ^
             K.get(keyOffset++);
        t3 = T5.get((t3>>>24)     ) ^ T6.get((t2>>>16)&0xFF) ^
             T7.get((t1>>> 8)&0xFF) ^ T8.get((t0     )&0xFF) ^
             K.get(keyOffset++);

        t1 = K.get(0);
        out[outOffset++] = (byte)(Si.get((a0 >>> 24)       ) ^ (t1 >>> 24));
        out[outOffset++] = (byte)(Si.get((t3 >>> 16) & 0xFF) ^ (t1 >>> 16));
        out[outOffset++] = (byte)(Si.get((a2 >>>  8) & 0xFF) ^ (t1 >>>  8));
        out[outOffset++] = (byte)(Si.get((a1       ) & 0xFF) ^ (t1       ));
        t1 = K.get(1);
        out[outOffset++] = (byte)(Si.get((a1 >>> 24)       ) ^ (t1 >>> 24));
        out[outOffset++] = (byte)(Si.get((a0 >>> 16) & 0xFF) ^ (t1 >>> 16));
        out[outOffset++] = (byte)(Si.get((t3 >>>  8) & 0xFF) ^ (t1 >>>  8));
        out[outOffset++] = (byte)(Si.get((a2       ) & 0xFF) ^ (t1       ));
        t1 = K.get(2);
        out[outOffset++] = (byte)(Si.get((a2 >>> 24)       ) ^ (t1 >>> 24));
        out[outOffset++] = (byte)(Si.get((a1 >>> 16) & 0xFF) ^ (t1 >>> 16));
        out[outOffset++] = (byte)(Si.get((a0 >>>  8) & 0xFF) ^ (t1 >>>  8));
        out[outOffset++] = (byte)(Si.get((t3       ) & 0xFF) ^ (t1       ));
        t1 = K.get(3);
        out[outOffset++] = (byte)(Si.get((t3 >>> 24)       ) ^ (t1 >>> 24));
        out[outOffset++] = (byte)(Si.get((a2 >>> 16) & 0xFF) ^ (t1 >>> 16));
        out[outOffset++] = (byte)(Si.get((a1 >>>  8) & 0xFF) ^ (t1 >>>  8));
        out[outOffset  ] = (byte)(Si.get((a0       ) & 0xFF) ^ (t1       ));
    }
}
