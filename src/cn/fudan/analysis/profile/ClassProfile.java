package cn.fudan.analysis.profile;

import cn.fudan.analysis.tree.ClassNode;
import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.util.HashHelper;
import cn.fudan.libpecker.model.SimpleClassProfile;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.util.*;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class ClassProfile implements ProfileSignature, SimpleClassProfile {
    public ClassNode classNode;

    // class info
    public int accessFlags;
    public HashSet<String> accessFlagSet;
    public ClassNameProfile superClassProfile;
    public HashSet<ClassNameProfile> interfaceProfileSet;

    // field info
    public HashSet<FieldProfile> staticFieldProfiles;
    public HashSet<FieldProfile> instanceFieldProfiles;

    // method info
    public HashSet<MethodProfile> staticMethodProfiles;
    public HashSet<MethodProfile> instanceMethodProfiles;

    public ClassProfile(ClassNode classNode, Set<String> targetSdkClassNameSet) {
        this.classNode = classNode;
        this.accessFlags = classNode.getClassDefInDex().getAccessFlags();
        this.accessFlagSet = new HashSet<>();
        for (AccessFlags flags : AccessFlags.getAccessFlagsForField(this.accessFlags)) {
            this.accessFlagSet.add(flags.toString());
        }

        // extends
        this.superClassProfile = ClassNameProfile.createInstance(classNode.getClassDefInDex().getSuperclass(), this, targetSdkClassNameSet);

        // implements
        this.interfaceProfileSet = new HashSet<>();
        for (String implementedClass : classNode.getClassDefInDex().getInterfaces()) {
            this.interfaceProfileSet.add(ClassNameProfile.createInstance(implementedClass, this, targetSdkClassNameSet));
        }

        this.staticFieldProfiles = new HashSet<>();
        this.instanceFieldProfiles = new HashSet<>();
        for (Field f : classNode.getFields()) {
            DexBackedField df = (DexBackedField) f;
            FieldProfile fieldProfile = new FieldProfile(df, this, targetSdkClassNameSet);
            if (fieldProfile.isStatic())
                this.staticFieldProfiles.add(fieldProfile);
            else
                this.instanceFieldProfiles.add(fieldProfile);
        }

        this.staticMethodProfiles = new HashSet<>();
        this.instanceMethodProfiles = new HashSet<>();
        for (Method m : classNode.getNormalizedMethods()) {
            if (DexHelper.isCompilerGeneratedMethod(m)) {
                continue;
            }

            DexBackedMethod dm = (DexBackedMethod) m;
            MethodProfile mp = new MethodProfile(dm, this, targetSdkClassNameSet);
            if (mp.isStatic())
                this.staticMethodProfiles.add(mp);
            else
                this.instanceMethodProfiles.add(mp);
        }
    }

    @Override
    public String getClassName() {
        return classNode.getClassName();
    }

    public synchronized static String getMethodHash(MethodProfile profile) {
        if (profile.hash == null)
            profile.hash = HashHelper.hash(profile.composeSignature());
        return profile.hash;
    }

    public synchronized static String getMethodHashStrict(MethodProfile profile) {
        if (profile.hashStrict == null)
            profile.hashStrict = HashHelper.hash(profile.composeSignatureStrict());
        return profile.hashStrict;
    }

    public synchronized static String getFieldHash(FieldProfile profile) {
        if (profile.hash == null)
            profile.hash = HashHelper.hash(profile.composeSignature());
        return profile.hash;
    }

    public synchronized static String getFieldHashStrict(FieldProfile profile) {
        if (profile.hashStrict == null)
            profile.hashStrict = HashHelper.hash(profile.composeSignatureStrict());
        return profile.hashStrict;
    }

    private String hash;
    private String hashStrict;
    private String hashBasic;
    private String hashBasicStrict;

    @Override
    public synchronized String getClassHash() {
        if (hash == null)
            hash = HashHelper.hash(this.composeSignature());
        return hash;
    }

    @Override
    public synchronized String getClassHashStrict() {
        if (hashStrict == null)
            hashStrict = HashHelper.hash(this.composeSignatureStrict());
        return hashStrict;
    }

    @Override
    public synchronized String getBasicHash() {
        if (hashBasic == null)
            hashBasic = HashHelper.hash(this.composeClassSignatureBasic());
        return hashBasic;
    }

    @Override
    public synchronized String getBasicHashStrict() {
        if (hashBasicStrict == null)
            hashBasicStrict = HashHelper.hash(this.composeClassSignatureBasicStrict());
        return hashBasicStrict;
    }

    private List<String> methodHashList = null;
    private List<String> methodHashStrictList = null;
    private List<String> fieldHashList = null;
    private List<String> fieldHashStrictList = null;

    @Override
    public List<String> getMethodHashList() {
        if (methodHashList != null)
            return methodHashList;

        synchronized (this) {
            methodHashList = new ArrayList<>();

            for (MethodProfile methodProfile : staticMethodProfiles) {
                methodHashList.add(getMethodHash(methodProfile));
            }
            for (MethodProfile methodProfile : instanceMethodProfiles) {
                methodHashList.add(getMethodHash(methodProfile));
            }

            return methodHashList;
        }
    }


    @Override
    public List<String> getMethodHashStrictList() {
        if (methodHashStrictList != null)
            return methodHashStrictList;

        synchronized (this) {
            methodHashStrictList = new ArrayList<>();

            for (MethodProfile methodProfile : staticMethodProfiles) {
                methodHashStrictList.add(getMethodHashStrict(methodProfile));
            }
            for (MethodProfile methodProfile : instanceMethodProfiles) {
                methodHashStrictList.add(getMethodHashStrict(methodProfile));
            }

            return methodHashStrictList;
        }
    }

    @Override
    public List<String> getFieldHashList() {
        if (fieldHashList != null)
            return fieldHashList;

        synchronized (this) {
            fieldHashList = new ArrayList<>();

            for (FieldProfile fieldProfile : staticFieldProfiles) {
                if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                    continue;

                fieldHashList.add(getFieldHash(fieldProfile));
            }
            for (FieldProfile fieldProfile : instanceFieldProfiles) {
                if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                    continue;

                fieldHashList.add(getFieldHash(fieldProfile));
            }

            return fieldHashList;
        }
    }

    @Override
    public List<String> getFieldHashStrictList() {
        if (fieldHashStrictList != null)
            return fieldHashStrictList;

        synchronized (this) {
            fieldHashStrictList = new ArrayList<>();

            for (FieldProfile fieldProfile : staticFieldProfiles) {
                if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                    continue;

                fieldHashStrictList.add(getFieldHashStrict(fieldProfile));
            }
            for (FieldProfile fieldProfile : instanceFieldProfiles) {
                if (fieldProfile.isFinal() && fieldProfile.field.getName().equals("serialVersionUID"))
                    continue;

                fieldHashStrictList.add(getFieldHashStrict(fieldProfile));
            }

            return fieldHashStrictList;
        }
    }

    private String composeHashStr(List<String> hashList) {
        StringBuilder sb = new StringBuilder();

        sb.append(hashList.size());
        for (String hash : hashList) {
            sb.append(ProfileSignature.PROFILE_SEP_COMMA);
            sb.append(hash);
        }

        return sb.toString();
    }

    private String composeClassSignatureBasic() {
        StringBuilder basicSig = new StringBuilder();

        basicSig.append(genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        basicSig.append(superClassProfile.composeSignature());
        basicSig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + interfaceProfileSet.size());
        List<String> interfaceList = new ArrayList<>();
        for (ClassNameProfile profile : interfaceProfileSet) {
            interfaceList.add(profile.composeSignature());
        }
        Collections.sort(interfaceList);
        for (String profile : interfaceList) {
            basicSig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile);
        }

        return basicSig.toString();
    }

    private String composeClassSignatureBasicStrict() {
        StringBuilder basicSig = new StringBuilder();

        basicSig.append(genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        basicSig.append(superClassProfile.composeSignature());

        return basicSig.toString();
    }

    public static int genSigForAccessFlags(int accessFlags) {
        int d = accessFlags & AccessFlags.STATIC.getValue();
        int e = accessFlags & AccessFlags.SYNCHRONIZED.getValue();
        int f = accessFlags & AccessFlags.NATIVE.getValue();
        int h = accessFlags & AccessFlags.INTERFACE.getValue();

        return d | e | f | h;
    }

    @Override
    public String composeSignatureStrict() {
        StringBuilder hashStr = new StringBuilder();

        hashStr.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(superClassProfile.composeSignature());
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON + interfaceProfileSet.size());
        List<String> interfaceList = new ArrayList<>();
        for (ClassNameProfile profile : interfaceProfileSet) {
            interfaceList.add(profile.composeSignature());
        }
        Collections.sort(interfaceList);
        for (String profile : interfaceList) {
            hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile);
        }

        List<String> hashList = new ArrayList<>();
        for (FieldProfile profile : staticFieldProfiles) {
            if (profile.isFinal() && profile.field.getName().equals("serialVersionUID"))
                continue;

            hashList.add(getFieldHashStrict(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (FieldProfile profile : instanceFieldProfiles) {
            hashList.add(getFieldHashStrict(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (MethodProfile profile : staticMethodProfiles) {
            hashList.add(getMethodHashStrict(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (MethodProfile profile : instanceMethodProfiles) {
            hashList.add(getMethodHashStrict(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        return hashStr.toString();
    }

    @Override
    public String composeSignature() {
        StringBuilder hashStr = new StringBuilder();

        hashStr.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(superClassProfile.composeSignature());
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON + interfaceProfileSet.size());
        List<String> interfaceList = new ArrayList<>();
        for (ClassNameProfile profile : interfaceProfileSet) {
            interfaceList.add(profile.composeSignature());
        }
        Collections.sort(interfaceList);
        for (String profile : interfaceList) {
            hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile);
        }

        List<String> hashList = new ArrayList<>();
        for (FieldProfile profile : staticFieldProfiles) {
            if (profile.isFinal() && profile.field.getName().equals("serialVersionUID"))
                continue;

            hashList.add(getFieldHash(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (FieldProfile profile : instanceFieldProfiles) {
            hashList.add(getFieldHash(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (MethodProfile profile : staticMethodProfiles) {
            hashList.add(getMethodHash(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        hashList = new ArrayList<>();
        for (MethodProfile profile : instanceMethodProfiles) {
            hashList.add(getMethodHash(profile));
        }
        Collections.sort(hashList);
        hashStr.append(ProfileSignature.PROFILE_SEP_SEMICOLON);
        hashStr.append(composeHashStr(hashList));

        return hashStr.toString();
    }
}
