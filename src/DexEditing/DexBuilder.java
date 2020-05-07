/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DexEditing;

import InstructionMgmt.InstructionBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.immutable.ImmutableAnnotation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
/**
 *
 * @author fabri
 */
class DexBuilder {
    
   
    protected static ImmutableClassDef newClass(ClassDef classDef, List<Method> methods){
        return new ImmutableClassDef(classDef.getType(),
                        classDef.getAccessFlags(),
                        classDef.getSuperclass(),
                        classDef.getInterfaces(),
                        classDef.getSourceFile(),
                        classDef.getAnnotations(),
                        classDef.getFields(),
                        methods);
    }
    
    protected static ImmutableMethod newMethod(Method method, MethodImplementation implementation){
        return new ImmutableMethod(method.getDefiningClass(),
                                   method.getName(),
                                   method.getParameters(),
                                   method.getReturnType(),
                                   method.getAccessFlags(),
                                   method.getAnnotations(),
                                   implementation);
    }
    
    protected static Method newConstructor(String className, String superClass) throws IOException
    {
        String methodName = "<init>";
        ImmutableList<MethodParameter> parameters = ImmutableList.of();
        String returnType = "V";
        int accessFlag = AccessFlags.CONSTRUCTOR.getValue();
        ImmutableSet<ImmutableAnnotation> annotations = ImmutableSet.of();
        
        MethodImplementationBuilder implementation = new MethodImplementationBuilder(1);

        implementation.addInstruction(InstructionBuilder.INVOKE_DIRECT(1, 0, 0, 0, 0, 0,
                superClass, "<init>", Lists.newArrayList(), "V"));
        implementation.addInstruction(InstructionBuilder.RETURN_VOID());
        
        MethodImplementation methodImplementation = implementation.getMethodImplementation();

        Method m = new ImmutableMethod(className, methodName, parameters, returnType, accessFlag, annotations, methodImplementation);
        
        return m;
    }
        

    protected static MethodImplementation newEmptyVoidImplementation(int registerCount){
        MethodImplementationBuilder methodImplementation = new MethodImplementationBuilder(registerCount);
        methodImplementation.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        
        return methodImplementation.getMethodImplementation();
    }
    
    
    protected static MethodImplementation addInstructionAtBeginning(List<BuilderInstruction> oldList, List<BuilderInstruction> newList, int registerCount){
        MethodImplementationBuilder methodImplementation = new MethodImplementationBuilder(registerCount);
        oldList = new ArrayList<>(oldList);

        for (int i = 0; i < newList.size(); i++){
            methodImplementation.addInstruction(newList.get(i));
        }
        for (int i = 0; i < oldList.size(); i++){
            methodImplementation.addInstruction(oldList.get(i));
        }
        
        return methodImplementation.getMethodImplementation();        
    }
    
    protected static MethodImplementation addInstructionBeforeReturn(List<BuilderInstruction> oldList, List<BuilderInstruction> newList, int registerCount){
        MethodImplementationBuilder methodImplementation = new MethodImplementationBuilder(registerCount);
        oldList = new ArrayList<>(oldList);
        
        for (int i = 0; i < oldList.size()-1; i++){
            methodImplementation.addInstruction(oldList.get(i));
        }
        for (BuilderInstruction builderInstruction : newList) {
            methodImplementation.addInstruction(builderInstruction);
        }
        methodImplementation.addInstruction(oldList.get(oldList.size()-1));
        
        return methodImplementation.getMethodImplementation();        
    }



}
