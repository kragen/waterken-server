// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joe_e.Struct;
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
import org.joe_e.charset.UTF8;
import org.joe_e.reflect.Reflection;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.type.Typedef;
import org.waterken.syntax.Deserializer;
import org.waterken.syntax.Importer;

/**
 * <a href="http://www.json.org/">JSON</a> deserialization.
 */
public final class
JSONDeserializer extends Struct implements Deserializer, Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public @deserializer
    JSONDeserializer() {}
    
    // org.waterken.syntax.Deserializer interface

    public ConstArray<?>
    run(final String base, final Importer connect,
            final ClassLoader code, final InputStream content,
            final ConstArray<Type> parameters) throws Exception {
        final ConstArray<?>[] r = { null };
        JSONParser.drive(base, UTF8.input(content), new JSONParser.Builder() {

            public ArrayBuilder
            startArray() throws Exception {
                final ConstArray.Builder<Object> builder =
                    ConstArray.builder(parameters.length());
                return new ArrayBuilder() {
                    private int i = 0;

                    public void
                    finish() { r[0] = builder.snapshot(); }

                    public ValueConstructor
                    startElement() throws Exception {
                        return new ValueConstructor(base, connect, code,
                                parameters.get(i++), new Receiver() {
                            public void
                            run(final Object value) { builder.append(value); }
                        });
                    }
                };
            }

            public ObjectBuilder
            startObject() { throw new RuntimeException(); }

            public void
            writeKeyword(final String token) { throw new RuntimeException(); }

            public void
            writeString(final String token) { throw new RuntimeException(); }
        });
        return r[0];
    }
    
    static private interface
    Receiver {
        void run(Object value) throws Exception;
    }

    static private final TypeVariable<?> R = Typedef.name(Volatile.class, "T");
    static private final TypeVariable<?> T = Typedef.name(Iterable.class, "T");
    
    static private final class
    ValueConstructor implements JSONParser.Builder {
        private final String base;
        private final Importer connect;
        private final ClassLoader code;
        private final Type required;
        private final Receiver out;
        
        private final Type promised;
        private final Type expected;
        
        protected
        ValueConstructor(final String base, final Importer connect,
                         final ClassLoader code, final Type required,
                         final Receiver out) {
            this.base = base;
            this.connect = connect;
            this.code = code;
            this.required = required;
            this.out = out;
            
            promised = Typedef.value(R, required);
            expected = null != promised ? promised : required;
        }

        public void
        writeKeyword(final String token) throws Exception {
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
            out.run(null != promised ? ref(value) : value);
        }

        public void
        writeString(final String token) throws Exception {
            final Object value;
            if (char.class == expected || Character.class == expected) {
                if (1 != token.length()) { throw new Exception(); }
                value = token.charAt(0);
            } else {
                value = token;
            }
            out.run(null != promised ? ref(value) : value);
        }

        public ArrayBuilder
        startArray() {
            final Class<?> rawExpected = Typedef.raw(expected);
            final ConstArray.Builder<?> builder = 
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
            final ValueConstructor startElement = new ValueConstructor(base,
                    connect, code, Typedef.value(T, expected), new Receiver() {
                @SuppressWarnings("unchecked") public void
                run(final Object value) {
                    final ConstArray.Builder raw = builder;
                    raw.append(value);
                }
            });
            return new ArrayBuilder() {
                public void
                finish() throws Exception {
                    final ConstArray<?> value = builder.snapshot();
                    out.run(null != promised ? ref(value) : value);
                }

                public ValueConstructor
                startElement() { return startElement; }
            };
        }

        public ObjectBuilder
        startObject() throws Exception {
            return new ObjectBuilder() {
                private Constructor<?> make;
                private Type[] paramv;
                private String[] namev;
                private Object[] argv;
                private boolean[] donev;
                private boolean finished;
                
                private void
                determine(final Class<?> explicit) throws NoSuchMethodException{
                    final Class<?> actual =
                        null != explicit ? explicit : Typedef.raw(expected);
                    for (Constructor<?> c : Reflection.constructors(actual)) {
                        if (c.isAnnotationPresent(deserializer.class)) {
                            make = c;
                            break;
                        }
                    }
                    if (null==make && Throwable.class.isAssignableFrom(actual)){
                        make = Reflection.constructor(actual);
                    }
                    paramv = make.getGenericParameterTypes();
                    int i = 0;
                    for (final Type p : paramv) {
                        paramv[i++] = Typedef.bound(p, expected);
                    }
                    namev = new String[paramv.length];
                    i = 0;
                    for (Annotation[] as : make.getParameterAnnotations()) {
                        for (final Annotation a : as) {
                            if (a instanceof name) {
                                namev[i] = ((name)a).value();
                                break;
                            }
                        }
                        ++i;
                    }
                    argv = new Object[paramv.length];
                    donev = new boolean[paramv.length];
                }

                public void
                finish() throws Exception {
                    if (finished) { return; }
                    finished = true;
                    if (null == make) { determine(null); }
                    for (int i = donev.length; 0 != i--;) {
                        if (!donev[i]) {
                            argv[i] = defaultValue(paramv[i]);
                            donev[i] = true;
                        }
                    }
                    final Object value = Reflection.construct(make, argv);
                    if (value instanceof Rejected) {
                        final Rejected<?> p = (Rejected<?>)value;
                        if (Double.class==required || double.class==required) {
                            out.run(Double.NaN);
                        } else if(Float.class==required||float.class==required){
                            out.run(Float.NaN);
                        } else {
                            out.run(p._(Typedef.raw(required)));
                        }
                    } else {
                        out.run(value);
                    }
                }

                public ValueConstructor
                startMember(final String name) throws Exception {
                    if (finished) { throw new Exception(); }
                    if ("@".equals(name)) {
                        finished = true;
                        if (null != make) { throw new Exception(); }
                        return new ValueConstructor(base, connect, code,
                                String.class, new Receiver() {
                            public void
                            run(final Object value) throws Exception {
                                out.run(connect.run(Typedef.raw(required),
                                                    (String)value, base));
                            }                            
                        });
                    }
                    if ("$".equals(name)) {
                        if (null != make) { throw new Exception(); }
                        return new ValueConstructor(base, connect, code,
                                PowerlessArray.class, new Receiver() {
                            public void
                            run(final Object value) throws Exception {
                                Class<?> explicit = null;
                                for (final Object name : (ConstArray<?>)value) {
                                    try {
                                        explicit = Java.load(code,(String)name);
                                        break;
                                    } catch (final ClassNotFoundException e) {}
                                }
                                determine(explicit);
                            }
                        });
                    }
                    if (null == make) { determine(null); }
                    int i = namev.length;
                    while (0 != i-- && !name.equals(namev[i])) {}
                    if (-1 != i) {
                        if (donev[i]) { throw new Exception(); }
                        donev[i] = true;
                    }
                    final int position = i;
                    return new ValueConstructor(base, connect, code,
                            -1 != i ? paramv[i] : Object.class, new Receiver() {
                        public void
                        run(final Object value) throws Exception {
                            if (-1 != position) {
                                argv[position] = value;
                            }
                        }                            
                    });
                }
            };
        }
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
