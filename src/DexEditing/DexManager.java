package DexEditing;

import com.google.common.collect.Lists;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Dex Manager
 *
 * @author fabri
 * @author Michele Scalas
 */
public class DexManager {
    private final int apiLevel;
    private final DexFile origDexFile;
    private final String dexPath;
    private final String dexName;
    private final List<ClassDef> classes;

    public DexManager(DexFile dexFile, String dexFilePath) {

        this.dexPath = dexFilePath;
        dexName = Paths.get(this.getDexPath()).getFileName().toString();
        this.origDexFile = dexFile;

        this.apiLevel = this.origDexFile.getOpcodes().api;
        this.classes = Lists.newArrayList(this.origDexFile.getClasses());
    }

    /*PRIVATE METHODS*/
    private boolean modifyDexFile(int classIndex, int methodIndex,
                                  MethodImplementation implementation) {
        ClassDef classDef = this.getClass(classIndex);

        List<Method> newMethods = Lists.newArrayList(classDef.getMethods());

        Method targetMethod = newMethods.get(methodIndex);
        newMethods.set(methodIndex, DexBuilder.newMethod(targetMethod,
                implementation));

        this.setClass(classIndex, DexBuilder.newClass(classDef, newMethods));

        return true;
    }

    private MethodImplementation getMethodImplementationByIndex(int classIndex, int methodIndex) {

        ClassDef classDef = this.getClass(classIndex);
        Method method =
                Lists.newArrayList(classDef.getMethods()).get(methodIndex);

        MethodImplementation m = method.getImplementation();
        if (m == null)
            throw new NullPointerException("the method has no implementation");
        else return m;

    }

    private boolean isMethodEmpty(MutableMethodImplementation implementation) {
        if (implementation.getInstructions().size() == 1) {
            Opcode opcode = implementation.getInstructions().get(0).getOpcode();
            return opcode.equals(Opcode.RETURN_VOID) || opcode.equals(Opcode.RETURN) || opcode.equals(Opcode.RETURN_OBJECT) || opcode.equals(Opcode.RETURN_VOID_BARRIER) || opcode.equals(Opcode.RETURN_VOID_NO_BARRIER) || opcode.equals(Opcode.RETURN_WIDE);
        }
        return false;
    }

    /*PUBLIC METHODS*/

    /**
     * Adds adversarial instructions and write them to dex
     *
     * @param contClassIndex  index of the class to be modified
     * @param contMethodIndex index of the method to be modified
     * @param advInstructions list of adversarial instructions to inject
     * @param nAdvRegisters   number of registers of the adversarial
     *                        instructions
     * @param atBeginning     true if the code must be put at the beginning
     * @return true if the injection has been successful
     */
    public boolean addInstruction(int contClassIndex, int contMethodIndex,
                                  List<BuilderInstruction> advInstructions,
                                  int nAdvRegisters, boolean atBeginning) {
        // get implementation of class to be modified
        MethodImplementation container =
                getMethodImplementationByIndex(contClassIndex, contMethodIndex);
        MutableMethodImplementation oldImplementation =
                new MutableMethodImplementation(container);

        // assign the proper number of registers
        int registerCount;
        if (oldImplementation.getRegisterCount() >= nAdvRegisters) {
            registerCount = oldImplementation.getRegisterCount();
        } else {
            registerCount =
                    oldImplementation.getRegisterCount() + nAdvRegisters;
        }

        // insert new instructions in the right position
        // TODO refactor in a method (in DexBuilder?)
        List<BuilderInstruction> mergedInstructions = new ArrayList<>();
        if (isMethodEmpty(oldImplementation) || atBeginning) {
            mergedInstructions.addAll(advInstructions);
            mergedInstructions.addAll(oldImplementation.getInstructions());
        } else {
            List<BuilderInstruction> oldInstrs =
                    oldImplementation.getInstructions();
            BuilderInstruction lastOldInstr =
                    oldInstrs.get(oldInstrs.size() - 1);
            if (lastOldInstr.getOpcode() == Opcode.THROW) {
                mergedInstructions.addAll(oldInstrs);
                mergedInstructions.addAll(advInstructions);
            } else {
                for (int i = 0; i < oldInstrs.size() - 1; i++)
                    mergedInstructions.add(oldInstrs.get(i));
                mergedInstructions.addAll(advInstructions);
                mergedInstructions.add(lastOldInstr);
            }
        }

        MethodImplementationBuilder advMethodImpl =
                new MethodImplementationBuilder(registerCount);
        for (BuilderInstruction advInstr : mergedInstructions)
            advMethodImpl.addInstruction(advInstr);

        return modifyDexFile(contClassIndex, contMethodIndex,
                advMethodImpl.getMethodImplementation());
    }

    /**
     * Select the methods from current dex file that are suitable to be
     * modified with injected calls
     *
     * @param userOnly true if the methods to select must be user-implemented
     *                 and not part of API libraries (androidx and support)
     * @return a list of lists. Each inner list contains the index of the
     * selected class and the index of the selected method within that class.
     */
    public List<List<Integer>> getMethodsForInjection(boolean userOnly) {
        // todo make parallel
        List<List<Integer>> outList = new ArrayList<>();
        List<ClassDef> cDefList = Lists.newArrayList(origDexFile.getClasses());
        for (int i = 0; i < cDefList.size(); i++) {
            ClassDef classDef = cDefList.get(i);
            String clName = classDef.getType();
            if (!(clName.contains("$")))
                if (!userOnly || !(clName.contains("androidx") || clName.contains("support")))
                    if (classDef.getAccessFlags() == AccessFlags.PUBLIC.getValue()) {
                        List<Method> mDefList =
                                Lists.newArrayList(classDef.getMethods());
                        for (int j = 0; j < mDefList.size(); j++) {
                            Method methodDef = mDefList.get(j);
                            if (methodDef.getReturnType().equals("V") && !(methodDef.getName().contains("<") || methodDef.getImplementation() == null)) {
                                List<? extends Instruction> mInstrs =
                                        Lists.newArrayList(methodDef.getImplementation().getInstructions());
                                List<String> opcList =
                                        mInstrs.stream().map(a -> a.getOpcode().name).collect(Collectors.toList());
                                if (!(opcList.contains("packed-switch") || opcList.contains("sparse-switch")))
                                    if (!opcList.contains("goto"))
                                        if (!opcList.contains("fill-array" +
                                                "-data"))
                                            outList.add(List.of(i, j));
                            }
                        }
                    }
        }

        return outList;
    }

    /*GETTERS*/

    public DexFile getOrigDexFile() {
        return this.origDexFile;
    }

    public List<ClassDef> getClasses() {
        return this.classes;
    }

    public ClassDef getClass(int classIndex) {
        return this.getClasses().get(classIndex);
    }

    public String getDexPath() {
        return dexPath;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getDexName() {
        return dexName;
    }

    /*SETTERS*/

    public void setClass(int classIndex, ClassDef builder) {
        this.classes.set(classIndex, builder);

    }

}

