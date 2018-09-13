package cn.fudan.common.util;

import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.analysis.util.DexHelper;

import java.util.List;
import java.util.Map;

/**
 * Created by yuanxzhang on 08/04/2017.
 */
public class PackageNameUtil {

    public static String getParentPackageName(String packageName) {
        return DexHelper.getParentPackageName(packageName);
    }

    public static int packageNameLength(String packageName) {
        if (packageName == null || packageName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
            return 0;
        if (! packageName.contains("."))
            return 1;
        else
            return packageName.split("\\.").length;
    }

    //test if parentPackageName is parent of childPackageName, return parent distance (-1 means not parent)
    public static int isParentPackageName(String parentPackageName, String childPackageName) {
        if (parentPackageName == null || childPackageName == null)
            return -1;
        if (parentPackageName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
            return -1;
        if (parentPackageName.length() > childPackageName.length())
            return -1;

        int distance = 0;

        String childParentPackageName = DexHelper.getParentPackageName(childPackageName);
        distance ++;

        while (! childParentPackageName.equals(parentPackageName)) {
            if (childParentPackageName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
                return -1;

            childParentPackageName = DexHelper.getParentPackageName(childParentPackageName);
            distance ++;
        }
        return distance;
    }

    //test if childPackageName is child of parentPackageName, return parent distance (-1 means not child)
    public static int isChildPackageName(String childPackageName, String parentPackageName) {
        return isParentPackageName(parentPackageName, childPackageName);
    }

    public static boolean isSiblingPackageName(String firstPackageName, String secondPackageName) {
        if (firstPackageName == null || secondPackageName == null)
            return false;

        String a = getParentPackageName(firstPackageName);
        if (a.equals(PackageNode.Factory.DEFAULT_PACKAGE))
            return false;
        String b = getParentPackageName(secondPackageName);
        return a.equals(b);
    }

    public static boolean inSameRootPackage(String libPackageName1, String libPackageName2, Map<String, List<String>> rootPackageMap) {
        for (List<String> sameRootPackageList : rootPackageMap.values())
            if (sameRootPackageList.contains(libPackageName1))
                return sameRootPackageList.contains(libPackageName2);
        return false;
    }

    public static String maxCommonPackageName(String libPackageName1, String libPackageName2) {
        if (libPackageName1.equals(libPackageName2))
            return libPackageName1;

        if (isParentPackageName(libPackageName1, libPackageName2) > 0)
            return libPackageName1;

        libPackageName1 = getParentPackageName(libPackageName1);
        while (! libPackageName1.equals(PackageNode.Factory.DEFAULT_PACKAGE)) {
            if (isParentPackageName(libPackageName1, libPackageName2) > 0)
                return libPackageName1;
            libPackageName1 = getParentPackageName(libPackageName1);
        }

        return PackageNode.Factory.DEFAULT_PACKAGE;
    }

    public static int packageNameDistance(String prefixPackageName, String packageName) {
        if (prefixPackageName == null || prefixPackageName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
            return packageNameLength(packageName);

        if (! packageName.startsWith(prefixPackageName))
            return -1;

        return packageNameLength(packageName)-packageNameLength(prefixPackageName);
    }

    public static void main(String[] args) {
        String existingLibPackageName = "a.b.c";
        String existingApkPackageName = "x.a";
        String libPackageName = "a.b.d.f";
        String apkPackageName = "x.d.x";

        String maxCommonLibPackageName = PackageNameUtil.maxCommonPackageName(libPackageName, existingLibPackageName);
        String maxCommonApkPackageName = PackageNameUtil.maxCommonPackageName(apkPackageName, existingApkPackageName);

        int libPackageNameDistance1 = PackageNameUtil.packageNameDistance(maxCommonLibPackageName, existingLibPackageName);
        int apkPackageNameDistance1 = PackageNameUtil.packageNameDistance(maxCommonApkPackageName, existingApkPackageName);
        if (libPackageNameDistance1 < 0 || apkPackageNameDistance1 < 0 || libPackageNameDistance1 != apkPackageNameDistance1)
            System.out.println("not match");

        int libPackageNameDistance2 = PackageNameUtil.packageNameDistance(maxCommonLibPackageName, libPackageName);
        int apkPackageNameDistance2 = PackageNameUtil.packageNameDistance(maxCommonApkPackageName, apkPackageName);
        if (libPackageNameDistance2 < 0 || apkPackageNameDistance2 < 0 || libPackageNameDistance2 != apkPackageNameDistance2)
            System.out.println("not match");
        System.out.println("match");
    }
}
