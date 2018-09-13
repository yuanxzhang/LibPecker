package cn.fudan.analysis.profile;

import cn.fudan.analysis.util.DexHelper;

import java.util.Set;

/**
 * Created by yuanxzhang on 15/04/2017.
 */
public class ClassNameProfile implements ProfileSignature {
    public enum ClassType {
        SYS_LIB_CLASS,
        SAME_PKG_CLASS,
        OTHER_PKG_CLASS
    }

    public ClassType type;
    public String name;
    public int arrayDimension;

    private ClassNameProfile() {
    }

    public static ClassNameProfile createInstance(String thisClassType, ClassProfile profileReferer, Set<String> targetSdkClassNameSet) {
        ClassNameProfile nameProfile = new ClassNameProfile();

        nameProfile.arrayDimension = DexHelper.getArrayDepth(thisClassType);
        nameProfile.name = DexHelper.getBaseClassTypeOfArray(thisClassType);
        if (!DexHelper.isPrimitiveClass(nameProfile.name))
            nameProfile.name = DexHelper.classType2Name(nameProfile.name);

        if (targetSdkClassNameSet.contains(nameProfile.name) || DexHelper.isPrimitiveClass(nameProfile.name)) {
            nameProfile.type = ClassType.SYS_LIB_CLASS;
        } else if (DexHelper.getPackageName(nameProfile.name).equals(DexHelper.getPackageName(profileReferer.getClassName()))) {
            nameProfile.type = ClassType.SAME_PKG_CLASS;
        } else {
            nameProfile.type = ClassType.OTHER_PKG_CLASS;
        }
        return nameProfile;
    }

    @Override
    public String composeSignatureStrict() {
        return composeSignature();
    }

    @Override
    public String composeSignature() {
        StringBuilder sig = new StringBuilder();
        sig.append(type.ordinal() + ProfileSignature.PROFILE_SEP_COMMA);
        sig.append(arrayDimension + ProfileSignature.PROFILE_SEP_COMMA);

        switch (type) {
            case SYS_LIB_CLASS:
                sig.append(name);
                break;
            case SAME_PKG_CLASS:
                sig.append(ProfileSignature.CLASS_NAME_SAME_PKG);
                break;
            case OTHER_PKG_CLASS:
                sig.append(ProfileSignature.CLASS_NAME_OTHER);
                break;
            default:
                sig.append(ProfileSignature.CLASS_NAME_OTHER);
                break;
        }

        return sig.toString();
    }
}
