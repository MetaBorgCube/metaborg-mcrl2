package mcrl2typecheck.strategies;

import java.util.Collection;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;

import mcrl2typecheck.strategies.subtyping.IConstraintStore;

public class get_constraints_0_0 extends ConstraintStoreStrategy {

    public static get_constraints_0_0 instance = new get_constraints_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm storeTerm) {
        final ITermFactory TF = context.getFactory();
        final IConstraintStore<IStrategoTerm> store = store(storeTerm);
        final Collection<IStrategoTerm> entries = Iterables2.stream(store.constraints())
                .map(e -> TF.makeTuple(e.getKey(), e.getValue())).collect(Collectors.toList());
        return TF.makeList(entries);
    }

}