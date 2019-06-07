package mcrl2typecheck.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        // @formatter:off
        super(new Strategy[] {
            constraint_store_to_string_1_0.instance,
            gc_constraint_store_1_1.instance,
            get_store_constraints_0_0.instance,
            init_constraint_store_0_0.instance,
            merge_constraint_stores_1_0.instance,
            tell_constraint_store_1_1.instance
        });
        // @formatter:on
    }
}