package cn.fudan.analysis.profile;

import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.LibPeckerConfig;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedTypeEncodedValue;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.value.EncodedValue;

import java.util.*;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class MethodProfile implements ProfileSignature {
    public DexBackedMethod method;

    public int accessFlags;
    public HashSet<String> accessFlagSet;
    public ClassNameProfile returnTypeProfile;
    public ArrayList<ClassNameProfile> parameterTypeProfileList;
    public HashSet<ClassNameProfile> throwExceptionClassNameProfileSet;

    public String hash;
    public String hashStrict;

    public MethodProfile(DexBackedMethod method, ClassProfile thisClassProfile, Set<String> targetSdkClassNameSet) {
        this.method = method;

        this.accessFlags = method.getAccessFlags();
        this.accessFlagSet = new HashSet<>();
        for (AccessFlags flags : AccessFlags.getAccessFlagsForField(method.getAccessFlags())) {
            this.accessFlagSet.add(flags.toString());
        }

        this.returnTypeProfile = ClassNameProfile.createInstance(method.getReturnType(), thisClassProfile, targetSdkClassNameSet);

        this.parameterTypeProfileList = new ArrayList<>();
        for (MethodParameter parameter : method.getParameters()) {
            this.parameterTypeProfileList.add(ClassNameProfile.createInstance(parameter.getType(),
                    thisClassProfile, targetSdkClassNameSet));
        }

        Set<? extends Annotation> annotations = this.method.getAnnotations();
        throwExceptionClassNameProfileSet = new HashSet<>();
        for (Annotation annotation : annotations) {
            if (LibPeckerConfig.DALVIK_THROWS.equals(DexHelper.classType2Name(annotation.getType()))) {
                for (AnnotationElement element : annotation.getElements()) {
                    if (element.getValue() instanceof DexBackedArrayEncodedValue) {
                        List<? extends EncodedValue> values = ((DexBackedArrayEncodedValue) element.getValue()).getValue();
                        for (EncodedValue value : values) {
                            if (value instanceof DexBackedTypeEncodedValue) {
                                throwExceptionClassNameProfileSet.add(ClassNameProfile.createInstance(((DexBackedTypeEncodedValue) value).getValue(),
                                        thisClassProfile, targetSdkClassNameSet));
                            } else {
                                throw new RuntimeException("missing some cases in get thrown exception class info in method: "
                                        + method.getDefiningClass() + "," + method.getName());
                            }
                        }
                    } else
                        throw new RuntimeException("missing some cases in get thrown exception class info in method: "
                                + method.getDefiningClass() + "," + method.getName());
                }
            }
        }
    }

    public boolean isStatic() {
        return this.accessFlagSet.contains(AccessFlags.STATIC.toString());
    }

    public boolean isFinal() {
        return this.accessFlagSet.contains(AccessFlags.FINAL.toString());
    }

    public String descriptor() {
        StringBuilder sb = new StringBuilder(method.getReturnType() + "-" + method.getName() + "-");
        for (MethodParameter parameter : method.getParameters()) {
            sb.append(parameter.getType() + ",");
        }
        return sb.toString();
    }

    @Override
    public String composeSignature() {
        StringBuilder sig = new StringBuilder();

        sig.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        sig.append(returnTypeProfile.composeSignature());
        sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + parameterTypeProfileList.size());
        for (ClassNameProfile profile : parameterTypeProfileList) {
            sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile.composeSignature());
        }

        sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + throwExceptionClassNameProfileSet.size());
        List<ClassNameProfile> exceptionClassList = new ArrayList<>(this.throwExceptionClassNameProfileSet);
        Collections.sort(exceptionClassList, new Comparator<ClassNameProfile>() {
            @Override
            public int compare(ClassNameProfile o1, ClassNameProfile o2) {
                return o1.composeSignature().compareTo(o2.composeSignature());
            }
        });
        for (ClassNameProfile profile : exceptionClassList) {
            sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile.composeSignature());
        }

        return sig.toString();
    }

    @Override
    public String composeSignatureStrict() {
        StringBuilder sig = new StringBuilder();

        sig.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        sig.append(returnTypeProfile.composeSignatureStrict());
        sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + parameterTypeProfileList.size());
        for (ClassNameProfile profile : parameterTypeProfileList) {
            sig.append(ProfileSignature.PROFILE_SEP_SEMICOLON + profile.composeSignatureStrict());
        }

        return sig.toString();
    }
}
