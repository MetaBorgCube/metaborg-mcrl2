package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mcrl2typecheck.strategies.subtyping.IConstraintStore;

public class constraint_store_to_string_1_0 extends ConstraintStoreStrategy {

    public static constraint_store_to_string_1_0 instance = new constraint_store_to_string_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, Strategy pp) {
        final ITermFactory TF = context.getFactory();
        final IConstraintStore<IStrategoTerm> store = store(current);
        return TF.makeString(store.toString(typeToString(context, pp)));
    }

}