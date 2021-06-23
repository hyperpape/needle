package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// TODO: this name stinks
public class MapVars implements Vars {

    private final Map<String, Integer> vars = new HashMap<>();

    void addVar(String varName, int index) {
        Objects.requireNonNull(varName, "No null var names");
        if (varName.isBlank()) {
            throw new IllegalArgumentException("No empty vars");
        }
        if (!StringUtils.isAlphanumeric(varName)) {
            throw new IllegalArgumentException("No non-alphanumeric vars");
        }
        if (vars.containsKey(varName)) {
            throw new IllegalStateException("Cannot redefine VarName=" + varName);
        }
        vars.put(varName, index);
    }

    @Override
    public int indexByName(String name) {
        Integer index = vars.get(name);
        if (index == null) {
            throw new IllegalStateException("VarName=" + name + " not found");
        }
        return index;
    }
}
