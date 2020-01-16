package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.ConstraintStore.ConstraintException;
import mcrl2typecheck.strategies.ConstraintStore.Ops;

public class merge_constraint_stores_1_0 extends Strategy {

    static final Strategy instance = new merge_constraint_stores_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storesTerm, Strategy opsStr) {
        final Ops<IStrategoTerm> ops = ConstraintStoreUtils.ops(context, opsStr);
        final ConstraintStore.Transient<IStrategoTerm> constraintStore = ConstraintStore.<IStrategoTerm>of().melt(ops);
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