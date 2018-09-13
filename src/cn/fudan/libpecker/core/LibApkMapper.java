package cn.fudan.libpecker.core;

import cn.fudan.common.util.PackageNameUtil;
import cn.fudan.libpecker.model.ApkPackageProfile;
import cn.fudan.libpecker.model.LibPackageProfile;

import java.util.*;

/**
 * Created by yuanxzhang on 28/04/2017.
 */
public class LibApkMapper {
    private Set<String> libPackages;
    private Set<String> apkPackages;

    private Map<String, String> libPackageToApkPackageMap;
    private Map<String, String> libClassToApkClassMap;
    private Map<String, List<String>> libRootPackageMap;

    private LibApkMapper(){}
    public LibApkMapper(Map<String, LibPackageProfile> libPackageProfileMap, Map<String, ApkPackageProfile> apkPackageProfileMap, Map<String, List<String>> rootPackageMap) {
        libPackages = new HashSet<>();
        apkPackages = new HashSet<>();

        libPackageToApkPackageMap = new HashMap<>();
        libClassToApkClassMap = new HashMap<>();

        libPackages.addAll(libPackageProfileMap.keySet());
        apkPackages.addAll(apkPackageProfileMap.keySet());
        libRootPackageMap = rootPackageMap;
    }

    public Set<String> getLibPackages() {return libPackages;}
    public Map<String, String> getExistingPackageMap() {return libPackageToApkPackageMap;}
    public Map<String, String> getExistingClassMap() {return libClassToApkClassMap;}

    public boolean makePair(PackagePairCandidate libPackagePairCandidate, ApkPackageProfile apkPackageProfile) {
        String libPackageName = libPackagePairCandidate.libPackageProfile.packageName;
        String apkPackageName = apkPackageProfile == null ? null : apkPackageProfile.packageName;

        if (! libPackages.contains(libPackageName))
            return false;
        if (apkPackageName == null) {//always true if pair to null
            libPackages.remove(libPackageName);
            libPackageToApkPackageMap.put(libPackageName, null);
            for (String libClassName : libPackagePairCandidate.libPackageProfile.getClassList())
                libClassToApkClassMap.put(libClassName, null);
            return true;
        }
        else {
            if (! apkPackages.contains(apkPackageName))
                return false;
            else {
                if (libPackageName.equals(apkPackageName)) {//always true if same
                    libPackages.remove(libPackageName);
                    apkPackages.remove(apkPackageName);
                    libPackageToApkPackageMap.put(libPackageName, apkPackageName);
                    Map<String, String> classNameMap = libPackagePairCandidate.getClassNameMap(apkPackageProfile);
                    for (String libClassName : classNameMap.keySet())
                        libClassToApkClassMap.put(libClassName, classNameMap.get(libClassName));
                    for (String libClassName : libPackagePairCandidate.libPackageProfile.getClassList()) {
                        if (! libClassToApkClassMap.containsKey(libClassName))
                            libClassToApkClassMap.put(libClassName, null);
                    }

                    return true;
                }
                else {
                    for (String existingLibPackageName : libPackageToApkPackageMap.keySet()) {
                        if (libPackageToApkPackageMap.get(existingLibPackageName) == null)
                            continue;

                        int distance = 0;
                        if (PackageNameUtil.isSiblingPackageName(existingLibPackageName, libPackageName)) {
                            if (PackageNameUtil.isSiblingPackageName(libPackageToApkPackageMap.get(existingLibPackageName), apkPackageName))
                                ;
                            else
                                return false;
                        }
                        else if ((distance = PackageNameUtil.isParentPackageName(existingLibPackageName, libPackageName)) > 0) {
                            if (distance == PackageNameUtil.isParentPackageName(libPackageToApkPackageMap.get(existingLibPackageName), apkPackageName))
                                ;
                            else
                                return false;
                        }
                        else if ((distance = PackageNameUtil.isChildPackageName(existingLibPackageName, libPackageName)) > 0) {
                            if (distance == PackageNameUtil.isChildPackageName(libPackageToApkPackageMap.get(existingLibPackageName), apkPackageName))
                                ;
                            else
                                return false;
                        }
                        else if (PackageNameUtil.inSameRootPackage(libPackageName, existingLibPackageName, libRootPackageMap)) {
                            String maxCommonLibPackageName = PackageNameUtil.maxCommonPackageName(libPackageName, existingLibPackageName);
                            String maxCommonApkPackageName = PackageNameUtil.maxCommonPackageName(apkPackageName, libPackageToApkPackageMap.get(existingLibPackageName));

                            int libPackageNameDistance1 = PackageNameUtil.packageNameDistance(maxCommonLibPackageName, existingLibPackageName);
                            int apkPackageNameDistance1 = PackageNameUtil.packageNameDistance(maxCommonApkPackageName, libPackageToApkPackageMap.get(existingLibPackageName));
                            if (libPackageNameDistance1 < 0 || apkPackageNameDistance1 < 0 || libPackageNameDistance1 != apkPackageNameDistance1)
                                return false;

                            int libPackageNameDistance2 = PackageNameUtil.packageNameDistance(maxCommonLibPackageName, libPackageName);
                            int apkPackageNameDistance2 = PackageNameUtil.packageNameDistance(maxCommonApkPackageName, apkPackageName);
                            if (libPackageNameDistance2 < 0 || apkPackageNameDistance2 < 0 || libPackageNameDistance2 != apkPackageNameDistance2)
                                return false;
                        }
                        else
                            ;
                    }
                    libPackages.remove(libPackageName);
                    apkPackages.remove(apkPackageName);
                    libPackageToApkPackageMap.put(libPackageName, apkPackageName);
                    Map<String, String> classNameMap = libPackagePairCandidate.getClassNameMap(apkPackageProfile);
                    for (String libClassName : classNameMap.keySet())
                        libClassToApkClassMap.put(libClassName, classNameMap.get(libClassName));
                    for (String libClassName : libPackagePairCandidate.libPackageProfile.getClassList()) {
                        if (! libClassToApkClassMap.containsKey(libClassName))
                            libClassToApkClassMap.put(libClassName, null);
                    }
                    return true;
                }
            }
        }
    }

    public boolean finishPairing() {
        /*
         if all lib packages has been paired, then finish pairing
         note that null will be paired with lib package if not candidate has been found
         */
        return libPackages.isEmpty();
    }

    public double similarity(Map<String, PackagePairCandidate> libCandidatePackageProfileMap) {
        Map<String, Double> libPackageWeightMap = new HashMap<>();
        double totalWeight = 0;
        for (String libPackageName : libCandidatePackageProfileMap.keySet()) {
            libPackageWeightMap.put(libPackageName, libCandidatePackageProfileMap.get(libPackageName).libPackageProfile.getPackageWeight());
            totalWeight += libPackageWeightMap.get(libPackageName);
        }

        double matchedWeight = 0;
        for (String libPackageName : libPackageToApkPackageMap.keySet()) {
            String apkPackageName = libPackageToApkPackageMap.get(libPackageName);

            if (apkPackageName == null)
                continue;

            matchedWeight += libPackageWeightMap.get(libPackageName) * libCandidatePackageProfileMap.get(libPackageName).getApkPackageSimilarity(apkPackageName);
        }

        return matchedWeight / totalWeight;
    }

    public double similarityUpperBound(Map<String, PackagePairCandidate> libCandidatePackageProfileMap) {
        Map<String, Double> libPackageWeightMap = new HashMap<>();
        double totalWeight = 0;
        for (String libPackageName : libCandidatePackageProfileMap.keySet()) {
            libPackageWeightMap.put(libPackageName, libCandidatePackageProfileMap.get(libPackageName).libPackageProfile.getPackageWeight());
            totalWeight += libPackageWeightMap.get(libPackageName);
        }

        double matchedWeight = 0;
        for (PackagePairCandidate libPackagePairCandidate : libCandidatePackageProfileMap.values()) {

            matchedWeight += libPackageWeightMap.get(libPackagePairCandidate.libPackageName) * libPackagePairCandidate.getMaxSimilarity();
        }

        return matchedWeight / totalWeight;
    }

    public LibApkMapper deepClone() {
        LibApkMapper newPartition = new LibApkMapper();
        newPartition.libPackages = new HashSet<>();
        newPartition.apkPackages = new HashSet<>();
        newPartition.libPackageToApkPackageMap = new HashMap<>();
        newPartition.libClassToApkClassMap = new HashMap<>();
        newPartition.libRootPackageMap = this.libRootPackageMap;

        newPartition.libPackages.addAll(this.libPackages);
        newPartition.apkPackages.addAll(this.apkPackages);
        for (String libPackageName : this.libPackageToApkPackageMap.keySet()) {
            newPartition.libPackageToApkPackageMap.put(libPackageName, this.libPackageToApkPackageMap.get(libPackageName));
        }
        for (String libClassName : this.libClassToApkClassMap.keySet()) {
            newPartition.libClassToApkClassMap.put(libClassName, this.libClassToApkClassMap.get(libClassName));
        }

        return newPartition;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (String libPkgName : libPackageToApkPackageMap.keySet()) {
            output.append("\t<"+libPkgName+"> :" + libPackageToApkPackageMap.get(libPkgName)+"\n");
        }
        for (String libPkgName : libPackages) {
            if (! libPackageToApkPackageMap.containsKey(libPkgName))
                output.append("\t<"+libPkgName+"> : null\n");
        }

        return output.toString();
    }
}
