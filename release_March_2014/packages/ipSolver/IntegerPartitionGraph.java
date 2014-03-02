package ipSolver;

import general.IntegerPartition;

public class IntegerPartitionGraph
{
	public Node[][] nodes;	
	public int largestIntegerBeingSplitInThisGraph;
	
	//*****************************************************************************************************
	
	/**
	 * Constructor. Places the subspaces into an integer partition graph
	 */
	public IntegerPartitionGraph( Subspace[][] subspaces, int numOfAgents, int largestIntegerBeingSplitInThisGraph )
	{
		this.largestIntegerBeingSplitInThisGraph = largestIntegerBeingSplitInThisGraph;
		
		//create a node for each subspace
		nodes = new Node[ numOfAgents ][];
		for(int level=0; level<numOfAgents; level++){ //For each level
			nodes[level] = new Node[ subspaces[level].length ];
			for(int i=0; i<subspaces[level].length; i++){ //For each subspace in the current level
				nodes[level][i] = new Node( subspaces[level][i] );
			}
		}
		//Create the edges of the graph
		for(int level=0; level<numOfAgents - 1; level++){ //For each level
			for(int i=0; i<nodes[level].length; i++) //For each node in the current level
			{
				IntegerPartition[] listOfDirectlyConnectedIntegerPartitions = nodes[level][i].integerPartition.getListOfDirectedlyConnectedIntegerPartitions( largestIntegerBeingSplitInThisGraph, 0 );
				if( listOfDirectlyConnectedIntegerPartitions == null ){
					nodes[level][i].edgesFromThisNode = null;
				}else{
					nodes[level][i].edgesFromThisNode = new Edge[ listOfDirectlyConnectedIntegerPartitions.length ];
					int index=0;			
					for(int j=0; j<nodes[level+1].length; j++){ //For each node in the NEXT level
						int[] integersThatResultedFromTheSplit = getIntegersThatResultedFromTheSplit( nodes[level][i], listOfDirectlyConnectedIntegerPartitions, nodes[level+1][j], largestIntegerBeingSplitInThisGraph );
						if( integersThatResultedFromTheSplit != null )
						{
							int[] sortedParts1 = nodes[level  ][i].integerPartition.partsSortedAscendingly; 
							int[] sortedParts2 = nodes[level+1][j].integerPartition.partsSortedAscendingly;
							int partThatWasSplit= -1;
							for(int k=sortedParts1.length-1; k>=0; k--){ //compare the two arrays, STARTING FROM THE END
								if( sortedParts1[k] != sortedParts2[k+1]){
									partThatWasSplit = sortedParts1[k];
									break;
								}
							}
							nodes[level][i].edgesFromThisNode[ index ] = new Edge( nodes[level+1][j], partThatWasSplit, integersThatResultedFromTheSplit );												
							index++;
							if( index == nodes[level][i].edgesFromThisNode.length )
								break;
						}
					}
				}
			}
		}				
	}
	
	//*****************************************************************************************************
	
	/**
	 * Updates the edges, based on the fact that the current "largestIntegerBeingSplitInThisGraph" is greater than
	 * the previous "largestIntegerBeingSplitInThisGraph".
	 */
	public void updateEdges( int numOfAgents, int largestIntegerBeingSplitInThisGraph )
	{
		int prev_largestIntegerBeingSplitInThisGraph = this.largestIntegerBeingSplitInThisGraph;
		if( prev_largestIntegerBeingSplitInThisGraph >= largestIntegerBeingSplitInThisGraph )
			return;
		
		//Update the edges of the graph
		for(int level=1; level<numOfAgents - 1; level++){ //For each level STARTINNG FROM THE SECOND ONE!!!
			for(int i=0; i<nodes[level].length; i++) //For each node in the current level
			{
				IntegerPartition[] listOfDirectlyConnectedIntegerPartitions = nodes[level][i].integerPartition.getListOfDirectedlyConnectedIntegerPartitions( largestIntegerBeingSplitInThisGraph, prev_largestIntegerBeingSplitInThisGraph );
				if( listOfDirectlyConnectedIntegerPartitions != null )
				{
					//in this case, we need to ADD to the existing list of edges
					int index;
					if( nodes[level][i].edgesFromThisNode == null ){ //create a list of edges from scratch
						index = 0;
						nodes[level][i].edgesFromThisNode = new Edge[ listOfDirectlyConnectedIntegerPartitions.length ];
					}else{ //add to the existing list of edges
						index = nodes[level][i].edgesFromThisNode.length;
						Edge[] tempListOfEdges = new Edge[ nodes[level][i].edgesFromThisNode.length + listOfDirectlyConnectedIntegerPartitions.length ]; 
						for(int j=0; j<nodes[level][i].edgesFromThisNode.length; j++)
							tempListOfEdges[j] = nodes[level][i].edgesFromThisNode[j];
						nodes[level][i].edgesFromThisNode = tempListOfEdges;						
					}
					for(int j=0; j<nodes[level+1].length; j++){ //For each node in the NEXT level
						int[] integersThatResultedFromTheSplit = getIntegersThatResultedFromTheSplit( nodes[level][i], listOfDirectlyConnectedIntegerPartitions, nodes[level+1][j], largestIntegerBeingSplitInThisGraph ); 
						if( integersThatResultedFromTheSplit != null )
						{
							int[] sortedParts1 = nodes[level  ][i].integerPartition.partsSortedAscendingly; 
							int[] sortedParts2 = nodes[level+1][j].integerPartition.partsSortedAscendingly;
							int partThatWasSplit= -1;
							for(int k=sortedParts1.length-1; k>=0; k--){ //compare the two arrays, STARTING FROM THE END
								if( sortedParts1[k] != sortedParts2[k+1]){
									partThatWasSplit = sortedParts1[k];
									break;
								}
							}
							nodes[level][i].edgesFromThisNode[ index ] = new Edge( nodes[level+1][j], partThatWasSplit, integersThatResultedFromTheSplit );												
							index++;
							if( index == nodes[level][i].edgesFromThisNode.length )
								break;
						}
					}
				}
			}
		}
		this.largestIntegerBeingSplitInThisGraph = largestIntegerBeingSplitInThisGraph;
	}
	
	//*****************************************************************************************************
	
	/**
	 * Given two nodes that are at two consecutive levels, the method returns true
	 * iff there is an edge between the two nodes
	 */
	private int[] getIntegersThatResultedFromTheSplit( Node nodeOnLowLevel, IntegerPartition[] listOfDirectlyConnectedIntegerPartitions,
			Node nodeOnHighLevel, int largestIntegerBeingSplitInThisGraph )
	{
		int[] multiplicity1  = nodeOnHighLevel.integerPartition.sortedMultiplicity;
		int   underlyingSet1 = nodeOnHighLevel.integerPartition.sortedUnderlyingSetInBitFormat;
		
		for(int i=0; i<listOfDirectlyConnectedIntegerPartitions.length; i++)
		{
			int[] multiplicity2  = listOfDirectlyConnectedIntegerPartitions[i].sortedMultiplicity;
			int   underlyingSet2 = listOfDirectlyConnectedIntegerPartitions[i].sortedUnderlyingSetInBitFormat;

			if( underlyingSet1 != underlyingSet2 )
				continue;

			boolean notEqual = false;
			for(int j=0; j<multiplicity1.length; j++){
				if( multiplicity1[j] != multiplicity2[j] ){
					notEqual = true;
					break;
				}
			}
			if( notEqual )
				continue;
			
			return( listOfDirectlyConnectedIntegerPartitions[i].tempIntegersThatResultedFromASplit );
		}
		return( null );
	}
	
	//*****************************************************************************************************
	
	/**
	 * For every node in the graph, this methods determines whether it is reachable
	 * from the bottom node, and that is given the m parameter
	 */
	public void setReachabilityOfSubspaces( int m, int numOfAgents )
	{
		//For every node, initlialize its reachability from the bottom node
		for(int level=0; level<2; level++)
			for(int i=0; i<nodes[level].length; i++)
				nodes[level][i].subspace.isReachableFromBottomNode = true;
		for(int level = 2; level<numOfAgents; level++)
			for(int i=0; i<nodes[level].length; i++)
				nodes[level][i].subspace.isReachableFromBottomNode = false;

		//Delete the edges that split an integer greater than, or equal to, m
		for(int level = 1; level<numOfAgents-1; level++){
			for(int i=0; i<nodes[level].length; i++){
				Node curNode = nodes[level][i];
				if( curNode.edgesFromThisNode != null )
					for(int j=0; j<curNode.edgesFromThisNode.length; j++){
						if((curNode.edgesFromThisNode[j] != null )&&( curNode.edgesFromThisNode[j].partThatWasSplit >= m ))
							curNode.edgesFromThisNode[j]  = null;
					}
			}
		}
		//For every node, compute its reachability from the bottom node
		for(int level=1; level<numOfAgents-1; level++){
			for(int i=0; i<nodes[level].length; i++){
				Node curNode = nodes[level][i];
				if( curNode.subspace.isReachableFromBottomNode == false ){
					continue;
				}
				if( curNode.edgesFromThisNode != null )
					for(int j=0; j<curNode.edgesFromThisNode.length; j++){
						if( curNode.edgesFromThisNode[j] != null )
							curNode.edgesFromThisNode[j].node.subspace.isReachableFromBottomNode = true;					
					}
			}
		}
	}
	
	//*****************************************************************************************************
	
	/**
	 * Returns a list of nodes that are reachable from the given node. This, of course, only
	 * considers the edges that are currently present in this integer partition graph.
	 */
	public Node[] getReachableNodes( Node node )
	{
		if( node.edgesFromThisNode == null ) return( null );
		
		int numOfIntegersInNode = node.integerPartition.partsSortedAscendingly.length;

		//mark all nodes above "node" as unreachable
		node.tempIntegerRoots = null;
		for(int level=numOfIntegersInNode; level<nodes.length; level++) {
			for(int i=0; i<nodes[level].length; i++){
				nodes[level][i].tempFlag = false;				
				nodes[level][i].tempIntegerRoots = null;
			}
		}
		//mark all nodes that are "directly" connected to "node" as reachable
		for( int i=0; i<node.edgesFromThisNode.length; i++ ){
			node.edgesFromThisNode[i].node.tempFlag = true;
			setIntegerRoots( node, node.edgesFromThisNode[i].node, node.edgesFromThisNode[i].twoPartsThatResultedFromTheSplit, node.edgesFromThisNode[i].partThatWasSplit ); 
		}
		//continue to mark nodes as reachable...
		int numOfReachableNodes = 0;
		for(int level=numOfIntegersInNode; level<nodes.length-1; level++) {
			for(int i=0; i<nodes[level].length; i++){
				if( nodes[level][i].tempFlag ){
					numOfReachableNodes++;
					if( nodes[level][i].edgesFromThisNode != null ){
						for( int j=0; j<nodes[level][i].edgesFromThisNode.length; j++ ){
							if( nodes[level][i].edgesFromThisNode[j].node.tempFlag == false ){
								nodes[level][i].edgesFromThisNode[j].node.tempFlag = true;
								setIntegerRoots( nodes[level][i], nodes[level][i].edgesFromThisNode[j].node, nodes[level][i].edgesFromThisNode[j].twoPartsThatResultedFromTheSplit, nodes[level][i].edgesFromThisNode[j].partThatWasSplit );
							}
						}
					}
				}
			}
		}
		//Put the nodes that have "flag = true" in the list of reachable nodes
		int index=0;
		Node[] listOfReachableNodes = new Node[ numOfReachableNodes ];
		for(int level=numOfIntegersInNode; level<nodes.length-1; level++) {
			for(int i=0; i<nodes[level].length; i++){
				if( nodes[level][i].tempFlag ){
					listOfReachableNodes[index] = nodes[level][i];
					index++;
				}
			}
		}		
		return( listOfReachableNodes );
	}
	
	//*****************************************************************************************************
	
	/**
	 * for every integer "i", we keep track of its root, i.e., the integer that I kept splitting until I got "i"
	 */
	private void setIntegerRoots( Node lowerNode, Node upperNode, int[] twoPartsThatResultedFromTheSplit, int partThatWasSplit )
	{
		int[] upperIntegers = upperNode.integerPartition.partsSortedAscendingly;
		int[] lowerIntegers = lowerNode.integerPartition.partsSortedAscendingly;

		//initializate all roots to be equal to -1
		upperNode.tempIntegerRoots = new int[ upperIntegers.length ];
		for(int i=0; i < upperNode.tempIntegerRoots.length; i++)
			upperNode.tempIntegerRoots[i] = -1;

		if( lowerNode.tempIntegerRoots == null )
		{
			//set the root for every one of two parts that resulted from the split
			for(int k=0; k<twoPartsThatResultedFromTheSplit.length; k++)
				for(int j=0; j < upperIntegers.length; j++)
					if(( twoPartsThatResultedFromTheSplit[k] == upperIntegers[j] )&&( upperNode.tempIntegerRoots[j] == -1 )){
						upperNode.tempIntegerRoots[j] = partThatWasSplit;
						break;
					}
			//set the root for every other integer to be equal to the integer itself
			for(int j=0; j < upperIntegers.length; j++)
				if( upperNode.tempIntegerRoots[j] == -1 )
					upperNode.tempIntegerRoots[j] = upperIntegers[j];			
		}else{
			//Initialization
			int newRoot = -10;
			int indexOfNewRoot = -10;

			//get the new integer root		
			for(int i=0; i<lowerIntegers.length; i++)
				if( lowerIntegers[i] == partThatWasSplit ){
					indexOfNewRoot = i;
					newRoot = lowerNode.tempIntegerRoots[i];
				}

			//set the root of every integer except the two that resulted from the split
			for(int i=0; i < lowerNode.tempIntegerRoots.length; i++)
			{
				if( i == indexOfNewRoot ) continue;

				for(int j=0; j < upperIntegers.length; j++)
					if(( upperIntegers[j] == lowerIntegers[i] )&&( upperNode.tempIntegerRoots[j] == -1 )){
						upperNode.tempIntegerRoots[j] = lowerNode.tempIntegerRoots[i];
						break;
					}
			}
			//set the root for the two integers that resulted from the split
			for(int j=0; j < upperIntegers.length; j++)
				if( upperNode.tempIntegerRoots[j] == -1 )
					upperNode.tempIntegerRoots[j] =  newRoot;
		}
	}
}