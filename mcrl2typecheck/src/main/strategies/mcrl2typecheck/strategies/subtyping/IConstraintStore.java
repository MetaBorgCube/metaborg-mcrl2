package mcrl2typecheck.strategies.subtyping;

import java.util.Set;

import io.usethesource.capsule.Map;
import mb.nabl2.util.Tuple2;

public interface IConstraintStore<T> {

    default boolean isEmpty() {
        return size() == 0;
    }

    int size();

    Iterable<Map.Entry<T, T>> constraints();

    IConstraintStore<T> tellAll(IConstraintStore<T> store, IsVar<T> isVar, Decompose<T> decompose, ComputeBound<T> glb,
            ComputeBound<T> lub, FormatType<T> typeToString) throws UnsatisfiableException;

    IConstraintStore<T> tellAll(Iterable<Map.Entry<T, T>> constraints, IsVar<T> isVar, Decompose<T> decompose,
            ComputeBound<T> glb, ComputeBound<T> lub, FormatType<T> typeToString) throws UnsatisfiableException;

    Tuple2<Set<T>, Set<T>> polarities(Iterable<T> initialPosVars, Iterable<T> initialNegVars, GetVars<T> getPosVars,
            GetVars<T> getNegVars);

    Tuple2<IConstraintStore<T>, Iterable<Map.Entry<T, T>>> gc(Iterable<T> vars, GetVars<T> getVars,
            Polarity<T> polarity, Substitute<T> substitute, FormatType<T> pp);

    String toString(FormatType<T> typeToString);

}