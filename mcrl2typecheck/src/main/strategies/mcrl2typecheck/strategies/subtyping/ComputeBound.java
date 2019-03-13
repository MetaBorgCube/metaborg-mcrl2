package mcrl2typecheck.strategies.subtyping;

import java.util.Collection;

import org.metaborg.util.functions.Function2;

import mb.nabl2.util.Tuple2;

@FunctionalInterface
public interface ComputeBound<T> extends Function2<T, T, Tuple2<T, Collection<Tuple2<T, T>>>> {

}