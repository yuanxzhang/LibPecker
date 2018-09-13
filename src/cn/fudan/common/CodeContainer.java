package cn.fudan.common;

import org.jf.dexlib2.iface.ClassDef;

import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public abstract class CodeContainer {
    public abstract String codeHash();
    public abstract String codePath();
    public abstract Set<? extends ClassDef> getClasses();
}
