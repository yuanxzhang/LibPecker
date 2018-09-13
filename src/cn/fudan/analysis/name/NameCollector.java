package cn.fudan.analysis.name;

import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.CodeContainer;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.io.*;
import java.util.*;

/**
 * Created by yuanxzhang on 12/03/2017.
 */
public class NameCollector implements Serializable {

    static final long serialVersionUID = 197792367844872410L;

    private CodeContainer containter;
    private String containerHash;
    private String containerPath;
    private Set<String> classNames = new HashSet<>();
    private Map<String, List<String>> classMethodNames = new HashMap<>();
    private Map<String, List<String>> classFieldNames = new HashMap<>();

    public NameCollector() {}
    public NameCollector(CodeContainer container) {
        this.containter = container;
        this.containerHash = container.codeHash();
        this.containerPath = container.codePath();

        this.init();
    }

    public String getContainerHash() {
        return containerHash;
    }

    public String getContainerPath() {
        return containerPath;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(containerHash);
        out.writeObject(containerPath);
        out.writeObject(classNames);
        out.writeObject(classMethodNames);
        out.writeObject(classFieldNames);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.containter = null;
        this.containerHash = (String)in.readObject();
        this.containerPath = (String)in.readObject();
        this.classNames = (Set<String>)in.readObject();
        this.classMethodNames = (Map<String, List<String>>)in.readObject();
        this.classFieldNames = (Map<String, List<String>>)in.readObject();
    }

    private void init() {
        Set<? extends ClassDef> clazzes = this.containter.getClasses();
        for (ClassDef clazz : clazzes) {
            String className = DexHelper.classType2Name(clazz.getType());
            classNames.add(className);

            List<String> methodNames = new ArrayList<>();
            for (Method m : clazz.getMethods()) {
                if (! DexHelper.isCompilerGeneratedMethod(m)) {
                    String methodName = m.getName();
                    methodNames.add(methodName);
                }
            }
            classMethodNames.put(className, methodNames);

            List<String> fieldNames = new ArrayList<>();
            for (Field f : clazz.getFields()) {
                String fieldName = f.getName();
                fieldNames.add(fieldName);
            }
            classFieldNames.put(className, fieldNames);
        }
    }

    public Set<String> allPackageNames() {
        Set<String> pkgNames = new HashSet<>();
        for (String clzName : classNames) {
            pkgNames.add(DexHelper.getPackageName(clzName));
        }

        return pkgNames;
    }

    public Set<String> allClassNames() {
        return classNames;
    }

    public Set<String> allFieldNames() {
        Set<String> fieldNames = new HashSet<>();
        for (String className : classFieldNames.keySet()) {
            List<String> names = classFieldNames.get(className);
            fieldNames.addAll(names);
        }
        return fieldNames;
    }

    public Set<String> allMethodNames() {
        Set<String> methodNames = new HashSet<>();
        for (String className : classMethodNames.keySet()) {
            List<String> names = classMethodNames.get(className);
            methodNames.addAll(names);
        }
        return methodNames;
    }

    public Set<String> allClassNamesOfPackage(String pkgName) {
        if (pkgName == null)
            pkgName = PackageNode.Factory.DEFAULT_PACKAGE;

        Set<String> hitClassName = new HashSet<>();
        for (String clzName : classNames) {
            if (pkgName.equals(DexHelper.getPackageName(clzName)))
                hitClassName.add(clzName);
        }

        return hitClassName;
    }

    public List<String> allFieldNamesOfClass(String clzName) {
        return classFieldNames.get(clzName);
    }

    public List<String> allMethodNamesOfClass(String clzName) {
        return classMethodNames.get(clzName);
    }

    public void printPackages(OutputStream os) {
        PrintStream out = null;
        if (os instanceof PrintStream)
            out = (PrintStream) os;
        else
            out = new PrintStream(os);

        Set<String> pkgs = this.allPackageNames();
        for (String pkg : pkgs) {
            out.println("[pkg] " + pkg);
        }
    }

    public void printClasses(OutputStream os) {
        PrintStream out = null;
        if (os instanceof PrintStream)
            out = (PrintStream) os;
        else
            out = new PrintStream(os);

        Set<String> classes = this.allClassNames();
        for (String clz : classes) {
            out.println("[clz] " + clz);
        }
    }

    public void printMethods(OutputStream os) {
        PrintStream out = null;
        if (os instanceof PrintStream)
            out = (PrintStream) os;
        else
            out = new PrintStream(os);

        Set<String> methods = this.allMethodNames();
        for (String m : methods) {
            out.println("[method] " + m);
        }
    }

    public void printFields(OutputStream os) {
        PrintStream out = null;
        if (os instanceof PrintStream)
            out = (PrintStream) os;
        else
            out = new PrintStream(os);

        Set<String> fields = this.allFieldNames();
        for (String f : fields) {
            out.println("[field] " + f);
        }
    }
}
