package cn.fudan.libpecker.analysis;

import cn.fudan.analysis.name.NameAnalysis;
import cn.fudan.analysis.name.NameCollector;
import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.common.CodeContainer;
import cn.fudan.common.util.PackageNameUtil;

import java.util.*;

/**
 * Created by yuanxzhang on 26/04/2017.
 */
public class RootPackageAnalysis {

    private static class PackageTreeNode {
        int level;//start from 1
        String nodeName;
        List<PackageTreeNode> subNodes;

        public int  getSubNodesNum(){
            return subNodes.size();
        }

        public static final PackageTreeNode CURRENT = new PackageTreeNode();

        public boolean belong(String packageName) {
            assert (packageName != null);

            if (packageName.equals(PackageNode.Factory.DEFAULT_PACKAGE)) {
                if (nodeName.equals(PackageNode.Factory.DEFAULT_PACKAGE))
                    return true;
                else
                    return false;
            }
            else {
                String rootName = topNodeName(packageName);
                return rootName.equals(nodeName);
            }
        }

        public static String topNodeName(String packageName) {
            if (! packageName.contains("."))
                return packageName;
            else
                return packageName.substring(0, packageName.indexOf('.'));
        }

        public static String nextLevelName(String packageName) {
            if (! packageName.contains("."))
                return null;
            else
                return packageName.substring(packageName.indexOf('.')+1);
        }

        public boolean insert(String packageName) {
            if (! belong(packageName))
                return false;

            if (subNodes == null)
                subNodes = new ArrayList<>();

            String newPackageName = nextLevelName(packageName);
            if (newPackageName == null) {
                subNodes.add(CURRENT);
                return true;
            }

            String newNodeName = topNodeName(newPackageName);
            for (PackageTreeNode node : subNodes)  {
                if (node == CURRENT)
                    continue;
                if (node.nodeName.equals(newNodeName))
                    return node.insert(newPackageName);
            }

            PackageTreeNode newSubNode = new PackageTreeNode();
            newSubNode.level = level + 1;
            newSubNode.nodeName = newNodeName;
            subNodes.add(newSubNode);
            return newSubNode.insert(newPackageName);
        }
    }

    //return value：root package name -> [sub package name in container including the root package name itself if it presents in container]
    public static Map<String, List<String>> extractRootPackages(CodeContainer container) {

        NameCollector names = NameAnalysis.analyzeNames(container);
        Set<String> packageNames = names.allPackageNames();

        //treeilize all package names
        List<PackageTreeNode> rootNodes = new ArrayList<>();
        for (String packageName : packageNames) {
            if (packageName.equals(PackageNode.Factory.DEFAULT_PACKAGE)) {
                PackageTreeNode newRootNode = new PackageTreeNode();
                newRootNode.level = 1;
                newRootNode.nodeName = PackageNode.Factory.DEFAULT_PACKAGE;
                newRootNode.subNodes = new ArrayList<>();
                newRootNode.subNodes.add(PackageTreeNode.CURRENT);
                rootNodes.add(newRootNode);
                continue;
            }

            boolean handled = false;
            for (PackageTreeNode node : rootNodes) {
                if (node.belong(packageName)) {
                    node.insert(packageName);
                    handled = true;
                }
            }

            if (! handled) {
                PackageTreeNode newRootNode = new PackageTreeNode();
                newRootNode.level = 1;
                newRootNode.nodeName = PackageTreeNode.topNodeName(packageName);
                newRootNode.insert(packageName);
                rootNodes.add(newRootNode);
            }
        }

        //find root packages from package trees
        Set<String> rootPackageNames = new HashSet<>();
        for (PackageTreeNode rootNode : rootNodes) {
            if (rootNode.subNodes == null)
                rootPackageNames.add(rootNode.nodeName);
            else {
                if (rootNode.subNodes.contains(PackageTreeNode.CURRENT)) {
                    rootPackageNames.add(rootNode.nodeName);
                }
                else {
                    for (PackageTreeNode subNode : rootNode.subNodes) {
                        String currentRootPackageName = rootNode.nodeName+"."+subNode.nodeName;
                        while (subNode.subNodes != null && subNode.subNodes.size() == 1 && ! subNode.subNodes.contains(PackageTreeNode.CURRENT)) {
                            subNode = subNode.subNodes.get(0);
                            currentRootPackageName += ".";
                            currentRootPackageName += subNode.nodeName;
                        }
                        rootPackageNames.add(currentRootPackageName);
                    }
                }
            }
        }

        //group package names into root packages
        Map<String, List<String>> rootPackageNameMap = new HashMap<>();
        for (String rootPackageName : rootPackageNames)
            rootPackageNameMap.put(rootPackageName, new ArrayList<String>());
        for (String packageName : packageNames) {
            boolean categorized = false;
            for (String rootPackageName : rootPackageNames) {
                if (rootPackageName.equals(packageName)
                        || PackageNameUtil.isParentPackageName(rootPackageName, packageName) > 0) {
                    rootPackageNameMap.get(rootPackageName).add(packageName);
                    categorized = true;
                    break;
                }
            }

            if (! categorized) {
                throw new RuntimeException("can not happen");
            }
        }

        return rootPackageNameMap;
    }
}
