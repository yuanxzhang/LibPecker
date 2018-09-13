package cn.fudan.libpecker.core;

import cn.fudan.common.LibPeckerConfig;
import cn.fudan.libpecker.model.ApkPackageProfile;
import cn.fudan.libpecker.model.LibPackageProfile;
import cn.fudan.libpecker.model.SimpleClassProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class ProfileComparator {

    private static double getClassMatchSimilarityThreshold(SimpleClassProfile libClassProfile) {
        int memberCount = 1 + libClassProfile.getMethodHashList().size() + libClassProfile.getFieldHashList().size();
        if (memberCount <= 5)
            return 1.0;
        if (memberCount <= 10)
            return 0.9;
        if (memberCount <= 15)
            return 0.8;
        if (memberCount <= 20)
            return 0.7;
        if (memberCount <= 25)
            return 0.6;
        else
            return 0.5;
    }

    public static double rawPackageSimilarity(LibPackageProfile libPackageProfile, ApkPackageProfile apkPackageProfile,
                                              /*as return value*/Map<String, String> classNameMap, /*as return value*/Map<String, Double> classRawSimilarity) {
        List<String> weightedClassList = libPackageProfile.getWeightClassList();
        for (String libClassName : weightedClassList) {
            SimpleClassProfile simpleLibClassProfile = libPackageProfile.classProfileMap.get(libClassName);
            double RAW_CLASS_SIMILARITY_THRESHOLD = getClassMatchSimilarityThreshold(simpleLibClassProfile);

            SimpleClassProfile bestMatchApkClassProfile = null;
            double maxSimilarity = 0;

            for (SimpleClassProfile apkClassProfile : apkPackageProfile.classProfileMap.values()) {
                if (classNameMap.values().contains(apkClassProfile.getClassName()))
                    continue;

                double similarity = ProfileComparator.rawClassSimilarity(simpleLibClassProfile, apkClassProfile);
                if (similarity >= RAW_CLASS_SIMILARITY_THRESHOLD) {
                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                        bestMatchApkClassProfile = apkClassProfile;
                    }
                }
            }

            if (bestMatchApkClassProfile != null) {
                classNameMap.put(libClassName, bestMatchApkClassProfile.getClassName());
                classRawSimilarity.put(libClassName, maxSimilarity);
            }
            else {
                classNameMap.put(libClassName, null);
                classRawSimilarity.put(libClassName, 0.0);
            }
        }

        double weightedSimilarity = 0.0;
        for (String libClassName : weightedClassList) {
            if (LibPeckerConfig.DEBUG_LIBPECKER) {
                if (libPackageProfile.packageName.equals(LibPeckerConfig.DEBUG_LIBPECKER_LIB_PKG_NAME)
                        && apkPackageProfile.packageName.equals(LibPeckerConfig.DEBUG_LIBPECKER_APK_PKG_NAME)) {
                    System.out.println("\t class name: "+libClassName);
                    System.out.println("\t\t class weight: "+libPackageProfile.getClassWeight(libClassName));
                    System.out.println("\t\t class similarity: "+classRawSimilarity.get(libClassName));
                    System.out.println("\t\t class match: "+classNameMap.get(libClassName));
                }
            }
            weightedSimilarity += classRawSimilarity.get(libClassName)*libPackageProfile.getClassWeight(libClassName);
        }

        return weightedSimilarity;
    }

    public static double rawClassSimilarity(SimpleClassProfile libClassProfile, SimpleClassProfile apkClassProfile) {
        String apkClassHash = apkClassProfile.getClassHash();
        if (libClassProfile.getClassHash().equals(apkClassHash))
            return 1;
        if (libClassProfile.getClassHashStrict().equals(apkClassProfile.getClassHashStrict()))
            return 1;

        if (! libClassProfile.getBasicHash().equals(apkClassProfile.getBasicHash())
                && ! libClassProfile.getBasicHashStrict().equals(apkClassProfile.getBasicHashStrict()))
            return 0;
        else {
            List<String> apkMethodHashList = new ArrayList<>(apkClassProfile.getMethodHashList());
            List<String> apkFieldHashList = new ArrayList<>(apkClassProfile.getFieldHashList());

            double rate = 1.0*(libClassProfile.getMethodHashList().size()+libClassProfile.getFieldHashList().size())/(apkMethodHashList.size()+apkFieldHashList.size());
            if (rate > 1.0)
                rate = 1.0;

            int sameCounter = 1;
            for (int i = 0; i < libClassProfile.getMethodHashList().size(); i ++) {
                if (apkMethodHashList.contains(libClassProfile.getMethodHashList().get(i))) {
                    sameCounter ++;

                    apkMethodHashList.remove(libClassProfile.getMethodHashList().get(i));
                    continue;
                }
                if (apkMethodHashList.contains(libClassProfile.getMethodHashStrictList().get(i))) {
                    sameCounter ++;

                    apkMethodHashList.remove(libClassProfile.getMethodHashStrictList().get(i));
                    continue;
                }
            }

            for (int i = 0; i < libClassProfile.getFieldHashList().size(); i ++) {
                if (apkFieldHashList.contains(libClassProfile.getFieldHashList().get(i))) {
                    sameCounter ++;

                    apkFieldHashList.remove(libClassProfile.getFieldHashList().get(i));
                    continue;
                }
                if (apkFieldHashList.contains(libClassProfile.getFieldHashStrictList().get(i))) {
                    sameCounter ++;

                    apkFieldHashList.remove(libClassProfile.getFieldHashStrictList().get(i));
                    continue;
                }
            }

            double similarity = rate*sameCounter/(1+libClassProfile.getFieldHashList().size()+libClassProfile.getMethodHashList().size());

            return similarity;
        }
    }
}
