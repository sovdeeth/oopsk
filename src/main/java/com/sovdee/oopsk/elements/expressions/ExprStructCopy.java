package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.sovdee.oopsk.core.Struct;
import org.jetbrains.annotations.Nullable;

public class ExprStructCopy extends SimplePropertyExpression<Struct, Struct> {

    static {
        register(ExprStructCopy.class, Struct.class, "[a] struct copy", "structs");
    }

    @Override
    public @Nullable Struct convert(Struct struct) {
        return new Struct(struct);
    }

    @Override
    public Class<? extends Struct> getReturnType() {
        return Struct.class;
    }

    @Override
    protected String getPropertyName() {
        return "copy";
    }
}
