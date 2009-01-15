// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import static org.ref_send.promise.Fulfilled.detach;
import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.promise.eventual.Eventual.near;
import static org.ref_send.test.Logic.and;

import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.list.List;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.ref_send.test.Test;

/**
 * Checks invariants of the ref_send API.
 */
public final class
Main extends Struct implements Test, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    private final Eventual _;

    /**
     * Constructs an instance
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    static public Test
    make(final Eventual _) {
        return new Main(_);
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Task<?>> work = List.list();
        final Eventual _ = new Eventual(work.appender());
        final Test test = new Main(_);
        final Promise<Boolean> result = test.start();
        while (!work.isEmpty()) { work.pop().run(); }
        if (!result.cast()) { throw new Exception("test incomplete"); }
    }
    
    // org.ref_send.test.Test interface

    /**
     * Starts all the tests.
     */
    public Promise<Boolean>
    start() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(4);
        r.append(testNormal());
        r.append(testNull());
        r.append(testDouble());
        r.append(testFloat());
        return and(_, r.snapshot());
    }
    
    // org.waterken.eq.Main interface

    /**
     * Tests promises for a normal reference.
     */
    public Promise<Boolean>
    testNormal() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(3);
        
        class Normal implements Receiver<Void>, Equatable, Serializable {
            static private final long serialVersionUID = 1L;

            public void run(final Void ignored) {}
        }
        final Normal x = new Normal();
        final Receiver<Void> ix = x;
        final Promise<Receiver<Void>> p = ref(ix);
        check(p.equals(p));
        check(ref(x).equals(p));
        check(x == p.cast());
        class EQ extends Do<Receiver<Void>,Promise<Boolean>>
                 implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Receiver<Void> arg) throws Exception {
                check(x == arg);
                return ref(true);
            }
        }
        r.append(_.when(p, new EQ()));
        r.append(_.when(ix, new EQ()));
        final Receiver<Void> x_ = _._(ix);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_._(ix).equals(x_));
        check(_.cast(Receiver.class, p).equals(x_));
        check(Eventual.promised(x_).equals(p));
        check(x == near(x_));
        r.append(_.when(x_, new EQ()));
        
        return and(_, r.snapshot());
    }

    /**
     * Tests promises for a <code>null</code>.
     */
    public Promise<Boolean>
    testNull() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(4);
        
        final Promise<Receiver<Void>> p = ref(null);
        check(p.equals(p));
        check(!ref(null).equals(p));
        try {
            p.cast();
            check(false);
        } catch (final NullPointerException e) {}
        class NE extends Do<Receiver<Void>,Promise<Boolean>>
                 implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Receiver<Void> arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NullPointerException) {return ref(true);}
                throw reason;
            }
        }
        r.append(_.when(p, new NE()));
        final Receiver<Void> x = null;
        r.append(_.when(x, new NE()));
        final Promise<Receiver<Void>> sneaky = detach(null); 
        r.append(_.when(sneaky, new NE()));
        final Receiver<Void> x_ = _.cast(Receiver.class, p);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_.cast(Receiver.class, p).equals(x_));
        check(Eventual.promised(x_).equals(p));
        r.append(_.when(x_, new NE()));
        
        return and(_, r.snapshot());
    }

    /**
     * Tests promises for {@link Double}.
     */
    public Promise<Boolean>
    testDouble() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(5);
        
        // check normal handling
        final Promise<Double> pMin = ref(Double.MIN_VALUE);
        check(pMin.equals(pMin));
        check(ref(Double.MIN_VALUE).equals(pMin));
        check(Double.MIN_VALUE == pMin.cast());
        class EQ extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                check(Double.MIN_VALUE == arg);
                return ref(true);
            }
        }
        r.append(_.when(pMin, new EQ()));
        r.append(_.when(Double.MIN_VALUE, new EQ()));
        
        r.append(testDoubleNaN(Double.NaN));
        r.append(testDoubleNaN(Double.NEGATIVE_INFINITY));
        r.append(testDoubleNaN(Double.POSITIVE_INFINITY));

        return and(_, r.snapshot());
    }
    
    private Promise<Boolean>
    testDoubleNaN(final double NaN) throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(3);
        
        final Promise<Double> pNaN = ref(NaN);
        check(pNaN.equals(pNaN));
        check(!pNaN.equals(ref(NaN)));
        try {
            pNaN.cast();
            check(false);
        } catch (final ArithmeticException e) {}
        class ENaN extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof ArithmeticException) { return ref(true); }
                throw reason;
            }
        }
        r.append(_.when(pNaN, new ENaN()));
        r.append(_.when(NaN, new ENaN()));
        r.append(_.when(detach(NaN), new ENaN()));

        return and(_, r.snapshot());
    }

    /**
     * Tests promises for {@link Float}.
     */
    public Promise<Boolean>
    testFloat() throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(5);
        
        // check normal handling
        final Promise<Float> pMin = ref(Float.MIN_VALUE);
        check(pMin.equals(pMin));
        check(ref(Float.MIN_VALUE).equals(pMin));
        check(Float.MIN_VALUE == pMin.cast());
        class EQ extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                check(Float.MIN_VALUE == arg);
                return ref(true);
            }
        }
        r.append(_.when(pMin, new EQ()));
        r.append(_.when(Float.MIN_VALUE, new EQ()));
        
        r.append(testFloatNaN(Float.NaN));
        r.append(testFloatNaN(Float.NEGATIVE_INFINITY));
        r.append(testFloatNaN(Float.POSITIVE_INFINITY));

        return and(_, r.snapshot());
    }
    
    private Promise<Boolean>
    testFloatNaN(final float NaN) throws Exception {
        final ConstArray.Builder<Volatile<Boolean>> r = ConstArray.builder(3);
        
        final Promise<Float> pNaN = ref(NaN);
        check(pNaN.equals(pNaN));
        check(!pNaN.equals(ref(NaN)));
        try {
            pNaN.cast();
            check(false);
        } catch (final ArithmeticException e) {}
        class ENaN extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof ArithmeticException) { return ref(true); }
                throw reason;
            }
        }
        r.append(_.when(pNaN, new ENaN()));
        r.append(_.when(NaN, new ENaN()));
        r.append(_.when(detach(NaN), new ENaN()));

        return and(_, r.snapshot());
    }
    
    static private void
    check(final boolean valid) throws Exception {
        if (!valid) { throw new Exception(); }
    }
}
