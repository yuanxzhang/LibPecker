package cn.fudan.analysis.tree;

import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.CodeContainer;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class ClassNode {
    private CodeContainer container;
    private ClassDef classDefInDex;
    private String className;

    public ClassDef getClassDefInDex() {
        return classDefInDex;
    }

    private ClassNode(CodeContainer container, String className, ClassDef clazz) {this.container = container;this.className = className;this.classDefInDex = clazz;}
    public String getClassName() {
        return className;
    }

    public Set<Method> getNormalizedMethods() {
        Set<Method> normalizedMethods = new HashSet<>();
        for (Method m : classDefInDex.getMethods()) {
            if (! DexHelper.isCompilerGeneratedMethod(m))
                normalizedMethods.add(m);
        }
        return normalizedMethods;
    }

    public int getNormalizedMethodsNum(){
        return getNormalizedMethods().size();
    }


    public Iterable<? extends Method> getMethods() {
        return classDefInDex.getMethods();
    }

    public Iterable<? extends Field> getFields() {
        return classDefInDex.getFields();
    }

    @Override
    public String toString() {
        return this.className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassNode) {
            return this.getClassName().equals(((ClassNode) obj).getClassName());
        }
        return false;
    }

    public static class Factory {
        private static HashMap<CodeContainer, HashMap<String, ClassNode>> instances = new HashMap<>();

        public synchronized static void clear() {
            instances.clear();
        }

        public synchronized static void clear(CodeContainer container) {
            if (instances.containsKey(container)) {
                instances.remove(container);
            }
        }

        public synchronized static ClassNode createInstanceIfNotExist(CodeContainer container, String className, ClassDef clazz) {
            if (! instances.containsKey(container)) {
                instances.put(container, new HashMap<String, ClassNode>());
            }

            if (instances.get(container).containsKey(className))
                return instances.get(container).get(className);
            else {
                ClassNode classNode = new ClassNode(container, className, clazz);
                instances.get(container).put(className, classNode);
                return classNode;
            }
        }

        public synchronized static boolean containsInstance(CodeContainer container, String className) {
            if (! instances.containsKey(container)) {
                return false;
            }
            else
                return instances.get(container).containsKey(className);
        }

        public synchronized static ClassNode getInstance(CodeContainer container, String className) {
            if (! instances.containsKey(container)) {
                return null;
            }
            else
                return instances.get(container).get(className);
        }
    }
}
