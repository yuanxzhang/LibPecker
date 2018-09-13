package cn.fudan.libpecker.model;

import cn.fudan.analysis.profile.ClassProfile;
import cn.fudan.analysis.profile.ProfileGenerator;
import cn.fudan.analysis.tree.PackageNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class ApkPackageProfile implements Serializable {
    static final long serialVersionUID = 179792789887672412L;

    public String packageName;

    public Map<String, Integer> classBBWeightMap;
    public Map<String, Integer> classDepWeightMap;

    public Map<String, SimpleClassProfile> classProfileMap = new HashMap<>();

    public int includeClassNum;
    public int subPackagesNum;

    protected void finalize(){
        packageName = null;
        classProfileMap.clear();
        classBBWeightMap.clear();
        classDepWeightMap.clear();
    }

    public ApkPackageProfile(PackageNode packageNode, Set<String> targetSdkClassNameSet) {
        this.packageName = packageNode.getPackageName();

        Set<ClassProfile> classProfiles = ProfileGenerator.generate(packageNode, targetSdkClassNameSet);
        for (ClassProfile classProfile : classProfiles) {
            classProfileMap.put(classProfile.getClassName(), classProfile);
        }

        this.subPackagesNum = packageNode.getSubPackagesNum();
        this.includeClassNum = this.classProfileMap.size();
    }

    public ApkPackageProfile(PackageNode packageNode, Map<String, Integer> classBBWeightMap, Map<String, Integer> classDepWeightMap, Set<String> targetSdkClassNameSet) {
        this.packageName = packageNode.getPackageName();

        this.subPackagesNum = packageNode.getSubPackagesNum();
        this.includeClassNum = packageNode.getIncludedClassNum();
        this.classBBWeightMap = new HashMap<>();
        this.classDepWeightMap = new HashMap<>();

        Set<ClassProfile> classProfiles = ProfileGenerator.generate(packageNode, targetSdkClassNameSet);
        for (ClassProfile classProfile : classProfiles) {
            this.classProfileMap.put(classProfile.getClassName(), classProfile);
            this.classBBWeightMap.put(classProfile.getClassName(), classBBWeightMap.get(classProfile.getClassName()));
            this.classDepWeightMap.put(classProfile.getClassName(), classDepWeightMap.get(classProfile.getClassName()));
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(packageName);

        Map<String, SimpleClassProfile> cachedClassProfileMap = new HashMap<>();
        for (String className : classProfileMap.keySet()) {
            if (classProfileMap.get(className) instanceof CachedClassProfile)
                cachedClassProfileMap.put(className, classProfileMap.get(className));
            else
                cachedClassProfileMap.put(className, CachedClassProfile.create((ClassProfile)classProfileMap.get(className)));
        }
        out.writeObject(cachedClassProfileMap);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.packageName = (String)in.readObject();
        this.classProfileMap = (Map<String, SimpleClassProfile>)in.readObject();
    }
}
