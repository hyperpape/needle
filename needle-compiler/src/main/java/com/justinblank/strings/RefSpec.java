package com.justinblank.strings;

import org.apache.commons.lang3.StringUtils;

class RefSpec {

    final String name;
    final String className;
    final String descriptor;
    // Purely for debugging and pretty-printing
    final boolean isSelf;

    RefSpec(String name, String className, String descriptor) {
        this(name, className, descriptor, false);
    }

    RefSpec(String name, String className, String descriptor, boolean isSelf) {
        if (name != null && StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Cannot pass an empty name to RefSpec constructor");
        }
        this.name = name;
        this.className = className;
        this.isSelf = isSelf;
        this.descriptor = descriptor;
    }
}
