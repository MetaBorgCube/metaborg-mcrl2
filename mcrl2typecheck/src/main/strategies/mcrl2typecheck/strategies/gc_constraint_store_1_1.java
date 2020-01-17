package mcrl2typecheck.strategies;

import java.util.List;
import java.util.Map.Entry;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.ConstraintStore.Ops;

public class gc_constraint_store_1_1 extends Strategy {

    static final Strategy instance = new gc_constraint_store_1_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm liveTerms, Strategy opsStr,
            IStrategoTerm storeTerm) {
        final Ops<IStrategoTerm> ops = ConstraintStoreUtils.ops(context, opsStr);
        final ConstraintStore.Transient<IStrategoTerm> constraintStore =
                ConstraintStoreUtils.matchStore(storeTerm).melt(ops);
        final List<Entry<IStrategoTerm, IStrategoTerm>> subst =
                constraintStore.gc(ConstraintStoreUtils.matchTypes(liveTerms));
        final IStrategoTerm newStoreTerm = ConstraintStoreUtils.buildStore(constraintStore.freeze());
        final ITermFactory TF = context.getFactory();
        final IStrategoTerm result = TF.makeTuple(newStoreTerm, ConstraintStoreUtils.buildConstraints(subst, TF));
        return result;
    }

}