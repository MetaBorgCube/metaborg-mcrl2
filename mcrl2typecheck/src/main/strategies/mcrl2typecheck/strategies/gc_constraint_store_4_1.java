package mcrl2typecheck.strategies;

import java.util.Map.Entry;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.util.Tuple2;
import mcrl2typecheck.strategies.subtyping.IConstraintStore;

public class gc_constraint_store_4_1 extends ConstraintStoreStrategy {

    public static gc_constraint_store_4_1 instance = new gc_constraint_store_4_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm, Strategy getVars, Strategy polarity,
            Strategy substitute, Strategy pp, IStrategoTerm vars) {
        final ITermFactory TF = context.getFactory();
        final IConstraintStore<IStrategoTerm> store = store(storeTerm);
        final Tuple2<IConstraintStore<IStrategoTerm>, Iterable<Entry<IStrategoTerm, IStrategoTerm>>> result =
                store.gc(toList(vars), getVars(context, getVars), polarity(context, polarity),
                        substitute(context, substitute), typeToString(context, pp));
        return TF.makeTuple(new StrategoBlob(result._1()), tuplesToPairList(context, result._2()));
    }

}