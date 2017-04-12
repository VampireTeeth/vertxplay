package org.vexavior.vertx.demo.asm;

import org.objectweb.asm.ClassReader;

import java.io.IOException;

/**
 * Created by sliu11 on 12/04/2017.
 */
public class ASMDemo1 {

    public static void main(String[] args) throws IOException {
        ClassPrinter classPrinter = new ClassPrinter();
        ClassReader cr = new ClassReader("java.lang.Runnable");
        cr.accept(classPrinter,0);
    }
}
