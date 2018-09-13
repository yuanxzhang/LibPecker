package cn.fudan.libpecker.model;

import cn.fudan.analysis.dep.DepAnalysis;
import cn.fudan.analysis.profile.ClassProfile;
import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.Lib;
import cn.fudan.libpecker.analysis.ClassWeightAnalysis;
import cn.fudan.libpecker.analysis.RootPackageAnalysis;

import java.util.*;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class LibProfile {

    public Map<String, LibPackageProfile> packageProfileMap;//pkg name -> [LibPackageProfile, ...]
    public Map<String, List<String>> rootPackageMap;//root package -> [package names, ...]

    private LibProfile(){}

    public String getRootPackage(String packageName) {
        for (String rootPackageName : rootPackageMap.keySet()) {
            for (String pkg : rootPackageMap.get(rootPackageName)) {
                if (pkg.equals(packageName))
                    return rootPackageName;
            }
        }
        return null;
    }

    public List<String> getPackagesWithSameRoot(String packageName) {
        for (String rootPackageName : rootPackageMap.keySet()) {
            for (String pkg : rootPackageMap.get(rootPackageName)) {
                if (pkg.equals(packageName))
                    return rootPackageMap.get(rootPackageName);
            }
        }
        return null;
    }

    public static LibProfile create(Lib lib, Set<String> targetSdkClassNameSet) {
        LibProfile libProfile = new LibProfile();
        libProfile.rootPackageMap = RootPackageAnalysis.extractRootPackages(lib);

        Map<String, Integer> classBBWeightMap = ClassWeightAnalysis.getClassBBWeight(lib);
        DepAnalysis depAnalysis = new DepAnalysis(lib, (HashSet)targetSdkClassNameSet);
        Map<String, Integer> classDepWeightMap = ClassWeightAnalysis.getClassDepWeight(depAnalysis);
        Map<String, Set<SimpleClassProfile>> packageProfileMap = getClassProfilesGroupedByPackage(depAnalysis.allClassProfiles);

        libProfile.packageProfileMap = new HashMap<>();
        for (String packageName : packageProfileMap.keySet()) {
            libProfile.packageProfileMap.put(packageName, new LibPackageProfile(packageName, classBBWeightMap, classDepWeightMap, packageProfileMap.get(packageName)));
        }

        classBBWeightMap.clear();
        classDepWeightMap.clear();
        packageProfileMap.clear();
        return libProfile;
    }

    private static Map<String, Set<SimpleClassProfile>> getClassProfilesGroupedByPackage(Set<ClassProfile> classProfileSet) {
        Map<String, Set<SimpleClassProfile>> packageClassProfileMap = new HashMap<>();
        for (ClassProfile classProfile : classProfileSet) {
            String packageName = DexHelper.getPackageName(classProfile.getClassName());
            if (! packageClassProfileMap.containsKey(packageName))
                packageClassProfileMap.put(packageName, new HashSet<SimpleClassProfile>());
            packageClassProfileMap.get(packageName).add(classProfile);
        }

        return packageClassProfileMap;
    }

}
