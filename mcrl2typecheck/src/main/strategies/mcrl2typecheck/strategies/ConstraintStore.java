package mcrl2typecheck.strategies;

import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Immutable;
import io.usethesource.capsule.SetMultimap;

public class ConstraintStore<T> {

    private Map.Immutable<T, T> lowerBounds;
    private Map.Immutable<T, T> upperBounds;
    private SetMultimap.Immutable<T, T> closure;

    private ConstraintStore(Immutable<T, T> lowerBounds, Immutable<T, T> upperBounds,
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

    public static <T> ConstraintStore<T> of() {
        return new ConstraintStore<>(Map.Immutable.of(), Map.Immutable.of(), SetMultimap.Immutable.of());
    }

}