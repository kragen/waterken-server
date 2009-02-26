// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import static org.ref_send.promise.Eventual.near;
import static org.ref_send.promise.Eventual.ref;
import static org.ref_send.test.Logic.and;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;

/**
 * Checks invariants of the ref_send API.
 */
public final class
SoundCheck {
    private SoundCheck() {}
    
    /**
     * Runs a unit test.
     * @param _ eventual operator
     */
    static public Promise<Boolean>
    make(final Eventual _) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        r = r.with(testNormal(_, _));
        r = r.with(testNull(_, null));
        r = r.with(testDouble(_));
        r = r.with(testFloat(_));
        return and(_, r);
    }

    /**
     * Tests promises for a normal reference.
     */
    static private <T extends Receiver<?>> Promise<Boolean>
    testNormal(final Eventual _, final T x) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        
        final Promise<T> p = ref(x);
        check(p.equals(p));
        check(ref(x).equals(p));
        check(x.equals(p.call()));
        class EQ extends Do<T,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final T arg) throws Exception {
                check(x.equals(arg));
                return ref(true);
            }
        }
        r = r.with(_.when(p, new EQ()));
        r = r.with(_.when(x, new EQ()));
        r = r.with(_.when(new Sneaky<T>(x), new EQ()));
        
        final T x_ = _._(x);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_._(x).equals(x_));
        check(_.cast(Receiver.class, p).equals(x_));
        check(ref(x_).equals(p));
        check(x == near(x_));
        r = r.with(_.when(x_, new EQ()));
        
        return and(_, r);
    }

    /**
     * Tests promises for a <code>null</code>.
     */
    static private <T extends Receiver<?>> Promise<Boolean>
    testNull(final Eventual _, final T x) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        
        final Promise<T> p = ref(x);
        check(p.equals(p));
        check(!ref(x).equals(p));
        try {
            p.call();
            check(false);
        } catch (final NullPointerException e) {}
        class NE extends Do<T,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final T arg) throws Exception { throw new Exception(); }
            
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NullPointerException) {return ref(true);}
                throw reason;
            }
        }
        r = r.with(_.when(p, new NE()));
        r = r.with(_.when(x, new NE()));
        r = r.with(_.when(new Sneaky<T>(x), new NE()));
        
        final T x_ = _.cast(Receiver.class, p);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_.cast(Receiver.class, p).equals(x_));
        check(ref(x_).equals(p));
        r = r.with(_.when(x_, new NE()));
        
        return and(_, r);
    }
    
    static private <T> Promise<Boolean>
    testNaN(final Eventual _, final T x) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        
        final Promise<T> p = ref(x);
        check(p.equals(p));
        check(!p.equals(ref(x)));
        try {
            p.call();
            check(false);
        } catch (final ArithmeticException e) {}
        class ENaN extends Do<T,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final T arg) throws Exception { throw new Exception(); }
            
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof ArithmeticException) { return ref(true); }
                throw reason;
            }
        }
        r = r.with(_.when(p, new ENaN()));
        r = r.with(_.when(x, new ENaN()));
        r = r.with(_.when(new Sneaky<T>(x), new ENaN()));

        return and(_, r);
    }

    /**
     * Tests promises for {@link Double}.
     */
    static private Promise<Boolean>
    testDouble(final Eventual _) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        
        // check normal handling
        final Promise<Double> pMin = ref(Double.MIN_VALUE);
        check(pMin.equals(pMin));
        check(ref(Double.MIN_VALUE).equals(pMin));
        check(Double.MIN_VALUE == pMin.call());
        class EQ extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                check(Double.MIN_VALUE == arg);
                return ref(true);
            }
        }
        r = r.with(_.when(pMin, new EQ()));
        r = r.with(_.when(Double.MIN_VALUE, new EQ()));
        
        r = r.with(testNaN(_, Double.NaN));
        r = r.with(testNaN(_, Double.NEGATIVE_INFINITY));
        r = r.with(testNaN(_, Double.POSITIVE_INFINITY));

        return and(_, r);
    }

    /**
     * Tests promises for {@link Float}.
     */
    static private Promise<Boolean>
    testFloat(final Eventual _) throws Exception {
        ConstArray<Promise<Boolean>> r = new ConstArray<Promise<Boolean>>();
        
        // check normal handling
        final Promise<Float> pMin = ref(Float.MIN_VALUE);
        check(pMin.equals(pMin));
        check(ref(Float.MIN_VALUE).equals(pMin));
        check(Float.MIN_VALUE == pMin.call());
        class EQ extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                check(Float.MIN_VALUE == arg);
                return ref(true);
            }
        }
        r = r.with(_.when(pMin, new EQ()));
        r = r.with(_.when(Float.MIN_VALUE, new EQ()));
        
        r = r.with(testNaN(_, Float.NaN));
        r = r.with(testNaN(_, Float.NEGATIVE_INFINITY));
        r = r.with(testNaN(_, Float.POSITIVE_INFINITY));

        return and(_, r);
    }
    
    static private void
    check(final boolean valid) throws Exception {
        if (!valid) { throw new Exception(); }
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Promise<?>> work = List.list();
        final Promise<Boolean> result = make(new Eventual(work.appender()));
        while (!work.isEmpty()) { work.pop().call(); }
        if (!result.call()) { throw new Exception("test failed"); }
    }
}
