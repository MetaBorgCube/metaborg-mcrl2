package mcrl2typecheck.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.util.Tuple2;
import mcrl2typecheck.strategies.subtyping.IConstraintStore;
import mcrl2typecheck.strategies.subtyping.UnsatisfiableException;

public class merge_constraint_stores_5_0 extends ConstraintStoreStrategy {

    public static merge_constraint_stores_5_0 instance = new merge_constraint_stores_5_0();

    @Override public IStrategoTerm invokeDynamic(Context context, IStrategoTerm current, Strategy[] strategies,
            IStrategoTerm[] t) {
        final Strategy isVar = strategies[0];
        final Strategy decompose = strategies[1];
        final Strategy glb = strategies[2];
        final Strategy lub = strategies[3];
        final Strategy pp = strategies[4];
        final Tuple2<IStrategoTerm, IStrategoTerm> storeTerms = pairToTuple(current);
        final IConstraintStore<IStrategoTerm> store1 = store(storeTerms._1());
        final IConstraintStore<IStrategoTerm> store2 = store(storeTerms._2());
        try {
            final IConstraintStore<IStrategoTerm> newStore = store1.tellAll(store2, isVar(context, isVar),
                    decompose(context, decompose), bound(context, glb), bound(context, lub), typeToString(context, pp));
            return new StrategoBlob(newStore);
        } catch(UnsatisfiableException ex) {
            return null;
        }
    }

}