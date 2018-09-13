package cn.fudan.libpecker.model;

import cn.fudan.analysis.profile.ClassProfile;
import cn.fudan.analysis.profile.FieldProfile;
import cn.fudan.analysis.profile.MethodProfile;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuanxzhang on 27/04/2017.
 */
public class CachedClassProfile implements Serializable, SimpleClassProfile {
    static final long serialVersionUID = 197792367124872413L;

    private String className;
    private String classHash;
    private String classHashStrict;

    private String basicHash;
    private String basicHashStrict;
    private List<String> methodHashList;
    private List<String> fieldHashList;
    private List<String> methodHashStrictList;
    private List<String> fieldHashStrictList;

    private CachedClassProfile() {}

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(className);
        out.writeObject(classHash);
        out.writeObject(classHashStrict);
        out.writeObject(basicHash);
        out.writeObject(basicHashStrict);
        out.writeObject(methodHashList);
        out.writeObject(fieldHashList);
        out.writeObject(methodHashStrictList);
        out.writeObject(fieldHashStrictList);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.className = (String) in.readObject();
        this.classHash = (String) in.readObject();
        this.classHashStrict = (String) in.readObject();
        this.basicHash = (String) in.readObject();
        this.basicHashStrict = (String) in.readObject();
        this.methodHashList = (List<String>) in.readObject();
        this.fieldHashList = (List<String>) in.readObject();
        this.methodHashStrictList = (List<String>) in.readObject();
        this.fieldHashStrictList = (List<String>) in.readObject();
    }

    public static CachedClassProfile create(ClassProfile classProfile) {
        CachedClassProfile cachedClassProfile = new CachedClassProfile();
        cachedClassProfile.className = classProfile.getClassName();
        cachedClassProfile.classHash = classProfile.getClassHash();
        cachedClassProfile.classHashStrict = classProfile.getClassHashStrict();
        cachedClassProfile.basicHash = classProfile.getBasicHash();
        cachedClassProfile.basicHashStrict = classProfile.getBasicHashStrict();

        cachedClassProfile.methodHashList = new ArrayList<>();
        cachedClassProfile.methodHashStrictList = new ArrayList<>();
        for (MethodProfile methodProfile : classProfile.staticMethodProfiles) {
            cachedClassProfile.methodHashList.add(ClassProfile.getMethodHash(methodProfile));
            cachedClassProfile.methodHashStrictList.add(ClassProfile.getMethodHashStrict(methodProfile));
        }
        for (MethodProfile methodProfile : classProfile.instanceMethodProfiles) {
            cachedClassProfile.methodHashList.add(ClassProfile.getMethodHash(methodProfile));
            cachedClassProfile.methodHashStrictList.add(ClassProfile.getMethodHashStrict(methodProfile));
        }

        cachedClassProfile.fieldHashList = new ArrayList<>();
        cachedClassProfile.fieldHashStrictList = new ArrayList<>();
        for (FieldProfile fieldProfile : classProfile.staticFieldProfiles) {
            if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                continue;

            cachedClassProfile.fieldHashList.add(ClassProfile.getFieldHash(fieldProfile));
            cachedClassProfile.fieldHashStrictList.add(ClassProfile.getFieldHashStrict(fieldProfile));
        }
        for (FieldProfile fieldProfile : classProfile.instanceFieldProfiles) {
            if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                continue;

            cachedClassProfile.fieldHashList.add(ClassProfile.getFieldHash(fieldProfile));
            cachedClassProfile.fieldHashStrictList.add(ClassProfile.getFieldHashStrict(fieldProfile));
        }

        return cachedClassProfile;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getClassHash() {
        return classHash;
    }

    @Override
    public String getClassHashStrict() {
        return classHashStrict;
    }

    @Override
    public String getBasicHash() {
        return basicHash;
    }

    @Override
    public String getBasicHashStrict() {
        return basicHashStrict;
    }

    @Override
    public List<String> getMethodHashList() {
        return methodHashList;
    }

    @Override
    public List<String> getMethodHashStrictList() {
        return methodHashStrictList;
    }

    @Override
    public List<String> getFieldHashList() {
        return fieldHashList;
    }

    @Override
    public List<String> getFieldHashStrictList() {
        return fieldHashStrictList;
    }
}
