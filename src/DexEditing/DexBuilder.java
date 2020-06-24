package DexEditing;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fabri
 */
class DexBuilder {


    protected static ImmutableClassDef newClass(ClassDef classDef,
                                                List<Method> methods) {
        return new ImmutableClassDef(classDef.getType(),
                classDef.getAccessFlags(), classDef.getSuperclass(),
                classDef.getInterfaces(), classDef.getSourceFile(),
                classDef.getAnnotations(), classDef.getFields(), methods);
    }

    protected static ImmutableMethod newMethod(Method method,
                                               MethodImplementation implementation) {
        return new ImmutableMethod(method.getDefiningClass(),
                method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(),
                method.getAnnotations(), implementation);
    }

    @Deprecated
    protected static MethodImplementation addInstructionAtBeginning(List<BuilderInstruction> oldList, List<BuilderInstruction> newList, int registerCount) {
        MethodImplementationBuilder methodImplementation =
                new MethodImplementationBuilder(registerCount);
        oldList = new ArrayList<>(oldList);

        for (BuilderInstruction builderInstruction : newList) {
            methodImplementation.addInstruction(builderInstruction);
        }
        for (BuilderInstruction builderInstruction : oldList) {
            methodImplementation.addInstruction(builderInstruction);
        }

        return methodImplementation.getMethodImplementation();
    }

    @Deprecated
    protected static MethodImplementation addInstructionBeforeReturn(List<BuilderInstruction> oldList, List<BuilderInstruction> newList, int registerCount) {
        MethodImplementationBuilder methodImplementation =
                new MethodImplementationBuilder(registerCount);
        oldList = new ArrayList<>(oldList);

        for (int i = 0; i < oldList.size() - 1; i++) {
            methodImplementation.addInstruction(oldList.get(i));
        }
        for (BuilderInstruction builderInstruction : newList) {
            methodImplementation.addInstruction(builderInstruction);
        }
        methodImplementation.addInstruction(oldList.get(oldList.size() - 1));

        return methodImplementation.getMethodImplementation();
    }


}
