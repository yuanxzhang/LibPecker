package cn.fudan.analysis.dep;

import cn.fudan.analysis.profile.ClassProfile;

/**
 * Created by lemonleaves on 2017/4/14.
 */
public class ConcreteDepNode extends DepNode {
    public ClassProfile classProfile;
    public int[][] countOfDepEdgePointToEachTypeOfClass; // DepEdge: 6 (DepEdgeType) * TypeOfClass: 3 (system/package/other)
    // 6 types: DEP_EDGE_EXTENDS, DEP_EDGE_IMPLEMENTS, DEP_EDGE_FIELD_IN, DEP_EDGE_METHOD_PARAMETER, DEP_EDGE_METHOD_RETURN, DEP_EDGE_METHOD_EXCEPTION

    public ConcreteDepNode(ClassProfile classProfile) {
        super(classProfile.getClassName());
        this.classProfile = classProfile;
        this.countOfDepEdgePointToEachTypeOfClass = new int[6][3];
    }

    public void addTypecount(int edgetype, int pointtype) {
        this.countOfDepEdgePointToEachTypeOfClass[edgetype][pointtype]++;
    }

    public int getCountByDepEdgeAndPointToType(int edgetype, int pointtype) {
        return this.countOfDepEdgePointToEachTypeOfClass[edgetype][pointtype];
    }

}