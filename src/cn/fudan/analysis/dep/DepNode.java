package cn.fudan.analysis.dep;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class DepNode {
    public Set<DepEdge> edges;
    public Set<DepEdge> sinkedges;
    public String classname;

    public DepNode(String classname) {
        this.classname = classname;
        this.edges = new HashSet<>();
        this.sinkedges = new HashSet<>();
    }

    public void addDepEdge(DepEdge de) {
        this.edges.add(de);
    }

    public void addSinkEdge(DepEdge de) {
        this.sinkedges.add(de);
    }
}

