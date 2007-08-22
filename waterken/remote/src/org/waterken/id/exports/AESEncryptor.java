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
AESEncryptor extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    static private final ByteArray S;
    static private final IntArray T1;
    static private final IntArray T2;
    static private final IntArray T3;
    static private final IntArray T4;
    static {
        // calculate S-boxes and T-boxes
        final byte[] STmp  = new byte[256];
        final int[] T1Tmp = new int[256];
        final int[] T2Tmp = new int[256];
        final int[] T3Tmp = new int[256];
        final int[] T4Tmp = new int[256];
        for (int i = 0; i < 256; i++) {
            final char c = AES.SS.charAt(i >>> 1);
            STmp[i] = (byte)(((i & 1) == 0) ? c >>> 8 : c & 0xFF);
            final int s = STmp[i] & 0xFF;
            int s2 = s << 1;
            if (s2 >= 0x100) {
                s2 ^= AES.ROOT;
            }
            final int s3 = s2 ^ s;

            final int t = (s2 << 24) | (s << 16) | (s << 8) | s3;
            T1Tmp[i] = t; 
            T2Tmp[i] = (t >>>  8) | (t << 24);
            T3Tmp[i] = (t >>> 16) | (t << 16);
            T4Tmp[i] = (t >>> 24) | (t <<  8);
        }

        // Assign static constants.
        S = ByteArray.array(STmp);
        T1 = IntArray.array(T1Tmp);
        T2 = IntArray.array(T2Tmp);
        T3 = IntArray.array(T3Tmp);
        T4 = IntArray.array(T4Tmp);
    }

    static private final ByteArray rcon;
    static {
        // round constants
        final byte[] rconTmp = new byte[30];
        int r = 1;
        rconTmp[0] = 1;
        for (int i = 1; i < 30; i++) {
            r <<= 1;
            if (r >= 0x100) {
                r ^= AES.ROOT;
            }
            rconTmp[i] = (byte)r;
        }

        // Assign static constants.
        rcon = ByteArray.array(rconTmp);
    }

    /**
     * Expand a user-supplied key material into a session key.
     * @param keyBytes  128/192/256-bit user-key
     * @exception IllegalArgumentException  <code>keyBytes</code> invalid
     */
    static int[]
    makeKey(final byte[] keyBytes) {
        final int rounds          = getRounds(keyBytes.length);
        final int roundKeyCount = (rounds + 1) * 4;

        final int[] K = new int[roundKeyCount];

        final int KC = keyBytes.length / 4; // keylen in 32-bit elements
        final int[] tk = new int[KC];

        int i, j;

        // copy user material bytes into temporary ints
        for (i = 0, j = 0; i < KC; ) {
            tk[i++] = (keyBytes[j++]       ) << 24 |
                      (keyBytes[j++] & 0xFF) << 16 |
                      (keyBytes[j++] & 0xFF) <<  8 |
                      (keyBytes[j++] & 0xFF);
        }

        // copy values into round key arrays
        int t = 0;
        for ( ; t < KC; t++) { K[t] = tk[t]; }

        int tt, rconpointer = 0;
        while (t < roundKeyCount) {
            // extrapolate using phi (the round key evolution function)
            tt = tk[KC - 1];
            tk[0] ^= (S.get((tt >>> 16) & 0xFF)       ) << 24 ^
                     (S.get((tt >>>  8) & 0xFF) & 0xFF) << 16 ^
                     (S.get((tt       ) & 0xFF) & 0xFF) <<  8 ^
                     (S.get((tt >>> 24)       ) & 0xFF)       ^
                     (rcon.get(rconpointer++)         ) << 24;
            if (KC != 8) {
                for (i = 1, j = 0; i < KC;) { tk[i++] ^= tk[j++]; }
            } else {
                for (i = 1, j = 0; i < KC / 2;) { tk[i++] ^= tk[j++]; }
                tt = tk[KC / 2 - 1];
                tk[KC / 2] ^= (S.get((tt       ) & 0xFF) & 0xFF)       ^
                              (S.get((tt >>>  8) & 0xFF) & 0xFF) <<  8 ^
                              (S.get((tt >>> 16) & 0xFF) & 0xFF) << 16 ^
                              (S.get((tt >>> 24)       )       ) << 24;
                for (j = KC / 2, i = j + 1; i < KC;) { tk[i++] ^= tk[j++]; }
            }

            // copy values into round key arrays
            for (j = 0; (j < KC) && (t < roundKeyCount); j++, t++) {
                K[t] = tk[j];
            }
        }

        return K;
    }

    /**
     * Return the number of rounds for a given Rijndael keysize.
     * @param keySize  size of the user key material in bytes,
     *                 MUST be one of (16, 24, 32)
     * @return number of rounds
     */
    static private int
    getRounds(final int keySize) { return (keySize >> 2) + 6; }

    // Per key state
    
    /** (ROUNDS-1) * 4 */
    private final int limit;

    /** Subkeys */
    private final IntArray K;
    
    AESEncryptor(final byte[] key) {
        final int len = key.length;
        if (len != 16 && len != 24 && len != 32 ) {
            throw new RuntimeException("Invalid user key length");
        }

        limit = getRounds(len) * 4;
        K = IntArray.array(makeKey(key));
    }

    /**
     * Encrypt exactly one block of plaintext.
     */
    void
    run(final byte[] in, int inOffset, final byte[] out, int outOffset) {

        // plaintext to ints + key
        int keyOffset = 0;
        int t0   = ((in[inOffset++]       ) << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) <<  8 |
                    (in[inOffset++] & 0xFF)        ) ^ K.get(keyOffset++);
        int t1   = ((in[inOffset++]       ) << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) <<  8 |
                    (in[inOffset++] & 0xFF)        ) ^ K.get(keyOffset++);
        int t2   = ((in[inOffset++]       ) << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) <<  8 |
                    (in[inOffset++] & 0xFF)        ) ^ K.get(keyOffset++);
        int t3   = ((in[inOffset++]       ) << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) <<  8 |
                    (in[inOffset++] & 0xFF)        ) ^ K.get(keyOffset++);

        // apply round transforms
        while( keyOffset < limit ) {
            int a0, a1, a2;
            a0 = T1.get((t0 >>> 24)       ) ^
                 T2.get((t1 >>> 16) & 0xFF) ^
                 T3.get((t2 >>>  8) & 0xFF) ^
                 T4.get((t3       ) & 0xFF) ^ K.get(keyOffset++);
            a1 = T1.get((t1 >>> 24)       ) ^
                 T2.get((t2 >>> 16) & 0xFF) ^
                 T3.get((t3 >>>  8) & 0xFF) ^
                 T4.get((t0       ) & 0xFF) ^ K.get(keyOffset++);
            a2 = T1.get((t2 >>> 24)       ) ^
                 T2.get((t3 >>> 16) & 0xFF) ^
                 T3.get((t0 >>>  8) & 0xFF) ^
                 T4.get((t1       ) & 0xFF) ^ K.get(keyOffset++);
            t3 = T1.get((t3 >>> 24)       ) ^
                 T2.get((t0 >>> 16) & 0xFF) ^
                 T3.get((t1 >>>  8) & 0xFF) ^
                 T4.get((t2       ) & 0xFF) ^ K.get(keyOffset++);
            t0 = a0; t1 = a1; t2 = a2;
        }

        // last round is special
        int tt = K.get(keyOffset++);
        out[outOffset++] = (byte)(S.get((t0 >>> 24)       ) ^ (tt >>> 24));
        out[outOffset++] = (byte)(S.get((t1 >>> 16) & 0xFF) ^ (tt >>> 16));
        out[outOffset++] = (byte)(S.get((t2 >>>  8) & 0xFF) ^ (tt >>>  8));
        out[outOffset++] = (byte)(S.get((t3       ) & 0xFF) ^ (tt       ));
        tt = K.get(keyOffset++);
        out[outOffset++] = (byte)(S.get((t1 >>> 24)       ) ^ (tt >>> 24));
        out[outOffset++] = (byte)(S.get((t2 >>> 16) & 0xFF) ^ (tt >>> 16));
        out[outOffset++] = (byte)(S.get((t3 >>>  8) & 0xFF) ^ (tt >>>  8));
        out[outOffset++] = (byte)(S.get((t0       ) & 0xFF) ^ (tt       ));
        tt = K.get(keyOffset++);
        out[outOffset++] = (byte)(S.get((t2 >>> 24)       ) ^ (tt >>> 24));
        out[outOffset++] = (byte)(S.get((t3 >>> 16) & 0xFF) ^ (tt >>> 16));
        out[outOffset++] = (byte)(S.get((t0 >>>  8) & 0xFF) ^ (tt >>>  8));
        out[outOffset++] = (byte)(S.get((t1       ) & 0xFF) ^ (tt       ));
        tt = K.get(keyOffset++);
        out[outOffset++] = (byte)(S.get((t3 >>> 24)       ) ^ (tt >>> 24));
        out[outOffset++] = (byte)(S.get((t0 >>> 16) & 0xFF) ^ (tt >>> 16));
        out[outOffset++] = (byte)(S.get((t1 >>>  8) & 0xFF) ^ (tt >>>  8));
        out[outOffset  ] = (byte)(S.get((t2       ) & 0xFF) ^ (tt       ));
    }
}
