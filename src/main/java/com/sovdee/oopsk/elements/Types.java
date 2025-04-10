package com.sovdee.oopsk.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import com.sovdee.oopsk.core.Struct;

public class Types {

    static {
        Classes.registerClass(
                new ClassInfo<>(Struct.class, "struct")
                        .user("structs?")
                        .name("Struct")
                        .description("A struct is a collection of typed fields.")
                        .parser(new Parser<>() {
                            @Override
                            public boolean canParse(ParseContext context) {
                                return false;
                            }

                            @Override
                            public String toString(Struct o, int flags) {
                                return "struct " + o.getTemplate().getName();
                            }

                            @Override
                            public String toVariableNameString(Struct o) {
                                return "struct " + o.getTemplate().getName() + " (" + o.hashCode() + ")";
                            }
                        })
        );
    }

}
