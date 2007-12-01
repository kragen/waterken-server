// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.ref_send.Variable;
import org.ref_send.promise.Promise;
import org.waterken.dns.Resource;

/**
 * A {@link Section} implementation.
 */
final class
SectionX implements Section, Serializable {
    static private final long serialVersionUID = 1L;

    private ConstArray<ResourceSlot> slots = ConstArray.array();

    public ConstArray<ResourceSlot>
    getEntries() { return slots; }

    public ResourceSlot
    add() throws Disallowed {
        if (slots.length() == 4) { throw new Disallowed(); }
        
        final ResourceSlot slot = new ResourceSlot();
        final ByteArray addr = ByteArray.array(new byte[] { 127, 0, 0, 1 });
        slot.put(new Resource(Resource.A, Resource.IN, 0, addr));
        slots = slots.with(slot);
        return slot;
    }

    public void
    remove(final Variable<? extends Promise<Resource>> editor) {
        final ResourceSlot[] v = new ResourceSlot[slots.length() - 1];
        int i = 0;
        for (final ResourceSlot slot : slots) {
            if (!slot.equals(editor)) {
                v[i++] = slot;
            }
        }
        slots = ConstArray.array(v);
    }
}
