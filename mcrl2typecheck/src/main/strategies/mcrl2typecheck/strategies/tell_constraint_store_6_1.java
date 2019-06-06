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

public class tell_constraint_store_6_1 extends Strategy {

    static final Strategy instance = new tell_constraint_store_6_1();

    @Override public IStrategoTerm invokeDynamic(Context context, IStrategoTerm constraintsTerm, Strategy[] svars,
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
                ConstraintStoreUtils.matchStore(tvars[0]).melt(isVar, sub, glb, lub, bot, top);
        try {
            constraintStore.addAll(ConstraintStoreUtils.matchConstraints(constraintsTerm));
        } catch(ConstraintException ex) {
            return null;
        }
        return ConstraintStoreUtils.buildStore(constraintStore.freeze());
    }

}