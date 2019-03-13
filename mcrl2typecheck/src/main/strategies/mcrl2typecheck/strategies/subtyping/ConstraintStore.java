package mcrl2typecheck.strategies.subtyping;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IRelation2;
import rx.functions.Action2;
import rx.functions.Action3;

public class ConstraintStore<T> implements IConstraintStore<T> {

    private static final boolean DEBUG = false;
    private static final ILogger logger = LoggerUtils.logger(ConstraintStore.class);

    private final IRelation2.Immutable<T, T> closure;
    private final Map.Immutable<T, T> lowerBounds;
    private final Map.Immutable<T, T> upperBounds;

    private ConstraintStore(IRelation2.Immutable<T, T> closure, Map.Immutable<T, T> lowerBounds,
            Map.Immutable<T, T> upperBounds) {
        this.closure = closure;
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
    }

    @Override public int size() {
        return lowerBounds.size() + closure.size() + upperBounds.size();
    }

    @Override public Iterable<Map.Entry<T, T>> constraints() {
        return Iterables.concat(Iterables.transform(lowerBounds.entrySet(), this::flip), upperBounds.entrySet(),
                closure.entrySet());
    }

    @Override public IConstraintStore<T> tellAll(IConstraintStore<T> store, IsVar<T> isVar, Decompose<T> decompose,
            ComputeBound<T> glb, ComputeBound<T> lub, FormatType<T> typeToString) throws UnsatisfiableException {
        if(size() > store.size()) {
            return tellAll(store.constraints(), isVar, decompose, glb, lub, typeToString);
        } else {
            return store.tellAll(constraints(), isVar, decompose, glb, lub, typeToString);
        }
    }

    @Override public IConstraintStore<T> tellAll(Iterable<Entry<T, T>> constraints, IsVar<T> isVar,
            Decompose<T> decompose, ComputeBound<T> glb, ComputeBound<T> lub, FormatType<T> typeToString)
            throws UnsatisfiableException {
        final IRelation2.Transient<T, T> closure = this.closure.melt();
        final Map.Transient<T, T> lowerBounds = this.lowerBounds.asTransient();
        final Map.Transient<T, T> upperBounds = this.upperBounds.asTransient();

        Iterables2.stream(constraints).forEach(tys -> {
            if(DEBUG)
                logger.info("tell {} <: {}", typeToString.apply(tys.getKey()), typeToString.apply(tys.getValue()));
        });
        final Deque<Entry<T, T>> worklist = Lists.newLinkedList(constraints);
        final Action3<String, T, T> tell = (s, t1, t2) -> {
            if(DEBUG)
                logger.info("{} {} <: {}", s, typeToString.apply(t1), typeToString.apply(t2));
            worklist.add(ImmutableTuple2.of(t1, t2));
        };
        final Action2<String, Collection<Tuple2<T, T>>> tellAll = (s, ts) -> {
            ts.forEach(tu -> tell.call(s, tu._1(), tu._2()));
        };

        while(!worklist.isEmpty()) {
            final Entry<T, T> constraint = worklist.pop();
            final boolean isVar1 = isVar.test(constraint.getKey());
            final boolean isVar2 = isVar.test(constraint.getValue());
            if(isVar1 && isVar2) { // V1 <: V2
                final T var1 = constraint.getKey();
                final T var2 = constraint.getValue();
                // skip if equal or already present
                if(var1.equals(var2) || closure.containsEntry(var1, var2)) {
                    continue;
                }
                // compute variable closure
                for(T preVar1 : Iterables2.cons(var1, closure.inverse().get(var1))) {
                    for(T postVar2 : Iterables2.cons(var2, closure.get(var2))) {
                        if(DEBUG)
                            logger.info("close {} <: {}", typeToString.apply(preVar1), typeToString.apply(postVar2));
                        closure.put(preVar1, postVar2);
                    }
                }
                // propagate bounds
                if(lowerBounds.containsKey(var1)) {
                    final T lb = lowerBounds.get(var1);
                    tell.call("close", lb, var2);
                }
                // propagate upper bounds down
                if(upperBounds.containsKey(var2)) {
                    final T ub = upperBounds.get(var2);
                    tell.call("close", var1, ub);
                }
            } else if(isVar1) { // V1 <: T2
                final T var1 = constraint.getKey();
                final T type2 = constraint.getValue();
                for(T preVar1 : Iterables2.cons(var1, closure.inverse().get(var1))) {
                    final T upperBound;
                    if(upperBounds.containsKey(preVar1)) {
                        final T ub = upperBounds.get(preVar1);
                        final Tuple2<T, Collection<Tuple2<T, T>>> glbResult = glb.apply(ub, type2);
                        if(glbResult == null) {
                            throw new UnsatisfiableException(
                                    logger.format("glb {} {}", typeToString.apply(ub), typeToString.apply(type2)));
                        }
                        if(DEBUG)
                            logger.info("close {} <: {} [glb {} {}]", typeToString.apply(preVar1),
                                    typeToString.apply(glbResult._1()), typeToString.apply(ub),
                                    typeToString.apply(type2));
                        upperBounds.__put(preVar1, (upperBound = glbResult._1()));
                        tellAll.call("glb", glbResult._2());
                    } else {
                        if(DEBUG)
                            logger.info("close {} <: {}", typeToString.apply(preVar1), typeToString.apply(type2));
                        upperBounds.__put(preVar1, (upperBound = type2));
                    }
                    if(lowerBounds.containsKey(preVar1)) {
                        final T lb = lowerBounds.get(preVar1);
                        tell.call("check", lb, upperBound);
                    }
                }
            } else if(isVar2) { // T1 <: V2
                final T type1 = constraint.getKey();
                final T var2 = constraint.getValue();
                for(T postVar2 : Iterables2.cons(var2, closure.get(var2))) {
                    final T lowerBound;
                    if(lowerBounds.containsKey(postVar2)) {
                        final T lb = lowerBounds.get(postVar2);
                        final Tuple2<T, Collection<Tuple2<T, T>>> lubResult = lub.apply(type1, lb);
                        if(lubResult == null) {
                            throw new UnsatisfiableException(
                                    logger.format("lub {} {}", typeToString.apply(type1), typeToString.apply(lb)));
                        }
                        if(DEBUG)
                            logger.info("close {} <: {} [lub {} {}]", typeToString.apply(lubResult._1()),
                                    typeToString.apply(postVar2), typeToString.apply(lb), typeToString.apply(type1));
                        lowerBounds.__put(postVar2, (lowerBound = lubResult._1()));
                        tellAll.call("lub", lubResult._2());
                    } else {
                        if(DEBUG)
                            logger.info("close {} <: {}", typeToString.apply(type1), typeToString.apply(postVar2));
                        lowerBounds.__put(postVar2, (lowerBound = type1));
                    }
                    if(upperBounds.containsKey(postVar2)) {
                        final T ub = upperBounds.get(postVar2);
                        tell.call("check", lowerBound, ub);
                    }
                }
            } else { // T1 <: T2
                final T type1 = constraint.getKey();
                final T type2 = constraint.getValue();
                // decompose constraint
                final Collection<Tuple2<T, T>> pairs = decompose.apply(type1, type2);
                if(pairs == null) {
                    throw new UnsatisfiableException(
                            logger.format("{} <: {}", typeToString.apply(type1), typeToString.apply(type2)));
                }
                tellAll.call("decompose", pairs);
            }
        }

        return new ConstraintStore<>(closure.freeze(), lowerBounds.freeze(), upperBounds.freeze());
    }

    @Override public Tuple2<Set<T>, Set<T>> polarities(Iterable<T> initialPosVars, Iterable<T> initialNegVars,
            GetVars<T> getPosVars, GetVars<T> getNegVars) {
        final Set<T> posVars = Sets.newHashSet();
        final Set<T> negVars = Sets.newHashSet();
        final Deque<T> posWorklist = Lists.newLinkedList(initialPosVars);
        final Deque<T> negWorklist = Lists.newLinkedList(initialNegVars);
        while(!(posWorklist.isEmpty() && negWorklist.isEmpty())) {
            while(!posWorklist.isEmpty()) {
                final T posVar = posWorklist.pop();
                if(!posVars.add(posVar)) {
                    continue;
                }
                final T lb = lowerBounds.get(posVar);
                if(lb == null) {
                    continue;
                }
                posWorklist.addAll(getPosVars.apply(lb));
                negWorklist.addAll(getNegVars.apply(lb));
            }
            while(!negWorklist.isEmpty()) {
                final T negVar = negWorklist.pop();
                if(!negVars.add(negVar)) {
                    continue;
                }
                final T ub = upperBounds.get(negVar);
                if(ub != null) {
                    negWorklist.addAll(getPosVars.apply(ub));
                    posWorklist.addAll(getNegVars.apply(ub));
                }
            }
        }
        return ImmutableTuple2.of(posVars, negVars);
    }

    @Override public Tuple2<IConstraintStore<T>, Iterable<Entry<T, T>>> gc(Iterable<T> vars, GetVars<T> getVars,
            Polarity<T> polarity, Substitute<T> substitute, FormatType<T> pp) {
        return gcSimple(vars, getVars, polarity, substitute, pp);
    }

    private Tuple2<IConstraintStore<T>, Iterable<Entry<T, T>>> gcSimple(Iterable<T> vars, GetVars<T> getVars,
            Polarity<T> polarity, Substitute<T> substitute, FormatType<T> pp) {
        final List<Map.Entry<T, T>> substitution = Lists.newArrayList();
        final IRelation2.Transient<T, T> closure = HashTrieRelation2.Transient.of();
        final Map.Transient<T, T> lowerBounds = Map.Transient.of();
        final Map.Transient<T, T> upperBounds = Map.Transient.of();

        final Set<T> reachable = reachableVars(vars, getVars);
        final Set<T> unreachable = Sets.difference(allVars(), reachable).immutableCopy();
        for(T var : unreachable) {
            if(polarity.test(var)) { // positive
                if(this.lowerBounds.containsKey(var)) {
                    substitution.add(ImmutableTuple2.of(var, this.lowerBounds.get(var)));
                } else {
                    final List<T> preVars = pred(var).stream().collect(Collectors.toList());
                    switch(preVars.size()) {
                        case 0:
                            break;
                        case 1:
                            substitution.add(ImmutableTuple2.of(var, preVars.get(0)));
                            break;
                        default:
                            logger.warn("Ignoring multiple negative variables in lower bound of {}", pp.apply(var));
                            break;
                    }
                }
            } else { // negative
                if(this.upperBounds.containsKey(var)) {
                    substitution.add(ImmutableTuple2.of(var, this.upperBounds.get(var)));
                } else {
                    final List<T> postVars = succ(var).stream().collect(Collectors.toList());
                    switch(postVars.size()) {
                        case 0:
                            break;
                        case 1:
                            substitution.add(ImmutableTuple2.of(var, postVars.get(0)));
                            break;
                        default:
                            logger.warn("Ignoring multiple positive variables in upper bound of {}", pp.apply(var));
                            break;
                    }
                }
            }
        }
        for(T var : reachable) {
            closure.inverse().putAll(var, pred(var));
            closure.putAll(var, succ(var));
            if(this.lowerBounds.containsKey(var)) {
                final T lb = this.lowerBounds.get(var);
                lowerBounds.__put(var, substitute.apply(substitution, lb));
            }
            if(this.upperBounds.containsKey(var)) {
                final T ub = this.upperBounds.get(var);
                upperBounds.__put(var, substitute.apply(substitution, ub));
            }
        }

        final IConstraintStore<T> newStore =
                new ConstraintStore<>(closure.freeze(), lowerBounds.freeze(), upperBounds.freeze());
        return ImmutableTuple2.of(newStore, substitution);
    }

    public Tuple2<IConstraintStore<T>, Iterable<Entry<T, T>>> gcIter(Iterable<T> vars, GetVars<T> getVars,
            Polarity<T> polarity, Substitute<T> substitute, FormatType<T> pp) {
        final List<Map.Entry<T, T>> substitution = Lists.newArrayList();
        final IRelation2.Transient<T, T> closure = HashTrieRelation2.Transient.of();
        final Map.Transient<T, T> lowerBounds = Map.Transient.of();
        final Map.Transient<T, T> upperBounds = Map.Transient.of();

        final Set<T> reachable = reachableVars(vars, getVars);
        final Set<T> unreachable = new HashSet<>(Sets.difference(allVars(), reachable));
        boolean progress = true;
        while(progress) {
            progress = false;
            for(T var : unreachable) {
                if(polarity.test(var)) { // positive
                    if(this.lowerBounds.containsKey(var)) {
                        substitution.add(ImmutableTuple2.of(var, this.lowerBounds.get(var)));
                    } else {

                    }
                } else { // negative
                    if(this.upperBounds.containsKey(var)) {
                        substitution.add(ImmutableTuple2.of(var, this.upperBounds.get(var)));
                    } else {

                    }
                }
            }
        }

        final IConstraintStore<T> newStore =
                new ConstraintStore<>(closure.freeze(), lowerBounds.freeze(), upperBounds.freeze());
        return ImmutableTuple2.of(newStore, substitution);
    }

    private Set<T> pred(T var) {
        return closure.inverse().get(var);
    }

    private Set<T> succ(T var) {
        return closure.get(var);
    }

    private Set<T> allVars() {
        final Set<T> all = Sets.newHashSet();
        all.addAll(closure.keySet());
        all.addAll(closure.inverse().keySet());
        all.addAll(lowerBounds.keySet());
        all.addAll(upperBounds.keySet());
        return all;
    }

    private Set<T> reachableVars(Iterable<T> vars, GetVars<T> getVars) {
        final Set<T> reachable = Sets.newHashSet();
        final Deque<T> worklist = Lists.newLinkedList(vars);
        while(!worklist.isEmpty()) {
            final T var = worklist.pop();
            if(!reachable.add(var)) {
                continue;
            }
            worklist.addAll(closure.get(var));
            worklist.addAll(closure.inverse().get(var));
            // FIXME: Maybe only for positive vars?
            final T lb = lowerBounds.get(var);
            if(lb != null) {
                worklist.addAll(getVars.apply(lb));
            }
            // FIXME: Maybe only for negative vars?
            final T ub = upperBounds.get(var);
            if(ub != null) {
                worklist.addAll(getVars.apply(ub));
            }
        }
        return reachable;
    }

    @Override public String toString() {
        return toString(T::toString);
    }

    @Override public String toString(FormatType<T> typeToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        final AtomicBoolean first = new AtomicBoolean(true);
        for(T var : Sets.union(lowerBounds.keySet(), upperBounds.keySet())) {
            sb.append(first.getAndSet(false) ? " " : "; ");
            if(lowerBounds.containsKey(var)) {
                final String lb = typeToString.apply(lowerBounds.get(var));
                sb.append(lb).append(" <: ");
            }
            sb.append(typeToString.apply(var));
            if(upperBounds.containsKey(var)) {
                final String ub = typeToString.apply(upperBounds.get(var));
                sb.append(" <: ").append(ub);
            }
        }
        closure.entrySet().forEach(e -> {
            sb.append(first.getAndSet(false) ? " " : "; ");
            final String v1 = typeToString.apply(e.getKey());
            final String v2 = typeToString.apply(e.getValue());
            sb.append(v1).append(" <: ").append(v2);
        });
        sb.append(first.getAndSet(false) ? "" : " ");
        sb.append("}");
        return sb.toString();
    }

    public static <T> ConstraintStore<T> of() {
        return new ConstraintStore<>(HashTrieRelation2.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
    }

    private Tuple2<T, T> flip(Entry<T, T> pair) {
        return ImmutableTuple2.of(pair.getValue(), pair.getKey());
    }

}