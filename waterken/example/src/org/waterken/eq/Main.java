// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.promise.eventual.Eventual.near;
import static org.ref_send.test.Logic.and;

import java.io.Serializable;
import java.util.ArrayList;

import org.joe_e.Equatable;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.list.List;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.NaN;
import org.ref_send.promise.NegativeInfinity;
import org.ref_send.promise.PositiveInfinity;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
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
     * Constructs an instance.
     * @param _ eventual operator
     */
    public
    Main(final Eventual _) {
        this._ = _;
    }
    
    // Command line interface

    /**
     * Executes the test.
     * @param args  ignored
     * @throws Exception    test failed
     */
    static public void
    main(final String[] args) throws Exception {
        final List<Task> work = List.list();
        final Eventual _ = new Eventual(new Token(), new Loop<Task>() {
            public void
            run(final Task task) { work.append(task); }
        });
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
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        r.add(testNormal());
        r.add(testNull());
        r.add(testDouble());
        r.add(testFloat());
        return and(_, r.toArray(new Promise[r.size()]));
    }
    
    // org.waterken.eq.Main interface

    /**
     * Tests promises for a normal reference.
     */
    public Promise<Boolean>
    testNormal() throws Exception {
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        class Normal implements Runnable, Equatable, Serializable {
            static private final long serialVersionUID = 1L;

            public void run() {}
        }
        final Normal x = new Normal();
        final Runnable ix = x;
        final Promise<Runnable> p = ref(ix);
        check(p.equals(p));
        check(ref(x).equals(p));
        check(x == p.cast());
        class EQ extends Do<Runnable,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Runnable arg) throws Exception {
                check(x == arg);
                return ref(true);
            }
        }
        r.add(_.when(p, new EQ()));
        r.add(_.when(ix, new EQ()));
        final Runnable x_ = _._(ix);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_._(ix).equals(x_));
        check(_.cast(Runnable.class, p).equals(x_));
        check(Eventual.promised(x_).equals(p));
        check(x == near(x_));
        r.add(_.when(x_, new EQ()));
        return and(_, r.toArray(new Promise[r.size()]));
    }

    /**
     * Tests promises for a <code>null</code>.
     */
    public Promise<Boolean>
    testNull() throws Exception {
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        final Promise<Runnable> p = ref(null);
        check(p.equals(p));
        check(!ref(null).equals(p));
        try {
            p.cast();
            check(false);
        } catch (final NullPointerException e) {}
        class NE extends Do<Runnable,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Runnable arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NullPointerException) {return ref(true);}
                throw reason;
            }
        }
        r.add(_.when(p, new NE()));
        final Runnable x = null;
        r.add(_.when(x, new NE()));
        final Promise<Runnable> sneaky = Fulfilled.detach(null); 
        r.add(_.when(sneaky, new NE()));
        final Runnable x_ = _.cast(Runnable.class, p);
        check(x_.equals(x_));
        check(_._(x_).equals(x_));
        check(_.cast(Runnable.class, p).equals(x_));
        check(Eventual.promised(x_).equals(p));
        r.add(_.when(x_, new NE()));
        return and(_, r.toArray(new Promise[r.size()]));
    }

    /**
     * Tests promises for {@link Double}.
     */
    public Promise<Boolean>
    testDouble() throws Exception {
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        
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
        r.add(_.when(pMin, new EQ()));
        r.add(_.when(Double.MIN_VALUE, new EQ()));
        
        // check NaN handling
        final Promise<Double> pNaN = ref(Double.NaN);
        check(pNaN.equals(pNaN));
        check(!pNaN.equals(ref(Double.NaN)));
        try {
            pNaN.cast();
            check(false);
        } catch (final NaN e) {}
        class ENaN extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NaN) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pNaN, new ENaN()));
        r.add(_.when(Double.NaN, new ENaN()));
        r.add(_.when(Fulfilled.detach(Double.NaN), new ENaN()));
        
        // check -infinity handling
        final Promise<Double> pMinusInfinity = ref(Double.NEGATIVE_INFINITY);
        check(pMinusInfinity.equals(pMinusInfinity));
        check(!pMinusInfinity.equals(ref(Double.NEGATIVE_INFINITY)));
        try {
            pMinusInfinity.cast();
            check(false);
        } catch (final NegativeInfinity e) {}
        class ENI extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NegativeInfinity) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pMinusInfinity, new ENI()));
        r.add(_.when(Double.NEGATIVE_INFINITY, new ENI()));
        r.add(_.when(Fulfilled.detach(Double.NEGATIVE_INFINITY), new ENI()));
        
        // check +infinity handling
        final Promise<Double> pPlusInfinity = ref(Double.POSITIVE_INFINITY);
        check(pPlusInfinity.equals(pPlusInfinity));
        check(!pPlusInfinity.equals(ref(Double.POSITIVE_INFINITY)));
        try {
            pPlusInfinity.cast();
            check(false);
        } catch (final PositiveInfinity e) {}
        class EPI extends Do<Double,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Double arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof PositiveInfinity) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pPlusInfinity, new EPI()));
        r.add(_.when(Double.POSITIVE_INFINITY, new EPI()));
        r.add(_.when(Fulfilled.detach(Double.POSITIVE_INFINITY), new EPI()));

        return and(_, r.toArray(new Promise[r.size()]));
    }

    /**
     * Tests promises for {@link Float}.
     */
    public Promise<Boolean>
    testFloat() throws Exception {
        final ArrayList<Promise<Boolean>> r = new ArrayList<Promise<Boolean>>();
        
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
        r.add(_.when(pMin, new EQ()));
        r.add(_.when(Float.MIN_VALUE, new EQ()));
        
        // check NaN handling
        final Promise<Float> pNaN = ref(Float.NaN);
        check(pNaN.equals(pNaN));
        check(!pNaN.equals(ref(Float.NaN)));
        try {
            pNaN.cast();
            check(false);
        } catch (final NaN e) {}
        class ENaN extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NaN) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pNaN, new ENaN()));
        r.add(_.when(Float.NaN, new ENaN()));
        r.add(_.when(Fulfilled.detach(Float.NaN), new ENaN()));
        
        // check -infinity handling
        final Promise<Float> pMinusInfinity = ref(Float.NEGATIVE_INFINITY);
        check(pMinusInfinity.equals(pMinusInfinity));
        check(!pMinusInfinity.equals(ref(Float.NEGATIVE_INFINITY)));
        try {
            pMinusInfinity.cast();
            check(false);
        } catch (final NegativeInfinity e) {}
        class ENI extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof NegativeInfinity) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pMinusInfinity, new ENI()));
        r.add(_.when(Float.NEGATIVE_INFINITY, new ENI()));
        r.add(_.when(Fulfilled.detach(Float.NEGATIVE_INFINITY), new ENI()));
        
        // check +infinity handling
        final Promise<Float> pPlusInfinity = ref(Float.POSITIVE_INFINITY);
        check(pPlusInfinity.equals(pPlusInfinity));
        check(!pPlusInfinity.equals(ref(Float.POSITIVE_INFINITY)));
        try {
            pPlusInfinity.cast();
            check(false);
        } catch (final PositiveInfinity e) {}
        class EPI extends Do<Float,Promise<Boolean>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Boolean>
            fulfill(final Float arg) throws Exception {
                throw new Exception();
            }
            public Promise<Boolean>
            reject(final Exception reason) throws Exception {
                if (reason instanceof PositiveInfinity) { return ref(true); }
                throw reason;
            }
        }
        r.add(_.when(pPlusInfinity, new EPI()));
        r.add(_.when(Float.POSITIVE_INFINITY, new EPI()));
        r.add(_.when(Fulfilled.detach(Float.POSITIVE_INFINITY), new EPI()));

        return and(_, r.toArray(new Promise[r.size()]));
    }
    
    static private void
    check(final boolean valid) throws Exception {
        if (!valid) { throw new Exception(); }
    }
}
