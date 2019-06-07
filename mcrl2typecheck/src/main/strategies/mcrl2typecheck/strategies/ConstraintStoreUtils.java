package mcrl2typecheck.strategies;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.PartialFunction1;
import org.metaborg.util.functions.PartialFunction2;
import org.metaborg.util.functions.Predicate1;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.stratego.StrategoBlob;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mcrl2typecheck.strategies.ConstraintStore.Ops;

public class ConstraintStoreUtils {

    public static IStrategoTerm buildStore(ConstraintStore<IStrategoTerm> constraintStore) {
        return new StrategoBlob(constraintStore);
    }

    @SuppressWarnings("unchecked") public static ConstraintStore<IStrategoTerm> matchStore(IStrategoTerm t) {
        return StrategoBlob.match(t, ConstraintStore.class)
                .orElseThrow(() -> new IllegalArgumentException("Expected constraint store, got " + t.toString(1)));
    }

    public static Ops<IStrategoTerm> ops(Context context, Strategy ops) {
        final Predicate1<IStrategoTerm> isVar = predicate1(context, ops, "is-var");
        final PartialFunction1<IStrategoTerm, List<IStrategoTerm>> allVars =
                function1(context, ops, "all-vars", ConstraintStoreUtils::matchTypes);
        final PartialFunction2<IStrategoTerm, IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>> sub =
                function2(context, ops, "sub", ConstraintStoreUtils::matchConstraints);
        final PartialFunction2<IStrategoTerm, IStrategoTerm, Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>> glb =
                function2(context, ops, "glb", ConstraintStoreUtils::matchTypeAndConstraints);
        final PartialFunction2<IStrategoTerm, IStrategoTerm, Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>> lub =
                function2(context, ops, "lub", ConstraintStoreUtils::matchTypeAndConstraints);
        final PartialFunction1<IStrategoTerm, IStrategoTerm> bot = function1(context, ops, "bot", t -> t);
        final PartialFunction1<IStrategoTerm, IStrategoTerm> top = function1(context, ops, "top", t -> t);
        final PartialFunction1<IStrategoTerm, String> toString = function1(context, ops, "pp", Tools::asJavaString);
        return new Ops<IStrategoTerm>() {

            @Override public boolean isVar(IStrategoTerm ty) {
                return isVar.test(ty);
            }

            @Override public Optional<List<IStrategoTerm>> allVars(IStrategoTerm ty) {
                return allVars.apply(ty);
            }

            @Override public Optional<List<Tuple2<IStrategoTerm, IStrategoTerm>>> sub(IStrategoTerm ty1,
                    IStrategoTerm ty2) {
                return sub.apply(ty1, ty2);
            }

            @Override public Optional<Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>>
                    glb(IStrategoTerm ty1, IStrategoTerm ty2) {
                return glb.apply(ty1, ty2);
            }

            @Override public Optional<Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>>
                    lub(IStrategoTerm ty1, IStrategoTerm ty2) {
                return lub.apply(ty1, ty2);
            }

            @Override public Optional<IStrategoTerm> bot(IStrategoTerm ty) {
                return bot.apply(ty);
            }

            @Override public Optional<IStrategoTerm> top(IStrategoTerm ty) {
                return top.apply(ty);
            }

            @Override public String toString(IStrategoTerm ty) {
                return toString.apply(ty).orElse(ty.toString());
            }

        };
    }

    public static Predicate1<IStrategoTerm> predicate1(Context context, Strategy s, String op) {
        final ITermFactory TF = context.getFactory();
        final IStrategoString opTerm = TF.makeString(op);
        return t -> s.invoke(context, TF.makeTuple(opTerm, t)) != null;
    }

    public static <U> PartialFunction1<IStrategoTerm, U> function1(Context context, Strategy s, String op,
            Function1<IStrategoTerm, U> map) {
        final ITermFactory TF = context.getFactory();
        final IStrategoString opTerm = TF.makeString(op);
        return (t) -> Optional.ofNullable(s.invoke(context, TF.makeTuple(opTerm, t))).map(map::apply);
    }

    public static <U> PartialFunction2<IStrategoTerm, IStrategoTerm, U> function2(Context context, Strategy s,
            String op, Function1<IStrategoTerm, U> map) {
        final ITermFactory TF = context.getFactory();
        final IStrategoString opTerm = TF.makeString(op);
        return (t1, t2) -> Optional.ofNullable(s.invoke(context, TF.makeTuple(opTerm, TF.makeTuple(t1, t2))))
                .map(map::apply);
    }

    public static List<IStrategoTerm> matchTypes(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        return ImmutableList.copyOf(t.getAllSubterms());
    }

    public static List<ConstraintStore<IStrategoTerm>> matchStores(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<ConstraintStore<IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm store : t.getAllSubterms()) {
            result.add(ConstraintStoreUtils.matchStore(store));
        }
        return result.build();
    }

    public static Tuple2<IStrategoTerm, List<Tuple2<IStrategoTerm, IStrategoTerm>>>
            matchTypeAndConstraints(IStrategoTerm t) {
        if(!Tools.isTermTuple(t) || t.getSubtermCount() != 2) {
            throw new java.lang.IllegalArgumentException("Expected tuple, got " + t.toString(1));
        }
        final IStrategoTerm ty = t.getSubterm(0);
        final IStrategoTerm constraintsTerm = t.getSubterm(1);
        return ImmutableTuple2.of(ty, matchConstraints(constraintsTerm));
    }

    public static List<Tuple2<IStrategoTerm, IStrategoTerm>> matchConstraints(IStrategoTerm t) {
        if(!Tools.isTermList(t)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + t.toString(1));
        }
        final ImmutableList.Builder<Tuple2<IStrategoTerm, IStrategoTerm>> result = ImmutableList.builder();
        for(IStrategoTerm pair : t.getAllSubterms()) {
            if(!Tools.isTermTuple(pair) || pair.getSubtermCount() != 2) {
                throw new java.lang.IllegalArgumentException("Expected tuple, got " + pair.toString(1));
            }
            final IStrategoTerm ty1 = pair.getSubterm(0);
            final IStrategoTerm ty2 = pair.getSubterm(1);
            result.add(ImmutableTuple2.of(ty1, ty2));
        }
        return result.build();
    }

}