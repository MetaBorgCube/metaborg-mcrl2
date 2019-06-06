package mcrl2typecheck.strategies;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class constraint_store_to_string_1_0 extends Strategy {

    static final Strategy instance = new constraint_store_to_string_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm, Strategy pp) {
        final ConstraintStore<IStrategoTerm> constraintStore = ConstraintStoreUtils.matchStore(storeTerm);
        final Function1<IStrategoTerm, String> toString = t -> {
            IStrategoTerm resultTerm = pp.invoke(context, t);
            return resultTerm != null ? Tools.asJavaString(resultTerm) : t.toString();
        };
        return context.getFactory().makeString(constraintStore.toString(toString));
    }

}