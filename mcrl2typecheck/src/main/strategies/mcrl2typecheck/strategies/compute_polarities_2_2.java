package mcrl2typecheck.strategies;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.nabl2.util.Tuple2;
import mcrl2typecheck.strategies.subtyping.IConstraintStore;

public class compute_polarities_2_2 extends ConstraintStoreStrategy {

    public static compute_polarities_2_2 instance = new compute_polarities_2_2();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm, Strategy getPosVars,
            Strategy getNegVars, IStrategoTerm posVars, IStrategoTerm negVars) {
        final ITermFactory TF = context.getFactory();
        final IConstraintStore<IStrategoTerm> store = store(storeTerm);
        final Tuple2<Set<IStrategoTerm>, Set<IStrategoTerm>> result = store.polarities(toList(posVars), toList(negVars),
                getVars(context, getPosVars), getVars(context, getNegVars));
        return TF.makeTuple(TF.makeList(result._1()), TF.makeList(result._2()));
    }

}