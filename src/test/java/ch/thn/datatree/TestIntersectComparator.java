package ch.thn.datatree;

import ch.thn.datatree.DataTreeUtil.TreeIntersectProcessor;
import ch.thn.datatree.core.CollectionTreeNodeInterface;

public class TestIntersectComparator<N1 extends CollectionTreeNodeInterface<?, N1>, 
									N2 extends CollectionTreeNodeInterface<?, N2>> 
				implements TreeIntersectProcessor<N1, N2, N1> {

	@Override
	public boolean equals(N1 masterNode, N2 slaveNode) {
		return masterNode.getNodeValue().toString().equals(slaveNode.getNodeValue().toString());
	}

	@Override
	public N1 intersectNode(N1 masterNode, N2 slaveNode) {
		//The intersect tree is a copy of the master tree
		return masterNode.nodeFactory(masterNode);
	}

}
