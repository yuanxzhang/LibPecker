package cn.fudan.common;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by yuanxzhang on 14/03/2017.
 */
public class LibPeckerConfig {
    public static final double LIB_APK_PAIR_THRESHOLD = 0.5;

    public static boolean DEBUG_LIBPECKER = false;
    public static String DEBUG_LIBPECKER_LIB_PKG_NAME = "";
    public static String DEBUG_LIBPECKER_APK_PKG_NAME = "";

    public static final String DALVIK_ANNOTATION_PKG = "dalvik.annotation.";
    public static final String DALVIK_SIGNATURE = "dalvik.annotation.Signature";
    public static final String DALVIK_INNER_CLASS = "dalvik.annotation.InnerClass";
    public static final String DALVIK_THROWS = "dalvik.annotation.Throws";
    public static final String DALVIK_ANNOTATION_DEFAULT = "dalvik.annotation.AnnotationDefault";


    public static final String DEFAULT_SDK_DEX_PATH = "./sdk/android-20.dex";
}
