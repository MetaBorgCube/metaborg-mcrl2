package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.ConstraintStore.Bot;
import mcrl2typecheck.strategies.ConstraintStore.ConstraintException;
import mcrl2typecheck.strategies.ConstraintStore.Glb;
import mcrl2typecheck.strategies.ConstraintStore.IsVar;
import mcrl2typecheck.strategies.ConstraintStore.Lub;
import mcrl2typecheck.strategies.ConstraintStore.Sub;
import mcrl2typecheck.strategies.ConstraintStore.Top;

public class merge_constraint_stores_6_0 extends Strategy {

    static final Strategy instance = new merge_constraint_stores_6_0();

    @Override public IStrategoTerm invokeDynamic(Context context, IStrategoTerm storesTerm, Strategy[] svars,
            IStrategoTerm[] tvars) {
        final IsVar<IStrategoTerm> isVar = ConstraintStoreUtils.predicate1(context, svars[0])::test;
        final Sub<IStrategoTerm> sub =
                ConstraintStoreUtils.function2(context, svars[1], ConstraintStoreUtils::matchConstraints)::apply;
        final Glb<IStrategoTerm> glb =
                ConstraintStoreUtils.function2(context, svars[2], ConstraintStoreUtils::matchTypeAndConstraints)::apply;
        final Lub<IStrategoTerm> lub =
                ConstraintStoreUtils.function2(context, svars[3], ConstraintStoreUtils::matchTypeAndConstraints)::apply;
        final Bot<IStrategoTerm> bot = ConstraintStoreUtils.function1(context, svars[4])::apply;
        final Top<IStrategoTerm> top = ConstraintStoreUtils.function1(context, svars[5])::apply;
        final ConstraintStore.Transient<IStrategoTerm> constraintStore =
                ConstraintStore.<IStrategoTerm>of().melt(isVar, sub, glb, lub, bot, top);
        try {
            for(ConstraintStore<IStrategoTerm> store : ConstraintStoreUtils.matchStores(storesTerm)) {
                constraintStore.addAll(store);
            }
        } catch(ConstraintException ex) {
            return null;
        }
        return ConstraintStoreUtils.buildStore(constraintStore.freeze());
    }

}