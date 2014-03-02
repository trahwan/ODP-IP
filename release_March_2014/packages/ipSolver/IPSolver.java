package ipSolver;

import java.io.BufferedReader;
import java.io.FileReader;

import general.*;
import inputOutput.*;
import mainSolver.*;

public class IPSolver
{
	/**
	 * the IP algorithm
	 */
	public void solve( Input input, Output output, Result result )
	{
		//Initialization
		int numOfAgents=input.numOfAgents;
		result.initialize();
				
		//Find the best CS in the first & last Levels of the Integer Partition Graph
		searchFirstAndLastLevel( input, result );

		// Calculate the average of the values of coalitions of size: s=1,2,...,numOfAgents.
		// While doing this, search the second level of the integer partition graph. Also, if required, 
		// search the side subspaces of levels 3,4,...,numOfAgents-1 (e.g. the subspace [5,1,1,...,1])
		double[] avgValueForEachSize = new double[numOfAgents];
		MethodsForScanningTheInput.scanTheInputAndComputeAverage( input, output, result, avgValueForEachSize );
		
		// Calculate the highest value, 2nd highest, 3rd highest, and so on, as long as this is useful   
		// (e.g. given 15 agents, for the coalitions of size 4, we only need to calculate the highest
		// 3 values since it is not possible to have more than 3 fours in a single integer partition
		double[][] maxValueForEachSize = setMaxValueForEachSize( input );
		
		//Create the integer partition graph
		Subspace[][] subspaces = initSubspaces(avgValueForEachSize,maxValueForEachSize,input);
		if( input.solverName == SolverNames.ODPIP )
			result.ipIntegerPartitionGraph = new IntegerPartitionGraph( subspaces, input.numOfAgents, 1 );
		else
			result.ipIntegerPartitionGraph = new IntegerPartitionGraph( subspaces, input.numOfAgents, ((int)Math.floor(2*input.numOfAgents/(double)3)));
		
		result.totalNumOfExpansions = computeTotalNumOfExpansions( result );
		
		//create subspaces, and calculate their bounds, average values, and size. Also compute
		//the overall upper and lower bounds on the value of the optimal coalition structure
		long numOfRemainingSubspaces = IntegerPartition.getNumOfIntegerPartitions( numOfAgents );
		numOfRemainingSubspaces -= disableSubspacesThatWereSearchedWhileScanningTheInput( result, input );

		//Set the upper and lower bounds on the value of the optimal coalition structure
		setUpperAndLowerBoundsOnOptimalValue( input, result );

		//Disable subspaces based on their bounds
		numOfRemainingSubspaces -= disableSubspacesWithUBLowerThanTheHighestLB( input, output, result );
		
		//Print the size, upperBound, and lowerBound for each subspace
		output.printDetailsOfSubspaces( input, result );

		//If there are no more subspaces to be searched, then...
		if( numOfRemainingSubspaces==0 ){
			finalize( result, input, output );
			return;
		}
		//Initialize the table used in the technique called: "Branch and bound in opposite direction"
		result.idpSolver_whenRunning_ODPIP = null;		
		if( input.solverName == SolverNames.ODPIP ){
			result.idpSolver_whenRunning_ODPIP = new IDPSolver_whenRunning_ODPIP( input, result );
			result.idpSolver_whenRunning_ODPIP.initValueOfBestPartitionFound( input, maxValueForEachSize );
			result.idpSolver_whenRunning_ODPIP.start();
		}
		//Calculate the value which, if found, causes the search to stop:
		double acceptableValue = result.ipUpperBoundOnOptimalValue*input.acceptableRatio/100;

		if( input.pruneSubspaces == false ) acceptableValue = Double.MAX_VALUE;
		
		//Assign to each subspace a priority based on the subspace-selection function, and then sort them based on priorities
		Node[] sortedNodes = getListOfSortedNodes( subspaces, input, result );

		//Check whether the current LB of the optimal is within the acceptable bound...
		while( ((double)result.ipLowerBoundOnOptimalValue ) < acceptableValue )
		{
			//Update the integer partition graph based on the max size that was completed by IDP so far
			if( input.solverName == SolverNames.ODPIP ) result.ipIntegerPartitionGraph.updateEdges( input.numOfAgents, result.get_dpMaxSizeThatWasComputedSoFar() );

			//disable any subspaces that are reachable from the bottom node
			if( input.solverName == SolverNames.ODPIP ) numOfRemainingSubspaces -= disableSubspacesReachableFromBottomNode( input, result );

			//Select the subspace to be searched:
			Node nodeToBeSearched = getFirstEnabledNode( sortedNodes );
			if( nodeToBeSearched == null ) break;

			//if IP is helping DP, then we need to group certain integers (depending on the size for which DP has finished computing)\
			ElementOfMultiset[] subsetToBePutAtTheBeginning = getRelevantNodes( nodeToBeSearched, input, result, 1);
			
			//Place the best subset at the beginning of the node's subspace.integers.
			putSubsetAtTheBeginning( nodeToBeSearched, subsetToBePutAtTheBeginning, input );

			//Compute the upper bounds that are used in the branch-and-bound formula\
			double[] sumOfMax = computeSumOfMax_splitOneInteger( nodeToBeSearched, maxValueForEachSize, input, result );

			//search the subspace
			int numOfIntegersToSplit = 0;
			if(( input.solverName == SolverNames.ODPIP )&&( subsetToBePutAtTheBeginning != null ))
				numOfIntegersToSplit = nodeToBeSearched.subspace.integers.length - General.getCardinalityOfMultiset(subsetToBePutAtTheBeginning);
			numOfRemainingSubspaces -= nodeToBeSearched.subspace.search( input,output,result,acceptableValue,avgValueForEachSize,sumOfMax,numOfIntegersToSplit);
			
			//update the upper bound on the optimal CS, as well as the acceptable value
			setUpperAndLowerBoundsOnOptimalValue( input, result );
			acceptableValue = result.ipUpperBoundOnOptimalValue*input.acceptableRatio/100;
			if( input.pruneSubspaces == false ) acceptableValue = Double.MAX_VALUE;

			//update the lower bound on the optimal CS
			if( result.ipLowerBoundOnOptimalValue < result.get_ipValueOfBestCSFound() ){ 
				result.ipLowerBoundOnOptimalValue = result.get_ipValueOfBestCSFound() ;
				numOfRemainingSubspaces -= disableSubspacesWithUBLowerThanTheHighestLB( input, output, result );
			}			
			//If there are no more subspaces to be searched, or if we found a good enough value, then...
			if(( numOfRemainingSubspaces==0 )||
					(( input.solverName == SolverNames.ODPIP )&&( result.get_dpMaxSizeThatWasComputedSoFar() >= ((int)Math.floor(2*input.numOfAgents/(double)3)) ))){			
				break;
			}
		}
		//prepare final result of IP before terminating
		finalize( result, input, output );
		if( result.idpSolver_whenRunning_ODPIP != null ) result.idpSolver_whenRunning_ODPIP.setStop(true);
	}

	//*****************************************************************************************************

	/**
	 * Disable any subspaces that are reachable from the bottom node, using only the movements that have been evaluated by IDP so far.
	 * 
	 * Returns the number of subspaces that it pruned.
	 */
	private long disableSubspacesReachableFromBottomNode( Input input, Result result )
	{
		Node bottomNode = result.ipIntegerPartitionGraph.nodes[0][0];

		//get the list of nodes that are reachable from the bottom node.
		Node[] reachableNodes = result.ipIntegerPartitionGraph.getReachableNodes( bottomNode );
		
		//disable all those nodes
		int numOfDisabledNodes=0;
		if( reachableNodes != null )
			for(int i=0; i<reachableNodes.length; i++)
				if( reachableNodes[i].subspace.enabled ){
					reachableNodes[i].subspace.enabled = false;
					//System.out.println(input.problemID+" - &&&&&& IDP pruned the subspace "+General.convertArrayToString(reachableNodes[i].subspace.integersSortedAscendingly));
					numOfDisabledNodes++;
				}
		return( numOfDisabledNodes );
	}
	
	//*****************************************************************************************************
	
	/**
	 * Prepare the final result of IP before terminating
	 */
	private void finalize( Result result, Input input, Output output )
	{
		if( input.solverName == SolverNames.ODPIP ){
			if( result.get_dpBestCSFound() != null )
				result.updateIPSolution( result.get_dpBestCSFound(), result.get_dpValueOfBestCSFound());
		}
		result.ipTime = System.currentTimeMillis() - result.ipStartTime;
		output.emptyStringBufferContentsIntoOutputFiles(input);
		output.printFinalResultsOfIPToFiles( input, result );
	}

	//*****************************************************************************************************
	
	/**
	 * Finds the best out of the first and the last level of the Integer partition graph
	 */ 	
	private void searchFirstAndLastLevel( Input input , Result result )
	{
		int numOfAgents = input.numOfAgents;
		boolean CS1IsFeasible = true;
		boolean CS2IsFeasible = true;
		
		//Calculate the value of the coalition structure that is made of singletons
		int[][] CS1 = new int[numOfAgents][1];
		for(int i=0; i<=numOfAgents-1; i++)	
			CS1[i][0] = (int)(i+1);

		//Calculate the value of the coalition structure that contains the grand coalition
		int[][] CS2 = new int[1][numOfAgents];
		for(int i=0; i<=numOfAgents-1; i++)
			CS2[0][i] = (int)(i+1);

		if( input.feasibleCoalitions != null ){	
			//Check whether CS1 is feasible
			for(int i=0; i<CS1.length; i++){
				int curCoalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( CS1[i] );
				if( input.feasibleCoalitions.contains( curCoalitionInBitFormat ) == false ){
					CS1IsFeasible = false;
					break;
				}
			}		
			//Check whether CS2 is feasible
			for(int i=0; i<CS2.length; i++){
				int curCoalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( CS2[i] );
				if( input.feasibleCoalitions.contains( curCoalitionInBitFormat ) == false ){
					CS2IsFeasible = false;
					break;
				}
			}
		}
		//Compute the values of CS1 and CS2		
		double valueOfCS1 = input.getCoalitionStructureValue( CS1 );
		double valueOfCS2 = input.getCoalitionStructureValue( CS2 );

		//Compare the two values
		if(( (CS1IsFeasible)&&(CS2IsFeasible == false) )||( (CS1IsFeasible)&&(CS2IsFeasible)&&(valueOfCS1>=valueOfCS2) )) {
			result.updateIPSolution( CS1, valueOfCS1 );
		}
		if(( (CS2IsFeasible)&&(CS1IsFeasible == false) )||( (CS2IsFeasible)&&(CS1IsFeasible)&&(valueOfCS2>=valueOfCS1) )) {
			result.updateIPSolution( CS2, valueOfCS2 );
		}
	}

	//******************************************************************************************************

	/**
	 * create subspaces, and calculate their bounds, average values, and size. Also compute
	 * the overall upper and lower bounds on the value of the optimal coalition structure
	 */
	private Subspace[][] initSubspaces(double[] avgValueForEachSize,double[][] maxValueForEachSize,Input input)
	{
		//Generate all integer partitions 
    	int[][][] integers=IntegerPartition.getIntegerPartitions(input.numOfAgents,input.orderIntegerPartitionsAscendingly);
		
    	//Initialize all subspaces and compute the upper and lower bounds on the optimal value
    	Subspace[][] subspaces = new Subspace[ integers.length ][];
		for(int level=0; level<input.numOfAgents; level++)
		{
			subspaces[level]=new Subspace[ integers[level].length ];
			for(int i=0; i<integers[level].length; i++){
				subspaces[level][i] = new Subspace( integers[level][i], avgValueForEachSize, maxValueForEachSize, input );
			}
		}
		return( subspaces );
	}

	//******************************************************************************************************
	
	/**
	 * For any subspace "S", set "S.enabled" to "false" if it has been searched while scanning the input
	 */
	private long disableSubspacesThatWereSearchedWhileScanningTheInput( Result result, Input input )	
	{
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		long numOfSubspacesThatThisMethodHasDisabled = 0;
		for(int level=0; level<nodes.length; level++) {
			for(int i=0; i<nodes[level].length; i++)
			{
				Subspace curSubspace = nodes[level][i].subspace;
				//identify whether the current integer partition is of size "1" or "2" or "numOfAgents"
				if(( level==0 )||( level==1 )||( level==(input.numOfAgents-1) )) {
					if( curSubspace.enabled ) {
						curSubspace.enabled = false;
						numOfSubspacesThatThisMethodHasDisabled++;
					}
				}
			}
		}
		return( numOfSubspacesThatThisMethodHasDisabled );
	}

	//******************************************************************************************************
	
	/**
	 * Calculate the highest value, 2nd highest, 3rd highest, and so on, as long as this is useful
	 * (e.g. given 15 agents, for the coalitions of size 4, we only need to calculate the highest
	 * 3 values since it is not possible to have more than 3 fours in a single integer partition
	 */ 
	private double[][] setMaxValueForEachSize( Input input )
	{
		//Initialization
		int numOfAgents = input.numOfAgents;
		int[] numOfRequiredMaxValues = new int[numOfAgents];
		long[] numOfCoalitions = new long[numOfAgents];
		double[][] maxValue = new double[numOfAgents][];		
		for(int size=1; size<=numOfAgents; size++)
		{
			numOfRequiredMaxValues[size-1] = (int)Math.floor( numOfAgents/(double)size );
			numOfCoalitions[size-1] = Combinations.binomialCoefficient( numOfAgents, size );
			maxValue[ size-1 ] = new double[ numOfRequiredMaxValues[size-1] ];
			for(int i=0; i<maxValue[size-1].length; i++)
				maxValue[ size-1 ][i] = 0;
		}	
		final boolean constraintsExist;
		if( input.feasibleCoalitions == null )
			constraintsExist = false;
		else
			constraintsExist = true;

		for(int coalitionInBitFormat=(int)Math.pow(2,numOfAgents)-1; coalitionInBitFormat>0; coalitionInBitFormat--)
			if(( constraintsExist==false )||( input.feasibleCoalitions.contains( coalitionInBitFormat ) ))
				//if a coalition is feasible, and it's upperBound is greater than the smallest element in "curMax" then update "curMax"
			{
				int size = (int)Combinations.getSizeOfCombinationInBitFormat( coalitionInBitFormat, numOfAgents);
				double[] curMaxValue = maxValue[ size-1 ];
				int j=numOfRequiredMaxValues[size-1]-1;
				if( input.getCoalitionValue( coalitionInBitFormat ) > curMaxValue[ j ] )
				{
					while(( j>0 )&&( input.getCoalitionValue( coalitionInBitFormat ) > curMaxValue[ j-1 ] ))
					{
						curMaxValue[ j ] = curMaxValue[ j-1 ];
						j--;
					}
					curMaxValue[ j ] = input.getCoalitionValue( coalitionInBitFormat );
				}
			}
		return(maxValue);
	}

	//******************************************************************************************************	

	/**
	 * This method disables any subspaces with a UB lower that the value of the current best solution
	 * (i.e. lower than LB*), and returns the number of subspaces that this method has disabled
	 */
	private long disableSubspacesWithUBLowerThanTheHighestLB( Input input,Output output,Result result )
	{
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		if( input.pruneSubspaces == false ) return(0);
		
		long numOfSubspacesThatThisMethodHasDisabled = 0;
		
		for(int level=0; level<nodes.length; level++)
		{
			for(int i=0; i<nodes[level].length; i++)
			{
				Subspace curSubspace = nodes[level][i].subspace; 
				if( curSubspace.enabled == true )
				{
					//if( curSubspace.UB < result.ipLowerBoundOnOptimalValue )
					if( curSubspace.UB - result.ipLowerBoundOnOptimalValue <-0.00000000005 )
					{
						curSubspace.enabled = false;
						numOfSubspacesThatThisMethodHasDisabled += 1;
					}
				}
			}
		}		
		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
		return( numOfSubspacesThatThisMethodHasDisabled );
	}

	//******************************************************************************************************

	/**
	 * Assign to each node a priority based on the subspace-selection function, and
	 * then sort the nodes based on their priorities.
	 */
	private Node[] getListOfSortedNodes( Subspace[][] subspaces, Input input, Result result )
	{
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		
		//*******************************************
		//***                                     ***
		//***     Assign priorities to nodes      ***
		//***                                     ***
		//*******************************************		
		
		for(int level=0; level<nodes.length; level++){ //For each level
			for(int i=0; i<nodes[level].length; i++) //For each subspace in the current level
			{
				Subspace curSubspace = nodes[level][i].subspace;
				curSubspace.priority = curSubspace.UB;
				//curSubspace.priority = -1 * curSubspace.sizeOfSubspace;
				//curSubspace.priority = curSubspace.LB;
			}
		}

		//********************************************************
		//***                                                  ***
		//***     Sort subspaces based on their priorities     ***
		//***                                                  ***
		//********************************************************		

		//Initialization
		Node[] sortedNodes = new Node[ IntegerPartition.getNumOfIntegerPartitions( input.numOfAgents ) ];
		int k=0;
		for(int level=0; level<nodes.length; level++){ //For each level
			for(int i=0; i<nodes[level].length; i++){ //For each subspace in the current level
				sortedNodes[k] = nodes[level][i];
				k++;
			}
		}
		//Sorting
		for(int i=sortedNodes.length-1; i>=0; i--)	// start at the end of the array
		{
			int indexOfSmallestElement = i;	// (1) default value of the index the of smallest element.
			for(int j=i; j>=0; j--)	        // (2) loop from the end of unsorted zone to the beginning of the array.
			{
				//compare current element to smallest one
				if(( sortedNodes[j].subspace.priority < sortedNodes[indexOfSmallestElement].subspace.priority )
						||( (sortedNodes[j].subspace.priority == sortedNodes[indexOfSmallestElement].subspace.priority)
								&&( sortedNodes[j].subspace.UB < sortedNodes[indexOfSmallestElement].subspace.UB ) ))
					indexOfSmallestElement = j;	// if it's smaller, it becomes the new smallest
			}
			// swap the two values
			Node temp = sortedNodes[i];
			sortedNodes[i] = sortedNodes[indexOfSmallestElement];
			sortedNodes[indexOfSmallestElement] = temp;
		}
		return( sortedNodes );
	}

	//******************************************************************************************************

	/**
	 * Returns the first enabled subspace in the given list of subspaces
	 */
	private Node getFirstEnabledNode( Node[] sortedNodes )
	{
		for(int i=0; i<sortedNodes.length; i++)
		{
			if( sortedNodes[i].subspace.enabled ) return( sortedNodes[i] );
		}
		return(null);
	}
	
	//******************************************************************************************************	
	
	/**
	 * Read the sequence of integer partitions from a file. Here, you specify the path of the file, but that is
	 * only starting from the work space, e.g., if the file's name is "result.txt", and it is in a folder called:
	 * "experiments", which is located in the workspace, then "filePathAndName" would be: "experiments/result.txt"
	 */
	private int[][] readSequenceOfSubspacesFromFile( String filePathAndName, Input input )
	{
		int[][] sequenceOfIntegerPartitions = new int[ IntegerPartition.getNumOfIntegerPartitions( input.numOfAgents ) ][];
		try{
			BufferedReader bufferReader = new BufferedReader( new FileReader(filePathAndName) );
			String integerPartitionAsString,line;
			int i=0;
			while( true )
			{
				//Read the next integer partition from the file
				line = bufferReader.readLine();
				if( line==null ) break;
				integerPartitionAsString = line;
				
				//Compute the number of integers in the integer partition
				int sizeOfIntegerPartition=1;
				for(int k=1; k<integerPartitionAsString.length(); k++)
					if( integerPartitionAsString.charAt(k)==',' )
						sizeOfIntegerPartition++;
				
				//convert "integerPartitionAsString" from "String" to "int[]"				
				int[] integerPartition = new int[sizeOfIntegerPartition];
				int j=0;
				int k=1;
				do{
					//Get an integer from the integer partition
					String integerAsString="";
					do{
						integerAsString += integerPartitionAsString.charAt(k);
						k++;
					}
					while((integerPartitionAsString.charAt(k)!=',')&&(integerPartitionAsString.charAt(k)!=' ')&&(integerPartitionAsString.charAt(k)!=']'));
					
					//increase the index (that is scanning "integerPartitionAsString") by 2, to jump over ", "
					k += 2; 
					
					//Add the integer to "integerPartition"
					integerPartition[j] = (new Integer( integerAsString )).intValue();
					j++;
				}
				while( k < integerPartitionAsString.length() );
				
				//Store the integer partition in "sequenceOfIntegerPartitions"
				sequenceOfIntegerPartitions[i] = integerPartition; 
				i++;
			}
			bufferReader.close();
			return( sequenceOfIntegerPartitions );
		}
		catch (Exception e){
			System.out.println(e);
			return( null );
		}
	}

	//******************************************************************************************************
	
	/** 
	 * This method sets "result.ipUpperBoundOnOptimalValue" to the "initialValue", and searches through
	 * the upper bounds of the "enabled" subspaces to see if there is one that is greater than that value
	 */ 
	private void setUpperAndLowerBoundsOnOptimalValue( Input input, Result result )
	{
		result.ipUpperBoundOnOptimalValue = result.get_ipValueOfBestCSFound();
		result.ipLowerBoundOnOptimalValue = result.get_ipValueOfBestCSFound();

		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		for(int level=0; level<nodes.length; level++){
			for(int i=0; i<nodes[level].length; i++)
			{
				Subspace curSubspace = nodes[level][i].subspace;
				if( curSubspace.enabled )
				{
					if( result.ipUpperBoundOnOptimalValue < curSubspace.UB )
						result.ipUpperBoundOnOptimalValue = curSubspace.UB;

					if( result.ipLowerBoundOnOptimalValue < curSubspace.LB )
						result.ipLowerBoundOnOptimalValue = curSubspace.LB;
				}
			}
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound".
	 * Here, none of the values in f are used.
	 */
	private double[] computeSumOfMax_splitNoIntegers( int[] integers, double[][] maxValueForEachSize )
	{
		//Initialization
		double[] sumOfMax = new double[ integers.length + 1 ];
		
		//Compute the upper bounds of the last list
		sumOfMax[ integers.length ] = maxValueForEachSize[ integers[integers.length-1]-1 ][0];

		//Compute the upper bounds of the remaining lists
		int j=0;
		for( int i=integers.length-1; i>0; i-- )
		{
			if( integers[i-1] == integers[i] )
				j++;
			else
				j=0;					
			sumOfMax[ i ] = sumOfMax[ i+1 ] + maxValueForEachSize[ integers[i-1]-1 ][ j ];
		}
		return( sumOfMax );
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound".
	 * Here, none of the values in f are used.
	 */
	private double[] computeSumOfMax_usefInsteadOfv( int[] integers, double[][] maxValueForEachSize, Result result )
	{
		//Initialization
		double[] sumOfMax = new double[ integers.length + 1 ];
		
		//Compute the upper bounds of the last list
		sumOfMax[ integers.length ] = result.get_max_f( integers[integers.length-1]-1 );
		for( int i=integers.length-1; i>0; i-- )
			sumOfMax[ i ] = sumOfMax[ i+1 ] + result.get_max_f( integers[i-1]-1 );

		return( sumOfMax );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound".
	 * Here, none of the values in f are used.
	 */
	private double computeUpperBound( ElementOfMultiset[] integers, double[][] maxValueForEachSize )
	{
		double upperBound = 0;
		for(int i=0; i<integers.length; i++)
		{
			int j=0;
			for(int k=0; k<integers[i].repetition; k++){
				upperBound += maxValueForEachSize[ integers[i].element - 1 ][j];
				j++;
			}
		}
		return( upperBound );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound"
	 */
	private double[] computeSumOfMax_splitManyIntegers( Node node, double[][] maxValueForEachSize, Input input, ElementOfMultiset[] subsetToBePutAtTheBeginning, Result result )
	{
		if(( input.solverName == SolverNames.IP )||( node.subspace.relevantNodes == null ))
			return( computeSumOfMax_splitNoIntegers( node.subspace.integers, maxValueForEachSize ) );

		//Initialization
		int[] integers = node.subspace.integers;
		Node[] relevantNodes = node.subspace.relevantNodes;
		double[] sumOfMax = new double[ integers.length + 1 ];

		//Count the number of integers in the subset that will be put at the beginning, i.e., the subset containing the
		//integers that are common between this node, and every relevant node (i.e., the integers that will not be split)
		int numOfIntegersToBePutAtTheBeginning = 0;
		if( subsetToBePutAtTheBeginning != null )
			for(int i=0; i<subsetToBePutAtTheBeginning.length; i++)
				numOfIntegersToBePutAtTheBeginning += subsetToBePutAtTheBeginning[i].repetition;

		//For this node, and every relevant node, we are going to keep a copy of its integer partition
		int[][] tempIntegers = new int[ relevantNodes.length + 1 ][];
		int[][] tempIntegerRoots = new int[ relevantNodes.length + 1 ][];
		tempIntegers[0] = General.copyArray( node.integerPartition.partsSortedAscendingly );
		tempIntegerRoots[0] = General.copyArray( node.tempIntegerRoots );
		for(int i=1; i<tempIntegers.length; i++){
			tempIntegers[i] = General.copyArray( relevantNodes[i-1].integerPartition.partsSortedAscendingly );
			tempIntegerRoots[i] = General.copyArray( relevantNodes[i-1].tempIntegerRoots );
		}
		//Compute sumOfMax[1]...
		double maxUB = node.subspace.UB;
		for(int i=0; i< relevantNodes.length; i++){
			if( maxUB < relevantNodes[i].subspace.UB ){
				maxUB = relevantNodes[i].subspace.UB ;
			}
		}
		sumOfMax[1] = maxUB;
		int index = 1;

		//Compute sumOfMax[i] : i>1
		while( index < integers.length )
		{
			sumOfMax[ index+1 ] = Integer.MIN_VALUE;
			int curInteger = integers[ index-1 ];
			for(int i=0; i<tempIntegers.length; i++)
			{
				ElementOfMultiset[] set = null;
				ElementOfMultiset[] subset = null;
				if( index <= numOfIntegersToBePutAtTheBeginning ){
					subset = new ElementOfMultiset[1];
					subset[0] = new ElementOfMultiset( curInteger, 1);
				}else{
					set = getSetOfIntegersGivenTheirRoot( tempIntegers[i], tempIntegerRoots[i], curInteger );
					subset = getSubsetOfIntegersGivenTheirSum( set, curInteger );
					if( subset == null ){
						subset = new ElementOfMultiset[1];
						subset[0] = new ElementOfMultiset( curInteger, 1);
					}
				}
				if( i==0 ){
					removeSubset( node, subset, tempIntegers, tempIntegerRoots, i );
				}else{
					removeSubset( relevantNodes[i-1], subset, tempIntegers, tempIntegerRoots, i );
				}
				double[] tempSumOfMax = computeSumOfMax_splitNoIntegers( tempIntegers[i], maxValueForEachSize );
				if( sumOfMax[ index+1 ] < tempSumOfMax[1] )
					sumOfMax[ index+1 ] = tempSumOfMax[1] ;
			}
			//Compute the bounds using f, and see if this improves things...
			double[] sumOfMaxUsingf = computeSumOfMax_usefInsteadOfv( tempIntegers[0], maxValueForEachSize, result);
			if( sumOfMax[ index+1 ] > sumOfMaxUsingf[1] )
				sumOfMax[ index+1 ] = sumOfMaxUsingf[1] ;
			
			index++;
		}
		return( sumOfMax );
	}

	//******************************************************************************************************
	
	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound"
	 * IMPORTANT: this version assumes that there is ONLY ONE integer that will be split at the end
	 */
	private double[] computeSumOfMax_splitOneInteger( Node node, double[][] maxValueForEachSize, Input input, Result result )
	{
		if(( input.solverName == SolverNames.IP )||( node.subspace.relevantNodes == null ))
			return( computeSumOfMax_splitNoIntegers( node.subspace.integers, maxValueForEachSize ) );
			
		int[] integers = node.subspace.integers;
		double[] sumOfMax = new double[ integers.length + 1 ];
		
		//compute the maximum upper bound of this node and all relevant nodes
		double maxUB = node.subspace.UB;
		for(int i=0; i<node.subspace.relevantNodes.length; i++)
			if( maxUB < node.subspace.relevantNodes[i].subspace.UB )
				maxUB = node.subspace.relevantNodes[i].subspace.UB ;

		//Compute the upper bound of the last list
		sumOfMax[ integers.length ] = maxValueForEachSize[ integers[integers.length-1]-1 ][0] + (maxUB - node.subspace.UB);
		double max_f = result.get_max_f( integers[integers.length-1]-1 ); 
		if(( max_f != 0 )&&( sumOfMax[ integers.length ] > max_f ))
			sumOfMax[ integers.length ] = max_f ;
		
		sumOfMax[ integers.length-1 ] = sumOfMax[ integers.length ] + maxValueForEachSize[ integers[integers.length-2]-1 ][0];
		int k=2;

		//Compute the upper bounds of the remaining lists
		int x = integers.length-k;
		int j=0;
		for( int i=x; i>0; i-- )
		{
			if( integers[i-1] == integers[i] )
				j++;
			else
				j=0;					
			sumOfMax[ i ] = sumOfMax[ i+1 ] + maxValueForEachSize[ integers[i-1]-1 ][ j ];
			k++;
		}
		return( sumOfMax );
	}

	//******************************************************************************************************
	
	/**
	 * This method initializes sumOfMax, which is used in "Subspace.search_useBranchAndBound"
	 */
	private double[] computeSumOfMax_splitOneInteger_improveUsingf( Node node, double[][] maxValueForEachSize, Input input, Result result )
	{
		if(( input.solverName == SolverNames.IP )||( node.subspace.relevantNodes == null ))
			return( computeSumOfMax_splitNoIntegers( node.subspace.integers, maxValueForEachSize ) );

		//Initialization
		int[] integers = node.subspace.integers;
		double[] sumOfMax = new double[ integers.length + 1 ];
		
		//Basically, "sumOfMax[i]  =  arrayOfMax[i][0] + arrayOfMax[i][1] + ... "
		double[][] arrayOfMax = new double[ integers.length + 1 ][];
		for(int i=1; i<arrayOfMax.length; i++){
			arrayOfMax[i] = new double[ arrayOfMax.length - i ];			
		}
		//compute the maximum upper bound of this node and all relevant nodes
		double maxUB = node.subspace.UB;
		for(int i=0; i<node.subspace.relevantNodes.length; i++)
			if( maxUB < node.subspace.relevantNodes[i].subspace.UB )
				maxUB = node.subspace.relevantNodes[i].subspace.UB ;

		//Compute the upper bound of the last list
		sumOfMax[ integers.length ] = maxValueForEachSize[ integers[integers.length-1]-1 ][0] + (maxUB - node.subspace.UB);
		double max_f = result.get_max_f( integers[integers.length-1]-1 );
		if(( max_f != 0 )&&( sumOfMax[ integers.length ] > max_f ))
			sumOfMax[ integers.length ] = max_f ;
		sumOfMax[ integers.length-1 ] = sumOfMax[ integers.length ] + maxValueForEachSize[ integers[integers.length-2]-1 ][0];

		//Update arrayOfMax
		for(int i=1; i<arrayOfMax.length; i++)
			arrayOfMax[i][ arrayOfMax[i].length-1 ] = maxValueForEachSize[ integers[integers.length-1]-1 ][0] + (maxUB - node.subspace.UB);
		for(int i=1; i<arrayOfMax.length-1; i++)
			arrayOfMax[i][ arrayOfMax[i].length-2 ] = maxValueForEachSize[ integers[integers.length-2]-1 ][0];				

		//Compute the upper bounds of the remaining lists
		int k=2;
		int x = integers.length-k;
		int j=0;
		for( int i=x; i>0; i-- )
		{
			if( integers[i-1] == integers[i] )
				j++;
			else
				j=0;					
			sumOfMax[ i ] = sumOfMax[ i+1 ] + maxValueForEachSize[ integers[i-1]-1 ][ j ];
			
			//Update arrayOfMax
			for(int w=1; w<arrayOfMax.length-k; w++)
				arrayOfMax[w][ arrayOfMax[w].length-(k+1) ] = maxValueForEachSize[ integers[i-1]-1 ][ j ];
			k++;
		}
		//the technique below did not make any difference in performace, so, to be safe, I disabled it
		if( input.solverName == SolverNames.ODPIP )
			improveUpperBoundUsingf( input.numOfAgents, sumOfMax, arrayOfMax, maxValueForEachSize, integers, result );

		return( sumOfMax );
	}

	//******************************************************************************************************
	
	/**
	 * Try to reduce the upper bound using f.
	 * Example: Given [1,1,2,3],  max_v1+max_v1+max_v2  can be replaced with: max_f4.
	 */
	private void improveUpperBoundUsingf( int numOfAgents, double[] sumOfMax, double[][] arrayOfMax, double[][] maxValueForEachSize, int[] integers, Result result )
	{
		//******************************************************************************
		//****                                                                      ****
		//****   try to start from the first list, and head towards the last list   ****
		//****                                                                      ****
		//******************************************************************************

		//for each list "i"...
		for(int i=1; i<sumOfMax.length; i++) //try to reduce "sumOfMax[i]"
		{
			//Initialization
			int start = i;
			double newSumOfMax = 0;
			int[] numOfTimesWeComputedASizeRegularly = new int[ numOfAgents+1 ];
			for(int size = 0; size <= numOfAgents; size++)
				numOfTimesWeComputedASizeRegularly[ size ]=0;

			/* In the 1st loop, we group a few integers (from "start" to "end"), where "start" equals "i"
			 * In the 2nd loop, we group a few integers (from "start" to "end"), where "start" equals the.
			 * previous "end" plus 1. This is repeated until we reach the last integer...  */
			do{ 
				//every time we enter this loop, we have the same "i", but a new "start"
				int sumOfIntegers = 0;
				int bestSumOfIntegers = -1;
				double bestSavings = 0;
				int bestEnd = 0;
				boolean exitLoop = false;
				//Given the current "start", we want to find the best "end"
				for(int end = start; end < sumOfMax.length; end++)
				{	
					sumOfIntegers += integers[ end-1 ];
					if( sumOfIntegers > result.get_dpMaxSizeThatWasComputedSoFar() ){
						exitLoop = true;
						break;
					}
					//Compute the savings if we use: "result.max_f[ sumOfIntegers-1 ]" instead
					//of: "arrayOfMax[i][start]+...+arrayOfMax[i][end]"
					double savings = -1 * result.get_max_f( sumOfIntegers-1 );
					for(int j=start; j<=end; j++)
						savings += arrayOfMax[i][ j-i ];
					
					if( bestSavings < savings ){
						bestSavings = savings ;
						bestSumOfIntegers = sumOfIntegers;
						bestEnd = end;
					}	
				}
				//if even the best savings are 0, then the current start is useless...
				if( bestSavings < 0.0000000001 ){
					int curSize = integers[start-1];
					newSumOfMax += maxValueForEachSize[ curSize-1 ][ numOfTimesWeComputedASizeRegularly[curSize] ];
					numOfTimesWeComputedASizeRegularly[ curSize ]++;
					start++;
				}else{
					newSumOfMax += result.get_max_f( bestSumOfIntegers-1 );
					start = bestEnd+1;
				}
			}
			while( start < sumOfMax.length );
			
			if( sumOfMax[i] > newSumOfMax )
				sumOfMax[i] = newSumOfMax ;
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * select the subset of integers that will be put at the beginning. The remaining integers will be put at
	 * the end and will be searched using the f table, not the v table.
	 * 
	 * Here, you explicitly specify the number of integers to be placed at the end.
	 * 
	 * Returns the subset of integers that is common between the node and all reachable nodes.
	 */
	private ElementOfMultiset[] getRelevantNodes( Node node, Input input, Result result, int numOfIntegersToSplitAtTheEnd )
	{
		if( input.solverName == SolverNames.IP ){
			node.subspace.relevantNodes = null;
			return(null);
		}
		int numOfIntegersInNode = node.integerPartition.partsSortedAscendingly.length;

		//get the list of nodes that are reachable from the original node.
		Node[] reachableNodes = result.ipIntegerPartitionGraph.getReachableNodes( node );
		if( reachableNodes == null ){
			node.subspace.relevantNodes = null;
			return(null);
		}		
		//create an iterator that will iterate over subsets of the original integer partition
		SubsetsOfMultiset[] subsetIterators = new SubsetsOfMultiset[ numOfIntegersToSplitAtTheEnd ];
		for(int s=0; s<numOfIntegersToSplitAtTheEnd; s++)
			subsetIterators[s] = new SubsetsOfMultiset( node.integerPartition.sortedMultiset, numOfIntegersInNode - (s+1), false);

		//Initialization
		ElementOfMultiset[] bestSubset = null;
		long bestSavings =0;
		int bestNumOfRelevantNodes=0;

		for(int s=0; s<numOfIntegersToSplitAtTheEnd; s++)
		{
			//Iterate over all subsets of size: s+1
			ElementOfMultiset[] subset = subsetIterators[s].getNextSubset();
			while(subset != null)
			{
				long savings = 0;
				int numOfRelevantSubspaces = 0;
				//out of all nodes reachable from "node", find the ones that contain "subset"
				for(int i=0; i<reachableNodes.length; i++) {
					if(( reachableNodes[i].integerPartition.contains( subset ) )&&( reachableNodes[i].subspace.enabled )){
						savings += reachableNodes[i].subspace.totalNumOfExpansionsInSubspace;
						numOfRelevantSubspaces++;
					}
				}
				//update the best subset
				if( bestSavings < savings ){
					bestSavings = savings;
					bestSubset  = General.copyMultiset( subset );
					bestNumOfRelevantNodes = numOfRelevantSubspaces;
				}
				//Get the next subset
				subset = subsetIterators[s].getNextSubset();
			}
		}
		//get the list of relevant subspaces, based on the best subset
		int index=0;
		if( bestNumOfRelevantNodes == 0  ){
			node.subspace.relevantNodes = null;
			return(null);
		}else{
			node.subspace.relevantNodes = new Node[ bestNumOfRelevantNodes ];
			for(int i=0; i<reachableNodes.length; i++) {
				if(( reachableNodes[i].integerPartition.contains( bestSubset ) )&&( reachableNodes[i].subspace.enabled )){
					node.subspace.relevantNodes[ index ] = reachableNodes[i];
					index++;
				}
			}
			return( bestSubset );
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * select the subset of integers that will be put at the beginning. The remaining integers will be put at
	 * the end and will be searched using the f table, not the v table.
	 * 
	 * Here, the number of integers to be put at the end is not limited; it can be as large as needed
	 * 
	 * Returns the subset of integers that is common between the node and all reachable nodes.
	 */
	private ElementOfMultiset[] getRelevantNodes( Node node, Input input, Result result )
	{
		if( input.solverName == SolverNames.IP ){
			node.subspace.relevantNodes = null;
			return(null);
		}
		//get the list of nodes that are reachable from the original node.
		Node[] reachableNodes = result.ipIntegerPartitionGraph.getReachableNodes( node );
		if( reachableNodes == null ){
			node.subspace.relevantNodes = null;
			return(null);
		}
		//Count how many of those nodes are actually enabled
		int numOfEnabledReachableNodes = 0;
		for(int j=0; j<reachableNodes.length; j++)
			if( reachableNodes[j].subspace.enabled )
				numOfEnabledReachableNodes++;

		//Put all reachable, enabled, nodes in the list of relevant nodes
		if( numOfEnabledReachableNodes == 0){
			node.subspace.relevantNodes = null;
			return( null );
		}else{
			node.subspace.relevantNodes = new Node[ numOfEnabledReachableNodes ];
			int index=0;
			for(int j=0; j<reachableNodes.length; j++){
				if( reachableNodes[j].subspace.enabled ){
					node.subspace.relevantNodes[ index ] = reachableNodes[j];
					index++;
				}
			}
		}
		Node[] relevantNodes = node.subspace.relevantNodes;
		
		//Create a copy of the multiset representation of the integer partition of the node
		ElementOfMultiset[] copyOfMultiset = General.copyMultiset( node.integerPartition.sortedMultiset );
		
		//For every element in the multiset
		for(int i=0; i<copyOfMultiset.length; i++)
		{			
			//count the minimum number of times this element appears in a reachable node
			int curElement = copyOfMultiset[i].element;
			int minNumOfRepetitions = copyOfMultiset[i].repetition;
			for(int j=0; j<relevantNodes.length; j++)
			{
				boolean foundElement = false;
				ElementOfMultiset[] multiset = relevantNodes[j].integerPartition.sortedMultiset;
				for(int k=0; k<multiset.length; k++)
					if( multiset[k].element == curElement ){
						foundElement = true;
						if( minNumOfRepetitions > multiset[k].repetition )
							minNumOfRepetitions = multiset[k].repetition ;
						break;
					}
				if( foundElement == false ){
					minNumOfRepetitions = 0;
					break;
				}
			}
			//set the number of repetitions to be the minimum
			copyOfMultiset[i].repetition = minNumOfRepetitions;
		}
		//Get rid of any elements that are repeated 0 times, and return the remaining
		int counter=0;
		for(int i=0; i<copyOfMultiset.length; i++){
			if( copyOfMultiset[i].repetition > 0 ){
				counter++;
			}
		}
		if( counter == 0 ){
			return( null );
		}else{
			ElementOfMultiset[] subset = new ElementOfMultiset[ counter ];
			int index=0;
			for(int i=0; i<copyOfMultiset.length; i++)
				if( copyOfMultiset[i].repetition > 0 ){
					subset[index] = new ElementOfMultiset( copyOfMultiset[i].element, copyOfMultiset[i].repetition);
					index++;
				}
			return( subset );
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * Place the best subset at the beginning of the node's subspace.integers.
	 * Also, place it at the beginning of every relevant node's subspace.integers.
	 */
	private void putSubsetAtTheBeginning( Node node, ElementOfMultiset[] subset, Input input )
	{
		if(( subset == null )||( input.solverName == SolverNames.IP )) return;
		
		//Put all remaining integers (i.e., those not in the subset) in a set represented as a MULTISET 
		ElementOfMultiset[] remainingIntegers_multiset = General.copyMultiset( node.integerPartition.sortedMultiset );
		for(int i=0; i<subset.length; i++)
			for(int j=0; j<remainingIntegers_multiset.length; j++)
				if( remainingIntegers_multiset[j].element    == subset[i].element ){
					remainingIntegers_multiset[j].repetition -= subset[i].repetition;
					break;
				}
		//Count the total number of remaining integers (i.e., those that are not in the subset)
		int counter=0;
		for(int i=0; i<remainingIntegers_multiset.length; i++)
			counter += remainingIntegers_multiset[i].repetition;
			
		//Put all remaining integers (i.e., those not in the subset) in a set represented as an ARRAY.
		int[] remainingIntegers_array = new int[ counter ];
		int index = 0;
		for(int i=0; i<remainingIntegers_multiset.length; i++)
			for(int j=0; j<remainingIntegers_multiset[i].repetition; j++){
				remainingIntegers_array[ index ] = remainingIntegers_multiset[i].element;
				index++;
			}	
		//place the subset at the beginning, and the remaining integers at the end
		int[] newIntegers = new int[ node.subspace.integers.length ];
		int index1 = 0;
		int index2 = newIntegers.length-counter;		
		for(int i=0; i<node.subspace.integers.length; i++)
		{
			boolean found = false;		
			for(int j=0; j<remainingIntegers_array.length; j++){
				if( remainingIntegers_array[j] == node.subspace.integers[i] )
				{
					newIntegers[ index2 ] = node.subspace.integers[i];
					index2++;
					remainingIntegers_array[j] = -1;
					found = true;
					break;
				}
			}
			if( found == false ){
				newIntegers[ index1 ] = node.subspace.integers[i];
				index1++;
			}
		}
		node.subspace.integers = newIntegers;
	}
	
	//******************************************************************************************************
	
	/**
	 * Returns the maximum multiplicity of the integer in the node, and in every relevant node
	 */
	private int getMaximumMultiplicity( int integer, Node node )
	{
		//initialize by setting the maximum multiplicity to be within the node itself
		int maximumMultiplicity=0;
		for(int i=0; i<node.integerPartition.sortedMultiset.length; i++)
			if( integer == node.integerPartition.sortedMultiset[i].element )
				maximumMultiplicity = node.integerPartition.sortedMultiset[i].repetition;
		
		//Now, consider the relevant nodes
		for(int k=0; k<node.subspace.relevantNodes.length; k++ )
		{
			Node curNode = node.subspace.relevantNodes[k];
			for(int i=0; i<curNode.integerPartition.sortedMultiset.length; i++)
				if( integer == curNode.integerPartition.sortedMultiset[i].element )
					if( maximumMultiplicity < curNode.integerPartition.sortedMultiset[i].repetition )
						maximumMultiplicity = curNode.integerPartition.sortedMultiset[i].repetition ; 
		}
		return( maximumMultiplicity );
	}
	
	//******************************************************************************************************
	
	/**
	 * Given "sumOfIntegers", this method returns a subset of "mutliset" of which the sum equals "sumOfIntegers"
	 */
	private ElementOfMultiset[] getSubsetOfIntegersGivenTheirSum( ElementOfMultiset[] multiset, int sumOfIntegers )
	{
		if( multiset == null ) return( null );
		
		int maxSize = Math.min( sumOfIntegers, General.getCardinalityOfMultiset(multiset));
		
		ElementOfMultiset[] subset;
		for(int size=1; size<=maxSize; size++)
		{
			SubsetsOfMultiset subsetsOfMultiset = new SubsetsOfMultiset( multiset, size, false );
			subset = subsetsOfMultiset.getNextSubset();
			while(subset != null){
				int sum=0;
				for( int i=0; i<subset.length; i++ )
					sum += subset[i].element * subset[i].repetition;

				if( sum == sumOfIntegers )
					return( subset );
				else
					subset = subsetsOfMultiset.getNextSubset();
			}
		}
		return( null );
	}
	
	//************************************************************************************************
	
	/**
	 * Given "sumOfIntegers", this method returns a subset of "mutliset" of which the sum equals "sumOfIntegers"
	 */
	private ElementOfMultiset[] getSetOfIntegersGivenTheirRoot( int[] tempIntegers, int[] tempIntegerRoots, int root )
	{
		if( tempIntegerRoots == null ) return( null );
		
		//count the integer that whose root equals "root"
		int counter=0;
		for(int i=0; i<tempIntegerRoots.length; i++){
			if( tempIntegerRoots[i] == root ){
				counter++;
			}
		}
		//keep only those integers
		if( counter == 0 ){
			return( null );
		}else{
			int[] resultAsArray = new int[ counter ];
			int index=0;
			for(int i=0; i<tempIntegerRoots.length; i++){
				if( tempIntegerRoots[i] == root ){
					resultAsArray[index] = tempIntegers[i];
					index++;
				}
			}
			return( General.convertArrayToMultiset( resultAsArray ) );
		}
	}
	
	//************************************************************************************************
	
	/**
	 * remove the subset of integers from the node
	 */
	public static void removeSubset( Node node, ElementOfMultiset[] subset, int[][] tempIntegers, int[][] tempIntegerRoots, int e )
	{
		if( subset == null ) return;
		
		int[] integers = General.copyArray( tempIntegers[e] );
		int[] integerRoots = General.copyArray( tempIntegerRoots[e] );
		ElementOfMultiset[] tempSubset = General.copyMultiset( subset );
		
		for(int i=0; i<integers.length; i++){
			for(int j=0; j<tempSubset.length; j++){
				if(( integers[i] == tempSubset[j].element )&&( tempSubset[j].repetition > 0 )){
					integers[i] = -1;
					tempSubset[j].repetition--;
					break;
				}
			}
		}
		//count the elements that have not been deleted
		int counter=0;
		for(int i=0; i<integers.length; i++){
			if( integers[i] > -1 ){
				counter++;
			}
		}
		//Get rid of any elements that have been deleted
		if( counter == 0 ){
			tempIntegers[e] = integers;
			tempIntegerRoots[e] = integerRoots;
		}else{
			tempIntegers[e] = new int[ counter ];
			if( integerRoots != null )
				tempIntegerRoots[e] = new int[ counter ];
			else
				tempIntegerRoots[e] = null;
			int index=0;
			for(int i=0; i<integers.length; i++)
				if( integers[i] > -1 ){
					tempIntegers[e][index] = integers[i];
					if( integerRoots != null )
						tempIntegerRoots[e][index] = integerRoots[i];
					index++;
				}
		}
	}
	
	//************************************************************************************************
	
	/**
	 * Returns the total number of expansions in the search space, given the tree-like representation of different integer-partition-based subspaces
	 */
	public long computeTotalNumOfExpansions( Result result )
	{
		long totalNumOfExpansions = 0;
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		for(int level=0; level<nodes.length; level++)
		{
			for(int i=0; i<nodes[level].length; i++)
			{
				Subspace curSubspace = nodes[level][i].subspace; 
				totalNumOfExpansions += curSubspace.totalNumOfExpansionsInSubspace;
			}
		}
		return totalNumOfExpansions;		
	}
}