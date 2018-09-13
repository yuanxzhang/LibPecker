package cn.fudan.analysis.dep;

import cn.fudan.analysis.profile.*;
import cn.fudan.common.CodeContainer;
import cn.fudan.common.Sdk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class DepAnalysis {
    private CodeContainer codeContainer;
    public Set<ClassProfile> allClassProfiles;
    private Set<DepNode> depGraph; // all nodes including system nodes
    private Set<String> systemClassSet;
    public HashMap<String, ConcreteDepNode> ownNodeMap;
    public HashMap<String, DepNode> systemNodeMap;
    public HashMap<String, DepNode> lostOtherPackageNodeMap;
    public HashMap<String,DepNode> allNodeMap;

    public DepAnalysis(CodeContainer codeContainer, Set<String> systemClassSet) {
        this.codeContainer = codeContainer;
        this.systemClassSet = systemClassSet;
        this.allClassProfiles = ProfileGenerator.generate(codeContainer, systemClassSet);
        this.depGraph = new HashSet<>();
        this.ownNodeMap = new HashMap<>();
        for (ClassProfile cp : this.allClassProfiles) {
            ConcreteDepNode node = new ConcreteDepNode(cp);
            this.depGraph.add(node);
            this.ownNodeMap.put(cp.getClassName(), node);
        }
        this.systemNodeMap = new HashMap<>();
        this.lostOtherPackageNodeMap = new HashMap<>();
        this.allNodeMap = new HashMap<>();
        this.beginDepAnalysis();
        this.depGraph.addAll(this.systemNodeMap.values());
        this.allNodeMap.putAll(ownNodeMap);
        this.allNodeMap.putAll(systemNodeMap);
        this.allNodeMap.putAll(lostOtherPackageNodeMap);
    }

    public DepAnalysis(CodeContainer codeContainer) {// default android version: 20
        this.codeContainer = codeContainer;
        this.systemClassSet = new HashSet<>();
        this.systemClassSet.addAll(Sdk.loadDefaultSdk().getTargetSdkClassNameSet());
        this.allClassProfiles = ProfileGenerator.generate(codeContainer, systemClassSet);
        this.depGraph = new HashSet<>();
        this.ownNodeMap = new HashMap<>();
        for (ClassProfile cp : this.allClassProfiles) {
            ConcreteDepNode node = new ConcreteDepNode(cp);
            this.depGraph.add(node);
            this.ownNodeMap.put(cp.getClassName(), node);
        }
        this.lostOtherPackageNodeMap = new HashMap<>();
        this.systemNodeMap = new HashMap<>();
        this.beginDepAnalysis();
        this.depGraph.addAll(this.systemNodeMap.values());
    }

    public Set<String> getSystemClassSet() {
        return systemClassSet;
    }

    public ClassProfile getClassProfile(String className) {
        if (ownNodeMap.containsKey(className))
            return ownNodeMap.get(className).classProfile;
        else
            return null;
    }

    // Get all class names it depends on, including system class
    public HashSet<String> getAllDependingClassName(String classname) {
        HashSet<String> set = new HashSet<>();
        if (this.ownNodeMap.containsKey(classname)) {
            ConcreteDepNode node = this.ownNodeMap.get(classname);
            for (DepEdge de : node.edges) {
                set.add(de.getSink().classname);
            }
            return set;
        } else
            return null;
    }

    // Get all ClassProfiles it depends on, except system class
    public HashSet<ClassProfile> getAllDependingClassProfile(String classname) {
        HashSet<ClassProfile> set = new HashSet<>();
        if (this.ownNodeMap.containsKey(classname)) {
            ConcreteDepNode node = this.ownNodeMap.get(classname);
            for (DepEdge de : node.edges) {
                if (de.getSink() instanceof ConcreteDepNode) {
                    set.add(((ConcreteDepNode) de.getSink()).classProfile);
                }
            }
            return set;
        } else
            return null;
    }

    // Get all class names it is depended on, including system class
    public HashSet<String> getAllDependedClassName(String classname) {
        HashSet<String> set = new HashSet<>();
        if (this.ownNodeMap.containsKey(classname)) {
            ConcreteDepNode node = this.ownNodeMap.get(classname);
            for (DepEdge de : node.sinkedges) {
                set.add(de.getSource().classname);
            }
            return set;
        } else
            return null;
    }

    // Get all ClassProfiles it is depended on, except system class
    public HashSet<ClassProfile> getAllDependedClassProfile(String classname) {
        HashSet<ClassProfile> set = new HashSet<>();
        if (this.ownNodeMap.containsKey(classname)) {
            ConcreteDepNode node = this.ownNodeMap.get(classname);
            for (DepEdge de : node.sinkedges) {
                if (de.getSource() instanceof ConcreteDepNode) {
                    set.add(((ConcreteDepNode) de.getSource()).classProfile);
                }
            }
            return set;
        } else
            return null;
    }

    private void beginDepAnalysis() {
        for (ConcreteDepNode dn : this.ownNodeMap.values()) {
            // EXTENDS
            if (dn.classProfile.superClassProfile.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                DepNode sysdn;
                if (this.systemNodeMap.containsKey(dn.classProfile.superClassProfile.name)) {
                    sysdn = this.systemNodeMap.get(dn.classProfile.superClassProfile.name);
                } else {
                    sysdn = new DepNode(dn.classProfile.superClassProfile.name);
                    this.systemNodeMap.put(dn.classProfile.superClassProfile.name, sysdn);
                }
                DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_EXTENDS, dn,
                        sysdn);
                dn.addDepEdge(de);
                dn.addTypecount(0, 0);
                de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                sysdn.addSinkEdge(de);
            } else if (dn.classProfile.superClassProfile.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_EXTENDS, dn,
                        this.ownNodeMap.get(dn.classProfile.superClassProfile.name));
                dn.addDepEdge(de);
                dn.addTypecount(0, 1);
                de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                if (this.ownNodeMap.containsKey(dn.classProfile.superClassProfile.name))
                    this.ownNodeMap.get(dn.classProfile.superClassProfile.name).addSinkEdge(de);
            } else if (dn.classProfile.superClassProfile.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                DepEdge de;
                if (this.ownNodeMap.containsKey(dn.classProfile.superClassProfile.name)) {
                    de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_EXTENDS, dn,
                            this.ownNodeMap.get(dn.classProfile.superClassProfile.name));
                    this.ownNodeMap.get(dn.classProfile.superClassProfile.name).addSinkEdge(de);
                } else {
                    DepNode node;
                    if (this.lostOtherPackageNodeMap.containsKey(dn.classProfile.superClassProfile.name))
                        node = this.lostOtherPackageNodeMap.get(dn.classProfile.superClassProfile.name);
                    else {
                        node = new DepNode(dn.classProfile.superClassProfile.name);
                        this.lostOtherPackageNodeMap.put(dn.classProfile.superClassProfile.name, node);
                    }
                    de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_EXTENDS, dn, node);
                    node.addSinkEdge(de);
                }
                dn.addDepEdge(de);
                dn.addTypecount(0, 2);
                de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
            }

            // IMPLEMENTS
            for (ClassNameProfile cnp : dn.classProfile.interfaceProfileSet) {
                if (cnp.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                    DepNode sysdn;
                    if (this.systemNodeMap.containsKey(cnp.name)) {
                        sysdn = this.systemNodeMap.get(cnp.name);
                    } else {
                        sysdn = new DepNode(cnp.name);
                        this.systemNodeMap.put(cnp.name, sysdn);
                    }
                    DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_IMPLEMENTS, dn,
                            sysdn);
                    dn.addDepEdge(de);
                    dn.addTypecount(1, 0);
                    de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                    sysdn.addSinkEdge(de);
                } else if (cnp.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                    DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_IMPLEMENTS, dn,
                            this.ownNodeMap.get(cnp.name));
                    dn.addDepEdge(de);
                    dn.addTypecount(1, 1);
                    de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                    if (this.ownNodeMap.containsKey(cnp.name))
                        this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                } else if (cnp.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                    DepEdge de;
                    if (this.ownNodeMap.containsKey(cnp.name)) {
                        de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_IMPLEMENTS, dn,
                                this.ownNodeMap.get(cnp.name));
                        this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                    } else {
                        DepNode node;
                        if (this.lostOtherPackageNodeMap.containsKey(cnp.name))
                            node = this.lostOtherPackageNodeMap.get(cnp.name);
                        else {
                            node = new DepNode(cnp.name);
                            this.lostOtherPackageNodeMap.put(cnp.name, node);
                        }
                        de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_IMPLEMENTS, dn, node);
                        node.addSinkEdge(de);
                    }
                    dn.addDepEdge(de);
                    dn.addTypecount(1, 2);
                    de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
                }
            }

            // FIELD_IN
            HashMap<String, DepEdge> existFieldMap = new HashMap<>();// point-to class -> DepEdge
            HashSet<FieldProfile> fieldProfileSet = new HashSet<>();
            fieldProfileSet.addAll(dn.classProfile.instanceFieldProfiles);
            fieldProfileSet.addAll(dn.classProfile.staticFieldProfiles);

            for (FieldProfile fp : fieldProfileSet) {
                if (existFieldMap.containsKey(fp.typeProfile.name)) {
                    existFieldMap.get(fp.typeProfile.name).addWeight();
                } else {
                    if (fp.typeProfile.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                        DepNode sysdn;
                        if (this.systemNodeMap.containsKey(fp.typeProfile.name)) {
                            sysdn = this.systemNodeMap.get(fp.typeProfile.name);
                        } else {
                            sysdn = new DepNode(fp.typeProfile.name);
                            this.systemNodeMap.put(fp.typeProfile.name, sysdn);
                        }
                        DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_FIELD_IN, dn,
                                sysdn);
                        dn.addDepEdge(de);
                        dn.addTypecount(2, 0);
                        de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                        sysdn.addSinkEdge(de);
                        existFieldMap.put(fp.typeProfile.name, de);
                    } else if (fp.typeProfile.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                        DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_FIELD_IN, dn,
                                this.ownNodeMap.get(fp.typeProfile.name));
                        dn.addDepEdge(de);
                        dn.addTypecount(2, 1);
                        de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                        if (this.ownNodeMap.containsKey(fp.typeProfile.name))
                            this.ownNodeMap.get(fp.typeProfile.name).addSinkEdge(de);
                        existFieldMap.put(fp.typeProfile.name, de);
                    } else if (fp.typeProfile.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                        DepEdge de;
                        if (this.ownNodeMap.containsKey(fp.typeProfile.name)) {
                            de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_FIELD_IN, dn,
                                    this.ownNodeMap.get(fp.typeProfile.name));
                            this.ownNodeMap.get(fp.typeProfile.name).addSinkEdge(de);
                        } else {
                            DepNode node;
                            if (this.lostOtherPackageNodeMap.containsKey(fp.typeProfile.name))
                                node = this.lostOtherPackageNodeMap.get(fp.typeProfile.name);
                            else {
                                node = new DepNode(fp.typeProfile.name);
                                this.lostOtherPackageNodeMap.put(fp.typeProfile.name, node);
                            }
                            de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_FIELD_IN, dn, node);
                            node.addSinkEdge(de);
                        }
                        dn.addDepEdge(de);
                        dn.addTypecount(2, 2);
                        de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
                        existFieldMap.put(fp.typeProfile.name, de);
                    }
                }
            }

            // Method level
            HashMap<String, DepEdge> existParameterMap = new HashMap<>();// point-to class -> DepEdge
            HashMap<String, DepEdge> existReturnMap = new HashMap<>();// point-to class -> DepEdge
            HashMap<String, DepEdge> existExceptionMap = new HashMap<>();// point-to class -> DepEdge

            HashSet<MethodProfile> allMethodProfile = new HashSet<>();
            allMethodProfile.addAll(dn.classProfile.staticMethodProfiles);
            allMethodProfile.addAll(dn.classProfile.instanceMethodProfiles);
            for (MethodProfile mp : allMethodProfile) {
                // METHOD_PARAMETER
                for (ClassNameProfile cnp : mp.parameterTypeProfileList) {
                    if (existParameterMap.containsKey(cnp.name)) {
                        existParameterMap.get(cnp.name).addWeight();
                    } else {
                        if (cnp.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                            DepNode sysdn;
                            if (this.systemNodeMap.containsKey(cnp.name)) {
                                sysdn = this.systemNodeMap.get(cnp.name);
                            } else {
                                sysdn = new DepNode(cnp.name);
                                this.systemNodeMap.put(cnp.name, sysdn);
                            }
                            DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_PARAMETER, dn,
                                    sysdn);
                            dn.addDepEdge(de);
                            dn.addTypecount(3, 0);
                            de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                            sysdn.addSinkEdge(de);
                            existParameterMap.put(cnp.name, de);
                        } else if (cnp.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                            DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_PARAMETER, dn,
                                    this.ownNodeMap.get(cnp.name));
                            dn.addDepEdge(de);
                            dn.addTypecount(3, 1);
                            de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                            if (this.ownNodeMap.containsKey(cnp.name))
                                this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                            existParameterMap.put(cnp.name, de);
                        } else if (cnp.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                            DepEdge de;
                            if (this.ownNodeMap.containsKey(cnp.name)) {
                                de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_PARAMETER, dn,
                                        this.ownNodeMap.get(cnp.name));
                                this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                            } else {
                                DepNode node;
                                if (this.lostOtherPackageNodeMap.containsKey(cnp.name))
                                    node = this.lostOtherPackageNodeMap.get(cnp.name);
                                else {
                                    node = new DepNode(cnp.name);
                                    this.lostOtherPackageNodeMap.put(cnp.name, node);
                                }
                                de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_PARAMETER, dn, node);
                                node.addSinkEdge(de);
                            }

                            dn.addDepEdge(de);
                            dn.addTypecount(3, 2);
                            de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
                            existParameterMap.put(cnp.name, de);
                        }
                    }
                }
                // METHOD_RETURN
                if (existReturnMap.containsKey(mp.returnTypeProfile.name)) {
                    existReturnMap.get(mp.returnTypeProfile.name).addWeight();
                } else {
                    if (mp.returnTypeProfile.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                        DepNode sysdn;
                        if (this.systemNodeMap.containsKey(mp.returnTypeProfile.name)) {
                            sysdn = this.systemNodeMap.get(mp.returnTypeProfile.name);
                        } else {
                            sysdn = new DepNode(mp.returnTypeProfile.name);
                            this.systemNodeMap.put(mp.returnTypeProfile.name, sysdn);
                        }
                        DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_RETURN, dn,
                                sysdn);
                        dn.addDepEdge(de);
                        dn.addTypecount(4, 0);
                        de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                        sysdn.addSinkEdge(de);
                        existReturnMap.put(mp.returnTypeProfile.name, de);
                    } else if (mp.returnTypeProfile.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                        DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_RETURN, dn,
                                this.ownNodeMap.get(mp.returnTypeProfile.name));
                        dn.addDepEdge(de);
                        dn.addTypecount(4, 1);
                        de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                        if (this.ownNodeMap.containsKey(mp.returnTypeProfile.name)) {
                            this.ownNodeMap.get(mp.returnTypeProfile.name).addSinkEdge(de);
                        }
                        existReturnMap.put(mp.returnTypeProfile.name, de);
                    } else if (mp.returnTypeProfile.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                        DepEdge de;
                        if (this.ownNodeMap.containsKey(mp.returnTypeProfile.name)) {
                            de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_RETURN, dn,
                                    this.ownNodeMap.get(mp.returnTypeProfile.name));
                            this.ownNodeMap.get(mp.returnTypeProfile.name).addSinkEdge(de);
                        } else {
                            DepNode node;
                            if (this.lostOtherPackageNodeMap.containsKey(mp.returnTypeProfile.name))
                                node = this.lostOtherPackageNodeMap.get(mp.returnTypeProfile.name);
                            else {
                                node = new DepNode(mp.returnTypeProfile.name);
                                this.lostOtherPackageNodeMap.put(mp.returnTypeProfile.name, node);
                            }
                            de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_RETURN, dn, node);
                            node.addSinkEdge(de);
                        }
                        dn.addDepEdge(de);
                        dn.addTypecount(4, 2);
                        de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
                        existReturnMap.put(mp.returnTypeProfile.name, de);
                    }
                }
                // METHOD_EXCEPTION
                if (mp.throwExceptionClassNameProfileSet != null) {
                    for (ClassNameProfile cnp : mp.throwExceptionClassNameProfileSet) {
                        if (existExceptionMap.containsKey(cnp.name)) {
                            existExceptionMap.get(cnp.name).addWeight();
                        } else {
                            if (cnp.type == ClassNameProfile.ClassType.SYS_LIB_CLASS) {
                                DepNode sysdn;
                                if (this.systemNodeMap.containsKey(cnp.name)) {
                                    sysdn = this.systemNodeMap.get(cnp.name);
                                } else {
                                    sysdn = new DepNode(cnp.name);
                                    this.systemNodeMap.put(cnp.name, sysdn);
                                }
                                DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_EXCEPTION, dn,
                                        sysdn);
                                sysdn.addSinkEdge(de);
                                dn.addDepEdge(de);
                                dn.addTypecount(5, 0);
                                de.setClassType(ClassNameProfile.ClassType.SYS_LIB_CLASS);
                                existExceptionMap.put(cnp.name, de);
                            } else if (cnp.type == ClassNameProfile.ClassType.SAME_PKG_CLASS) {
                                DepEdge de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_EXCEPTION, dn,
                                        this.ownNodeMap.get(cnp.name));
                                if (this.ownNodeMap.containsKey(cnp.name))
                                    this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                                dn.addDepEdge(de);
                                dn.addTypecount(5, 1);
                                de.setClassType(ClassNameProfile.ClassType.SAME_PKG_CLASS);
                                existExceptionMap.put(cnp.name, de);
                            } else if (cnp.type == ClassNameProfile.ClassType.OTHER_PKG_CLASS) {
                                DepEdge de;
                                if (this.ownNodeMap.containsKey(cnp.name)) {
                                    de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_EXCEPTION, dn,
                                            this.ownNodeMap.get(cnp.name));
                                    this.ownNodeMap.get(cnp.name).addSinkEdge(de);
                                } else {
                                    DepNode node;
                                    if (this.lostOtherPackageNodeMap.containsKey(cnp.name))
                                        node = this.lostOtherPackageNodeMap.get(cnp.name);
                                    else {
                                        node = new DepNode(cnp.name);
                                        this.lostOtherPackageNodeMap.put(cnp.name, node);
                                    }
                                    de = new DepEdge(DepEdge.DepEdgeType.DEP_EDGE_METHOD_EXCEPTION, dn, node);
                                    node.addSinkEdge(de);
                                }
                                dn.addDepEdge(de);
                                dn.addTypecount(5, 2);
                                de.setClassType(ClassNameProfile.ClassType.OTHER_PKG_CLASS);
                                existExceptionMap.put(cnp.name, de);
                            }
                        }
                    }
                }
            }

        }
    }

}
