package mcrl2typecheck.strategies;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.PartialFunction2;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.Tuple2;

public class ConstraintStore<T> {
    private static final ILogger logger = LoggerUtils.logger(ConstraintStore.class);

    private final Map.Immutable<T, T> lowerBounds;
    private final Map.Immutable<T, T> upperBounds;
    private final SetMultimap.Immutable<T, T> closure;

    private ConstraintStore(Map.Immutable<T, T> lowerBounds, Map.Immutable<T, T> upperBounds,
            SetMultimap.Immutable<T, T> closure) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.closure = closure;
    }

    public int size() {
        return lowerBounds.size() + upperBounds.size() + closure.size();
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

    public Transient<T> melt(IsVar<T> isVar, Sub<T> sub, Glb<T> glb, Lub<T> lub, Bot<T> bot, Top<T> top) {
        return new Transient<>(lowerBounds.asTransient(), upperBounds.asTransient(), closure.asTransient(), isVar, sub,
                glb, lub, bot, top);
    }

    public static <T> ConstraintStore<T> of() {
        return new ConstraintStore<>(Map.Immutable.of(), Map.Immutable.of(), SetMultimap.Immutable.of());
    }

    public static class Transient<T> {

        private final Map.Transient<T, T> lowerBounds;
        private final Map.Transient<T, T> upperBounds;
        private final SetMultimap.Transient<T, T> closure;

        private final IsVar<T> isVar;
        private final Sub<T> sub;
        private final Glb<T> glb;
        private final Lub<T> lub;
        private final Bot<T> bot;
        private final Top<T> top;

        public Transient(Map.Transient<T, T> lowerBounds, Map.Transient<T, T> upperBounds,
                SetMultimap.Transient<T, T> closure, IsVar<T> isVar, Sub<T> sub, Glb<T> glb, Lub<T> lub, Bot<T> bot,
                Top<T> top) {
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
            this.closure = closure;
            this.isVar = isVar;
            this.sub = sub;
            this.glb = glb;
            this.lub = lub;
            this.bot = bot;
            this.top = top;
        }

        public void add(T ty1, T ty2) throws ConstraintException {
            boolean isVar1 = isVar.test(ty1);
            boolean isVar2 = isVar.test(ty2);
            if(isVar1 && isVar2) {
                addClosure(ty1, ty2);
            } else if(isVar1) {
                addUpperBound(ty1, ty2);
            } else if(isVar2) {
                addLowerBound(ty2, ty1);
            } else {
                final List<Tuple2<T, T>> constraints = sub.apply(ty1, ty2).orElseThrow(() -> new ConstraintException());
                addAll(constraints);
            }
        }

        public void add(Map.Entry<T, T> constraint) throws ConstraintException {
            add(constraint.getKey(), constraint.getValue());
        }

        public void addAll(Iterable<? extends Map.Entry<T, T>> constraints) throws ConstraintException {
            for(Map.Entry<T, T> constraint : constraints) {
                add(constraint);
            }
        }

        public void addAll(ConstraintStore<T> store) throws ConstraintException {
            for(Map.Entry<T, T> lb : store.lowerBounds.entrySet()) {
                addLowerBound(lb.getKey(), lb.getValue());
            }
            for(Map.Entry<T, T> ub : store.upperBounds.entrySet()) {
                addUpperBound(ub.getKey(), ub.getValue());
            }
            for(Map.Entry<T, T> e : store.closure.entrySet()) {
                addClosure(e.getKey(), e.getValue());
            }
        }

        private void addClosure(T v1, T v2) throws ConstraintException {
            if(closure.__insert(v1, v2)) {
                logger.info("close {} <: {}", v1, v2);
            }
        }

        private void addLowerBound(T v, T lb) throws ConstraintException {
            if(!lowerBounds.containsKey(v)) {
                lowerBounds.__put(v, lb);
                if(!upperBounds.containsKey(v)) {
                    final T ub = top.apply(lb).orElseThrow(() -> new ConstraintException());
                    addUpperBound(v, ub);
                }
            } else {
                final Tuple2<T, List<Tuple2<T, T>>> typeAndConstraints =
                        glb.apply(lb, lowerBounds.get(v)).orElseThrow(() -> new ConstraintException());
                addAll(typeAndConstraints._2());
            }
        }

        private void addUpperBound(T v, T ub) throws ConstraintException {
            if(!upperBounds.containsKey(v)) {
                upperBounds.__put(v, ub);
                if(!lowerBounds.containsKey(v)) {
                    final T lb = bot.apply(ub).orElseThrow(() -> new ConstraintException());
                    addLowerBound(v, lb);
                }
            } else {
                final Tuple2<T, List<Tuple2<T, T>>> typeAndConstraints =
                        lub.apply(ub, upperBounds.get(v)).orElseThrow(() -> new ConstraintException());
                addAll(typeAndConstraints._2());
            }
        }

        public ConstraintStore<T> freeze() {
            return new ConstraintStore<>(lowerBounds.freeze(), upperBounds.freeze(), closure.freeze());
        }

    }

    @FunctionalInterface
    interface IsVar<T> extends Predicate1<T> {
    }

    @FunctionalInterface
    interface Sub<T> extends PartialFunction2<T, T, List<Tuple2<T, T>>> {
    }

    @FunctionalInterface
    interface Glb<T> extends PartialFunction2<T, T, Tuple2<T, List<Tuple2<T, T>>>> {
    }

    @FunctionalInterface
    interface Lub<T> extends PartialFunction2<T, T, Tuple2<T, List<Tuple2<T, T>>>> {
    }

    @FunctionalInterface
    interface Top<T> extends PartialFunction1<T, T> {
    }

    @FunctionalInterface
    interface Bot<T> extends PartialFunction1<T, T> {
    }

    public static class ConstraintException extends Exception {
        private static final long serialVersionUID = 1L;
    }

}