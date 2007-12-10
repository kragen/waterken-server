// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;
import org.web_send.graph.Framework;

/**
 * An {@link Extension} implementation.
 */
final class
ExtensionX extends Struct implements Extension, Serializable {
    static private final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final Framework framework;
    
    @SuppressWarnings("unused")
    private final Menu<Resource> answers;
    
    ExtensionX(final Framework framework,
               final Menu<Resource> answers) {
        this.framework = framework;
        this.answers = answers;
    }
}
