package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

import mb.nabl2.stratego.StrategoBlob;
import mcrl2typecheck.strategies.subtyping.IConstraintStore;
import mcrl2typecheck.strategies.subtyping.UnsatisfiableException;

public class tell_constraint_store_5_1 extends ConstraintStoreStrategy {

    public static tell_constraint_store_5_1 instance = new tell_constraint_store_5_1();

    @Override public IStrategoTerm invokeDynamic(Context context, IStrategoTerm current, Strategy[] strategies,
            IStrategoTerm[] terms) {
        final Strategy isVar = strategies[0];
        final Strategy decompose = strategies[1];
        final Strategy glb = strategies[2];
        final Strategy lub = strategies[3];
        final Strategy pp = strategies[4];
        final IStrategoTerm storeTerm = terms[0];
        final IConstraintStore<IStrategoTerm> store = store(storeTerm);
        try {
            final IConstraintStore<IStrategoTerm> newStore = store.tellAll(ImmutableList.of(pairToTuple(current)),
                    isVar(context, isVar), decompose(context, decompose), bound(context, glb), bound(context, lub),
                    typeToString(context, pp));
            return new StrategoBlob(newStore);
        } catch(UnsatisfiableException e) {
            return null;
        }
    }

}