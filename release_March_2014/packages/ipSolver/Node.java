package ipSolver;

import general.IntegerPartition;

/**
 * an object of a node in the integer partition graph
 */
public class Node
{
	public Subspace subspace;
	public IntegerPartition integerPartition;
	public Edge[] edgesFromThisNode;
	boolean tempFlag; //this is whenever the node needs to be marked for some reason
	public int[] tempIntegerRoots; //for every integer "i", I keep track of its root, i.e., the integer that I kept splitting until I got "i"

	public Node( Subspace subspace ){
		this.subspace = subspace;
		integerPartition = new IntegerPartition( subspace.integers );
		edgesFromThisNode = null;
	}
}
