package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class init_constraint_store_0_0 extends Strategy {

    static final Strategy instance = new init_constraint_store_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        return ConstraintStoreUtils.build(ConstraintStore.of());
    }

}