package ipSolver;

/**
 * an object of an edge in the integer partition graph
 */
public class Edge
{
	public Node node;
	public int partThatWasSplit;
	public int[] twoPartsThatResultedFromTheSplit;
	
	public Edge( Node node, int partThatWasSplit, int[] twoPartsThatResultedFromTheSplit ){
		this.node = node;
		this.partThatWasSplit = partThatWasSplit;
		this.twoPartsThatResultedFromTheSplit = twoPartsThatResultedFromTheSplit;
	}

}
