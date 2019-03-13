package mcrl2typecheck.strategies.subtyping;

import java.util.List;
import java.util.Map;

import org.metaborg.util.functions.Function2;

@FunctionalInterface
public interface Substitute<T> extends Function2<List<Map.Entry<T, T>> ,T, T> {

}