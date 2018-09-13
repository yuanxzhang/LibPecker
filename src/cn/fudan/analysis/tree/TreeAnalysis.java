package cn.fudan.analysis.tree;

import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.CodeContainer;
import org.jf.dexlib2.iface.ClassDef;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class TreeAnalysis {

    public static void freeMem() {
        PackageNode.Factory.clear();
        ClassNode.Factory.clear();
    }

    public static void freeMem(CodeContainer container) {
        PackageNode.Factory.clear(container);
        ClassNode.Factory.clear(container);
    }

    private static Set<PackageNode> groupClassesIntoPackages(CodeContainer container, Set<? extends ClassDef> clazzes) {
        Set<PackageNode> pkgs = new HashSet<>();
        for (ClassDef clazz : clazzes) {
            String pkgName = DexHelper.getPackageName(clazz);
            String clsName = DexHelper.classType2Name(clazz.getType());

            PackageNode node = PackageNode.Factory.createInstanceIfNotExist(container, pkgName);
            node.addClass(ClassNode.Factory.createInstanceIfNotExist(container, clsName, clazz));

            pkgs.add(node);
        }

        return pkgs;
    }

    public static Set<PackageNode> analyze(CodeContainer dexCode) {
        Set<PackageNode> packages = groupClassesIntoPackages(dexCode, dexCode.getClasses());
        for (PackageNode node : packages) {
            String pkgName = node.getPackageName();
            String parentPkgName = DexHelper.getParentPackageName(pkgName);
            while (! PackageNode.Factory.isDefaultPackage(parentPkgName)) {
                if (PackageNode.Factory.containsInstance(dexCode, parentPkgName)) {
                    PackageNode parentNode = PackageNode.Factory.getInstance(dexCode, parentPkgName);
                    parentNode.addSubPackage(node);
                    break;
                }

                parentPkgName = DexHelper.getParentPackageName(parentPkgName);
            }
        }

        return packages;
    }
}
