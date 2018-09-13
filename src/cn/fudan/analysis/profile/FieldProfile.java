package cn.fudan.analysis.profile;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.base.value.*;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.value.EncodedValue;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class FieldProfile implements ProfileSignature {
    public DexBackedField field;

    public int accessFlags;
    public HashSet<String> accessFlagSet;
    public ClassNameProfile typeProfile;
    public EncodedValue initialValue;

    public String hash;
    public String hashStrict;

    public FieldProfile(DexBackedField field, ClassProfile thisClassProfile, Set<String> targetSdkClassNameSet) {
        this.field = field;
        this.accessFlags = field.getAccessFlags();
        this.accessFlagSet = new HashSet<>();
        for (AccessFlags flags : AccessFlags.getAccessFlagsForField(field.getAccessFlags())) {
            this.accessFlagSet.add(flags.toString());
        }

        this.typeProfile = ClassNameProfile.createInstance(field.getType(), thisClassProfile, targetSdkClassNameSet);
        this.initialValue = field.getInitialValue();
    }

    public boolean isStatic() {
        return this.accessFlagSet.contains(AccessFlags.STATIC.toString());
    }

    public boolean isFinal() {
        return this.accessFlagSet.contains(AccessFlags.FINAL.toString());
    }

    public String getInitialValueString() {
        if (this.initialValue != null)
            return stringfyEncodedValue(this.initialValue);
        else
            return null;
    }

    private String stringfyEncodedValue(EncodedValue initialValue) {
        switch (initialValue.getValueType()) {
            case ValueType.BYTE:
                return "" + ((BaseByteEncodedValue) initialValue).getValue();
            case ValueType.SHORT:
                return "" + ((BaseShortEncodedValue) initialValue).getValue();
            case ValueType.CHAR:
                return "" + ((BaseCharEncodedValue) initialValue).getValue();
            case ValueType.INT:
                return "" + ((BaseIntEncodedValue) initialValue).getValue();
            case ValueType.LONG:
                return "" + ((BaseLongEncodedValue) initialValue).getValue();
            case ValueType.FLOAT:
                return "" + ((BaseFloatEncodedValue) initialValue).getValue();
            case ValueType.DOUBLE:
                return "" + ((BaseDoubleEncodedValue) initialValue).getValue();
            case ValueType.STRING:
                return "" + ((BaseStringEncodedValue) initialValue).getValue();
            case ValueType.BOOLEAN:
                return "" + ((BaseBooleanEncodedValue) initialValue).getValue();
            case ValueType.ENUM:
                FieldReference enumFieldReference = ((BaseEnumEncodedValue) initialValue).getValue();
                return enumFieldReference.getDefiningClass()
                        + enumFieldReference.getType() + enumFieldReference.getName();
            case ValueType.ARRAY:
                return ProfileSignature.FINAL_VALUE_ARRAY;
            case ValueType.NULL:
                return ProfileSignature.FINAL_VALUE_UNINITIALIZED;
            case ValueType.ANNOTATION:
                return ProfileSignature.FINAL_VALUE_ANNOTATION;
            case ValueType.TYPE:
                return ProfileSignature.FINAL_VALUE_TYPE;
            case ValueType.FIELD:
                return ProfileSignature.FINAL_VALUE_FIELD;
            case ValueType.METHOD:
                return ProfileSignature.FINAL_VALUE_METHOD;
            default:
                return ProfileSignature.FINAL_VALUE_UNINITIALIZED;
        }

    }

    public String descriptor() {
        return field.getType() + "-" + field.getName();
    }

    @Override
    public String composeSignatureStrict() {
        StringBuilder sig = new StringBuilder();

        sig.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        sig.append(typeProfile.composeSignatureStrict() + ProfileSignature.PROFILE_SEP_SEMICOLON);

        return sig.toString();
    }

    @Override
    public String composeSignature() {
        StringBuilder sig = new StringBuilder();

        sig.append(ClassProfile.genSigForAccessFlags(accessFlags) + ProfileSignature.PROFILE_SEP_SEMICOLON);
        sig.append(typeProfile.composeSignature() + ProfileSignature.PROFILE_SEP_SEMICOLON);
        if (isFinal())
            if (initialValue == null) {
                sig.append(ProfileSignature.FINAL_VALUE_UNINITIALIZED);
            } else {
                sig.append(stringfyEncodedValue(initialValue));
            }

        return sig.toString();
    }

}
