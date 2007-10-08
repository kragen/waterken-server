// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.joe_e.file.Filesystem;
import org.waterken.uri.Base32;

/**
 * A self-signed certificate generator.
 */
final class
GenKeyRSA {

    private
    GenKeyRSA() {}
    
    /**
     * @param args
     */
    static public void
    main(final String[] args) throws Exception {
        final int keysize = 0 < args.length ? Integer.parseInt(args[0]) : 1024;
        final int strength =
            (1024 >= keysize
                ? 80
            : (2048 >= keysize
                ? 112
            : 128)) / Byte.SIZE;
        final String suffix = 1 < args.length ? args[1] : ".yurl.net";
        System.err.println("Generating RSA key pair...");
        System.err.println("at domain: " + suffix);
        System.err.println("with keysize: " + keysize);
        System.err.println("and hash length: " + (strength * Byte.SIZE));

        // generate a new key pair
        final KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(keysize);
        final KeyPair p = g.generateKeyPair();
        final byte[] subjectPublicKeyInfo = p.getPublic().getEncoded();
        
        // produce the DER encoded CN field
        final byte[] cn; {
            
            // calculate the hostname
            final MessageDigest MD5 = MessageDigest.getInstance("MD5");
            final byte[] fingerprint = MD5.digest(subjectPublicKeyInfo);
            final byte[] guid = new byte[strength];
            System.arraycopy(fingerprint, 0, guid, 0, strength);
            final String label = "y-" + Base32.encode(guid);
            final byte[] hostname = (label + suffix).getBytes("US-ASCII");

            final DER out = new DER(11 + hostname.length);
            out.writeValue(hostname);
            out.writeLen();
            out.writeByte(0x13);
            out.writeByte(0x03);
            out.writeByte(0x04);
            out.writeByte(0x55);
            out.writeByte(0x03);
            out.writeByte(0x06);
            out.writeLen();
            out.writeByte(0x30);
            out.writeLen();
            out.writeByte(0x31);
            cn = out.toByteArray();
        }
                
        // produce the subject Name
        final byte[] subject; {
            final DER out = new DER(2 + cn.length);
            out.writeValue(cn);
            out.writeLen();
            out.writeByte(0x30);
            subject = out.toByteArray();
        }

        // produce the constant fields
        final byte[] version = { (byte)0xa0, 0x03, 0x02, 0x01, 0x02 };
        final byte[] serialNumber = { 0x02, 0x04, 0x00, 0x00, 0x00, 0x01 };
        final byte[] signatureAlgorithm = {
            0x30, 0x0d, 0x06, 0x09, 0x2a, (byte)0x86, 0x48, (byte)0x86,
            (byte)0xf7, 0x0d, 0x01, 0x01, 0x04, 0x05, 0x00
        };
        final byte[] issuer = subject;
        
        // produce the validity
        final byte[] validity; {
            final byte[] start = "0710070001Z".getBytes("US-ASCII");
            final byte[] end = "9912312359Z".getBytes("US-ASCII");
            validity = new byte[2 + 2 + start.length + 2 + end.length];
            int i = 0;
            validity[i++] = 0x30;
            validity[i++] = (byte)(validity.length - 2);
            validity[i++] = 0x17;
            validity[i++] = (byte)start.length;
            System.arraycopy(start, 0, validity, i, start.length);
            i += start.length;
            validity[i++] = 0x17;
            validity[i++] = (byte)end.length;
            System.arraycopy(end, 0, validity, i, end.length);
            i += end.length;
        }
        
        // produce the tbsCertificate
        final byte[] tbsCertificate; {
            final DER out = new DER(4 +
                                    version.length +
                                    serialNumber.length +
                                    signatureAlgorithm.length +
                                    issuer.length +
                                    validity.length +
                                    subject.length +
                                    subjectPublicKeyInfo.length);
            out.writeValue(subjectPublicKeyInfo);
            out.writeValue(subject);
            out.writeValue(validity);
            out.writeValue(issuer);
            out.writeValue(signatureAlgorithm);
            out.writeValue(serialNumber);
            out.writeValue(version);
            out.writeLen();
            out.writeByte(0x30);
            tbsCertificate = out.toByteArray();
        }
        
        // calculate the signature
        final Signature MD5withRSA = Signature.getInstance("MD5withRSA");
        MD5withRSA.initSign(p.getPrivate());
        MD5withRSA.update(tbsCertificate);
        final byte[] signatureBitstring = MD5withRSA.sign();
        final byte[] signature; {
            final DER out = new DER(4 + signatureBitstring.length);
            out.writeValue(signatureBitstring);
            out.writeByte(0x00);
            out.writeLen();
            out.writeByte(0x03);
            signature = out.toByteArray();
        }
        
        // produce the certificate
        final byte[] certificate; {
            final DER out = new DER(4 +
                                    tbsCertificate.length +
                                    signatureAlgorithm.length +
                                    signature.length);
            out.writeValue(signature);
            out.writeValue(signatureAlgorithm);
            out.writeValue(tbsCertificate);
            out.writeLen();
            out.writeByte(0x30);
            certificate = out.toByteArray();
        }
        
        // parse the certificate
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final Certificate cert = cf.generateCertificate(
                new ByteArrayInputStream(certificate));
        
        // store the private key and certificate
        final char[] password = "nopass".toCharArray();
        final KeyStore keys = KeyStore.getInstance(KeyStore.getDefaultType());
        keys.load(null, password);
        keys.setKeyEntry("mykey", p.getPrivate(), password,
                         new Certificate[] { cert });
        final OutputStream fout = Filesystem.writeNew(new File("keys.jks"));
        keys.store(fout, password);
        fout.close();
    }
    
    static private class
    DER {
        private byte[] buffer;
        private int i;
        
        DER(final int estimate) {
            buffer = new byte[estimate];
            i = buffer.length;
        }
        
        byte[]
        toByteArray() {
            if (0 != i) {
                final int n = buffer.length - i;
                System.arraycopy(buffer, i, buffer = new byte[n], i = 0, n);
            }
            return buffer;
        }
        
        void
        writeByte(int b) {
            if (0 == i) {
                i = buffer.length;
                System.arraycopy(buffer, 0, buffer = new byte[2 * i], i, i);
            }
            buffer[--i] = (byte)b;
        }
        
        void
        writeValue(final byte[] v) {
            if (v.length > i) {
                final int n = buffer.length - i;
                final int l = 2 * (v.length + n);
                System.arraycopy(buffer, i, buffer = new byte[l], i = l - n, n);
            }
            System.arraycopy(v, 0, buffer, i -= v.length, v.length);
        }
        
        void
        writeLen() {
            final int length = buffer.length - i;
            if (length <= 0x7F) {
                writeByte(length);
            } else {
                final int bytes =
                    length <= 0xFF
                        ? 1
                    : (length <= 0xFFFF
                        ? 2
                    : (length <= 0xFFFFFF
                        ? 3
                    : 4));
                for (int i = 0; i != bytes; ++i) {
                    writeByte(length >> (i * Byte.SIZE));
                }
                writeByte(0x80 | bytes);
            }
        }
    }
}
