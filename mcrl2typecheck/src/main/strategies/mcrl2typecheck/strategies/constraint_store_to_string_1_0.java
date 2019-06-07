package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.ConstraintStore.Ops;

public class constraint_store_to_string_1_0 extends Strategy {

    static final Strategy instance = new constraint_store_to_string_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm, Strategy opsStr) {
        final Ops<IStrategoTerm> ops = ConstraintStoreUtils.ops(context, opsStr);
        final ConstraintStore<IStrategoTerm> constraintStore = ConstraintStoreUtils.matchStore(storeTerm);
        return context.getFactory().makeString(constraintStore.toString(ops::toString));
    }

}