package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.objects.CustomClassInstance;
import com.sovdee.oopsk.objects.Field;
import com.sovdee.oopsk.objects.FieldInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprObjectField extends SimpleExpression<Object> {

    static {
        Skript.registerExpression(ExprObjectField.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING,
                "field <\\w+> of %struct%",
                "%struct%-\\><\\w+>");
    }

    private String fieldName;

    private boolean expectSingle;

    private @Nullable Field field;

    private Expression<CustomClassInstance> objectExpr;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        fieldName = parseResult.regexes.get(0).group(0);
        objectExpr = (Expression<CustomClassInstance>) expressions[0];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        @Nullable CustomClassInstance classInstance = objectExpr.getSingle(event);
        if (classInstance == null) return null;

        @Nullable FieldInstance fieldInstance = classInstance.getField(fieldName);
        if (fieldInstance == null) return null;

        return fieldInstance.getValue();
    }

    @Override
    public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        if (mode == ChangeMode.SET || mode == ChangeMode.DELETE) {
            return new Class[]{Object.class};
        }
        return new Class[0];
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        @Nullable CustomClassInstance object = this.objectExpr.getSingle(event);
        if (object == null) {
            Oopsk.warning("field name: " + fieldName + ", object: " + null);
            return;
        }

        @Nullable FieldInstance field = object.getField(fieldName);
        if (field == null) {
            Oopsk.warning("field " + fieldName + " of " + object.getName() + " does not exist");
            return;
        }

        switch (mode) {
            case SET:
                assert delta != null;
                field.setValue(delta);
                break;
            case DELETE:
                field.setValue(new Object[0]);
                break;
            default:
                assert false;
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field " + fieldName + " of " + objectExpr.toString(event, debug);
    }
}
