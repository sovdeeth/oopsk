package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.events.DynamicFieldEvalEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("This Struct")
@Description("Usable only in dynamic field expressions, this refers to whatever struct is evaluating this field.")
@Example("""
    struct Vector2:
        x: number
        y: number
        dynamic length: number = sqrt(this->x^2 + this-y^2)
    """)
@Since("1.0")
public class ExprThisStruct extends SimpleExpression<Struct> {

    static {
        Skript.registerExpression(ExprThisStruct.class, Struct.class, ExpressionType.SIMPLE, "this [struct]");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        // check for right event
        if (!getParser().isCurrentEvent(DynamicFieldEvalEvent.class)) {
            Skript.error("The 'this struct' expression can only be used in a struct template definition.");
            return false;
        }
        return true;
    }

    @Override
    protected Struct @Nullable [] get(Event event) {
        if (!(event instanceof DynamicFieldEvalEvent evalEvent))
            return new Struct[0];
        return new Struct[]{evalEvent.getStruct()};
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
    public String toString(@Nullable Event event, boolean debug) {
        return "this struct";
    }
}
