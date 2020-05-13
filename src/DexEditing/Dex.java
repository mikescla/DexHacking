/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22b;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22t;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.immutable.ImmutableAnnotation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * @author fabri
 */
public class Dex {
    private final int apiLevel;
    private final DexFile dexFile;
    private final List<ClassDef> classes;

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
                                  List<BuilderInstruction> advInstructions,
                                  int nAdvRegisters, boolean atBeginning) {
        List<List> fullMethodList =
                this.getallMethodImplementations(methodName);
        List mainMethod = findMain(fullMethodList);
        if (mainMethod.size() == 0) {
            int nToGet = 0;
            mainMethod = fullMethodList.get(nToGet);
        }
        if (mainMethod == null) {
            return false;
        }
        List data = Lists.newArrayList(mainMethod);
        int classIndex = (int) data.get(1);
        MutableMethodImplementation oldImplementation =
                new MutableMethodImplementation((MethodImplementation) data.get(2));
        if (oldImplementation == null) {
            return false;
        } else {
            int registerCount;
            if (oldImplementation.getRegisterCount() >= nAdvRegisters) {
                registerCount = oldImplementation.getRegisterCount();
            } else {
                registerCount =
                        oldImplementation.getRegisterCount() + nAdvRegisters;
            }

            List<BuilderInstruction> mergedInstructions = new ArrayList<>();
            if (isMethodEmpty(oldImplementation) || atBeginning) {
                mergedInstructions.addAll(advInstructions);
                mergedInstructions.addAll(oldImplementation.getInstructions());
            } else {
                List<BuilderInstruction> oldInstrs =
                        oldImplementation.getInstructions();
                for (int i = 0; i < oldInstrs.size() - 1; i++)
                    mergedInstructions.add(oldInstrs.get(i));
                mergedInstructions.addAll(advInstructions);
                mergedInstructions.add(oldInstrs.get(oldInstrs.size() - 1));
            }

            MethodImplementationBuilder advMethodImpl =
                    new MethodImplementationBuilder(registerCount);
            for (BuilderInstruction advInstr : mergedInstructions)
                advMethodImpl.addInstruction(advInstr);

            return modifyDexFile(methodName, classIndex,
                    advMethodImpl.getMethodImplementation());
        }
    }

    public boolean addInstructionWithLoop(String methodName,
                                          List<BuilderInstruction> advInstructions, int nAdvRegisters, boolean atBeginning, int innerOffset) {
        List<List> fullMethodList =
                this.getallMethodImplementations(methodName);
        List mainMethod = findMain(fullMethodList);
        if (mainMethod.size() == 0) {
            int nToGet = 1;
            mainMethod = fullMethodList.get(nToGet);
        }
        if (mainMethod == null) {
            return false;
        }
        List data = Lists.newArrayList(mainMethod);
        int classIndex = (int) data.get(1);
        MutableMethodImplementation oldImplementation =
                new MutableMethodImplementation((MethodImplementation) data.get(2));
        if (oldImplementation == null) {
            return false;
        } else {
            int registerCount;
            //                        registerCount =
            //                                oldImplementation
            //                                .getRegisterCount() +
            //                                        Math.abs(oldImplementation
            //                                        .getRegisterCount() -
            //                                        nAdvRegisters+2);
            if (oldImplementation.getRegisterCount() >= nAdvRegisters) {
                registerCount = oldImplementation.getRegisterCount();
            } else {
                registerCount =
                        oldImplementation.getRegisterCount() + nAdvRegisters;
            }

            advInstructions = putCorrectTarget(advInstructions, innerOffset,
                    oldImplementation.getInstructions());

            List<BuilderInstruction> mergedInstructions = new ArrayList<>();
            if (isMethodEmpty(oldImplementation) || atBeginning) {
                mergedInstructions.addAll(advInstructions);
                mergedInstructions.addAll(oldImplementation.getInstructions());
            } else {
                List<BuilderInstruction> oldInstrs =
                        oldImplementation.getInstructions();
                for (int i = 0; i < oldInstrs.size() - 1; i++)
                    mergedInstructions.add(oldInstrs.get(i));
                mergedInstructions.addAll(advInstructions);
                mergedInstructions.add(oldInstrs.get(oldInstrs.size() - 1));
            }

            MethodImplementationBuilder advMethodImpl =
                    new MethodImplementationBuilder(registerCount);
            for (BuilderInstruction advInstr : mergedInstructions)
                advMethodImpl.addInstruction(advInstr);

            return modifyDexFile(methodName, classIndex,
                    advMethodImpl.getMethodImplementation());
        }
    }

    private List findMain(List<List> fullMethodList) {
        for (List<String> classes : fullMethodList)
            if (classes.get(0).contains("Main")) return classes;
        return new ArrayList();
    }

    private List<BuilderInstruction> putCorrectTarget(List<BuilderInstruction> advInstructions, int innerOffset, List<BuilderInstruction> oldInstrs) {

        int noldInstrs = oldInstrs.size();
        int lastOldCodeAddr =
                oldInstrs.get(noldInstrs - 1).getLocation().getCodeAddress();

        BuilderInstruction22t ifIsntr =
                (BuilderInstruction22t) advInstructions.get(2);
        int nAdvInstrs = advInstructions.size();


        int gotoIndex = noldInstrs + 1;
        int gotoAddr = lastOldCodeAddr + 1;
        int ifIndex = noldInstrs + nAdvInstrs + 3;
        int ifAddr = lastOldCodeAddr + 2 + innerOffset + 5;

        MethodLocation ifLoc = null;
        MethodLocation gotoLoc = null;
        try {
            Class<org.jf.dexlib2.builder.MethodLocation> testCl =
                    MethodLocation.class;
            Class[] arr = new Class[3];
            arr[0] = BuilderInstruction.class;
            arr[1] = int.class;
            arr[2] = int.class;
            Constructor<MethodLocation> testConstr =
                    testCl.getDeclaredConstructor(arr);
            testConstr.setAccessible(true);
            gotoLoc = testConstr.newInstance(null, gotoAddr, gotoIndex);
            ifLoc = testConstr.newInstance(null, ifAddr, ifIndex);

        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }

        MethodImplementationBuilder dummyBuilder =
                new MethodImplementationBuilder(1);
        dummyBuilder.addLabel("gotoLab");
        dummyBuilder.addLabel("ifLab");

        Label a = dummyBuilder.getLabel("gotoLab");
        a.setLocation(gotoLoc);
        Label b = dummyBuilder.getLabel("ifLab");
        b.setLocation(ifLoc);

        BuilderInstruction newIf = new BuilderInstruction22t(Opcode.IF_GE,
                ifIsntr.getRegisterA(), ifIsntr.getRegisterB(), b);
        BuilderInstruction newgoto = new BuilderInstruction10t(Opcode.GOTO, a);

        ArrayList<BuilderInstruction> newList =
                new ArrayList<>(advInstructions);
        newList.set(2, newIf);
        newList.set(advInstructions.size() - 1, newgoto);

        return newList;

    }

    public ImmutableList<BuilderInstruction> addForLoop(int nRepetitions,
                                                        List<BuilderInstruction> innerInstructions, int innerInstrOffset, int nInnerRegisters, int oldOffset, boolean atBeginning) {

        int firstConst = 1;//3 + nInnerRegisters;
        int secConst = 0;//4 + nInnerRegisters;

        MethodImplementationBuilder dummyBuilder =
                new MethodImplementationBuilder(1);
        dummyBuilder.addLabel("gotoLab");
        dummyBuilder.addLabel("ifLab");

        Label gotoLab = dummyBuilder.getLabel("gotoLab");
        Label ifLab = dummyBuilder.getLabel("ifLab");

        List<BuilderInstruction> tempInstr = new ArrayList<>(Arrays.asList(
                /*00:
                0x00*/        new BuilderInstruction11n(Opcode.CONST_4,
                        secConst, 0),
                /*00:
                0x02*/        new BuilderInstruction11n(Opcode.CONST_4,
                        firstConst, nRepetitions),
                /*00: 0x04*/        new BuilderInstruction22t(Opcode.IF_GE,
                        secConst, firstConst, ifLab)));
        tempInstr.addAll(innerInstructions);
        tempInstr.addAll(Arrays.asList(
                /*00:
                0x4+i*/    new BuilderInstruction22b(Opcode.ADD_INT_LIT8,
                        secConst, secConst, 1),
                /*00: 0x4+i+2*/    new BuilderInstruction10t(Opcode.GOTO,
                        gotoLab)));

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
                if (currMethodName.equals(methodName) && !currMethodName.contains("android") && method.getImplementation() != null) {
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
            return opcode.equals(Opcode.RETURN_VOID) || opcode.equals(Opcode.RETURN) || opcode.equals(Opcode.RETURN_OBJECT) || opcode.equals(Opcode.RETURN_VOID_BARRIER) || opcode.equals(Opcode.RETURN_VOID_NO_BARRIER) || opcode.equals(Opcode.RETURN_WIDE);
        }
        return false;
    }
}

