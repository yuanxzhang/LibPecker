package cn.fudan.analysis.tree;

import cn.fudan.common.CodeContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class PackageNode {
    private CodeContainer container;
    private String packageName;

    private Set<ClassNode> includedClasss = new HashSet<>();
    private Set<PackageNode> subPackages = new HashSet<>();


    private PackageNode(CodeContainer container, String packageName) {this.container = container; this.packageName = packageName;}
    public String getPackageName() {return packageName;}

    public CodeContainer getContainer() {
        return container;
    }

    protected void finalize(){
        includedClasss.clear();
        subPackages.clear();
        packageName = null;

    }

    public void addClass(ClassNode clazz) {
        if (! includedClasss.contains(clazz))
            includedClasss.add(clazz);
    }

    public void addSubPackage(PackageNode pkg) {
        if (! subPackages.contains(pkg))
            subPackages.add(pkg);
    }

    public int getSubPackagesNum(){
        return subPackages.size();
    }

    public int getIncludedClassNum(){
        return includedClasss.size();
    }

    public Set<ClassNode> getIncludedClasss() {
        return includedClasss;
    }

    public Set<PackageNode> getSubPackages() {
        return subPackages;
    }

    @Override
    public String toString() {
        String text = this.packageName;
        text += "\n";
        for (PackageNode pkg : subPackages) {
            text += "    [pkg]"+pkg.getPackageName()+"\n";
        }
        for (ClassNode clazz : includedClasss) {
            text += "    [clz]"+clazz.getClassName()+"\n";
        }
        return text;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PackageNode) {
            return this.getPackageName().equals(((PackageNode) obj).getPackageName());
        }
        return false;
    }

    public static class Factory {
        private static HashMap<CodeContainer, HashMap<String, PackageNode>> instances = new HashMap<>();;

        public final static String DEFAULT_PACKAGE = ".";

        public synchronized static void clear() {
            instances.clear();
        }

        public synchronized static void clear(CodeContainer container) {
            if (instances.containsKey(container)) {
                instances.remove(container);
            }
        }

        public static boolean isDefaultPackage(String pkgName) {
            return pkgName == null || DEFAULT_PACKAGE.equals(pkgName);
        }

        public synchronized static PackageNode createInstanceIfNotExist(CodeContainer container, String pkgName) {
            if (! instances.containsKey(container))
                instances.put(container, new HashMap<String, PackageNode>());

            if (pkgName == null )
                pkgName = DEFAULT_PACKAGE;

            if (instances.get(container).containsKey(pkgName))
                return instances.get(container).get(pkgName);
            else {
                instances.get(container).put(pkgName, new PackageNode(container, pkgName));
                return instances.get(container).get(pkgName);
            }
        }

        public synchronized static boolean containsInstance(CodeContainer container, String pkgName) {
            if (! instances.containsKey(container)) {
                return false;
            }

            if (pkgName == null )
                pkgName = DEFAULT_PACKAGE;

            return instances.get(container).containsKey(pkgName);
        }

        public synchronized static PackageNode getInstance(CodeContainer container, String pkgName) {
            if (! instances.containsKey(container)) {
                return null;
            }

            if (pkgName == null )
                pkgName = DEFAULT_PACKAGE;

            return instances.get(container).get(pkgName);
        }
    }
}
