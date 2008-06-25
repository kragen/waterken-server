// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import java.security.SecureRandom;

import org.ref_send.log.Anchor;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.web_send.graph.Collision;

/**
 * The roots of a persistent object graph.
 * <p>
 * A {@link Root} provides administrative authority over a {@link Vat}.
 * {@link Vat} infrastructure code can stash hidden state in this mapping
 * by prefixing the binding name with a '<code>.</code>' character.
 * </p>
 */
public interface
Root {

    // known names
    
    /**
     * corresponding persistent {@link ClassLoader}
     */
    String code = ".code";
    
    /**
     * corresponding {@link Creator}
     */
    String creator = ".creator";
    
    /**
     * {@link Runnable deletes} the corresponding {@link Vat}
     */
    String destruct = ".destruct";

    /**
     * corresponding {@link Effect} {@link Loop loop} processed only if
     * the current {@link Vat#enter transaction} commits
     */
    String effect = ".effect";
    
    /**
     * corresponding persistent {@link Task event} {@link Loop loop}
     */
    String enqueue = ".enqueue";
    
    /**
     * corresponding event receiver accessor
     */
    String events = ".events";

    /**
     * corresponding URL
     */
    String here = ".here";
    
    /**
     * always bound to <code>null</code>
     */
    String nothing = "";

    /**
     * persistent {@link SecureRandom pseudo-random number generator}
     */
    String prng = ".prng";
    
    /**
     * corresponding project name
     */
    String project = ".project";
    
    /**
     * corresponding {@link Tracer} 
     */
    String tracer = ".tracer";

    /**
     * corresponding {@link Vat}
     */
    String vat = ".vat";

    /**
     * {@link Vat#extend} transaction to run each time vat is loaded
     */
    String wake = ".wake";

    // org.waterken.vat.Root interface
    
    /**
     * Gets the simple name of the containing vat.
     */
    String
    getVatName();
    
    /**
     * Creates a new log event identifier.
     */
    Anchor
    anchor();
    
    /**
     * Calculates a pipeline key.
     * @param m message key
     * @return return value key
     */
    String
    pipeline(String m);

    /**
     * Retrieves a stored value.
     * @param otherwise default value
     * @param name      name to lookup
     * @return stored value, or <code>otherwise</code>
     */
    <T> T
    fetch(Object otherwise, String name);

    /**
     * Assigns a chosen name to a given value.
     * @param name  name to bind
     * @param value value to store
     * @throws Collision    <code>name</code> is already bound
     */
    void
    link(String name, Object value) throws Collision;
    
    /**
     * Assigns a name to a given value.
     * @param value value to store
     * @return assigned name
     */
    String
    export(Object value);
    
    /**
     * Creates an entity-tag identifying all the state accessed by the current
     * transaction.
     * @return corresponding HTTP entity-tag, or <code>null</code> if none
     */
    String
    getTransactionTag();
}
