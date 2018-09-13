package cn.fudan.libpecker.core;

import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.common.LibPeckerConfig;
import cn.fudan.common.util.PackageNameUtil;
import cn.fudan.libpecker.model.ApkPackageProfile;
import cn.fudan.libpecker.model.LibPackageProfile;

import java.util.*;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class PackagePairCandidate {
    public static final double PACKAGE_CANDIDATE_FILTER = 0.4;

    public String libPackageName;
    public LibPackageProfile libPackageProfile;

    private List<ApkPackageProfile> apkPackageProfiles;
    private List<Double> candiApkPackageSimilarity;
    private Map<ApkPackageProfile, Map<String, String>> apkPackageClassNameMap;//for each apk package, records the lib class name map to apk class name

    public PackagePairCandidate(LibPackageProfile libPackage, Collection<ApkPackageProfile> apkPackages) {
        this.libPackageProfile = libPackage;
        this.libPackageName = libPackage.packageName;

        apkPackageProfiles = new ArrayList<>();
        candiApkPackageSimilarity = new ArrayList<>();
        apkPackageClassNameMap = new HashMap<>();

        for (ApkPackageProfile apkPackage : apkPackages) {
            matchClassHashPercent(apkPackage);
        }
    }

    private void matchClassHashPercent(ApkPackageProfile apkPackageProfile) {
        Map<String, String> classNameMap = new HashMap<>();
        Map<String, Double> classRawSimilarity = new HashMap<>();

        double weightedSimilarity = ProfileComparator.rawPackageSimilarity(libPackageProfile, apkPackageProfile,
                classNameMap, classRawSimilarity);

        if (LibPeckerConfig.DEBUG_LIBPECKER) {
            System.out.println("[pkg-level] " + libPackageName + " : " + apkPackageProfile.packageName + " : " + weightedSimilarity);
        }

        if (weightedSimilarity >= PACKAGE_CANDIDATE_FILTER) {
            apkPackageProfiles.add(apkPackageProfile);
            candiApkPackageSimilarity.add(weightedSimilarity);
            apkPackageClassNameMap.put(apkPackageProfile, classNameMap);
        }
        if (libPackageName.equals(apkPackageProfile.packageName)) {
            apkPackageProfiles.add(apkPackageProfile);
            candiApkPackageSimilarity.add(weightedSimilarity);
            apkPackageClassNameMap.put(apkPackageProfile, classNameMap);
        }
    }

    public List<ApkPackageProfile> getCandiApkPackages(){return apkPackageProfiles;}

    public double getApkPackageSimilarity(ApkPackageProfile apkPackageProfile) {
        if (apkPackageProfiles.contains(apkPackageProfile))
            return candiApkPackageSimilarity.get(apkPackageProfiles.indexOf(apkPackageProfile));
        else
            return 0.0;
    }

    public double getApkPackageSimilarity(String apkPackageName) {
        for (ApkPackageProfile apkPackageProfile : apkPackageProfiles) {
            if (apkPackageProfile.packageName.equals(apkPackageName))
                return candiApkPackageSimilarity.get(apkPackageProfiles.indexOf(apkPackageProfile));
        }
        return 0.0;
    }

    public Map<String, String> getClassNameMap(ApkPackageProfile apkPackageProfile) {
        return apkPackageClassNameMap.get(apkPackageProfile);
    }

    public double getMaxSimilarity() {
        double maxSimilarity = 0;
        for (Double similarity : candiApkPackageSimilarity) {
            if (maxSimilarity < similarity)
                maxSimilarity = similarity;
        }
        return maxSimilarity;
    }

    public ApkPackageProfile getMaxSimilarityCandiPackage() {
        ApkPackageProfile maxSimilarityPackage = null;
        double maxSimilarity = 0;
        for (int i = 0; i < apkPackageProfiles.size(); i ++) {
            double similarity = candiApkPackageSimilarity.get(i);
            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                maxSimilarityPackage = apkPackageProfiles.get(i);
            }
        }
        return maxSimilarityPackage;
    }

    public ApkPackageProfile perfectMatch() {
        if (this.libPackageName.equals(PackageNode.Factory.DEFAULT_PACKAGE)) //default package does not perfectly map to any package
            return null;

        for (ApkPackageProfile profile : apkPackageProfiles) {
            if (profile.packageName.equals(this.libPackageName))
                return profile;
        }
        return null;
    }

    public void justKeepPerfectMatch() {
        ApkPackageProfile perfectMatch = perfectMatch();
        apkPackageProfiles.clear();
        apkPackageProfiles.add(perfectMatch);
        candiApkPackageSimilarity.clear();
        matchClassHashPercent(perfectMatch);
    }

    public void filterRootPackageName(String rootPackage) {
        int libDistance = PackageNameUtil.isParentPackageName(rootPackage, this.libPackageName);
        if (libDistance > 0) {
            List<ApkPackageProfile> tempCandiApkPackages = new ArrayList<>();
            List<Double> tempCandiApkPackageSimilarity = new ArrayList<>();
            for (ApkPackageProfile apkPackage : apkPackageProfiles) {
                if (PackageNameUtil.isParentPackageName(rootPackage, apkPackage.packageName) == libDistance) {
                    tempCandiApkPackages.add(apkPackage);
                    tempCandiApkPackageSimilarity.add(candiApkPackageSimilarity.get(apkPackageProfiles.indexOf(apkPackage)));
                }
            }

            for (ApkPackageProfile apkPackage : apkPackageProfiles) {
                if (! tempCandiApkPackages.contains(apkPackage))
                    candiApkPackageSimilarity.remove(apkPackage);

            }
            apkPackageProfiles = tempCandiApkPackages;
            candiApkPackageSimilarity = tempCandiApkPackageSimilarity;
        }
    }
}
