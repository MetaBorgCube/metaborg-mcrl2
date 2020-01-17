package mcrl2typecheck.strategies;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.TopoSorter;
import mb.nabl2.util.TopoSorter.TopoSortedComponents;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IRelation2;

public class ConstraintStore<T> {

    private static final ILogger logger = LoggerUtils.logger(ConstraintStore.class);

    private static final boolean COMPUTE_CLOSURE = true;
    private static final boolean ADD_MISSING_BOUNDS = true;

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

    public boolean containsVar(T var) {
        return lowerBounds.containsKey(var) || upperBounds.containsKey(var) || closure.containsKey(var)
                || closure.containsValue(var);
    }

    public Iterable<T> allVars() {
        return Iterables.concat(lowerBounds.keySet(), upperBounds.keySet(), closure.keySet(), closure.valueSet());
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

        public boolean containsVar(T var) {
            return lowerBounds.containsKey(var) || upperBounds.containsKey(var) || closure.containsKey(var)
                    || closure.containsValue(var);
        }

        public Iterable<T> allVars() {
            return Iterables.concat(lowerBounds.keySet(), upperBounds.keySet(), closure.keySet(), closure.valueSet());
        }

        public void addAll(Collection<? extends Map.Entry<T, T>> constraints) throws ConstraintException {
            final Deque<Map.Entry<T, T>> worklist = new LinkedList<>();
            worklist.addAll(constraints);
            doAdd(worklist);
        }

        // FIXME All the work is redone, but we know the other store is already
        //       normalized. We only need to do something for the cases where
        //       the two stores interact
        public void addAll(ConstraintStore<T> other) throws ConstraintException {
            final Deque<Map.Entry<T, T>> worklist = new LinkedList<>();

            java.util.Map<Boolean, List<Entry<T, T>>> lbs = other.lowerBounds.entrySet().stream()
                    .collect(Collectors.partitioningBy(e -> containsVar(e.getKey())));
            java.util.Map<Boolean, List<Entry<T, T>>> ubs = other.upperBounds.entrySet().stream()
                    .collect(Collectors.partitioningBy(e -> containsVar(e.getKey())));
            java.util.Map<Boolean, List<Entry<T, T>>> cls = other.closure.entrySet().stream()
                    .collect(Collectors.partitioningBy(e -> containsVar(e.getKey()) || containsVar(e.getValue())));

            for(Map.Entry<T, T> e : lbs.get(true)) {
                addLowerBound(e.getKey(), e.getValue(), worklist::addLast);
            }
            lbs.get(false).forEach(e -> lowerBounds.put(e.getKey(), e.getValue()));

            for(Map.Entry<T, T> e : ubs.get(true)) {
                addUpperBound(e.getKey(), e.getValue(), worklist::addLast);
            }
            ubs.get(false).forEach(e -> upperBounds.put(e.getKey(), e.getValue()));

            for(Map.Entry<T, T> e : cls.get(true)) {
                addVars(e.getKey(), e.getValue(), worklist::addLast);
            }
            cls.get(false).forEach(e -> closure.put(e.getKey(), e.getValue()));

            doAdd(worklist);
        }

        private void doAdd(Deque<Map.Entry<T, T>> worklist) throws ConstraintException {
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
            if(closure.put(v1, v2) && COMPUTE_CLOSURE) {
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
                    for(T post2 : closure.get(v2)) {
                        add.apply(v1, post2);
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
            } else if(ADD_MISSING_BOUNDS) {
                final T ub = ops.top(lb).orElseThrow(() -> new ConstraintException());
                add.apply(v, ub);
            }

            if(COMPUTE_CLOSURE) {
                for(T post : closure.get(v)) {
                    add.apply(lb, post);
                }
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
            } else if(ADD_MISSING_BOUNDS) {
                final T lb = ops.bot(ub).orElseThrow(() -> new ConstraintException());
                add.apply(lb, v);
            }

            if(COMPUTE_CLOSURE) {
                for(T pre : closure.inverse().get(v)) {
                    add.apply(pre, ub);
                }
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

        public List<Entry<T, T>> gc(Collection<T> live) {
            final ImmutableList.Builder<Map.Entry<T, T>> substBuilder = ImmutableList.builder();
            final Deque<Map.Entry<T, T>> worklist = new LinkedList<>();
            fix: while(true) {
                final java.util.Set<T> inactiveVars = inactiveVars(live).stream().collect(Collectors.toSet());
                if(inactiveVars.isEmpty()) {
                    break;
                }
                for(Entry<T, T> bound : computeBounds(inactiveVars)) {
                    substBuilder.add(bound);
                    T v = bound.getKey();
                    T ty = bound.getValue();

                    // capture these before removing the variable
                    java.util.Set<T> lowerNegVars = getLowerNegsIfPosVar(v);

                    lowerBounds.__remove(v);
                    upperBounds.__remove(v);
                    closure.removeKey(v);
                    closure.removeValue(v);

                    try {
                        for(T lv : lowerNegVars) {
                            addUpperBound(lv, ty, worklist::addLast);
                        }
                        doAdd(worklist);
                        // Ignore the rest of the computed bounds, and go to next
                        // fixed point iteration. Not sure if this is necessary,
                        // but adding the bound may re-add constraints on the
                        // variables in ty (which comes later in the components)
                        continue fix;
                    } catch(ConstraintException ex) {
                        throw new IllegalStateException(
                                "Adding bounds during garbage collection should always succeed.", ex);
                    }


                }
            }

            final ImmutableList<Entry<T, T>> subst = substBuilder.build();
            return subst.reverse();
        }

        private java.util.Set<T> inactiveVars(Collection<T> live) {
            final java.util.Set<T> inactiveVars = Sets.newHashSet(allVars());
            // discover reachable
            final java.util.Set<T> visited = new HashSet<>();
            final Deque<T> worklist = new LinkedList<>();
            worklist.addAll(live);
            while(!worklist.isEmpty()) {
                final T ty = worklist.removeFirst();
                if(!ops.isVar(ty)) {
                    ops.allVars(ty).ifPresent(worklist::addAll);
                    continue;
                }
                final T var = ty;
                if(visited.contains(var)) {
                    continue;
                }
                inactiveVars.remove(var);
                visited.add(var);
                worklist.addAll(closure.get(var));
                worklist.addAll(closure.inverse().get(var));
                if(lowerBounds.containsKey(var)) {
                    worklist.add(lowerBounds.get(var));
                }
                if(upperBounds.containsKey(var)) {
                    worklist.add(upperBounds.get(var));
                }
            }
            return inactiveVars;
        }

        private List<Entry<T, T>> computeBounds(Collection<T> vars) {
            final TopoSortedComponents<T> components = TopoSorter.sort(vars, v -> {
                java.util.Set<T> negLVars = getLowerNegsIfPosVar(v);
                java.util.Set<T> bndVars = getBound(v).map(ty -> {
                    return ops.allVars(ty).orElseThrow(() -> new IllegalStateException("Failed to get vars of " + ty))
                            .stream().filter(vars::contains).collect(Collectors.toSet());
                }).orElse(Collections.emptySet());
                return Sets.union(negLVars, bndVars);
            });

            final ImmutableList.Builder<Map.Entry<T, T>> subst = ImmutableList.builder();
            for(Set.Immutable<T> component : components) {
                if(component.size() != 1) {
                    throw new IllegalStateException("Expected singleton components, got " + component);
                }
                T v = Iterables.getOnlyElement(component);
                getBound(v).ifPresent(ty -> subst.add(ImmutableTuple2.of(v, ty)));
            }

            return subst.build();
        }

        private Optional<T> getBound(T v) {
            if(ops.isPos(v)) {
                return Optional.ofNullable(lowerBounds.get(v));
            } else if(ops.isNeg(v)) {
                return Optional.ofNullable(upperBounds.get(v));
            } else {
                throw new IllegalStateException("Expceted variable, got " + v);
            }
        }

        private java.util.Set<T> getLowerNegsIfPosVar(T v) {
            if(ops.isPos(v)) {
                return closure.inverse().get(v).stream().filter(ops::isNeg).collect(Collectors.toSet());
            }
            return Collections.emptySet();
        }

        public ConstraintStore<T> freeze() {
            return new ConstraintStore<>(lowerBounds.freeze(), upperBounds.freeze(), closure.freeze());
        }

    }

    interface Ops<T> {

        boolean isVar(T ty);

        boolean isPos(T ty);

        boolean isNeg(T ty);

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