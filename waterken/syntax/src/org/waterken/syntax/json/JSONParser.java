// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import static org.ref_send.promise.Eventual.ref;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joe_e.array.BooleanArray;
import org.joe_e.array.ByteArray;
import org.joe_e.array.CharArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.DoubleArray;
import org.joe_e.array.FloatArray;
import org.joe_e.array.ImmutableArray;
import org.joe_e.array.IntArray;
import org.joe_e.array.LongArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.array.ShortArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.name;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.scope.Layout;
import org.ref_send.scope.Scope;
import org.ref_send.type.Typedef;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Importer;
import org.waterken.syntax.Syntax;

/**
 * Deserializes a JSON text stream.
 */
public final class
JSONParser {
    
    private final String base;
    private final Importer connect;
    private final ClassLoader code;
    private final JSONLexer lexer;
    
    /**
     * Constructs an instance.
     * @param base      base URL
     * @param connect   reference importer
     * @param code      class loader
     * @param text      UTF-8 JSON text stream
     */
    public
    JSONParser(final String base, final Importer connect,
               final ClassLoader code, final Reader text) {
        this.base = base;
        this.connect = connect;
        this.code = code;
        lexer = new JSONLexer(text);
    }
    
    /**
     * Deserializes an array of objects.
     * @param parameters    each expected type
     * @return parsed argument list
     * @throws IOException  any I/O error
     * @throws BadSyntax    invalid JSON text
     */
    public ConstArray<?>
    readTuple(final ConstArray<Type> parameters) throws IOException, BadSyntax {
        try {
            if (null == lexer.next()) { throw new EOFException(); }
            if (!"[".equals(lexer.getHead())) { throw new Exception(); }
            final ConstArray.Builder<Object> r =
                ConstArray.builder(parameters.length());
            if (!"]".equals(lexer.next())) {
                while (true) {
                    r.append(parseValue(r.length() < parameters.length()
                            ? parameters.get(r.length()) : Object.class));
                    if ("]".equals(lexer.getHead())) { break; }
                    if (!",".equals(lexer.getHead())) { throw new Exception(); }
                    lexer.next();
                }
            }
            if (null != lexer.next()) { throw new Exception(); }
            lexer.close();
            return r.snapshot();
        } catch (final IOException e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw e;
        } catch (final Exception e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw new BadSyntax(base, lexer.getSpan(), e);           
        }
    }
    
    /**
     * Deserializes an object.
     * @param type  expected type
     * @return parsed object
     * @throws IOException  any I/O error
     * @throws BadSyntax    invalid JSON text
     */
    public Object
    readValue(final Type type) throws IOException, BadSyntax {
        try {
            if (null == lexer.next()) { throw new EOFException(); }
            final Object r = parseValue(type);
            if (null != lexer.next()) { throw new Exception(); }
            lexer.close();
            return r;
        } catch (final IOException e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw e;
        } catch (final Exception e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw new BadSyntax(base, lexer.getSpan(), e);           
        }
    }

    static private final TypeVariable<?> R = Typedef.var(Promise.class, "T");
    static private final TypeVariable<?> T = Typedef.var(Iterable.class, "T");
    
    private Object
    parseValue(final Type required) throws Exception {
        final String token = lexer.getHead();
        return "[".equals(token) ? parseArray(required)
        : "{".equals(token) ? parseObject(required)
        : token.startsWith("\"") ? parseString(required)
        : parseKeyword(required);
    }
    
    private Object
    parseKeyword(final Type required) throws Exception {
        final String token = lexer.getHead();
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Object value;
        if ("null".equals(token)) {
            value = null;
        } else if ("false".equals(token)) {
            value = Boolean.FALSE;                              // intern value
        } else if ("true".equals(token)) {
            value = Boolean.TRUE;                               // intern value
        } else if (int.class == expected || Integer.class == expected) {
            value = Integer.valueOf(Integer.parseInt(token));   // intern value
        } else if (long.class == expected || Long.class == expected) {
            value = Long.valueOf(Long.parseLong(token));        // intern value
        } else if (byte.class == expected || Byte.class == expected) {
            value = Byte.valueOf(Byte.parseByte(token));        // intern value
        } else if (short.class == expected || Short.class == expected) {
            value = Short.valueOf(Short.parseShort(token));     // intern value
        } else if (BigInteger.class == expected) {
            value = new BigInteger(token);
        } else if (double.class == expected || Double.class == expected) {
            value = Double.valueOf(token);  // accepts a superset of JSON
        } else if (float.class == expected || Float.class == expected) {
            value = Float.valueOf(token);   // accepts a superset of JSON
        } else if (BigDecimal.class == expected) {
            value = new BigDecimal(token);
        } else if (token.indexOf('.') != -1 ||
                   token.indexOf('e') != -1 || token.indexOf('E') != -1) {
            value = Double.valueOf(token);  // accepts a superset of JSON
        } else {
            final BigInteger x = new BigInteger(token);
            int bits = x.bitLength();
            if (x.signum() > 0) { ++bits; }
            if (bits <= Integer.SIZE) {
                value = Integer.valueOf(x.intValue());          // intern value
            } else if (bits <= Long.SIZE) {
                value = Long.valueOf(x.longValue());            // intern value
            } else {
                value = x;
            }
        }
        lexer.next();   // pop the keyword from the stream
        return null != value && null != promised ? ref(value) : value;
    }
    
    private Object
    parseString(final Type required) throws Exception {
        final String text = string(lexer.getHead());
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Object value;
        if (char.class == expected || Character.class == expected) {
            if (1 != text.length()) { throw new Exception(); }
            value = Character.valueOf(text.charAt(0));          // intern value
        } else {
            value = text;
        }
        lexer.next();   // pop the string from the stream
        return null != promised ? ref(value) : value;
    }
    
    private Object
    parseArray(final Type required) throws Exception {
        if (!"[".equals(lexer.getHead())) { throw new Exception(); }
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Class<?> rawExpected = Typedef.raw(expected);
        final @SuppressWarnings("unchecked") ConstArray.Builder<Object> builder= 
            (ConstArray.Builder)(
                BooleanArray.class == rawExpected ? BooleanArray.builder() :
                CharArray.class == rawExpected ? CharArray.builder() :
                ByteArray.class == rawExpected ? ByteArray.builder() :
                ShortArray.class == rawExpected ? ShortArray.builder() :
                IntArray.class == rawExpected ? IntArray.builder() :
                LongArray.class == rawExpected ? LongArray.builder() :
                FloatArray.class == rawExpected ? FloatArray.builder() :
                DoubleArray.class == rawExpected ? DoubleArray.builder() :
                PowerlessArray.class.isAssignableFrom(rawExpected) ?
                    PowerlessArray.builder() :
                ImmutableArray.class.isAssignableFrom(rawExpected) ?
                    ImmutableArray.builder() :
                ConstArray.builder());
        if (!"]".equals(lexer.next())) {
            final Type elementT = Typedef.value(T, expected);
            while (true) {
                builder.append(parseValue(elementT));
                if ("]".equals(lexer.getHead())) { break; }
                if (!",".equals(lexer.getHead())) { throw new Exception(); }
                lexer.next();
            }
        }
        lexer.next();   // skip past the closing bracket
        final ConstArray<?> value = builder.snapshot();
        return null != promised ? ref(value) : value;
    }
    
    private Object
    parseObject(final Type required) throws Exception {
        if (!"{".equals(lexer.getHead())) { throw new Exception(); }
        lexer.next();       // skip past the opening curly

        /*
         * SECURITY CLAIM: The held ClassLoader will only be used to construct
         * pass-by-construction Joe-E objects. Although any class may be loaded
         * via the ClassLoader, the class will only be used to access a Joe-E
         * constructor. If the loaded class is not a Joe-E class, no Joe-E
         * constructor will be found and deserialization will fail.
         * 
         * A "pass-by-construction" Joe-E class is a Joe-E class which either:
         * - declares a public constructor with the deserializer annotation
         * - is a subclass of Throwable and declares a public no-arg constructor
         */
        
        // determine the actual type and a corresponding constructor
        final Class<?> actual;
              Constructor<?> make = null;
        final PowerlessArray.Builder<String> names = PowerlessArray.builder(1);
        final ConstArray.Builder<Object> values = ConstArray.builder(1);
        if ("\"class\"".equals(lexer.getHead())) {
            if (!":".equals(lexer.next())) { throw new Exception(); }
            lexer.next();
            Class<?> type = null;
            final Object value = parseValue(PowerlessArray.class);
            if (value instanceof PowerlessArray<?>) {
                for (final Object typename : (PowerlessArray<?>)value) {
                    try {
                        final Class<?> t = JSON.load(code, (String)typename);
                        if (null == type) { type = t; }
                        make = Syntax.deserializer(t);
                        if (null != make) { break; }
                    } catch (final ClassCastException e) {
                    } catch (final ClassNotFoundException e) {}
                }
            }
            actual = type;
            names.append("class");
            values.append(value);
            if (",".equals(lexer.getHead())) { lexer.next(); }
        } else {
            actual = null;
        }
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Class<?> concrete = null!=actual ? actual : Typedef.raw(expected);
        if (null == make && null == actual) {
            make = Syntax.deserializer(concrete);
        }
        final String[] namev;
        final Type[] paramv;
        final Object[] argv;
        final boolean[] donev;
        if (null == make) {
            namev = new String[] {};
            paramv = null;
            argv = null;
            donev = null;
        } else {
            paramv = make.getGenericParameterTypes(); {
                int i = 0;
                for (final Type p : paramv) {
                    paramv[i++] = Typedef.bound(p, expected);
                }
            }
            namev = new String[paramv.length]; {
                int i = 0;
                for (final Annotation[] as : make.getParameterAnnotations()) {
                    for (final Annotation a : as) {
                        if (a instanceof name) {
                            namev[i] = ((name)a).value();
                            break;
                        }
                    }
                    ++i;
                }
            }
            argv = new Object[paramv.length];
            donev = new boolean[paramv.length];
        }
        if (!"}".equals(lexer.getHead())) {
            while (true) {
                final String name = string(lexer.getHead());
                if (!":".equals(lexer.next())) { throw new Exception(); }
                lexer.next();
                int slot = namev.length;
                while (0 != slot-- && !name.equals(namev[slot])) {}
                final Type type = -1 != slot ?
                        paramv[slot] :
                    "=".equals(name) ?
                        (null != actual ? actual : required) :
                    "!".equals(name) ?
                        Exception.class :
                    Object.class;
                final Object value = parseValue(type);
                if (-1 != slot) {
                    if (donev[slot]) { throw new Exception(); }
                    donev[slot] = true;
                    argv[slot] = value;
                } else {
                    names.append(name);
                    values.append(value);
                }
                if ("}".equals(lexer.getHead())) { break; }
                if (!",".equals(lexer.getHead())) { throw new Exception(); }
                lexer.next();
            }
        }
        lexer.next();       // skip past the closing curly

        final PowerlessArray<String> undeclared = names.snapshot();
        final int rejected = find("!", undeclared);
        if (-1 != rejected) {
            final Object reason = values.snapshot().get(rejected);
            final Exception e = reason instanceof Exception ?
                    (Exception)reason :
                reason instanceof String ?
                    new Exception((String)reason) :
                new Exception();
            final Promise<?> r = Eventual.reject(e);
            return null != promised ? r : Eventual.cast(concrete, r);
        }
        
        final int remote = find("@", undeclared);
        if (-1 != remote) {
            final String href = (String)values.snapshot().get(remote);
            final Object r =
                connect.apply(href, base, null != actual ? actual : required);
            return null != r && null != promised ? ref(r) : r;
        }
        
        final int replacement = find("=", undeclared);
        if (-1 != replacement) {
            final Object r = values.snapshot().get(replacement);
            return null != r && null != promised ? ref(r) : r;
        }
        
        final Object r;
        if (null == make) {
            r = new Scope(new Layout(undeclared), values.snapshot());  
        } else {
            for (int i = donev.length; 0 != i--;) {
                if (!donev[i]) {
                    donev[i] = true;
                    argv[i] = Syntax.defaultValue(paramv[i]);
                }
            }
            r = Reflection.construct(make, argv);
        }
        return null != promised ? ref(r) : r;
    }
    
    static private int
    find(final String name, final PowerlessArray<String> names) {
        int r = names.length();
        while (0 != r-- && !name.equals(names.get(r))) {}
        return r;
    }
    
    static private String
    string(final String token) throws Exception {
        if (!token.startsWith("\"")) { throw new Exception(); }
        return token.substring(1, token.length() - 1);
    }
    
    /*
     * This implementation provides few security properties. In particular,
     * clients of this class should assume:
     * - repeated deserialization of a JSON text may produce different Java
     * object graphs
     * - deserialized application objects can detect the number of times they
     * have been deserialized
     * - via the Importer, application objects may gain access to any reference
     * they are able to name
     * - an object of any pass-by-construction Joe-E class may be constructed
     * from whole cloth
     */
}