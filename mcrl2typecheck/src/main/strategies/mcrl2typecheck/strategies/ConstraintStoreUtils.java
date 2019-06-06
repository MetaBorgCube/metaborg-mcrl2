package mcrl2typecheck.strategies;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.PartialFunction2;
import org.metaborg.util.functions.Predicate1;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.stratego.StrategoBlob;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class ConstraintStoreUtils {

    public static IStrategoTerm buildStore(ConstraintStore<IStrategoTerm> constraintStore) {
        return new StrategoBlob(constraintStore);
    }

    @SuppressWarnings("unchecked") public static ConstraintStore<IStrategoTerm> matchStore(IStrategoTerm t) {
        return StrategoBlob.match(t, ConstraintStore.class)
                .orElseThrow(() -> new IllegalArgumentException("Expected constraint store, got " + t.toString(1)));
    }

    public static Predicate1<IStrategoTerm> predicate1(Context context, Strategy s) {
        return t -> s.invoke(context, t) != null;
    }

    public static PartialFunction1<IStrategoTerm, IStrategoTerm> function1(Context context, Strategy s) {
        return (t) -> Optional.ofNullable(s.invoke(context, t));
    }

    public static <U> PartialFunction2<IStrategoTerm, IStrategoTerm, U> function2(Context context, Strategy s,
            Function1<IStrategoTerm, U> map) {
        return (t1, t2) -> Optional.ofNullable(s.invoke(context, context.getFactory().makeTuple(t1, t2)))
                .map(map::apply);
    }

    public static List<ConstraintStore<IStrategoTerm>> matchStores(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<ConstraintStore<IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm store : t.getAllSubterms()) {
            result.add(ConstraintStoreUtils.matchStore(store));
        }
        return result.build();
    }

    public static Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>
            matchTypeAndConstraints(IStrategoTerm t) {
        if(!Tools.isTermTuple(t) || t.getSubtermCount() != 2) {
            throw new java.lang.IllegalArgumentException("Expected tuple, got " + t.toString(1));
        }
        final IStrategoTerm ty = t.getSubterm(0);
        final IStrategoTerm constraintsTerm = t.getSubterm(1);
        return ImmutableTuple2.of(ty, matchConstraints(constraintsTerm));
    }

    public static List<Tuple2<IStrategoTerm, IStrategoTerm>> matchConstraints(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<Tuple2<IStrategoTerm, IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm pair : t.getAllSubterms()) {
            if(!Tools.isTermTuple(pair) || pair.getSubtermCount() != 2) {
                throw new java.lang.IllegalArgumentException("Expected tuple, got " + pair.toString(1));
            }
            final IStrategoTerm ty1 = pair.getSubterm(0);
            final IStrategoTerm ty2 = pair.getSubterm(1);
            result.add(ImmutableTuple2.of(ty1, ty2));
        }
        return result.build();
    }

}