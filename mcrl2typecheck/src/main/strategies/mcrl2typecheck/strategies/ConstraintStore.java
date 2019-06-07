package mcrl2typecheck.strategies;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IRelation2;

public class ConstraintStore<T> {
    private static final ILogger logger = LoggerUtils.logger(ConstraintStore.class);

    private final Map.Immutable<T, T> lowerBounds;
    private final Map.Immutable<T, T> upperBounds;
    private final IRelation2.Immutable<T, T> closure;

    private ConstraintStore(Map.Immutable<T, T> lowerBounds, Map.Immutable<T, T> upperBounds,
            IRelation2.Immutable<T, T> closure) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.closure = closure;
    }

    public int size() {
        return lowerBounds.size() + upperBounds.size() + closure.size();
    }

    public List<Tuple2<T, T>> getConstraints() {
        final ImmutableList.Builder<Tuple2<T, T>> constraints = ImmutableList.builder();
        for(Map.Entry<T, T> lb : lowerBounds.entrySet()) {
            constraints.add(ImmutableTuple2.of(lb.getValue(), lb.getKey()));
        }
        for(Map.Entry<T, T> ub : upperBounds.entrySet()) {
            constraints.add(ImmutableTuple2.of(ub.getKey(), ub.getValue()));
        }
        for(Map.Entry<T, T> e : closure.entrySet()) {
            constraints.add(ImmutableTuple2.of(e.getKey(), e.getValue()));
        }
        return constraints.build();
    }

    public String toString(Function1<T, String> toString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        AtomicBoolean first = new AtomicBoolean(true);
        for(T var : Sets.union(lowerBounds.keySet(), upperBounds.keySet())) {
            if(!first.getAndSet(false)) {
                sb.append(",");
            }
            if(lowerBounds.containsKey(var)) {
                sb.append(" ").append(toString.apply(lowerBounds.get(var))).append(" <:");
            }
            sb.append(" ").append(toString.apply(var));
            if(upperBounds.containsKey(var)) {
                sb.append(" <: ").append(toString.apply(upperBounds.get(var)));
            }
        }
        for(Map.Entry<T, T> entry : closure.entrySet()) {
            if(!first.getAndSet(false)) {
                sb.append(",");
            }
            sb.append(" ").append(toString.apply(entry.getKey())).append(" <: ")
                    .append(toString.apply(entry.getValue()));
        }
        if(!first.get()) {
            sb.append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(Object::toString);
    }

    public Transient<T> melt(Ops<T> ops) {
        return new Transient<>(lowerBounds.asTransient(), upperBounds.asTransient(), closure.melt(), ops);
    }

    public static <T> ConstraintStore<T> of() {
        return new ConstraintStore<>(Map.Immutable.of(), Map.Immutable.of(), HashTrieRelation2.Immutable.of());
    }

    public static class Transient<T> {

        private final Map.Transient<T, T> lowerBounds;
        private final Map.Transient<T, T> upperBounds;
        private final IRelation2.Transient<T, T> closure;

        private final Ops<T> ops;

        protected Transient(Map.Transient<T, T> lowerBounds, Map.Transient<T, T> upperBounds,
                IRelation2.Transient<T, T> closure, Ops<T> ops) {
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
            this.closure = closure;
            this.ops = ops;
        }

        public void addAll(Collection<? extends Map.Entry<T, T>> constraints) throws ConstraintException {
            final Deque<Map.Entry<T, T>> worklist = new LinkedList<>();
            worklist.addAll(constraints);
            while(!worklist.isEmpty()) {
                final Map.Entry<T, T> constraint = worklist.removeFirst();
                final T ty1 = constraint.getKey();
                final T ty2 = constraint.getValue();
                boolean isVar1 = ops.isVar(ty1);
                boolean isVar2 = ops.isVar(ty2);
                if(isVar1 && isVar2) {
                    addVars(ty1, ty2, worklist::addLast);
                } else if(isVar1) {
                    addUpperBound(ty1, ty2, worklist::addLast);
                } else if(isVar2) {
                    addLowerBound(ty2, ty1, worklist::addLast);
                } else {
                    checkTypes(ty1, ty2, worklist::addLast);
                }
            }
        }

        /*
         * add V1 <: V2
         *   - pre(V1) <: V2
         *   - V1 <: post(V2)
         *   - pre(V1) + V1 <: ub(V2)
         *   - lb(V1) <: V2 + post(V2)
         */
        private void addVars(T v1, T v2, Add<T> add) throws ConstraintException {
            if(v1.equals(v2)) {
                return;
            }
            if(closure.put(v1, v2)) {
                final T lb = lowerBounds.get(v1);
                if(lb != null) {
                    add.apply(lb, v2);
                }
                final T ub = upperBounds.get(v2);
                if(ub != null) {
                    add.apply(v1, ub);
                }
                for(T pre1 : closure.inverse().get(v1)) {
                    add.apply(pre1, v2);
                    if(ub != null) {
                        add.apply(pre1, ub);
                    }
                    for(T post2 : closure.get(v2)) {
                        add.apply(v1, post2);
                        if(lb != null) {
                            add.apply(lb, post2);
                        }
                    }
                }
            }
        }

        /*
         * add T1 <: V2
         *   - lub
         *   - invent ub
         *   - lb(V2) <: ub(V2)
         *   - T1 <: post(V2)
         */
        private void addLowerBound(T v, T lb, Add<T> add) throws ConstraintException {
            if(lowerBounds.containsKey(v)) {
                final Tuple2<T, List<Tuple2<T, T>>> typeAndConstraints =
                        ops.lub(lb, lowerBounds.get(v)).orElseThrow(() -> new ConstraintException());
                lb = typeAndConstraints._1();
                add.applyAll(typeAndConstraints._2());
            }
            lowerBounds.__put(v, lb);
            if(upperBounds.containsKey(v)) {
                add.apply(lb, upperBounds.get(v));
            } else {
                final T ub = ops.top(lb).orElseThrow(() -> new ConstraintException());
                add.apply(v, ub);
            }
            for(T post : closure.get(v)) {
                add.apply(lb, post);
            }
        }

        /*
         * add V1 <: T2
         *   - glb
         *   - invent lb
         *   - lb(V1) <: ub(V1)
         *   - pre(V1) <: T2
         */
        private void addUpperBound(T v, T ub, Add<T> add) throws ConstraintException {
            if(upperBounds.containsKey(v)) {
                final Tuple2<T, List<Tuple2<T, T>>> typeAndConstraints =
                        ops.glb(ub, upperBounds.get(v)).orElseThrow(() -> new ConstraintException());
                ub = typeAndConstraints._1();
                add.applyAll(typeAndConstraints._2());
            }
            upperBounds.__put(v, ub);
            if(lowerBounds.containsKey(v)) {
                add.apply(lowerBounds.get(v), ub);
            } else {
                final T lb = ops.bot(ub).orElseThrow(() -> new ConstraintException());
                add.apply(lb, v);
            }
            for(T pre : closure.inverse().get(v)) {
                add.apply(pre, ub);
            }
        }

        /*
         * add T1 <: T2
         *   - sub
         */
        private void checkTypes(T ty1, T ty2, Add<T> add) throws ConstraintException {
            final List<Tuple2<T, T>> constraints = ops.sub(ty1, ty2).orElseThrow(() -> new ConstraintException());
            add.applyAll(constraints);
        }

        public void gc(Iterable<T> live) {

        }

        public ConstraintStore<T> freeze() {
            return new ConstraintStore<>(lowerBounds.freeze(), upperBounds.freeze(), closure.freeze());
        }

    }

    interface Ops<T> {

        boolean isVar(T ty);

        Optional<List<T>> allVars(T ty);

        Optional<List<Tuple2<T, T>>> sub(T ty1, T ty2);

        Optional<Tuple2<T, List<Tuple2<T, T>>>> glb(T ty1, T ty2);

        Optional<Tuple2<T, List<Tuple2<T, T>>>> lub(T ty1, T ty2);

        Optional<T> bot(T ty);

        Optional<T> top(T ty);

        String toString(T ty);

    }

    @FunctionalInterface
    interface Add<T> extends Action1<Map.Entry<T, T>> {

        default void apply(T ty1, T ty2) {
            apply(ImmutableTuple2.of(ty1, ty2));
        }

        default void applyAll(Iterable<? extends Map.Entry<T, T>> constraints) {
            constraints.forEach(this::apply);
        }

    }

    public static class ConstraintException extends Exception {
        private static final long serialVersionUID = 1L;
    }

}