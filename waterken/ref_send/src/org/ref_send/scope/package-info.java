// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html

/**
 * A pass-by-copy interface.
 * <p>The API in this package enables creation of pass-by-copy data structures
 * without the requirement to define corresponding Java classes imposed by the
 * {@linkplain org.ref_send pass-by-construction} API.</p>
 * <p>Instances of {@link org.ref_send.scope.Scope} are sent pass-by-copy when
 * used as arguments in remote invocations. The networking code in the Waterken
 * Server automatically produces a corresponding JSON object with the names and
 * values specified in a {@link org.ref_send.scope.Scope}. For example, this
 * code:</p>
 * <pre>
 * Layout.define("name",        "year", "premium").
 *          make("Tyler Close", 2008,   true);
 * </pre>
 * <p>produces a {@link org.ref_send.scope.Scope} that will be encoded in JSON
 * as:</p>
 * <pre> 
 * {
 *   "name" : "Tyler Close",
 *   "year" : 2008,
 *   "premium" : true
 * }
 * </pre>
 * <p>And the following code:</p>
 * <pre>
 * Layout.define("name",        "year", "premium", "mailto", "preferences").
 * 	  make("Tyler Close", 2008,   true,      Layout.define(
 *                   "street",              "city",      "state", "zip").
 *              make("1501 Page Mill Road", "Palo Alto", "CA",    "94304"),
 *              ConstArray.array("2-day-shipping", "monthly"));
 * </pre>
 * <p>produces a {@link org.ref_send.scope.Scope} that will be encoded in JSON
 * as:</p>
 * <pre> 
 * {
 *   "name" : "Tyler Close",
 *   "year" : 2008,
 *   "premium" : true,
 *   "mailto" : {
 *     "street" : "1501 Page Mill Road",
 *     "city" : "Palo Alto",
 *     "state" : "CA",
 *     "zip" : "94304"
 *   },
 *   "preferences" : [ "2-day-shipping", "monthly" ]
 * }
 * </pre>
 * @see org.ref_send
 */
@org.joe_e.IsJoeE package org.ref_send.scope;
