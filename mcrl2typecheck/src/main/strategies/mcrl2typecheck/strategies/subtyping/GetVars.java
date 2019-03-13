package mcrl2typecheck.strategies.subtyping;

import java.util.Collection;

import org.metaborg.util.functions.Function1;

@FunctionalInterface
public interface GetVars<T> extends Function1<T, Collection<T>> {

}