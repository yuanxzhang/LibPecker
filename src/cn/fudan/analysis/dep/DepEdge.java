package cn.fudan.analysis.dep;

import cn.fudan.analysis.profile.ClassNameProfile;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class DepEdge {
    public enum DepEdgeType {
        DEP_EDGE_EXTENDS, DEP_EDGE_IMPLEMENTS, DEP_EDGE_FIELD_IN, DEP_EDGE_METHOD_PARAMETER, DEP_EDGE_METHOD_RETURN, DEP_EDGE_METHOD_EXCEPTION
    }

    private DepEdgeType edgeType;
    private DepNode source;
    private DepNode sink;
    private int weight;
    private ClassNameProfile.ClassType classType; // respect to source

    public DepEdge(DepEdgeType edgeType, DepNode source, DepNode sink) {
        this.setEdgeType(edgeType);
        this.setSource(source);
        this.setSink(sink);
        this.setWeight(1);
    }

    public void printDepEdge() {
        System.out.println(this.getEdgeType().toString() + ": ->"
                + this.getSink().classname + "\tweight: " + this.getWeight());
    }

    public void printSinkDepEdge() {
        System.out.println(this.getEdgeType().toString() + ": <-"
                + this.getSource().classname + "\tweight: " + this.getWeight());
    }

    public ClassNameProfile.ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassNameProfile.ClassType classType) {
        this.classType = classType;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void addWeight() {
        this.weight++;
    }

    public DepNode getSource() {
        return source;
    }

    public void setSource(DepNode source) {
        this.source = source;
    }

    public DepNode getSink() {
        return sink;
    }

    public void setSink(DepNode sink) {
        this.sink = sink;
    }

    public DepEdgeType getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(DepEdgeType edgeType) {
        this.edgeType = edgeType;
    }
}
