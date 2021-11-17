package io.github.bsless.double_dispatch_lite;

import clojure.lang.*;

public class DoubleDispatchFn extends AFn implements IDoubleDispatch {
    public final IFn dispatch_fn;
    public final IAtom mapping;
    public IFn f;
    public final IFn mapping_compiler;

    private void refresh() {
        this.f = (IFn)(mapping_compiler.invoke(mapping));
    }

    private DoubleDispatchFn(IFn dispatch_fn, IAtom mapping, IFn mapping_compiler) {
        this.dispatch_fn = dispatch_fn;
        this.mapping = mapping;
        this.mapping_compiler = mapping_compiler;
        refresh();
    }

    public static DoubleDispatchFn create(IFn dispatch_fn, IFn mapping_compiler) {
        IAtom mapping = new Atom(PersistentArrayMap.EMPTY);
        return new DoubleDispatchFn(dispatch_fn, mapping, mapping_compiler);
    }

    public void add(Object k, IFn f) {
        IFn swap = new AFn() {
                public Object invoke(Object map) {
                    IPersistentMap m = (IPersistentMap) map;
                    return (Object)(m.assoc(k, f));
                }
            };
        mapping.swap(swap);
        refresh();
    }

    public void remove(Object k) {
        IFn swap = new AFn() {
                public Object invoke(Object map) {
                    IPersistentMap m = (IPersistentMap) map;
                    return (Object)(m.without(k));
                }
            };
        mapping.swap(swap);
        refresh();
    }

    public Object invoke(Object a) {
        return f.invoke(dispatch_fn.invoke(a), a);
    }

    public Object invoke(Object a, Object b) {
        return f.invoke(dispatch_fn.invoke(a, b), a, b);
    }

    public Object invoke(Object a, Object b, Object c) {
        return f.invoke(dispatch_fn.invoke(a, b, c), a, b, c);
    }

    public Object invoke(Object a, Object b, Object c, Object d) {
        return f.invoke(dispatch_fn.invoke(a, b, c, d), a, b, c, d);
    }

}
