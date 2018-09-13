package cn.fudan.libpecker.model;

import java.util.List;

/**
 * Created by yuanxzhang on 19/05/2017.
 */
public interface SimpleClassProfile {
    String getClassName();

    String getClassHash();
    String getClassHashStrict();

    String getBasicHash();
    String getBasicHashStrict();

    List<String> getMethodHashList();
    List<String> getMethodHashStrictList();

    List<String> getFieldHashList();
    List<String> getFieldHashStrictList();
}
