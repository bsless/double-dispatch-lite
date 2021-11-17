package io.github.bsless.double_dispatch_lite;

import clojure.lang.IFn;

public interface IDoubleDispatch {
    void add(Object k, IFn f);
    void remove(Object k);
}
