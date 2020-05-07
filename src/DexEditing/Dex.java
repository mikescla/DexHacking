/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableAnnotation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.instruction.*;


/**
 * @author fabri
 */
public class Dex {
    private int apiLevel;
    private DexFile dexFile;
    private List<ClassDef> classes;

    //constructor
    public Dex(DexFile dexFile) {
        this.apiLevel = dexFile.getOpcodes().api;
        this.dexFile = dexFile;
        this.classes = Lists.newArrayList();
        extractClassesFromDexFile();
    }

    public DexFile getDexFile() {
        return this.dexFile;
    }

    public List<ClassDef> getDexClasses() {
        return this.classes;
    }


    public void addClassAndMethod(String className, String classAccessFlag,
                                  String methodName, String methodReturnType,
                                  String methodAccessFlag) throws IOException {
        ClassDef givenClass = this.getClass(className);
        List<Method> methods = new ArrayList<>(Arrays.asList(new Method[]{}));
        ImmutableSet<ImmutableAnnotation> annotations = ImmutableSet.of();
        int methodAccessFlagInt =
                AccessFlag.getAccessFlagFromString(methodAccessFlag);
        methodReturnType = ReturnType.getReturnTypeFromString(methodReturnType);
        MethodImplementation implementation =
                DexBuilder.newEmptyVoidImplementation(1);
        ImmutableList<MethodParameter> parameters = ImmutableList.of();

        if (givenClass == null) { // if the given class does not exists into
            // the dex file it adds a new class with the request method
            int classAccessFlagInt =
                    AccessFlag.getAccessFlagFromString(classAccessFlag);
            Collection<String> interfaces =
                    new ArrayList<>(Arrays.asList(new String[]{}));
            List<Field> fields = new ArrayList<>(Arrays.asList(new Field[]{}));
            String classSourceName =
                    className.split("/")[className.split("/").length - 1].replace(";", "");
            String superclass = "Ljava/lang/Object;";

            methods.add(DexBuilder.newConstructor(className, superclass));

            Method m = new ImmutableMethod(className, methodName, parameters,
                    methodReturnType, methodAccessFlagInt, annotations,
                    implementation);
            methods.add(m);

            this.classes.add(new ImmutableClassDef(className,
                    classAccessFlagInt, superclass, interfaces,
                    classSourceName + ".java", annotations, fields, methods));
        } else { // if the given class already exists into the dex file it
            // adds a new method into that class
            int index = this.classes.indexOf(givenClass);

            Method m = new ImmutableMethod(className, methodName, parameters,
                    methodReturnType, methodAccessFlagInt, annotations,
                    implementation);

            for (Method method : givenClass.getMethods()) {
                methods.add(method);
            }
            methods.add(m);

            this.classes.set(index, DexBuilder.newClass(givenClass, methods));
        }
    }

    public void addEmptyMethod(String className, String methodName,
                               String returnType, String accessFlag,
                               ImmutableList parameters) {
        ClassDef classDef = this.getClass(className);
        int index = this.classes.indexOf(classDef);

        int methodAccessFlag = AccessFlag.getAccessFlagFromString(accessFlag);
        List<Method> methods = Lists.newArrayList();
        ImmutableSet<ImmutableAnnotation> annotations = ImmutableSet.of();
        returnType = ReturnType.getReturnTypeFromString(returnType);
        MethodImplementation implementation =
                DexBuilder.newEmptyVoidImplementation(0);

        Method m = new ImmutableMethod(className, methodName, parameters,
                returnType, methodAccessFlag, annotations, implementation);

        for (Method method : classDef.getMethods()) {
            methods.add(method);
        }
        methods.add(m);

        this.classes.set(index, DexBuilder.newClass(classDef, methods));
    }


    public boolean addInstruction(String methodName,
                                  MethodImplementation implementation,
                                  boolean atBeginning) {
        List<List> fullMethodList =
                this.getallMethodImplementations(methodName);
        int nToGet = 1;
        if (fullMethodList.size() > 60) nToGet = 60;
        List methodParametersList = fullMethodList.get(nToGet);//this
        // .getMethodImplementation
        // (methodName);
        if (methodParametersList == null) {
            return false;
        }
        List data = Lists.newArrayList(methodParametersList);
        int classIndex = (int) data.get(1);
        MutableMethodImplementation oldImplementation =
                new MutableMethodImplementation((MethodImplementation) data.get(2));
        if (oldImplementation == null) {
            return false;
        } else {
            implementation = new MutableMethodImplementation(implementation);
            List<BuilderInstruction> methodInstructions = null;
            try {
                methodInstructions = oldImplementation.getInstructions();
            } catch (IllegalStateException ex) {
                return false;
            }
            List<BuilderInstruction> implInstructions =
                    (List<BuilderInstruction>) implementation.getInstructions();
            MethodImplementation newImplementation;
            int registerCount;

            if (oldImplementation.getRegisterCount() >= implementation.getRegisterCount()) {
                registerCount = oldImplementation.getRegisterCount();
            } else {
                registerCount =
                        oldImplementation.getRegisterCount() + implementation.getRegisterCount();
            }

            if (isMethodEmpty(oldImplementation) || atBeginning) {
                newImplementation =
                        DexBuilder.addInstructionAtBeginning(methodInstructions, implInstructions, registerCount);
            } else {
                newImplementation =
                        DexBuilder.addInstructionBeforeReturn(methodInstructions, implInstructions, registerCount);
            }
            return modifyDexFile(methodName, classIndex, newImplementation);
        }
    }

    public ImmutableList<Instruction> addForLoop(int nRepetitions,
                                                 List<Instruction> innerInstructions, int innerInstrOffset) {

        int firstConst = 0;
        int secConst = 1;

        List<Instruction> tempInstr = new ArrayList<>(Arrays.asList(
                /*00:
                0x00*/        new ImmutableInstruction11n(Opcode.CONST_4,
                        firstConst, nRepetitions),
                /*00:
                0x02*/        new ImmutableInstruction11n(Opcode.CONST_4,
                        secConst, 0),
                /*00: 0x04*/        new ImmutableInstruction22t(Opcode.IF_GE,
                        secConst, firstConst, 2 + innerInstrOffset + 3)));
        tempInstr.addAll(innerInstructions);
        tempInstr.addAll(Arrays.asList(
                /*00:
                0x4+i*/    new ImmutableInstruction22b(Opcode.ADD_INT_LIT8,
                        secConst, secConst, 1),
                /*00: 0x4+i+2*/    new ImmutableInstruction10t(Opcode.GOTO,
                        -(2 + innerInstrOffset + 2))));

        return ImmutableList.copyOf(tempInstr);
    }

    private boolean modifyDexFile(String methodName, int classIndex,
                                  MethodImplementation implementation) {
        ClassDef classDef = this.classes.get(classIndex);
        List<Method> methods = Lists.newArrayList();
        boolean modifiedMethod = false;

        if (classIndex != -1) {
            for (Method method : classDef.getMethods()) {
                if (method.getName().equals(methodName)) {
                    methods.add(DexBuilder.newMethod(method, implementation));
                    modifiedMethod = true;
                } else {
                    methods.add(method);
                }
            }
            if (modifiedMethod) {
                this.classes.set(classIndex, DexBuilder.newClass(classDef,
                        methods));
            }
            return modifiedMethod;
        } else {
            return modifiedMethod;
        }
    }

    private void extractClassesFromDexFile() {
        for (ClassDef classDef : this.dexFile.getClasses()) {
            this.classes.add(classDef);
        }
    }

    private ClassDef getClass(String className) {
        for (ClassDef classDef : this.classes) {
            if (classDef.getType().equals(className)) {
                return classDef;
            }
        }
        return null;
    }

    private List<List> getallMethodImplementations(String methodName) {
        List returnList = Lists.newArrayList();
        for (int i = 0; i < this.classes.size(); i++) {
            for (Method method : this.classes.get(i).getMethods()) {
                String currMethodName = method.getName();
                if (currMethodName.equals(methodName)
                        && !currMethodName.contains("android")
                        && method.getImplementation() != null) {
                    List innerList = new ArrayList();

                    innerList.add(this.classes.get(i).getType());
                    innerList.add(i);
                    innerList.add(method.getImplementation());
                    returnList.add(innerList);
                }
            }
        }
        return returnList;
}

    private List getMethodImplementation(String methodName) {
        /*
        Takes a method name and returns an array containing the name of the
        class
        that contains the given method and the implementation of that method
        */
        List returnList = Lists.newArrayList();
        int index = 0;
        for (int i = 0; i < this.classes.size(); i++) {
            for (Method method : this.classes.get(i).getMethods()) {
                if (!this.classes.get(i).getType().contains("$") && !this.classes.get(i).getType().contains("android/")) {
                    if (method.getName().equals(methodName)) {
                        if (method.getImplementation() != null) {
                            returnList.add(this.classes.get(i).getType());
                            returnList.add(i);
                            returnList.add(method.getImplementation());
                            return returnList;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isMethodEmpty(MutableMethodImplementation implementation) {
        if (implementation.getInstructions().size() == 1) {
            Opcode opcode = implementation.getInstructions().get(0).getOpcode();
            if (opcode.equals(Opcode.RETURN_VOID) || opcode.equals(Opcode.RETURN) || opcode.equals(Opcode.RETURN_OBJECT) || opcode.equals(Opcode.RETURN_VOID_BARRIER) || opcode.equals(Opcode.RETURN_VOID_NO_BARRIER) || opcode.equals(Opcode.RETURN_WIDE)) {
                return true;
            }
        }
        return false;
    }
}

