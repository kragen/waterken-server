// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.udp;

import static org.joe_e.file.Filesystem.file;
import static org.ref_send.promise.Eventual.near;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.SocketAddress;

import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Do;
import org.waterken.db.Database;
import org.waterken.db.DatabaseManager;
import org.waterken.db.Root;
import org.waterken.db.Transaction;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;
import org.waterken.udp.UDPDaemon;
import org.waterken.uri.Header;

/**
 * A DNS name server.
 */
public final class
NameServer extends UDPDaemon {
    static private final long serialVersionUID = 1L;

    private final File master;
    private final DatabaseManager<?> dbs;
    
    /**
     * Constructs an instance.
     * @param port      {@link #port}
     * @param master    root persistence folder
     */
    public @deserializer
    NameServer(@name("port") final int port,
               @name("master") final File master,
               @name("dbs") final DatabaseManager<?> dbs) {
        super(port);
        this.master = master;
        this.dbs = dbs;
    }
    
    // org.waterken.udp.Daemon interface

    public void
    accept(final SocketAddress from, final ByteArray msg,
           final Do<ByteArray,?> respond) throws Exception {
        final ByteArray response;
        try {
            response = process(msg);
        } catch (final Exception e) {
            final byte[] header = respond(msg.toByteArray());
            header[3] |= 2;
            respond.fulfill(ByteArray.array(header));
            return;
        }
        respond.fulfill(response);
    }
    
    // org.waterken.dns.udp.NameServer interface
    
    /**
     * address of the first question
     */
    static private final int QP = 0xC000 | 12;
    
    private ByteArray
    process(final ByteArray msg) throws Exception {
        final byte[] in = msg.toByteArray();
        final byte[] header = respond(in);

        // do some sanity checking
        if (0 != (in[2] & 0x02)) { throw new Exception(); } // TC bit set
        if (0 != (in[2] & 0x80)) { throw new Exception(); } // response bit set
        if (0 != (in[2] & 0x78)) {
            // unsupported query opcode
            header[3] |= 4; // set the RCODE to Not Implemented
            return ByteArray.array(header);
        }
        // by convention, only a single query is ever sent
        if (!(0 == in[4] && 1 == in[5])) { throw new Exception(); }

        // standard query
        
        // parse the question
        final String qname;
        final short qtype, qclass;
        final int qlen; {
            final StringBuilder buffer = new StringBuilder();
            int i = header.length;
            while (true) {
                final int length = in[i++] & 0xFF;
                // since we're only accepting a single query, assume
                // it's unreasonable to use DNS compression
                if (0x00 != (length & 0xC0)) { throw new Exception(); }
                if (0 == length) { break; }
                if (0 != buffer.length()) { buffer.append('.'); }
                buffer.append(ASCII.decode(in, i, length));
                i += length;
            }
            qname = buffer.toString();
            qtype = (short)(((in[i++] & 0xFF) << 8) | ( in[i++] & 0xFF));
            qclass = (short)(((in[i++] & 0xFF) << 8) | ( in[i++] & 0xFF));
            qlen = i - header.length;
        }
        
        // see if we've got any answers
        final PowerlessArray<ByteArray> answers;
        try {
            answers= dbs.connect(file(master, Header.toLowerCase(qname))).enter(
                Transaction.query, new Transaction<PowerlessArray<ByteArray>>(){
                public PowerlessArray<ByteArray>
                run(final Root local) throws Exception {
                    final Menu<ByteArray> top = local.fetch(null, Database.top);
                    final PowerlessArray.Builder<ByteArray> r =
                        PowerlessArray.builder(1);
                    for (final ByteArray x : near(top.getSnapshot())) {
                        r.append(x);
                    }
                    return r.snapshot();
                }
            }).cast();
        } catch (final Exception e) {
            header[3] |= (e instanceof FileNotFoundException ? 3 : 2);
            return ByteArray.array(header);
        }

        // encode the corresponding answers
        final ByteArray.Builder response = ByteArray.builder(512); 
        response.append(header);                    // output a header
        response.append(in, header.length, qlen);   // echo the question
        int ancount = 0;
        boolean truncated = false;
        for (final ByteArray a : answers) {
            if ((255 == qtype || qtype == Resource.type(a)) &&
                (255 == qclass || qclass == Resource.clazz(a))) {
                if (response.length() + 12 + Resource.length(a) > 512) {
                    truncated = true;
                    continue;
                }
                
                response.append((byte)(QP >>> 8));
                response.append((byte)(QP      ));
                response.append(a.toByteArray());
                
                ++ancount;
            }
        }
        final byte[] out = response.snapshot().toByteArray();
        out[2] |= 0x04;         // set the AA bit
        if (truncated) {
            out[2] |= 0x20;     // set the TC bit
        }
        out[5] = 1;             // qdcount = 1
        out[6] = (byte)(ancount >>> 8);
        out[7] = (byte)(ancount      );
        return ByteArray.array(out);
    }
    
    static private byte[]
    respond(final byte[] in) {
        final byte[] header = {
            in[0], in[1],   // id
            in[2], in[3],   // flags
            0x00, 0x00,     // qdcount
            0x00, 0x00,     // ancount
            0x00, 0x00,     // nscount
            0x00, 0x00      // arcount
        };
        header[2] |= 0x80;  // set the QR bit
        header[2] &= 0xFB;  // clear the AA bit
        header[2] &= 0xFD;  // clear the TC bit
        header[3] &= 0x7F;  // clear the RA bit
        header[3] &= 0x8F;  // clear the Z bits
        header[3] &= 0xF0;  // clear the RCODE bits
        return header;
    }
}
