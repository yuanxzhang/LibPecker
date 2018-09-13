package cn.fudan.common;

import cn.fudan.common.util.HashHelper;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanxzhang on 03/03/2017.
 */
public class Apk extends CodeContainer{
    private String apkHash;
    private String apkPath;
    private MultiDexContainer<? extends DexBackedDexFile> dexContainer;

    @Override
    public String codeHash() {
        return apkHash;
    }

    @Override
    public String codePath(){return apkPath;}

    @Override
    public Set<? extends ClassDef> getClasses() {
        Set<ClassDef> allClasses = new HashSet<>();
        try {
            for (String dexName : dexContainer.getDexEntryNames()) {
                allClasses.addAll(dexContainer.getEntry(dexName).getClasses());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allClasses;
    }

    public static Apk loadFromFile(File apkFile){
        Apk apk = new Apk();

        try {
            apk.dexContainer = DexFileFactory.loadDexContainer(apkFile, Opcodes.getDefault());
            apk.apkHash = HashHelper.md5_32(apkFile);
            apk.apkPath = apkFile.getPath();

            return apk;
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (DexFileFactory.UnsupportedFileTypeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Apk loadFromFile(String apkFilePath) {
        Apk apk = new Apk();

        try {
            apk.dexContainer = DexFileFactory.loadDexContainer(new File(apkFilePath), Opcodes.getDefault());
            apk.apkHash = HashHelper.md5_32(new File(apkFilePath));
            apk.apkPath = apkFilePath;

            return apk;
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (DexFileFactory.UnsupportedFileTypeException e) {
            e.printStackTrace();
        }
        return null;
    }
}
