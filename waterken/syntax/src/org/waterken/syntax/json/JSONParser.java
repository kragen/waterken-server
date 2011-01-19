// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import static org.ref_send.promise.Eventual.ref;
import static org.waterken.syntax.WrongToken.require;

import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
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
import org.ref_send.scope.Unavailable;
import org.ref_send.type.Typedef;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Importer;
import org.waterken.syntax.Syntax;
import org.waterken.syntax.WrongToken;

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
    readTuple(final Type... parameters) throws IOException, BadSyntax {
        try {
            // Check for varargs.
            final Type vparam;
            final Class<?> vclass;
            if (0 == parameters.length) {
                vparam = vclass = null;
            } else {
                final Type last = parameters[parameters.length - 1];
                if (last instanceof GenericArrayType) {
                    vparam = ((GenericArrayType)last).getGenericComponentType();
                    vclass = Typedef.raw(vparam);
                } else if (last instanceof Class && ((Class<?>)last).isArray()){
                    vparam = vclass = ((Class<?>)last).getComponentType();
                } else {
                    vparam = vclass = null;
                }
            }
            
            // Parse the received data.
            final ConstArray.Builder<Object> r =
                ConstArray.builder(parameters.length);
            require("[", lexer.next());
            if (!"]".equals(lexer.next())) {
                while (true) {
                    if (null != vclass && r.length() == parameters.length - 1) {
                        Object vargs = Array.newInstance(vclass, 1);
                        for (int j = 0; true; ++j) {
                            Array.set(vargs, j, parseValue(vparam));
                            if ("]".equals(lexer.next())) { break; }
                            require(",", lexer.getHead());
                            lexer.next();
                            System.arraycopy(vargs, 0,
                                vargs = Array.newInstance(vclass, j + 2), 0,
                                j + 1);
                        }
                        r.append(vargs);
                        break;
                    }
                    if (r.length() < parameters.length) {
                        r.append(parseValue(parameters[r.length()]));
                    } else {
                        // Discard extra arguments.
                        parseValue(Object.class);
                    }
                    if ("]".equals(lexer.next())) { break; }
                    require(",", lexer.getHead());
                    lexer.next();
                }
            }
            lexer.close();
            
            // Fill out any remaining parameters with default values.
            while (r.length() < parameters.length) {
                if (null != vclass && r.length() == parameters.length - 1) {
                    r.append(Array.newInstance(vclass, 0));
                } else {
                    r.append(Syntax.defaultValue(parameters[r.length()]));
                }
            }
            return r.snapshot();
        } catch (final IOException e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw e;
        } catch (final Exception e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw new BadSyntax(base, PowerlessArray.array(
                IntArray.array(lexer.getLine(), lexer.getColumn())), e);
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
            lexer.next();
            final Object r = parseValue(type);
            lexer.close();
            return r;
        } catch (final IOException e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw e;
        } catch (final Exception e) {
            try { lexer.close(); } catch (final Exception e2) {}
            throw new BadSyntax(base, PowerlessArray.array(
                IntArray.array(lexer.getLine(), lexer.getColumn())), e);
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
        return null != promised ? ref(value) : value;
    }

    private Object
    parseString(final Type required) throws Exception {
        final String text = string(lexer.getHead());
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Object value;
        if (char.class == expected || Character.class == expected) {
            if (1 != text.length()) { throw new WrongToken("\""); }
            value = Character.valueOf(text.charAt(0));          // intern value
        } else {
            value = text;
        }
        return null != promised ? ref(value) : value;
    }

    private Object
    parseArray(final Type required) throws Exception {
        require("[", lexer.getHead());
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Class<?> rawExpected = Typedef.raw(expected);
        final @SuppressWarnings({ "rawtypes", "unchecked" })
        ConstArray.Builder<Object> builder = (ConstArray.Builder)(
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
                if ("]".equals(lexer.next())) { break; }
                require(",", lexer.getHead());
                lexer.next();
            }
        }
        final ConstArray<?> value = builder.snapshot();
        return null != promised ? ref(value) : value;
    }

    private Object
    parseObject(final Type required) throws Exception {
        require("{", lexer.getHead());
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

        final PowerlessArray.Builder<String> names = PowerlessArray.builder(1);
        final ConstArray.Builder<Object> values = ConstArray.builder(1);

        // load any explicit type information
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        Class<?>[] types = new Class<?>[] { Typedef.raw(expected) };
        if (null != code && "\"class\"".equals(lexer.getHead())) {
            require(":", lexer.next());
            lexer.next();
            final Object value = parseValue(PowerlessArray.class);
            names.append("class");
            values.append(value);
            if (value instanceof PowerlessArray<?>) {
                for (final Object typename : (PowerlessArray<?>)value) {
                    try {
                        final Class<?> type = JSON.load(code, (String)typename);
                        boolean implied = false;
                        for (int i = 0; i != types.length; i += 1) {
                            if (type.isAssignableFrom(types[i])) {
                                implied = true;
                                break;
                            }
                            if (types[i].isAssignableFrom(type)) {
                            	types[i] = type;
                            	implied = true;
                            	break;
                            }
                        }
                        if (!implied) {
                            final int n = types.length;
                            System.arraycopy(
                                types, 0, types = new Class<?>[n + 1], 0, n);
                            types[n] = type;
                        }
                    } catch (final ClassCastException e) {
                      // Skip non-string typename in source.
                    } catch (final ClassNotFoundException e) {
                      // Skip unknown type.
                    }
                }
            }
            if (",".equals(lexer.next())) { lexer.next(); }
        }
        final Constructor<?> make; {
            Constructor<?> c = null;
            for (final Class<?> type : types) {
                c = Syntax.deserializer(type);
                if (null != c) { break; }
            }
            make = c;
        }
        final String[] namev;
        final Type[] paramv;
        final Object[] argv;
        final boolean[] donev;
        if (null == make) {
            namev = new String[] {};
            paramv = new Type[] {};
            argv = new Object[] {};
            donev = new boolean[] {};
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
                require(":", lexer.next());
                lexer.next();
                int slot = namev.length;
                while (0 != slot-- && !name.equals(namev[slot])) {}
                final Type type = -1 != slot ?
                        paramv[slot] :
                    "=".equals(name) ?
                        required :
                    "!".equals(name) ?
                        Exception.class :
                    Object.class;
                final Object value = parseValue(type);
                if (-1 != slot) {
                    if (donev[slot]) { throw new Unavailable(name); }
                    donev[slot] = true;
                    argv[slot] = value;
                } else {
                    names.append(name);
                    values.append(value);
                }
                if ("}".equals(lexer.next())) { break; }
                require(",", lexer.getHead());
                lexer.next();
            }
        }

        final PowerlessArray<String> undeclared = names.snapshot();
        final int rejected = find("!", undeclared);
        if (-1 != rejected) {
            final Object reason = values.snapshot().get(rejected);
            final Exception e = reason instanceof Exception ?
                    (Exception)reason :
                reason instanceof String ?
                    new RuntimeException((String)reason) :
                new RuntimeException();
            try {
            	return Eventual.cast(Typedef.raw(required), Eventual.reject(e));
            } catch (final ClassCastException ignored) {
            	throw e;
            }
        }

        final int remote = find("@", undeclared);
        if (-1 != remote) {
            final String href = (String)values.snapshot().get(remote);
            final Object r = null != promised ?
            	connect.apply(href, base, required) :
            	connect.apply(href, base, types);
            return null != promised ? ref(r) : r;
        }

        final int replacement = find("=", undeclared);
        if (-1 != replacement) { return values.snapshot().get(replacement); }

        final Object r;
        if (null == make) {
            r = Scope.make(new Layout<Object>(undeclared), values.snapshot());
        } else {
            for (int i = donev.length; 0 != i--;) {
                if (!donev[i]) {
                    donev[i] = true;
                    argv[i] = Syntax.defaultValue(paramv[i]);
                }
            }
            r = Reflection.construct(make, argv);
            if (r instanceof Throwable) {
                Reflection.clearStackTrace((Throwable)r);
            }
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
        if (!token.startsWith("\"")) { throw new WrongToken("\""); }
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
