package mcrl2typecheck.strategies;

import java.util.List;

import org.metaborg.util.functions.Predicate1;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

public class merge_constraint_stores_1_0 extends Strategy {

    static final Strategy instance = new merge_constraint_stores_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storesTerm, Strategy isVarStr) {
        final List<ConstraintStore<IStrategoTerm>> stores = matchStores(storesTerm);
        final Predicate1<IStrategoTerm> isVar = ConstraintStoreUtils.isVar(context, isVarStr);
        final ConstraintStore<IStrategoTerm> constraintStore = ConstraintStore.of();
        return ConstraintStoreUtils.build(constraintStore);
    }

    private List<ConstraintStore<IStrategoTerm>> matchStores(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<ConstraintStore<IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm store : t.getAllSubterms()) {
            result.add(ConstraintStoreUtils.match(store));
        }
        return result.build();
    }

}