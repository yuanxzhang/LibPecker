package cn.fudan.common;

import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.util.HashHelper;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class Sdk extends CodeContainer {
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

    private Set<String> classNameSet = null;

    public Set<String> getTargetSdkClassNameSet() {
        if (classNameSet != null)
            return classNameSet;
        else {
            synchronized (this) {
                classNameSet = new HashSet<>();
                for (ClassDef clazz : dex.getClasses()) {
                    String className = DexHelper.classType2Name(clazz.getType());
                    classNameSet.add(className);
                }

                return classNameSet;
            }
        }
    }

    public static Sdk loadDefaultSdk() {
        Sdk sdk = new Sdk();

        try {
            String sdkFilePath = LibPeckerConfig.DEFAULT_SDK_DEX_PATH;

            sdk.dex = DexFileFactory.loadDexFile(sdkFilePath, Opcodes.getDefault());
            sdk.dexHash = HashHelper.md5_32(new File(sdkFilePath));
            sdk.dexPath = sdkFilePath;

            return sdk;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
