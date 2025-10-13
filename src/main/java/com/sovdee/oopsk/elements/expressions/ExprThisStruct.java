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
import ch.njol.util.coll.CollectionUtils;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.elements.structures.StructStructTemplate;
import com.sovdee.oopsk.events.DynamicFieldEvalEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

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

    private Class<? extends Struct> structClass;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        // check for right event
        if (!getParser().isCurrentEvent(DynamicFieldEvalEvent.class)) {
            Skript.error("The 'this struct' expression can only be used in a struct template definition.");
            return false;
        }
        var structure = getParser().getCurrentStructure();
        if (!(structure instanceof StructStructTemplate templateStructure)) {
            Skript.error("The 'this struct' expression can only be used in a struct template definition.");
            return false;
        }
        structClass = templateStructure.customClass;
        return true;
    }

    @Override
    protected Struct @Nullable [] get(Event event) {
        if (!(event instanceof DynamicFieldEvalEvent evalEvent))
            return (Struct[]) Array.newInstance(structClass, 0);
        return CollectionUtils.array(evalEvent.getStruct());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Struct> getReturnType() {
        return structClass;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "this struct";
    }

}
