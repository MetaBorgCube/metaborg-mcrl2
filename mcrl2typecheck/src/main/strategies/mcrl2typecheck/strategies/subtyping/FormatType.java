package mcrl2typecheck.strategies.subtyping;

import org.metaborg.util.functions.Function1;

@FunctionalInterface
public interface FormatType<T> extends Function1<T, String> {

}
