package com.sovdee.oopsk.elements.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Struct;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Name("Struct Is From Template")
@Description({
        "Checks if a struct is from a specific template. The template name is case insensitive.",
        "The template name can be a string or a literal."
})
@Example("if {_message} is a \"message\" struct:")
@Example("broadcast whether {_message} or {_response} is a \"message\" struct")
@Since("1.0")
public class CondIsOfTemplate extends Condition {

    static {
        Skript.registerCondition(CondIsOfTemplate.class,
                "[the] %structs% (is|are)[not:n't| not] [a] %string% struct[s]",
                "[the] %structs% (were|was)[not:n't| not] made from [the] %string% template");
    }

    private Expression<Struct> structs;
    private Expression<String> templateName;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        setNegated(parseResult.hasTag("not"));
        structs = (Expression<Struct>) expressions[0];
        templateName = (Expression<String>) expressions[1];
        // Check if the template name is valid if it's a literal
        if (templateName instanceof Literal<String> nameLiteral) {
            String name = nameLiteral.getSingle().toLowerCase(Locale.ENGLISH);
            // Check if the template exists
            if (Oopsk.getTemplateManager().getTemplate(name) == null) {
                Skript.error("Template '" + name + "' does not exist");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        String templateName = this.templateName.getSingle(event);
        if (templateName == null)
            return isNegated();
        return structs.check(event, struct -> {
            // Check if the struct is of the specified template
            return struct.getTemplate().getName().equalsIgnoreCase(templateName);
        }, isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return structs + " are " + (isNegated() ? "not " : "") + templateName + " structs";
    }

}
