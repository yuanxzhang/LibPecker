package cn.fudan.libpecker.model;

import cn.fudan.analysis.profile.ClassProfile;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class LibPackageProfile implements Serializable {
    static final long serialVersionUID = 179792987844872412L;

    public String packageName;
    public Map<String, Integer> classBBWeightMap;
    public Map<String, Integer> classDepWeightMap;
    public Map<String, SimpleClassProfile> classProfileMap;

    public int includeClassNum;

    public LibPackageProfile(String packageName, Map<String, Integer> classBBWeightMap, Map<String, Integer> classDepWeightMap, Set<SimpleClassProfile> simpleLibClassProfiles) {
        this.packageName = packageName;
        this.classBBWeightMap = new HashMap<>();
        this.classDepWeightMap = new HashMap<>();
        this.classProfileMap = new HashMap<>();


        for (SimpleClassProfile classProfile : simpleLibClassProfiles) {
            this.classBBWeightMap.put(classProfile.getClassName(), classBBWeightMap.get(classProfile.getClassName()));
            this.classDepWeightMap.put(classProfile.getClassName(), classDepWeightMap.get(classProfile.getClassName()));
            this.classProfileMap.put(classProfile.getClassName(), classProfile);
        }

        this.includeClassNum = classProfileMap.size();
    }

    private Map<String, Double> classWeights = null;
    private double packageWeight = 0;
    private synchronized void constructClassWeights() {
        Map<String, Double> classRanks = new HashMap<>();
        for (String className : classBBWeightMap.keySet()) {
            double bbWeight = classBBWeightMap.get(className);
            double depWeight = classDepWeightMap.get(className);

            depWeight +=1;

            classRanks.put(className, bbWeight+depWeight);
        }

        for (double weight : classRanks.values())
            packageWeight += weight;

        classWeights = new HashMap<>();
        for (String className : classRanks.keySet())
            classWeights.put(className, classRanks.get(className)/packageWeight);
    }

    public double getPackageWeight() {
        if (classWeights == null)
            constructClassWeights();
        return packageWeight;
    }

    public double getClassWeight(String className) {
        if (classWeights == null)
            constructClassWeights();
        return classWeights.get(className);
    }

    public Set<String> getClassList() {
        return classWeights.keySet();
    }

    public List<String> getWeightClassList() {
        if (classWeights == null)
            constructClassWeights();

        List<String> classNames = new ArrayList<>(classWeights.keySet());
        Collections.sort(classNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (classWeights.get(o1).equals(classWeights.get(o2)))
                    return 0;
                return (classWeights.get(o1) < classWeights.get(o2)) ? 1 : -1;
            }
        });

        return classNames;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(packageName);
        out.writeObject(classBBWeightMap);
        out.writeObject(classDepWeightMap);

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
        this.classBBWeightMap = (Map<String, Integer>)in.readObject();
        this.classDepWeightMap = (Map<String, Integer>)in.readObject();
        this.classProfileMap = (Map<String, SimpleClassProfile>)in.readObject();
    }
}
