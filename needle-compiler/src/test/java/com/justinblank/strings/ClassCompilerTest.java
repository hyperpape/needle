package com.justinblank.strings;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

public class ClassCompilerTest {

    public static final AtomicInteger CLASS_NAME_COUNTER = new AtomicInteger();

    @Test
    public void testLoop() throws Exception {
        String testClassName = testClassName();
        ClassBuilder builder = new ClassBuilder(testClassName, "java/lang/Object", new String[]{});
        builder.emptyConstructor();
        var method = builder.mkMethod("iterate", List.of(), "I");

        int counter = 1;
        int accumulator = 2;

        var init = method.addBlock();
        var body = method.addBlock();
        var ret = method.addBlock();

        init.push(0);
        init.setVar(counter,"I");
        init.push(0);
        init.setVar(accumulator,"I");

        body.readVar(counter,"I");
        body.push(5);
        body.cmp(ret, Opcodes.IF_ICMPGT);

        body.readVar(accumulator,"I");
        body.readVar(counter,"I");
        body.operate(Opcodes.IADD);
        body.setVar(accumulator,"I");

        ret.readVar(accumulator,"I");
        ret.addReturn(Opcodes.IRETURN);
        Class<?> c = new ClassCompiler(builder).generateClass();
        Object o = c.getConstructors()[0].newInstance();
    }

    @Test
    public void testCall() throws Exception {
        String testClassName = testClassName();
        ClassBuilder builder = new ClassBuilder(testClassName, "java/lang/Object", new String[]{});
        builder.emptyConstructor();
        var vars = new MatchingVars(-2, -2, -2, -2, 1);
        var method = builder.mkMethod("foo", List.of(CompilerUtil.STRING_DESCRIPTOR), "I", vars);

        var body = method.addBlock();

        body.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        body.call("length","java/lang/String","()I");
        body.addReturn(Opcodes.IRETURN);

        Class<?> c = new ClassCompiler(builder).generateClass();
        Object o = c.getConstructors()[0].newInstance();
    }

    @Test
    public void testEliminatesDeadPrivateMethods() throws Exception {
        var testClassName = testClassName();
        ClassBuilder builder = new ClassBuilder(testClassName, "java/lang/Object", new String[]{});
        builder.emptyConstructor();
        var vars = new MatchingVars(-2, -2, -2, -2, 1);
        var method = builder.mkMethod("foo", List.of(CompilerUtil.STRING_DESCRIPTOR), "I", vars, ACC_PRIVATE);

        var body = method.addBlock();

        body.readVar(vars, MatchingVars.STRING, CompilerUtil.STRING_DESCRIPTOR);
        body.call("length","java/lang/String","()I");
        body.addReturn(Opcodes.IRETURN);

        Class<?> c = new ClassCompiler(builder).generateClass();
        Object o = c.getConstructors()[0].newInstance();
        assertThrows(NoSuchMethodException.class, () -> o.getClass().getMethod("foo"));
    }

    public static String testClassName() {
        return "TestClass" + CLASS_NAME_COUNTER.incrementAndGet();
    }
}
