package cn.fudan.libpecker.core;

import cn.fudan.libpecker.model.ApkPackageProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yuanxzhang on 28/04/2017.
 */
public class PackageMapEnumerator {
    Map<String, PackagePairCandidate> libCandidatePackageProfileMap;//lib pkg name -> all candidate packages in apk
    LibApkMapper seedPartition;

    public PackageMapEnumerator(Map<String, PackagePairCandidate> libCandidatePackageProfileMap, LibApkMapper seedPartition) {
        this.libCandidatePackageProfileMap = libCandidatePackageProfileMap;
        this.seedPartition = seedPartition;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("existing mapping: \n");
        for (String libPkgName : seedPartition.getExistingPackageMap().keySet()) {
            output.append("\t<"+libPkgName+"> : " + seedPartition.getExistingPackageMap().get(libPkgName)+"\n");
        }
        output.append("exhaustiveEnumerate space: \n");
        for (String libPkgName : libCandidatePackageProfileMap.keySet()) {
            output.append("\t<"+libPkgName+"> : " + libCandidatePackageProfileMap.get(libPkgName).getCandiApkPackages().size()+"\n");
        }

        return output.toString();
    }

    public List<LibApkMapper> exhaustiveEnumerate() {
        List<LibApkMapper> partitions = new ArrayList<>();
        partitions.add(seedPartition);

        //if we have X lib packages to pair, we need X steps to finish
        int maxEnumerateStep = seedPartition.getLibPackages().size();
        while (maxEnumerateStep > 0) {
            List<LibApkMapper> newPartitions = new ArrayList<>();
            for (LibApkMapper partition : partitions) {
                if (partition.finishPairing())
                    newPartitions.add(partition);
                else {
                    List<LibApkMapper> pairNextList = enumerateAllPartitions(partition);
                    if (pairNextList != null && pairNextList.size() > 0) {
                        for (LibApkMapper nextPartition : pairNextList) {
                            if (! newPartitions.contains(nextPartition))
                                newPartitions.add(nextPartition);
                        }
                    }
                }
            }

            partitions = newPartitions;
            maxEnumerateStep--;
        }

        return partitions;
    }

    private List<LibApkMapper> enumerateAllPartitions(LibApkMapper partition) {
        if (partition.finishPairing())
            return null;//all finished pairing, abort
        else {
            List<LibApkMapper> nextLayerPartitions = new ArrayList<>();
            String libPackageName = new ArrayList<>(partition.getLibPackages()).get(0);
            PackagePairCandidate packageCandidate = libCandidatePackageProfileMap.get(libPackageName);
            if (packageCandidate.getCandiApkPackages().size() == 0) {
                boolean paired = partition.makePair(packageCandidate, null);
                if (paired)
                    nextLayerPartitions.add(partition);
                else {
                    throw new RuntimeException("can not be true");
                }
            }
            else {
                for (ApkPackageProfile apkPackageCandidate : packageCandidate.getCandiApkPackages()) {
                    LibApkMapper newPartition = partition.deepClone();
                    boolean paired = newPartition.makePair(packageCandidate, apkPackageCandidate);
                    if (paired)//if apkPackage has been assigned to another lib package, pairing is failed
                        nextLayerPartitions.add(newPartition);
                    else {
                        paired = newPartition.makePair(packageCandidate, null);
                        if (paired)
                            nextLayerPartitions.add(newPartition);
                        else {
                            throw new RuntimeException("can not be true");
                        }
                    }
                }
            }

            return nextLayerPartitions;
        }
    }
}
