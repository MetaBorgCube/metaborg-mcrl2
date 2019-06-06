package mcrl2typecheck.strategies;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class get_store_constraints_0_0 extends Strategy {

    static final Strategy instance = new get_store_constraints_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm) {
        final ConstraintStore<IStrategoTerm> constraintStore = ConstraintStoreUtils.matchStore(storeTerm);
        List<IStrategoTerm> constraints = new ArrayList<>();
        constraintStore.getConstraints().forEach(c -> {
            constraints.add(context.getFactory().makeTuple(c.getKey(), c.getValue()));
        });
        return context.getFactory().makeList(constraints);
    }

}