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

## License

Copyright © 2021 Ben Sless

Distributed under the Eclipse Public License version 1.0.
