package cn.fudan.common;

import cn.fudan.common.util.HashHelper;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by yuanxzhang on 03/03/2017.
 */
public class Lib extends CodeContainer {
    private String dexHash;
    private String dexPath;
    private DexFile dex;

    @Override
    public String codeHash() {
        return dexHash;
    }

    @Override
    public String codePath(){return dexPath;}

    @Override
    public Set<? extends ClassDef> getClasses() {
        return dex.getClasses();
    }

    public static Lib loadFromFile(String libFilePath) {
        Lib lib = new Lib();
        try {
            lib.dex = DexFileFactory.loadDexFile(libFilePath, Opcodes.getDefault());
            lib.dexHash = HashHelper.md5_32(new File(libFilePath));
            lib.dexPath = libFilePath;

            return lib;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Lib loadFromFile(File libFile){
        Lib lib = new Lib();
        try {
            lib.dex = DexFileFactory.loadDexFile(libFile.getPath(), Opcodes.getDefault());
            lib.dexHash = HashHelper.md5_32(libFile);
            lib.dexPath = libFile.getPath();

            return lib;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
