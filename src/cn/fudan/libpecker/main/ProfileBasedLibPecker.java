package cn.fudan.libpecker.main;

import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.common.Apk;
import cn.fudan.common.LibPeckerConfig;
import cn.fudan.common.Lib;
import cn.fudan.common.Sdk;
import cn.fudan.common.util.PackageNameUtil;
import cn.fudan.libpecker.core.LibApkMapper;
import cn.fudan.libpecker.core.PackageMapEnumerator;
import cn.fudan.libpecker.core.PackagePairCandidate;
import cn.fudan.libpecker.model.*;

import java.util.*;


/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class ProfileBasedLibPecker {

    Set<String> targetSdkClassNameSet;
    LibProfile libProfile;
    public Map<String, ApkPackageProfile> apkPackageProfileMap;//pkg name -> ApkPackageProfile
    public Map<String, LibPackageProfile> libPackageProfileMap;//pkg name -> LibPackageProfile

    public ProfileBasedLibPecker(LibProfile libProfile, ApkProfile apkProfile, Set<String> targetSdkClassNameSet) {
        this.targetSdkClassNameSet = targetSdkClassNameSet;
        this.libProfile = libProfile;
        this.apkPackageProfileMap = apkProfile.packageProfileMap;
        this.libPackageProfileMap = this.libProfile.packageProfileMap;
    }

    public double calculateMaxProbability() {
        /*
        Step 0: fail-fast to check the basic sig of library classes
        * */
        List<String> libClassBasicSigList = new ArrayList<>();
        List<String> apkClassBasicSigList = new ArrayList<>();
        for (ApkPackageProfile apkPackageProfile : apkPackageProfileMap.values()) {
            for (SimpleClassProfile simpleClassProfile : apkPackageProfile.classProfileMap.values())
                apkClassBasicSigList.add(simpleClassProfile.getBasicHashStrict());
        }
        for (LibPackageProfile libPackageProfile : libPackageProfileMap.values()) {
            for (SimpleClassProfile simpleClassProfile : libPackageProfile.classProfileMap.values())
                libClassBasicSigList.add(simpleClassProfile.getBasicHashStrict());
        }
        int matchLibClassBasicHashSize = 0;
        for (String basicClassHash : libClassBasicSigList) {
            if (apkClassBasicSigList.contains(basicClassHash)) {
                matchLibClassBasicHashSize ++;
                apkClassBasicSigList.remove(basicClassHash);
            }
        }
        double classBasicHashRatioUpperBound = 1.0*matchLibClassBasicHashSize/libClassBasicSigList.size();
        if (classBasicHashRatioUpperBound < LibPeckerConfig.LIB_APK_PAIR_THRESHOLD) {
            if (LibPeckerConfig.DEBUG_LIBPECKER) {
                System.out.println("classBasicHashRatio not exceed threshold: " + classBasicHashRatioUpperBound);
            }
            return classBasicHashRatioUpperBound;
        }

        /*
        Step 1: candidate package calculation
        for each lib package, find all apk packages that has at least 50% matched class hashes
        */
        Map<String, PackagePairCandidate> libPackagePairCandidateMap = new HashMap<>();//pkg name -> PackagePairCandidate
        for (LibPackageProfile libPkg : libPackageProfileMap.values()) {
            PackagePairCandidate candidatePackages = new PackagePairCandidate(libPkg, apkPackageProfileMap.values());

            libPackagePairCandidateMap.put(libPkg.packageName, candidatePackages);
            if (LibPeckerConfig.DEBUG_LIBPECKER) {
                System.out.println(libPkg.packageName + ", weight: " + libPkg.getPackageWeight());
                for (ApkPackageProfile apkPackageProfile : candidatePackages.getCandiApkPackages()) {
                    System.out.println("\t"+apkPackageProfile.packageName+", " + candidatePackages.getApkPackageSimilarity(apkPackageProfile));
                }
            }
        }

        /*
        Step 2: link package
        use some rules to filter out some candidates,
        a) If a library package lp1 with pack- age name com.foo has app package candidates
        starting with the same package name, we can remove candidates with different root packages.
        b) If lp1 matches ap1 with package name a.b.c we deduce that a.b is one potential
        library root package within the app. By applying this to all pairs <lpi,apj> we receive
        a list of potential root packages.
        */
        LibApkMapper mapper = new LibApkMapper(libPackageProfileMap, apkPackageProfileMap, libProfile.rootPackageMap);
        //apply rule 1
        for (PackagePairCandidate packageCandidate : new ArrayList<>(libPackagePairCandidateMap.values())) {
            ApkPackageProfile perfectMatch = packageCandidate.perfectMatch();
            if (perfectMatch != null) {
                boolean paired = mapper.makePair(packageCandidate, perfectMatch);
                if (! paired) {
                    throw new RuntimeException("can not be true");
                }
                else {
                    packageCandidate.justKeepPerfectMatch();
                }
            }
        }
        //rule 2, for those perfect match packages, we extract their root packages,
        // and use these root packages to filter candidate apk package in other lib packages
        // e.g com.facebook.network is perfect match in apk, then other lib packages with com.facebook as root packages
        //  should only have apk package candidates start with com.facebook
        for (String libPackageName : mapper.getExistingPackageMap().keySet()) {
            if (libPackageName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
                continue;

            String parentPackageName = PackageNameUtil.getParentPackageName(libPackageName);
            String rootPackageName = libProfile.getRootPackage(libPackageName);
            while (parentPackageName.length() >= rootPackageName.length()) {
                for (PackagePairCandidate packageCandidate : libPackagePairCandidateMap.values()) {
                    packageCandidate.filterRootPackageName(parentPackageName);
                }

                parentPackageName = PackageNameUtil.getParentPackageName(parentPackageName);
            }
        }

        /*
        Step 2.9 optimization
        all enumeration would be quite slow, we can calculate the upper bound
        */
        double similarityUpperBound = mapper.similarityUpperBound(libPackagePairCandidateMap);
        if (similarityUpperBound < LibPeckerConfig.LIB_APK_PAIR_THRESHOLD) {
            if (LibPeckerConfig.DEBUG_LIBPECKER) {
                System.out.println("similarityUpperBound not exceed threshold: " + similarityUpperBound);
            }
            return similarityUpperBound;
        }

        /*
        Step 3: partition package
        exhaustiveEnumerate all candidate partitions
        */
        PackageMapEnumerator packageMapEnumerator = new PackageMapEnumerator(libPackagePairCandidateMap, mapper);
        List<LibApkMapper> allPartitions = packageMapEnumerator.exhaustiveEnumerate();
        if (LibPeckerConfig.DEBUG_LIBPECKER) {
            System.out.println();
            System.out.println(packageMapEnumerator);
        }


        /*
        Step 4: maximum total similarity
        */
        double maxSimilarity = 0;
        for (LibApkMapper partition : allPartitions) {
            double similarity = partition.similarity(libPackagePairCandidateMap);
            if (LibPeckerConfig.DEBUG_LIBPECKER) {
                System.out.println("similarity: " + similarity);
                System.out.println(partition);
            }
            if (maxSimilarity < similarity) {
                maxSimilarity = similarity;
                maxPartition = partition;
            }
        }
        if (LibPeckerConfig.DEBUG_LIBPECKER) {
            System.out.println(maxPartition);
        }

        return maxSimilarity;
    }

    private LibApkMapper maxPartition = null;
    public LibApkMapper getMaxPartition(){
        if (maxPartition == null) {
            calculateMaxProbability();
        }
        return maxPartition;
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(0);
    }

    public static double singleMain(String apkPath, String libPath) {
        Apk apk = Apk.loadFromFile(apkPath);
        if (apk == null) {
            fail("apk not parsed");
        }
        Lib lib = Lib.loadFromFile(libPath);
        if (lib == null) {
            fail("lib not parsed");
        }
        Sdk sdk = Sdk.loadDefaultSdk();
        if (sdk == null) {
            fail("default sdk not parsed");
        }
        Set<String> targetSdkClassNameSet = sdk.getTargetSdkClassNameSet();
        LibProfile libProfile = LibProfile.create(lib, targetSdkClassNameSet);

        ApkProfile apkProfile = ApkProfile.create(apk, targetSdkClassNameSet);

        ProfileBasedLibPecker pecker = new ProfileBasedLibPecker(libProfile, apkProfile, targetSdkClassNameSet);
        double similarity = pecker.calculateMaxProbability();

        return similarity;
    }

    public static void main(String[] args) {
        String apkPath = null;
        String libPath = null;

        if (args == null || args.length == 2) {
            apkPath = args[0];
            libPath = args[1];
        }
        else {
            fail("Usage: java -cp LibPecker.jar cn.fudan.libpecker.mainProfileBasedLibPecker <apk_path> <lib_path>");
        }

        long current = System.currentTimeMillis();
        double similarity = singleMain(apkPath, libPath);

        System.out.println("similarity: " + similarity);
        System.out.println("time: " + (System.currentTimeMillis() - current));
    }

}
