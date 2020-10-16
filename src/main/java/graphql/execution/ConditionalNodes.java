package graphql.execution;

import graphql.Internal;
import graphql.VisibleForTesting;
import graphql.language.Directive;

import java.util.List;
import java.util.Map;

import static graphql.Directives.*;
import static graphql.language.NodeUtil.directiveByName;


@Internal
public class ConditionalNodes {


    @VisibleForTesting
    ValuesResolver valuesResolver = new ValuesResolver();

    public boolean shouldInclude(Map<String, Object> variables, List<Directive> directives) {
        boolean skip = getDirectiveResult(variables, directives, SkipDirective.getName(), false);
        boolean include = getDirectiveResult(variables, directives, IncludeDirective.getName(), true);
        return !skip && include;
    }

    public boolean shouldDefer(Map<String, Object> variables, List<Directive> directives) {
        boolean defer = getDirectiveResult(variables, directives, DeferDirective.getName(), true);
        return defer;
    }

    private Directive getDirectiveByName(List<Directive> directives, String name) {
        if (directives.isEmpty()) {
            return null;
        }
        return directiveByName(directives, name).orElse(null);
    }

    private boolean getDirectiveResult(Map<String, Object> variables, List<Directive> directives, String directiveName, boolean defaultValue) {
        Directive directive = getDirectiveByName(directives, directiveName);
        if (directive != null) {
            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(SkipDirective.getArguments(), directive.getArguments(), variables);
            return (Boolean) argumentValues.get("if");
        }

        return defaultValue;
    }

}
