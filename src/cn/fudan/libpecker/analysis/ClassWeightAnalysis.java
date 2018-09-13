package cn.fudan.libpecker.analysis;

import cn.fudan.analysis.cfg.CFGAnalysis;
import cn.fudan.analysis.dep.DepAnalysis;
import cn.fudan.analysis.profile.ClassProfile;
import cn.fudan.analysis.util.DexHelper;
import cn.fudan.common.CodeContainer;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MethodImplementation;

import java.util.*;

/**
 * Created by yuanxzhang on 26/04/2017.
 */
public class ClassWeightAnalysis {

    public static Map<String, Integer> getClassBBWeight(CodeContainer container) {
        Set<? extends ClassDef> allClasses = container.getClasses();

        Map<String, Integer> mapClassName2BBCount = new HashMap<>();
        for (ClassDef classDef : allClasses) {
            String className = DexHelper.classType2Name(classDef.getType());
            int classBBCount = 0;
            for (org.jf.dexlib2.iface.Method method : classDef.getMethods()) {
                MethodImplementation impl = method.getImplementation();
                if (impl != null) {
                    classBBCount += CFGAnalysis.getBasicBlocks(impl).size();
                }
            }

            mapClassName2BBCount.put(className, classBBCount);
        }

        return mapClassName2BBCount;
    }

    public static Map<String, Integer> getClassDepWeight(CodeContainer container, HashSet<String> sdkClassSet) {
        DepAnalysis depAnalysis = new DepAnalysis(container, sdkClassSet);
        return getClassDepWeight(depAnalysis);
    }

    public static Map<String, Integer> getClassDepWeight(DepAnalysis depAnalysis) {
        Map<String, Integer> mapClassName2DepWeight = new HashMap<>();
        for (ClassProfile classProfile : depAnalysis.allClassProfiles) {
            String className = classProfile.getClassName();
            mapClassName2DepWeight.put(className, depAnalysis.getAllDependingClassProfile(className).size());
        }
        return mapClassName2DepWeight;
    }
}
