package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.sovdee.oopsk.core.Struct;
import org.jetbrains.annotations.Nullable;

@Name("Struct Copy")
@Description("Makes a copy of a struct. The field contents may or may not be copies, depending on their types. " +
        "Entities, for example, cannot be copied.")
@Example("set {_a} to a struct copy of {_b}->playerdata")
@Since("1.0")
public class ExprStructCopy extends SimplePropertyExpression<Struct, Struct> {

    static {
        register(ExprStructCopy.class, Struct.class, "[a] struct copy", "structs");
    }

    @Override
    public @Nullable Struct convert(Struct struct) {
        return Struct.newInstance(struct);
    }

    @Override
    public Class<? extends Struct> getReturnType() {
        return getExpr().getReturnType();
    }

    @Override
    protected String getPropertyName() {
        return "struct copy";
    }
}
