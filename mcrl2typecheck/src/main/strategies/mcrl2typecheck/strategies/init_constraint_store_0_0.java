package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;

import mb.nabl2.stratego.StrategoBlob;
import mcrl2typecheck.strategies.subtyping.ConstraintStore;

public class init_constraint_store_0_0 extends ConstraintStoreStrategy {

    public static init_constraint_store_0_0 instance = new init_constraint_store_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        return new StrategoBlob(ConstraintStore.of());
    }

}