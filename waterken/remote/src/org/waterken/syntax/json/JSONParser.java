// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import static org.ref_send.promise.Fulfilled.ref;

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
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.type.Typedef;
import org.waterken.syntax.Importer;

public final class
JSONParser {
    
    private final String base;
    private final Importer connect;
    private final ClassLoader code;
    private final JSONLexer lexer;
    
    private
    JSONParser(final String base, final Importer connect,
               final ClassLoader code, final Reader in) {
        this.base = base;
        this.connect = connect;
        this.code = code;
        lexer = new JSONLexer(in);
    }

    static public ConstArray<?>
    parse(final String base, final Importer connect,
            final ClassLoader code, final Reader in,
            final ConstArray<Type> parameters) throws Exception {
        final JSONParser parser = new JSONParser(base, connect, code, in);
        try {
            if (!"[".equals(parser.lexer.advance())) { throw new Exception(); }
            final ConstArray<?> r = parser.parseArguments(parameters);
            if (null != parser.lexer.getHead()) { throw new Exception(); }
            return r;
        } catch (final Exception e) {
            try { parser.lexer.close(); } catch (final Exception e2) {}
            throw new Exception("<" + parser.base + "> ( " +
                                parser.lexer.getLine() + ", " +
                                parser.lexer.getColumn() + " ) : ", e);           
        }
    }
    
    private ConstArray<?>
    parseArguments(final ConstArray<Type> parameters) throws Exception {
        if (!"[".equals(lexer.getHead())) { throw new Exception(); }
        final ConstArray.Builder<Object> r =
            ConstArray.builder(parameters.length());
        if (!"]".equals(lexer.advance())) {
            while (true) {
                r.append(parseValue(parameters.get(r.length())));
                final String token = lexer.getHead();
                if ("]".equals(token)) { break; }
                if (!",".equals(token)) { throw new Exception(); }
                lexer.advance();
            }
        }
        lexer.advance(); 
        return r.snapshot();
    }

    static private final TypeVariable<?> R = Typedef.name(Volatile.class, "T");
    static private final TypeVariable<?> T = Typedef.name(Iterable.class, "T");
    
    private Object
    parseValue(final Type required) throws Exception {
        final String token = lexer.getHead();
        return "[".equals(token)
            ? parseArray(required)
        : "{".equals(token)
            ? parseObject(required)
        : token.startsWith("\"")
            ? parseString(required)
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
            value = Boolean.FALSE;
        } else if ("true".equals(token)) {
            value = Boolean.TRUE;
        } else if (int.class == expected || Integer.class == expected) {
            value = Integer.parseInt(token);
        } else if (long.class == expected || Long.class == expected) {
            value = Long.parseLong(token);
        } else if (byte.class == expected || Byte.class == expected) {
            value = Byte.parseByte(token);
        } else if (short.class == expected || Short.class == expected) {
            value = Short.parseShort(token);
        } else if (BigInteger.class == expected) {
            value = new BigInteger(token);
        } else if (double.class == expected || Double.class == expected) {
            value = Double.valueOf(token);
        } else if (float.class == expected || Float.class == expected) {
            value = Float.valueOf(token);
        } else if (BigDecimal.class == expected) {
            value = new BigDecimal(token);
        } else if (token.indexOf('.') != -1 ||
                   token.indexOf('e') != -1 || token.indexOf('E') != -1) {
            value = Double.valueOf(token);
        } else {
            final BigInteger x = new BigInteger(token);
            int bits = x.bitLength();
            if (x.signum() > 0) { ++bits; }
            if (bits <= Byte.SIZE) {
                value = x.byteValue();
            } else if (bits <= Short.SIZE) {
                value = x.shortValue();
            } else if (bits <= Integer.SIZE) {
                value = x.intValue();
            } else if (bits <= Long.SIZE) {
                value = x.longValue();
            } else {
                value = x;
            }
        }
        lexer.advance();
        return null != promised ? ref(value) : value;
    }
    
    private Object
    parseString(final Type required) throws Exception {
        final String token = lexer.getHead();
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Object value;
        if (char.class == expected || Character.class == expected) {
            if (3 != token.length()) { throw new Exception(); }
            value = token.charAt(1);
        } else {
            value = token.substring(1, token.length() - 1);
        }
        lexer.advance();
        return null != promised ? ref(value) : value;
    }
    
    private @SuppressWarnings("unchecked") ConstArray<?>
    parseArray(final Type required) throws Exception {
        if (!"[".equals(lexer.getHead())) { throw new Exception(); }
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        final Class<?> rawExpected = Typedef.raw(expected);
        final ConstArray.Builder builder = 
            BooleanArray.class == rawExpected
                ? BooleanArray.builder()
            : CharArray.class == rawExpected
                ? CharArray.builder()
            : ByteArray.class == rawExpected
                ? ByteArray.builder()
            : ShortArray.class == rawExpected
                ? ShortArray.builder()
            : IntArray.class == rawExpected
                ? IntArray.builder()
            : LongArray.class == rawExpected
                ? LongArray.builder()
            : FloatArray.class == rawExpected
                ? FloatArray.builder()
            : DoubleArray.class == rawExpected
                ? DoubleArray.builder()
            : PowerlessArray.class.isAssignableFrom(rawExpected)
                ? PowerlessArray.builder()
            : ImmutableArray.class.isAssignableFrom(rawExpected)
                ? ImmutableArray.builder()
            : ConstArray.builder();
        if (!"]".equals(lexer.advance())) {
            final Type elementT = Typedef.value(T, expected);
            while (true) {
                builder.append(parseValue(elementT));
                if ("]".equals(lexer.getHead())) { break; }
                if (!",".equals(lexer.getHead())) { throw new Exception(); }
                lexer.advance();
            }
        }
        lexer.advance();
        return builder.snapshot();
    }
    
    private Object
    parseObject(final Type required) throws Exception {
        if (!"{".equals(lexer.getHead())) { throw new Exception(); }
        if ("\"@\"".equals(lexer.advance())) {
            if (!":".equals(lexer.advance())) { throw new Exception(); }
            final String hrefToken = lexer.advance();
            if (!hrefToken.startsWith("\"")) { throw new Exception(); }
            if (!"}".equals(lexer.advance())) { throw new Exception(); }
            final String href = hrefToken.substring(1, hrefToken.length() - 1);
            final Object value = connect.run(Typedef.raw(required), href, base);
            lexer.advance();
            return value;
        }
        final Type promised = Typedef.value(R, required);
        final Type expected = null != promised ? promised : required;
        Class<?> actual;
        if ("\"$\"".equals(lexer.getHead())) {
            if (!":".equals(lexer.advance())) { throw new Exception(); }
            if (!"[".equals(lexer.advance())) { throw new Exception(); }
            actual = null;
            for (final Object name : parseArray(PowerlessArray.class)) {
                try {
                    actual = Java.load(code, (String)name);
                    break;
                } catch (final ClassNotFoundException e) {}
            }
            if (",".equals(lexer.getHead())) {
                if ("}".equals(lexer.advance())) { throw new Exception(); }
            } else {
                if (!"}".equals(lexer.getHead())) { throw new Exception(); }
            }
        } else {
            actual = Typedef.raw(expected);
        }
        Constructor<?> make = null;
        for (final Constructor<?> c : Reflection.constructors(actual)) {
            if (c.isAnnotationPresent(deserializer.class)) {
                make = c;
                break;
            }
        }
        if (null == make && Throwable.class.isAssignableFrom(actual)) {
            make = Reflection.constructor(actual);
        }
        final Type[] paramv = make.getGenericParameterTypes(); {
            int i = 0;
            for (final Type p : paramv) {
                paramv[i++] = Typedef.bound(p, expected);
            }
        }
        final String[] namev = new String[paramv.length]; {
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
        final Object[] argv = new Object[paramv.length];
        final boolean[] donev = new boolean[paramv.length];
        if (!"}".equals(lexer.getHead())) {
            while (true) {
                final String nameToken = lexer.getHead();
                if (!nameToken.startsWith("\"")) { throw new Exception(); }
                final String name = nameToken.substring(1,nameToken.length()-1);
                int slot = namev.length;
                while (0 != slot-- && !name.equals(namev[slot])) {}
                if (-1 != slot) {
                    if (donev[slot]) { throw new Exception(); }
                    donev[slot] = true;
                }
                if (!":".equals(lexer.advance())) { throw new Exception(); }
                lexer.advance();
                final Type type = -1 != slot ? paramv[slot] : Object.class;
                final Object value = parseValue(type);
                if (-1 != slot) {
                    argv[slot] = value;
                }
                if ("}".equals(lexer.getHead())) { break; }
                if (!",".equals(lexer.getHead())) { throw new Exception(); }
                lexer.advance();
            }
        }
        for (int i = donev.length; 0 != i--;) {
            if (!donev[i]) {
                final Type requiredP = paramv[i];
                final Type promisedP = Typedef.value(R, requiredP);
                final Type expectedP = null!=promisedP ? promisedP : requiredP;
                final Object arg = defaultValue(expectedP);
                argv[i] = null != promised ? ref(arg) : arg;
                donev[i] = true;
            }
        }
        Object value = Reflection.construct(make, argv);
        if (value instanceof Rejected) {
            value = ((Rejected<?>)value)._(Typedef.raw(required));
        } else if (null != promised) {
            value = ref(value);
        }
        lexer.advance();    // skip past the closing curly
        return value;
    }
    
    static private Object
    defaultValue(final Type type) {
        final Object NULL = null;
        return boolean.class == type
            ? Boolean.FALSE
        : char.class == type
            ? Character.valueOf('\0')
        : byte.class == type
            ? Byte.valueOf((byte)0)
        : short.class == type
            ? Short.valueOf((short)0)
        : int.class == type
            ? Integer.valueOf(0)
        : long.class == type
            ? Long.valueOf(0)
        : float.class == type
            ? Float.valueOf(0.0f)
        : double.class == type
            ? Double.valueOf(0.0)
        : NULL;
    }
}