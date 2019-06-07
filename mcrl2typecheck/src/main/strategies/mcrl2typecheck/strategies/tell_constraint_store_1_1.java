package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.ConstraintStore.ConstraintException;
import mcrl2typecheck.strategies.ConstraintStore.Ops;

public class tell_constraint_store_1_1 extends Strategy {

    static final Strategy instance = new tell_constraint_store_1_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm constraintsTerm, Strategy opsStr,
            IStrategoTerm storeTerm) {
        final Ops<IStrategoTerm> ops = ConstraintStoreUtils.ops(context, opsStr);
        final ConstraintStore.Transient<IStrategoTerm> constraintStore =
                ConstraintStoreUtils.matchStore(storeTerm).melt(ops);
        try {
            constraintStore.addAll(ConstraintStoreUtils.matchConstraints(constraintsTerm));
        } catch(ConstraintException ex) {
            return null;
        }
        return ConstraintStoreUtils.buildStore(constraintStore.freeze());
    }

}