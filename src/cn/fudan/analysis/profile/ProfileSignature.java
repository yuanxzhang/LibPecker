package cn.fudan.analysis.profile;

/**
 * Created by yuanxzhang on 15/04/2017.
 */
public interface ProfileSignature {
    String CLASS_NAME_SAME_PKG = "X";
    String CLASS_NAME_OTHER = "Y";
    String FINAL_VALUE_UNINITIALIZED = "uninitialized";
    String FINAL_VALUE_ARRAY = "array";
    String FINAL_VALUE_ANNOTATION = "annotation";
    String FINAL_VALUE_METHOD = "method";
    String FINAL_VALUE_FIELD = "field";
    String FINAL_VALUE_TYPE = "type";
    String PROFILE_SEP_COMMA = ",";
    String PROFILE_SEP_SEMICOLON = ";";

    String composeSignature();
    String composeSignatureStrict();
}
