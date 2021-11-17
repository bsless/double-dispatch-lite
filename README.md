# io.github.bsless/double-dispatch-lite

Experimental implementation of double dispatch with no table lookup (and
no hierarchies)

Can also be thought of as faster multimethods with slightly less capabilities.

## Usage

```clojure
(require '[bsless.double-dispatch-lite :as dd])

(dd/defmulti* wizardry8 even?)

(dd/defmethod* wizardry8 true [_] "even!")
(dd/defmethod* wizardry8 false [_] "odd!")

```

### Extension

By default, dispatch values will be compared using `=`.

This can be overriden by using the `->=` constructor:

```clojure
(defmethod dd/->= String [^String x] #(.equals x %))
```

It dispatches by `clojure.core/type`.

## But, why?

Mainly, to see if it's possible.

Potential use cases can be needing a faster, JIT friendlier
implementation.

## Limitations

- `defmethod*` is not thread safe
- No hierarchies
- Haven't tested performance for large tables

## Design

Why is it fast?

Generally, double dispatch follows the form:

```clojure
(apply (get dispatch-table (apply dispatch-fn args)) args)
```

Where the dispatch table is a mapping from values to functions.

What we'd like is to unroll this lookup.

Just like sometimes, linear search in an array is faster then hash table
lookup, unrolling those iterations can be even faster.

### Unrolling

The code we want to run looks something like:

```clojure
(fn [arg]
  (let [dv (dispatch-fn arg)]
    (if (= dv k0)
      (f0 arg)
      (if (= dv k1)
        (f1 arg)
        (if (= dv k2)
          (f2 arg)
          ,,,)))))
```

Where `ki` is the ith dispatch key, `fi` is the function associated with
it in the dispatch table:

```clojure
{ki fi}
```

We can generalize this to a form of current/rest:


```clojure
(fn [arg]
  (let [dv (dispatch-fn arg)]
    (if (= dv k)
      (f arg)
      (f-rest dv arg))))
```

Now, let's say we don't know `dv`, `f-rest`, `k` or `f`, then we'll need
to turn them into parameters.

First, extract the dispatch value, `dv`:


```clojure
(fn [dv arg]
  (if (= dv k)
    (f arg)
    (f-rest dv arg)))
```

Then, close over the rest we don't know:

```clojure
(fn [dv arg]
  (fn [f-rest k f]
    (if (= dv k)
      (f arg)
      (f-rest dv arg))))
```

But we can know `f-rest`, `k` and `f` before we know `dv` and `arg`, so
we can switch the closure order:

```clojure
(fn [f-rest k f]
  (fn [dv arg]
    (if (= dv k)
      (f arg)
      (f-rest dv arg))))
```

Now we have a function we can pass to `reduce-kv` over the dispatch table!

```clojure
(reduce-kv
 (fn [f-rest k f]
   (fn [dv arg]
     (if (= dv k)
       (f arg)
       (f-rest dv arg))))
 default-fn
 dispatch-table)
```

What we get back is a function of the dispatch value and the argument, to use it:

```clojure
(f* (dispatch-fn arg) arg)
```

And thus double dispatch is unrolled.

### Equality

Additionally, we can optimize the equality checks since we know the
values and their types.

Since we know the type of `k` at closure time, we can select for an
optimal equality predicate instead of one which dynamically dispatches
through instance checks at run time like `=` does.

We want some mapping of:

```
predFactory -> a -> (a -> Boolean)
```

We'll call it `->=`, and we want to use it like so:

```clojure
(fn [f-rest k f]
  (let [=* (->= k)]
    (fn [dv arg]
      (if (=* dv)
        (f arg)
        (f-rest dv arg)))))
```

It's a function which returns a function that checks whether its argument is equal to `k`.

Since we know `k`'s type beforehand, we can dispatch on it:

```clojure
(defmulti ->= type)
(defmethod ->= String [^String x] #(.equals x %))
```

This way, we have closed over everything we know when we know it.

## License

Copyright © 2021 Ben Sless

Distributed under the Eclipse Public License version 1.0.
