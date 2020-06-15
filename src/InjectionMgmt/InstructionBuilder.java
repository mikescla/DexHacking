/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package InjectionMgmt;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

/**
 * @author fabri
 */
public class InstructionBuilder {
    private DexFile dexFile;

    //Constructor
    public InstructionBuilder(DexFile dexFile) {
    }

    public static BuilderInstruction10x NO_OP() {
        return new BuilderInstruction10x(Opcode.NOP);
    }

    public static BuilderInstruction21c SGET_OBJECT(int registerA,
                                                    String className,
                                                    String methodName,
                                                    String refType) {
        return new BuilderInstruction21c(Opcode.SGET_OBJECT, registerA,
                new ImmutableFieldReference(className, methodName, refType));
    }

    public static BuilderInstruction22c IGET_OBJECT(int registerA,
                                                    int registerB,
                                                    String className) {
        return new BuilderInstruction22c(Opcode.IGET_OBJECT, registerA,
                registerB, new ImmutableMethodReference(className, "msg",
                Lists.newArrayList("Ljava/lang/String;"), ""));
    }

    public static BuilderInstruction21c CONST_STRING(int registerA,
                                                     String text) {
        return new BuilderInstruction21c(Opcode.CONST_STRING, registerA,
                new ImmutableStringReference(text));
    }

    public static BuilderInstruction35c INVOKE_STATIC(int registerCount,
                                                      int registerC,
                                                      int registerD,
                                                      int registerE,
                                                      int registerF,
                                                      int registerG,
                                                      String definingClass,
                                                      String name,
                                                      Iterable parameters,
                                                      String returnType) {
        return new BuilderInstruction35c(Opcode.INVOKE_STATIC, registerCount,
                registerC, registerD, registerE, registerF, registerG,
                new ImmutableMethodReference(definingClass, name, parameters,
                        returnType));
    }

    public static BuilderInstruction35c INVOKE_DIRECT(int registerCount,
                                                      int registerC,
                                                      int registerD,
                                                      int registerE,
                                                      int registerF,
                                                      int registerG,
                                                      String definingClass,
                                                      String name,
                                                      Iterable parameters,
                                                      String returnType) {
        return new BuilderInstruction35c(Opcode.INVOKE_DIRECT, registerCount,
                registerC, registerD, registerE, registerF, registerG,
                new ImmutableMethodReference(definingClass, name, parameters,
                        returnType));
    }


    public static BuilderInstruction10x RETURN_VOID() {
        return new BuilderInstruction10x(Opcode.RETURN_VOID);
    }


    public static BuilderInstruction21c NEW_INSTANCE(int registerA,
                                                     String className) {
        return new BuilderInstruction21c(Opcode.NEW_INSTANCE, registerA,
                new ImmutableTypeReference(className));
    }


    public static BuilderInstruction35c INVOKE_VIRTUAL(int registerCount,
                                                       int registerC,
                                                       int registerD,
                                                       int registerE,
                                                       int registerF,
                                                       int registerG,
                                                       String definingClass,
                                                       String name,
                                                       Iterable parameters,
                                                       String returnType) {
        return new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, registerCount, registerC, registerD, registerE, registerF, registerG, new ImmutableMethodReference(definingClass, name, parameters, returnType));
    }

    public static BuilderInstruction21c CONST_4(int registerA, int value) {
        return new BuilderInstruction21c(Opcode.CONST_4, registerA,
                new ImmutableStringReference(Integer.toHexString(value)));
    }

    public static BuilderInstruction11n CONST_4_Instr(int registerA,
                                                      int value) {
        return new BuilderInstruction11n(Opcode.CONST_4, registerA, value);
    }

}
