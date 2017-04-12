package org.vexavior.vertx.demo.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by sliu11 on 12/04/2017.
 */
public class ClassWriterDemo {

    public static void main(String[] args) throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8,
                ACC_PUBLIC+ ACC_ABSTRACT+ ACC_INTERFACE,
                "pkg/Comparable", null, "java/lang/Object",
                new String[]{"pkg/Mesurable"});

        cw.visitField(ACC_PUBLIC+ACC_STATIC+ACC_FINAL, "LESS", "I", null, new Integer(-1)).visitEnd();
        cw.visitField(ACC_PUBLIC+ACC_STATIC+ACC_FINAL, "EQUAL", "I", null, new Integer(0)).visitEnd();
        cw.visitField(ACC_PUBLIC+ACC_STATIC+ACC_FINAL, "GREATER", "I", null, new Integer(1)).visitEnd();
        cw.visitMethod(ACC_PUBLIC+ACC_STATIC, "compareTo", "(Ljava/lang/Object;)I", null, null).visitEnd();
        cw.visitEnd();
        ByteArrayInputStream is = new ByteArrayInputStream(cw.toByteArray());
        try {
            ClassReader cr = new ClassReader(is);
            cr.accept(new ClassPrinter(), 0);
        } finally {
            is.close();
        }

    }

}
