package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Field;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.core.StructTemplate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("Struct Creation")
@Description("Creates an instance of a struct based off a template. The template name is case insensitive. Initial field values can be set by adding entries with the format 'fieldname: value'.")
@Example("set {a} to a message struct")
@Example("return a result struct instance")
@Example("""
    return a result struct instance with the initial values of:
        valid: true
        error: "No error"
        data: {_data::*}
    """)
@Since("1.0")
public class ExprSecStructInstance extends SectionExpression<Struct> implements SyntaxRuntimeErrorProducer {

    static {
        Skript.registerExpression(ExprSecStructInstance.class, Struct.class, ExpressionType.SIMPLE,
                "[a[n]] <([\\w ]+)> struct instance [with [the] [initial] values [of]]");
    }

    private String name;
    private Node node;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, ParseResult parseResult, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        name = parseResult.regexes.get(0).group(1);
        if (name == null) {
            Skript.error("Struct name cannot be null.");
            return false;
        }
        name = name.trim().toLowerCase(Locale.ENGLISH);
        if (Oopsk.getTemplateManager().getTemplate(name) == null) {
            Skript.error("A struct by the name of '" + name + "' does not exist.");
            return false;
        }
        this.node = getParser().getNode();

        // parse starting field values
        if (node != null)
            return parseInitialValues(node, Oopsk.getTemplateManager().getTemplate(name));

        return true;
    }

    private static final Pattern ENTRY_PATTERN = Pattern.compile("([\\w ]+): (.+)");
    private final Map<String, Expression<?>> parsedFieldValues = new HashMap<>();

    /**
     * Parses the initial values of the struct instance.
     * @param node the node to parse
     * @param template the template to parse the values for
     * @return Whether the parsing was successful
     */
    public boolean parseInitialValues(@NotNull SectionNode node, StructTemplate template) {
        // for each node, parse as `fieldname: value`
        // if the field is not found, error
        // if the value is not valid, error
        // if it is valid, save expression for later evaluation
        for (Node child : node) {
            if (child instanceof SimpleNode fieldNode) {
                String entry = fieldNode.getKey();
                if (entry == null)
                    throw new IllegalStateException("Null node found.");
                // split into field name and value
                Matcher matcher = ENTRY_PATTERN.matcher(entry);
                if (!matcher.matches()) {
                    Skript.error("Invalid field node: " + entry);
                    return false;
                }

                String fieldName = matcher.group(1).trim().toLowerCase(Locale.ENGLISH);
                String value = matcher.group(2).trim();

                // check if field exists
                Field<?> field = template.getField(fieldName);
                if (field == null) {
                    Skript.error("Field '" + fieldName + "' does not exist in struct '" + name + "'.");
                    return false;
                }

                // dynamic fields can't be changed.
                if (field.dynamic()) {
                    Skript.error("Cannot assign values to dynamic fields.");
                    return false;
                }

                // parse the value
                Expression<?> expr = new SkriptParser(value, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(field.type().getC());
                expr = LiteralUtils.defendExpression(expr);
                if (expr == null || !LiteralUtils.canInitSafely(expr)) {
//                    Skript.error("Invalid value for field '" + fieldName + "' in struct '" + name + "'.");
                    return false;
                }
                // store in map
                parsedFieldValues.put(fieldName, expr);
            } else {
                Skript.error("Invalid field node: " + child.getKey());
                return false;
            }
        }
        return true;
    }

    @Override
    protected Struct @Nullable [] get(Event event) {
        StructTemplate template = Oopsk.getTemplateManager().getTemplate(name);
        if (template == null) {
            error("A struct by the name of '" + name + "' does not exist.");
            return CollectionUtils.array();
        }
        Struct struct = Oopsk.getStructManager().createStruct(template, event, parsedFieldValues);
        return CollectionUtils.array(struct);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Struct> getReturnType() {
        var template = Oopsk.getTemplateManager().getTemplate(name);
        if (template != null && template.getCustomClass() != null) {
            return template.getCustomClass();
        }
        return Struct.class;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a " + name + " struct instance";
    }

}
