package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.sovdee.oopsk.core.Struct;
import org.jetbrains.annotations.Nullable;

@Name("Template of Struct")
@Description("Returns the name of the template used to create a struct.")
@Example("""
        set {_struct} to a message struct
        set {_template} to template of {_struct}
        # {_template} is now "message"
        """)
@Since("1.0")
public class ExprStructTemplate extends SimplePropertyExpression<Struct, String> {

    static {
        register(ExprStructTemplate.class, String.class, "[struct] template", "structs");
    }

    @Override
    public @Nullable String convert(Struct from) {
        return from.getTemplate().getName();
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "template";
    }

}
