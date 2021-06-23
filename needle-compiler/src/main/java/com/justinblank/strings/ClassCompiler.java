package com.justinblank.strings;

import com.justinblank.classloader.MyClassLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.justinblank.strings.CompilerUtil.pushInt;
import static org.objectweb.asm.Opcodes.*;

// TODO: Properly decouple this class and collaborators from DFA specific operations to make it truly general purpose
// OR merge them together again
// TODO: Simplify/remove functionality that just passes through to ASM
public class ClassCompiler {

    private final ClassWriter classWriter;
    private final ClassVisitor classVisitor;
    private final ClassBuilder classBuilder;
    private final String className;
    private final boolean debug;
    private int lineNumber = 1;

    protected ClassCompiler(ClassBuilder classBuilder) {
        this(classBuilder, false);
    }

    protected ClassCompiler(ClassBuilder classBuilder, boolean debug) {
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        this.debug = debug;
        if (debug) {
            this.classVisitor = new CheckClassAdapter(this.classWriter);
        }
        else {
            this.classVisitor = this.classWriter;
        }
        this.classBuilder = classBuilder;
        this.className = classBuilder.getClassName();
    }

    byte[] generateClassAsBytes() {
        return writeClassAsBytes();
    }

    Class<?> generateClass() {
        byte[] classBytes = generateClassAsBytes();
        return MyClassLoader.getInstance().loadClass(classBuilder.getClassName(), classBytes);
    }

    protected String getClassName() {
        return className;
    }

    protected byte[] writeClassAsBytes() {
        if (debug) {
            var stringWriter = new StringWriter();
            var printer = new ClassPrinter(new PrintWriter(stringWriter));
            printer.printClass(classBuilder);
            System.out.println(stringWriter);
        }
        defineClass(classBuilder);
        addFields();

        writeStaticBlocks();
        for (var method : classBuilder.allMethods()) {
            writeMethod(method);
        }
        byte[] classBytes = classWriter.toByteArray();
        if (debug) {
            try (FileOutputStream fos = new FileOutputStream("target/" + className + ".class")) {
                fos.write(classBytes);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return classBytes;
    }

    protected ClassBuilder getClassBuilder() {
        return classBuilder;
    }

    private void writeStaticBlocks() {
        if (classBuilder.staticBlocks.isEmpty()) {
            return;
        }
        var mv = classVisitor.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();

        for (var b : classBuilder.staticBlocks) {
            visitBlock(mv, Optional.empty(), b);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(6, 6);
        mv.visitEnd();
    }

    private void defineClass(ClassBuilder builder) {
        classVisitor.visit(Opcodes.V9, ACC_PUBLIC, builder.getClassName(), null, classBuilder.superClass, classBuilder.interfaces);
    }

    protected final void addFields() {
        for (var field : classBuilder.fields) {
            classVisitor.visitField(field.modifier, field.name, field.descriptor, field.signature, field.value);
        }
    }

    void writeMethod(Method method) {
        try {
            var mv = classVisitor.visitMethod(ACC_PUBLIC, method.methodName, method.descriptor(), null, null);
            var vars = method.getMatchingVars();

            mv.visitCode();
            for (var block : method.blocks) {
                visitBlock(mv, vars, block);
            }
            mv.visitMaxs(12, 12);
            mv.visitEnd();
        }
        catch (Exception e) {
            System.out.println("Error in Method=" + method.methodName);
            throw e;
        }
    }

    private void visitBlock(MethodVisitor mv, Optional<Vars> vars, Block block) {
        mv.visitLabel(block.getLabel());
        for (var op : block.operations) {
            writeOperation(mv, vars, op);
        }
    }

    private void writeOperation(MethodVisitor mv, Optional<Vars> vars, Operation op) {
        switch (op.inst) {
            case INCREMENT_INDEX:
                mv.visitIincInsn(vars.get().indexByName(MatchingVars.INDEX), 1);
                return;
            case DECREMENT_INDEX:
                mv.visitIincInsn(vars.get().indexByName(MatchingVars.INDEX), -1);
                return;
            case RETURN:
            case PASSTHROUGH:
                mv.visitInsn(op.count);
                return;
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
            case CALL:
                var spec = op.spec;
                var name = spec.isSelf ? this.className : spec.className;
                int opcode;
                var isInterface = false;
                if (op.inst == Operation.Inst.INVOKESTATIC) {
                    opcode = INVOKESTATIC;
                }
                else if (op.inst == Operation.Inst.INVOKESPECIAL) {
                    opcode = INVOKESPECIAL;
                }
                else if (op.inst == Operation.Inst.INVOKEINTERFACE) {
                    opcode = INVOKEINTERFACE;
                    isInterface = true;
                }
                else {
                    opcode = INVOKEVIRTUAL;
                }
                mv.visitMethodInsn(opcode, name, spec.name, spec.descriptor, isInterface);
                return;
            case VALUE:
                pushInt(mv, op.count);
                return;
            case READ_VAR:
                handleReadVar(mv, op, vars);
                return;
            case READ_FIELD:
                spec = op.spec;
                name = spec.isSelf ? this.className : spec.className;
                mv.visitFieldInsn(GETFIELD, name, op.spec.name, op.spec.descriptor);
                return;
            case READ_STATIC:
                spec = op.spec;
                name = spec.isSelf ? this.className : spec.className;
                mv.visitFieldInsn(GETSTATIC, name, op.spec.name, op.spec.descriptor);
                return;
            case SET_VAR:
                if (op.spec != null) {
                    switch (op.spec.descriptor) {
                        case "I":
                        case "Z":
                        case "D":
                        case "F":
                        case "C":
                            mv.visitVarInsn(ISTORE, op.count);
                            break;
                        default:
                            mv.visitVarInsn(ASTORE, op.count);
                    }
                }
                else {
                    mv.visitVarInsn(ISTORE, op.count);
                }
                return;
            case SET_FIELD:
                mv.visitFieldInsn(PUTFIELD, op.spec.className, op.spec.name, op.spec.descriptor);
                return;
            case PUT_STATIC:
                spec = op.spec;
                name = spec.isSelf ? this.className : spec.className;
                mv.visitFieldInsn(PUTSTATIC, name, op.spec.name, op.spec.descriptor);
                return;
            case STORE_MATCH:
                // TODO: check
                mv.visitVarInsn(ISTORE, vars.get().indexByName(MatchingVars.STATE));
                return;
            case JUMP:
                mv.visitJumpInsn(op.count, op.target.getLabel());
                return;
            case NEW:
                mv.visitTypeInsn(NEW, op.spec.descriptor);
                return;
            case TABLESWITCH:
                var blocks = op.blockTargets;
                var labels = blocks.stream().map(Block::getLabel).collect(Collectors.toList());
                mv.visitTableSwitchInsn(op.count, blocks.size() - 1, op.target.getLabel(), labels.toArray(new Label[0]));
                return;
            case LOOKUPSWITCH:
                blocks = op.blockTargets;
                labels = blocks.stream().map(Block::getLabel).collect(Collectors.toList());
                var keys = new int[labels.size()];
                for (var i = 0; i < op.ints.size(); i++) {
                    keys[i] = op.ints.get(i);
                }
                mv.visitLookupSwitchInsn(op.blockTargets.get(0).getLabel(), keys, labels.toArray(new Label[0]));
                return;
            default:
                throw new IllegalStateException("Unrecognized opcode: " + op.inst);
            }
        }

    private void handleReadVar(MethodVisitor mv, Operation op, Optional<Vars> vars) {
        switch (op.spec.descriptor) {
            case "C":
            case "I":
            case "B":
            case "Z":
                checkVarIndex(op.count);
                mv.visitVarInsn(ILOAD, op.count);
                return;
            case "L":
                checkVarIndex(op.count);
                mv.visitVarInsn(LLOAD, op.count);
                return;
            case "F":
                checkVarIndex(op.count);
                mv.visitVarInsn(FLOAD, op.count);
                return;
            case "D":
                checkVarIndex(op.count);
                mv.visitVarInsn(DLOAD, op.count);
                return;
            default:
                AtomicInteger count = new AtomicInteger(op.count);
                vars.ifPresent((v) -> {
                    if (op.spec.name != null) {
                        count.set(v.indexByName(op.spec.name));
                    }
                });
                checkVarIndex(count.get());
                mv.visitVarInsn(ALOAD, count.get());
                return;
        }
    }

    private void checkVarIndex(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Illegal variable index=" + count);
        }
    }

    protected int newLine() {
        return ++lineNumber;
    }

    protected int currentLine() {
        return lineNumber;
    }

    protected void visitLine(MethodVisitor mv) {
        var label = new Label();
        mv.visitLabel(label);
        mv.visitLineNumber(newLine(), label);
    }
}
