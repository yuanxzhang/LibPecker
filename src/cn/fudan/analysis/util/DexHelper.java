package cn.fudan.analysis.util;

import cn.fudan.analysis.tree.PackageNode;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;

import java.util.HashMap;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class DexHelper {

    private static HashMap<String, String> shortyMap = new HashMap<String, String>();

    static {
        shortyMap.put("V", "void");
        shortyMap.put("Z", "boolean");
        shortyMap.put("B", "byte");
        shortyMap.put("S", "short");
        shortyMap.put("C", "char");
        shortyMap.put("I", "int");
        shortyMap.put("J", "long");
        shortyMap.put("F", "float");
        shortyMap.put("D", "double");
    }

    public static String classType2Name(String classType) {
        classType = classType.replace('/', '.');
        classType = classType.substring(1, classType.length() - 1);
        return classType;
    }

    public static String className2Type(String className) {
        className = className.replace('.', '/');
        return "L" + className + ";";
    }

    public static String getParentPackageName(String pkgName) {
        if (pkgName.indexOf('.') > 0)
            return pkgName.substring(0, pkgName.lastIndexOf('.'));
        else
            return PackageNode.Factory.DEFAULT_PACKAGE;
    }

    public static String getPackageName(String className) {
        if (className.indexOf('.') > 0)
            return className.substring(0, className.lastIndexOf('.'));
        else
            return PackageNode.Factory.DEFAULT_PACKAGE;
    }

    public static String getPackageName(ClassDef clazz) {
        String className = DexHelper.classType2Name(clazz.getType());
        return getPackageName(className);
    }

    public static boolean isCompilerGeneratedMethod(Method m) {
        int accessFlagValue = m.getAccessFlags();
        AccessFlags[] flags = AccessFlags.getAccessFlagsForMethod(accessFlagValue);
        for (int i = 0; i < flags.length; i ++) {
            if (flags[i].getValue() == AccessFlags.BRIDGE.getValue())
                return true;
            if (flags[i].getValue() == AccessFlags.SYNTHETIC.getValue())
                return true;
        }
        return false;
    }

    public static boolean isPrimitiveClass(String classType) {
        return classType.length() == 1;
    }

    public static String getPrimitiveClassName(String primitiveType) {
        return shortyMap.get(primitiveType);
    }

    public static boolean isInnerClass(String className) {
        return className.indexOf('$') >= 0;
    }

    public static String getClassShortName(String className) {
        String shortName;
        if (className.indexOf('.') > 0)
            shortName = className.substring(className.lastIndexOf('.') + 1);
        else
            shortName = className;

        if (isInnerClass(shortName))
            return shortName.substring(0, shortName.indexOf("$"));
        else
            return shortName;
    }

    public static int getArrayDepth(String classType) {
        int arrayDepth = 0;
        while (classType.startsWith("[")) {
            classType = classType.substring(1);
            arrayDepth += 1;
        }
        return arrayDepth;
    }

    public static String getBaseClassTypeOfArray(String classType) {
        while (classType.startsWith("["))
            classType = classType.substring(1);
        return classType;
    }
}
