package org.vexavior.vertx.demo.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

/**
 * Created by sliu11 on 12/04/2017.
 */
public class ClassPrinter extends ClassVisitor {

    public ClassPrinter() {
        super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        StringBuilder sb = new StringBuilder();
        boolean isInterface = (access & ACC_INTERFACE) == 0x00;
        boolean hasSuper = superName != null;
        boolean hasInterfaces = interfaces != null && interfaces.length > 0;

        sb.append(isInterface ? "interface " : "class ")
                .append(name);
        if (superName != null) {
            sb.append(" extends ");
            sb.append(superName);
        }
        if (interfaces != null && interfaces.length > 0) {
            for (String intf : interfaces) {

            }
        }
        System.out.println(name + " extends " + superName + " {");
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        System.out.println("\t" + desc + " " + name);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("\t" + name + desc);
        return null;
    }

    @Override
    public void visitEnd() {
        System.out.println("}");
    }
}
