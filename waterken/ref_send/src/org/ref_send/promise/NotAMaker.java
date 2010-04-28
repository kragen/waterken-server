// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import org.joe_e.Powerless;
import org.joe_e.reflect.Reflection;
import org.ref_send.Record;
import org.ref_send.deserializer;

/**
 * Indicates the class provided to {@link Eventual#spawn spawn} is not a Maker.
 * <p>
 * A Maker class <strong>MUST</strong>:
 * </p>
 * <ul>
 * <li>be declared in a {@link org.joe_e.IsJoeE Joe-E} package</li>
 * <li>have a single <code>public static</code> method named
 *     "<code>make</code>"</li>
 * </ul>
 */
public class
NotAMaker extends NullPointerException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    NotAMaker() {}

    /**
     * Constructs an instance.
     */
    public @deserializer
    NotAMaker(final Class<?> maker) {
        super(Reflection.getName(maker));
    }
}
