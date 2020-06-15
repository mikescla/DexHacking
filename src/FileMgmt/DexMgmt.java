/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileMgmt;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author fabri
 */
public class DexMgmt {

    public static DexFile loadDexFile(String path, int apiLevel)
    {
        File dexInputFile = new File(path);
        if (!dexInputFile.exists()) {
            System.err.println("Can't find the file " + dexInputFile.getName());
            System.exit(1);
        }
        try {
            return DexFileFactory.loadDexFile(dexInputFile,
                    Opcodes.forApi(apiLevel));
        } catch (IOException ex) {
            System.out.println("IOException thrown when trying to open a dex" + " file: " + ex);
        }
        return null;
    }

    @Deprecated
    public static void writeDexFile(String path, List<ClassDef> classes,
                                    DexFile dexFile) throws IOException {
        DexFileFactory.writeDexFile(path, new DexFile() {
            @Override
            public Set<? extends ClassDef> getClasses() {
                return new AbstractSet<ClassDef>() {

                    @Override
                    public Iterator<ClassDef> iterator() {
                        return classes.iterator();
                    }

                @Override public int size() {
                    return classes.size();
                }
                };
            }

            @Override
            public Opcodes getOpcodes() {
                return dexFile.getOpcodes();
            }
        });
    }
}
