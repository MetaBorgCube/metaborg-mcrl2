package mcrl2typecheck.strategies;

import java.util.List;

import org.metaborg.util.functions.Predicate1;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class tell_constraint_store_1_1 extends Strategy {

    static final Strategy instance = new tell_constraint_store_1_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm constraintsTerm, Strategy isVarStr,
            IStrategoTerm storeTerm) {
        final ConstraintStore<IStrategoTerm> constraintStore = ConstraintStoreUtils.match(storeTerm);
        final Predicate1<IStrategoTerm> isVar = ConstraintStoreUtils.isVar(context, isVarStr);
        final List<Tuple2<IStrategoTerm, IStrategoTerm>> constraints = matchPairs(constraintsTerm);
        return ConstraintStoreUtils.build(constraintStore);
    }

    private List<Tuple2<IStrategoTerm, IStrategoTerm>> matchPairs(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<Tuple2<IStrategoTerm, IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm pair : t.getAllSubterms()) {
            if(!Tools.isTermTuple(pair) || pair.getSubtermCount() != 2) {
                throw new java.lang.IllegalArgumentException("Expected tuple, got " + pair.toString(1));
            }
            final IStrategoTerm t1 = pair.getSubterm(0);
            final IStrategoTerm t2 = pair.getSubterm(1);
            result.add(ImmutableTuple2.of(t1, t2));
        }
        return result.build();
    }

}