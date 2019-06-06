package mcrl2typecheck.strategies;

import org.metaborg.util.functions.Predicate1;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.nabl2.terms.stratego.StrategoBlob;

public class ConstraintStoreUtils {

    public static IStrategoTerm build(ConstraintStore<IStrategoTerm> constraintStore) {
        return new StrategoBlob(constraintStore);
    }

    @SuppressWarnings("unchecked") public static ConstraintStore<IStrategoTerm> match(IStrategoTerm t) {
        return StrategoBlob.match(t, ConstraintStore.class)
                .orElseThrow(() -> new IllegalArgumentException("Expected constraint store, got " + t.toString(1)));
    }

    public static Predicate1<IStrategoTerm> isVar(Context context, Strategy isVar) {
        return t -> {
            return isVar.invoke(context, t) != null;
        };
    }

}