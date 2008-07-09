// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import static org.joe_e.array.PowerlessArray.builder;

import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.joe_e.Struct;
import org.joe_e.array.ArrayBuilder;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.UTF8;
import org.joe_e.reflect.Reflection;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.promise.Inline;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.type.Typedef;
import org.waterken.id.Exporter;
import org.waterken.io.Content;
import org.waterken.io.open.Open;
import org.waterken.syntax.Serializer;

/**
 * <a href="http://www.json.org/">JSON</a> serialization.
 */
public final class
JSONSerializer extends Struct implements Serializer, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    JSONSerializer() {}

    // org.waterken.syntax.Serializer interface

    public Content
    run(final boolean mode, final Exporter export, final ConstArray<?> values) {
        return new Content() {
            public void
            writeTo(final OutputStream out) throws Exception {
                final Writer text = UTF8.output(Open.output(out));
                final JSONWriter top = JSONWriter.make(text);
                final JSONWriter.ArrayWriter aout = top.startArray();
                for (final Object x : values) {
                    serialize(mode,export, Object.class,x, aout.startElement());
                }
                aout.finish();
                if (!top.isWritten()) { throw new NullPointerException(); }
                text.flush();
                text.close();
            }
        };
    }

    static private final TypeVariable<?> R = Typedef.name(Volatile.class, "T");
    static private final TypeVariable<?> T = Typedef.name(Iterable.class, "T");

    static private void
    serialize(final boolean mode, final Exporter export,
              final Type implicit, final Object value,
              final JSONWriter.ValueWriter out) throws Exception {
        final Class<?> actual = null != value ? value.getClass() : Void.class;
        if (String.class == actual) {
            out.writeString((String)value);
        } else if (Integer.class == actual) {
            out.writeInt((Integer)value);
        } else if (Boolean.class == actual) {
            out.writeBoolean((Boolean)value);
        } else if (Long.class == actual) {
            try {
                out.writeLong((Long)value);
            } catch (final ArithmeticException e) {
                serialize(mode, export, implicit, new Rejected<Long>(e), out);
            }
        } else if (Double.class == actual) {
            try {
                out.writeDouble((Double)value);
            } catch (final ArithmeticException e) {
                serialize(mode, export, implicit, new Rejected<Double>(e), out);
            }
        } else if (Float.class == actual) {
            try {
                out.writeFloat((Float)value);
            } catch (final ArithmeticException e) {
                serialize(mode, export, implicit, new Rejected<Float>(e), out);
            }
        } else if (Byte.class == actual) {
            out.writeByte((Byte)value);
        } else if (Short.class == actual) {
            out.writeShort((Short)value);
        } else if (Character.class == actual) {
            out.writeString(((Character)value).toString());
        } else if (Void.class == actual) {
            out.writeNull();
        } else if (Class.class == actual) {
            final Class<?> c = (Class<?>)value;
            final JSONWriter.ObjectWriter oout = out.startObject();
            if (Class.class != implicit) {
                serialize(mode, export, PowerlessArray.class,
                          PowerlessArray.array("class"), oout.startMember("$"));
            }
            oout.startMember("name").writeString(Java.name(c));
            oout.finish();
        } else if (Inline.class == actual) {
            final Type r = Typedef.value(R, implicit);
            serialize(mode, export, null != r ? r : Object.class,
                      ((Inline<?>)value).cast(), out);
        } else if (value instanceof ConstArray) {
            final Type elementType = Typedef.bound(T, implicit);
            final JSONWriter.ArrayWriter aout = out.startArray();
            for (final Object i : (ConstArray<?>)value) {
                serialize(render, export, elementType, i, aout.startElement());
            }
            aout.finish();
        } else if (value instanceof Record || value instanceof Throwable) {
            final JSONWriter.ObjectWriter oout = out.startObject();
            final Class<?> top = Typedef.raw(implicit);
            if (actual != top) {
                serialize(render, export, PowerlessArray.class,
                          upto(actual, top), oout.startMember("$"));
            }
            for (final Field f : Reflection.fields(actual)) {
                final int flags = f.getModifiers();
                if (!Modifier.isStatic(flags) && Modifier.isFinal(flags) &&
                    Modifier.isPublic(f.getDeclaringClass().getModifiers())) {
                    final Object member = Reflection.get(f, value);
                    if (null != member) {
                        serialize(render, export,
                                  Typedef.bound(f.getGenericType(), actual),
                                  member, oout.startMember(f.getName()));
                    }
                }
            }
            oout.finish();
        } else if (render == mode || Java.isPBC(actual)) {
            out.writeLink(export.run(value));
        // rest is introspection support not used in normal messaging
        } else {
            /*
             * This branch documents runtime type information about a
             * pass-by-reference object. The information is provided for two
             * purposes: to help the human programmer, and to support type
             * dependent rendering on the client side. To date, the method
             * information is only being used as programmer documentation. The
             * client-side rendering is only using the type declaration.
             */
            final JSONWriter.ObjectWriter oout = out.startObject();
            serialize(mode, export, PowerlessArray.class, types(actual),
                      oout.startMember("$"));
            for (final Method m : Reflection.methods(actual)) {
                final int flags = m.getModifiers();
                if (!Modifier.isStatic(flags) && !Java.isSynthetic(flags)) {
                    final String name = Java.property(m);
                    final JSONWriter.ObjectWriter mout = oout.startMember(
                        null != name ? name : m.getName()).startObject();

                    // output the return type
                    final Type outType = m.getGenericReturnType();
                    if (void.class != outType && Void.class != outType) {
                        describeType(outType, mout.startMember("out"));
                    }

                    // output the parameter types
                    if (null == name) {
                        final JSONWriter.ArrayWriter pout =
                            mout.startMember("in").startArray();
                        for (final Type p : m.getGenericParameterTypes()) {
                            describeType(p, pout.startElement());
                        }
                        pout.finish();
                    }

                    // output the error types
                    final JSONWriter.ArrayWriter eout =
                        mout.startMember("error").startArray();
                    for (final Type e : m.getGenericExceptionTypes()) {
                        describeType(e, eout.startElement());
                    }
                    eout.finish();

                    mout.finish();
                }
            }
            oout.finish();
        }
    }

    static private void
    describeType(final Type type, final JSONWriter out) throws Exception {
        final Type pR = Typedef.value(R, type);
        if (null != pR) {
            describeType(pR, out);
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType generic = (ParameterizedType)type;
            final JSONWriter.ObjectWriter oout = out.startObject();
            final Class<?> raw = Typedef.raw(generic.getRawType());
            oout.startMember("name").writeString(Java.name(jsonType(raw)));
            final JSONWriter.ArrayWriter pout =
                oout.startMember("parts").startArray();
            for (final Type argument : generic.getActualTypeArguments()) {
                describeType(argument, pout.startElement());
            }
            pout.finish();
            oout.finish();
        } else {
            final Class<?> c = Typedef.raw(type);
            final JSONWriter.ObjectWriter oout = out.startObject();
            oout.startMember("name").writeString(Java.name(jsonType(c)));
            oout.finish();
        }
    }

    /**
     * Java has a more complex type hierarchy than does Javascript. This method
     * collapses parts of the Java type hierarchy to closer match Javascript.
     */
    static private Class<?>
    jsonType(final Class<?> r) {
        return Boolean.class == r
            ? boolean.class
        : byte.class == r || Byte.class == r ||
          short.class == r || Short.class == r ||
          int.class == r || Integer.class == r ||
          long.class == r || Long.class == r ||
          java.math.BigInteger.class == r ||
          float.class == r || Float.class == r ||
          double.class == r || Double.class == r ||
          java.math.BigDecimal.class == r
            ? Number.class
        : char.class == r || Character.class == r
            ? String.class
        : Class.class == r
            ? Type.class
        : Field.class == r || Member.class == r || Constructor.class == r
            ? Method.class
        : Exception.class == r
            ? RuntimeException.class
        : ConstArray.class.isAssignableFrom(r)
            ? ConstArray.class
        : r;
    }

    /**
     * Enumerate an inheritance chain from [ bottom, top ).
     * @param bottom    bottom of the inheritance chain
     * @param top       top of the inheritance chain.
     */
    static private PowerlessArray<String>
    upto(final Class<?> bottom, final Class<?> top) {
        final Class<?> limit = Struct.class.isAssignableFrom(bottom)
            ? Struct.class
        : RuntimeException.class.isAssignableFrom(bottom)
            ? (Exception.class.isAssignableFrom(top)
                ? RuntimeException.class
            : Exception.class)
        : Exception.class.isAssignableFrom(bottom)
            ? Throwable.class
        : Object.class;
        final PowerlessArray.Builder<String> r = builder(4);
        for (Class<?> i = bottom; top != i && limit != i; i=i.getSuperclass()) {
            if (Modifier.isPublic(i.getModifiers())) {
                try { r.append(Java.name(i)); } catch (final Exception e) {}
            }
        }
        return r.snapshot();
    }
    
    /**
     * Enumerate all types implemented by a class.
     */
    static private PowerlessArray<String>
    types(final Class<?> actual) {
        final Class<?> end =
            Struct.class.isAssignableFrom(actual) ? Struct.class : Object.class;
        final PowerlessArray.Builder<String> r = builder(4);
        for (Class<?> i=actual; end!=i; i=i.getSuperclass()) { ifaces(i, r); }
        return r.snapshot();
    }

    /**
     * List all the interfaces implemented by a class.
     */
    static private void
    ifaces(final Class<?> type, final ArrayBuilder<String> r) {
        if (type == Serializable.class) { return; }
        if (Modifier.isPublic(type.getModifiers())) {
            try { r.append(Java.name(type)); } catch (final Exception e) {}
        }
        for (final Class<?> i : type.getInterfaces()) { ifaces(i, r); }
    }
}
