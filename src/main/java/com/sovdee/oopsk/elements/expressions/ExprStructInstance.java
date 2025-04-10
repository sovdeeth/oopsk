package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.core.StructTemplate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

public class ExprStructInstance extends SimpleExpression<Struct> implements SyntaxRuntimeErrorProducer {

    static {
        Skript.registerExpression(ExprStructInstance.class, Struct.class, ExpressionType.SIMPLE,
                "[a[n]] <(\\w+)> struct [instance]");
    }

    private String name;
    private Node node;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        name = parseResult.regexes.get(0).group(1);
        if (name == null)
            return false;
        if (Oopsk.getTemplateManager().getTemplate(name) == null) {
            Skript.error("A struct by the name of '" + name + "' does not exist.");
            return false;
        }
        node = getParser().getNode();
        return true;
    }

    @Override
    protected Struct @Nullable [] get(Event event) {
        StructTemplate template = Oopsk.getTemplateManager().getTemplate(name);
        if (template == null)
            error("A struct by the name of '" + name + "' does not exist.");
        return new Struct[] {Oopsk.getStructManager().createStruct(template, event)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<Struct> getReturnType() {
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
