package mcrl2typecheck.strategies;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;

import mb.nabl2.stratego.StrategoBlob;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mcrl2typecheck.strategies.subtyping.ComputeBound;
import mcrl2typecheck.strategies.subtyping.Decompose;
import mcrl2typecheck.strategies.subtyping.FormatType;
import mcrl2typecheck.strategies.subtyping.GetVars;
import mcrl2typecheck.strategies.subtyping.IConstraintStore;
import mcrl2typecheck.strategies.subtyping.IsVar;
import mcrl2typecheck.strategies.subtyping.Polarity;
import mcrl2typecheck.strategies.subtyping.Substitute;

abstract class ConstraintStoreStrategy extends Strategy {

    @SuppressWarnings("unchecked") protected IConstraintStore<IStrategoTerm> store(IStrategoTerm term) {
        return StrategoBlob.match(term, IConstraintStore.class)
                .orElseThrow(() -> new IllegalArgumentException("Expected store, got " + term));
    }

    protected IsVar<IStrategoTerm> isVar(Context context, Strategy isVar) {
        return (t) -> {
            return isVar.invoke(context, t) != null;
        };
    }

    protected Polarity<IStrategoTerm> polarity(Context context, Strategy isVar) {
        return (t) -> {
            return isVar.invoke(context, t) != null;
        };
    }

    protected ComputeBound<IStrategoTerm> bound(Context context, Strategy bounds) {
        final ITermFactory TF = context.getFactory();
        return (ty1, ty2) -> {
            final IStrategoTerm lubTerm = bounds.invoke(context, TF.makeTuple(ty1, ty2));
            if(lubTerm == null) {
                return null;
            }
            final Tuple2<IStrategoTerm, IStrategoTerm> typeAndPairs = pairToTuple(lubTerm);
            final Collection<Tuple2<IStrategoTerm, IStrategoTerm>> pairs = pairsToTuples(typeAndPairs._2());
            return ImmutableTuple2.of(typeAndPairs._1(), pairs);
        };
    }

    protected Decompose<IStrategoTerm> decompose(Context context, Strategy decompose) {
        final ITermFactory TF = context.getFactory();
        return (ty1, ty2) -> {
            final IStrategoTerm pairs = decompose.invoke(context, TF.makeTuple(ty1, ty2));
            if(pairs == null) {
                return null;
            }
            return pairsToTuples(pairs);
        };
    }

    protected FormatType<IStrategoTerm> typeToString(Context context, Strategy pp) {
        return ty -> {
            return Optional.ofNullable(pp.invoke(context, ty)).map(Tools::asJavaString).orElseGet(() -> ty.toString());
        };
    }

    protected GetVars<IStrategoTerm> getVars(Context context, Strategy getVars) {
        return ty -> {
            return Optional.ofNullable(getVars.invoke(context, ty)).filter(Tools::isTermList)
                    .map(vs -> ImmutableList.copyOf(vs.getAllSubterms()))
                    .orElseThrow(() -> new java.lang.IllegalArgumentException("Cannot get vars of " + ty));
        };
    }

    protected Substitute<IStrategoTerm> substitute(Context context, Strategy substitute) {
        return (sbs, ty) -> {
            final IStrategoTerm input = context.getFactory().makeTuple(tuplesToPairList(context, sbs), ty);
            return Optional.ofNullable(substitute.invoke(context, input))
                    .orElseThrow(() -> new java.lang.IllegalArgumentException("Cannot substitute in " + ty));
        };
    }

    protected Tuple2<IStrategoTerm, IStrategoTerm> pairToTuple(IStrategoTerm pair) {
        if(!Tools.isTermTuple(pair) || pair.getSubtermCount() != 2) {
            throw new java.lang.IllegalArgumentException("Expected pair of types, got " + pair);
        }
        return ImmutableTuple2.of(Tools.termAt(pair, 0), Tools.termAt(pair, 1));
    }

    private Collection<Tuple2<IStrategoTerm, IStrategoTerm>> pairsToTuples(IStrategoTerm pairs) {
        if(!Tools.isTermList(pairs)) {
            throw new IllegalArgumentException("Expected list of pairs of types, got " + pairs);
        }
        return Arrays.asList(pairs.getAllSubterms()).stream().map(this::pairToTuple).collect(Collectors.toList());
    }

    protected Collection<IStrategoTerm> toList(IStrategoTerm list) {
        if(!Tools.isTermList(list)) {
            throw new IllegalArgumentException("Expected list of types, got " + list);
        }
        return ImmutableList.copyOf(list.getAllSubterms());
    }

    protected IStrategoTuple tupleToPair(Context context, Map.Entry<IStrategoTerm, IStrategoTerm> tuple) {
        return context.getFactory().makeTuple(tuple.getKey(), tuple.getValue());
    }

    protected IStrategoList tuplesToPairList(Context context,
            Iterable<Map.Entry<IStrategoTerm, IStrategoTerm>> tuples) {
        return context.getFactory().makeList(
                Iterables2.stream(tuples).map(tuple -> tupleToPair(context, tuple)).collect(Collectors.toList()));
    }

}