package ipSolver;

import java.util.Arrays;
import java.util.TreeSet;

import mainSolver.Result;
import inputOutput.Input;
import inputOutput.Output;
import inputOutput.SolverNames;
import general.Combinations;
import general.General;
import general.RandomPermutation;
import general.RandomSubsetOfGivenSet;

public class Subspace
{
	public long sizeOfSubspace;
	public long totalNumOfExpansionsInSubspace;
	public int[] integers;
	public int[] integersSortedAscendingly;
	public double UB,Avg,LB;
	public boolean enabled;
	public double priority;
	public long timeToSearchThisSubspace;
	public long numOfSearchedCoalitionsInThisSubspace;
	public double numOfSearchedCoalitionsInThisSubspace_confidenceInterval;
	public Node[] relevantNodes;
	public boolean isReachableFromBottomNode;
	
	//******************************************************************************************************

	/**
	 * the constructors
	 */
	public Subspace( int[] integers )
	{
		this.integers = integers;
		this.integersSortedAscendingly = General.sortArray( integers, true );
		this.sizeOfSubspace   = computeNumOfCSInSubspace( this.integers );
	}
	
	//******************************************************************************************************
	
	public Subspace( int[] integers, double[] avgValueForEachSize, double[][] maxValueForEachSize, Input input )
	{	
		this.integers = integers; //this.integers = orderParts( integers, avgValueForEachSize, maxValueForEachSize, input );
		this.integersSortedAscendingly = General.sortArray( integers, true );
		this.timeToSearchThisSubspace = 0;
		this.enabled = true;
		
		//compute the number of coalition structures in this subspace
		this.sizeOfSubspace   = computeNumOfCSInSubspace( this.integers );
		
		//compute the total number of expansions in this subspace. This number depends on the sorting of the integers
		this.totalNumOfExpansionsInSubspace = computeTotalNumOfExpansionsInSubspace( this.integers, input);
		
		//initialize the number of searched coalitions (for the subspaces of which the integer partition is of size: 1, 2, or "n"
		if( this.integers.length == 2 )
		{
			int size1 = this.integers[0]; int size2 = this.integers[1];
			int numOfCombinationsOfSize1 = (int) Combinations.binomialCoefficient( input.numOfAgents, size1 );
			int temp; 
			if( size1 != size2 ) 
				temp = numOfCombinationsOfSize1;
			else
				temp =(numOfCombinationsOfSize1/2);

			this.numOfSearchedCoalitionsInThisSubspace = 2*temp;;
		}
		else
			if(( this.integers.length == 1 )||( this.integers.length == input.numOfAgents ))
				this.numOfSearchedCoalitionsInThisSubspace = this.integers.length;
			else
				this.numOfSearchedCoalitionsInThisSubspace = 0;
		
		//Calculating UB:
		int j=0;				
		this.UB=0;
		for(int k=0; k<=this.integers.length-1; k++)
		{
			if(( k>0 )&&( this.integers[k] == this.integers[k-1] ))
				j++;
			else
				j=0;					
			this.UB += maxValueForEachSize[ this.integers[k]-1 ][ j ];
		}

		//Calculating Avg:
		this.Avg=0;
		for(int k=0; k<=this.integers.length-1; k++)
			this.Avg += avgValueForEachSize[ this.integers[k]-1 ];
		
		//Calculating LB:
		this.LB = this.Avg;
	}
	
	//******************************************************************************************************

	/**
	 * Search this subspace using the IP algorithm
	 * Returns the number of subspaces that were searched
	 */	
	public int search( Input input, Output output, Result result, double acceptableValue, double[] avgValueForEachSize, double[] sumOfMax, int numOfIntegersToSplit)
	{
		long timeBeforeSearchingThisSubspace = System.currentTimeMillis();

		if( input.solverName == SolverNames.ODPIP ){
			if( result.get_dpMaxSizeThatWasComputedSoFar() >= ((int)Math.floor(2*input.numOfAgents/(double)3)) ){
				this.enabled = false;
				return(1);
			}
		}
		if( input.printTheSubspaceThatIsCurrentlyBeingSearched )
			System.out.println( input.problemID+" - Searching "+General.convertArrayToString( integersSortedAscendingly )+"   -->   "+General.convertArrayToString( integers ) );
		
		if( input.useSamplingWhenSearchingSubspaces ){
			if( input.samplingDoneInGreedyFashion )
				searchUsingSamplingInGreedyFashion( input, output, result, acceptableValue );
			else
				searchUsingSampling( input, output, result );
			//System.out.println("I am sampling from the subspace: "+General.convertArrayToString(integers));
		}else
			search_useBranchAndBound( input, output, result, acceptableValue, sumOfMax, numOfIntegersToSplit);

		//If we are running IDP-IP, then we need to split the resulting CS in the optimal way
		if( input.solverName == SolverNames.ODPIP ){
			int[][] CS = result.idpSolver_whenRunning_ODPIP.dpSolver.getOptimalSplit( result.get_ipBestCSFound() );
			result.updateIPSolution(CS, input.getCoalitionStructureValue(CS));
		}
		timeToSearchThisSubspace = System.currentTimeMillis() - timeBeforeSearchingThisSubspace;
		output.printTimeRequiredForEachSubspace( this, input);
		this.enabled = false;
		int numOfSearchedSubspaces = 1;
		//disable every relevant subspace
		if(( input.solverName == SolverNames.ODPIP )&&( relevantNodes != null )){
			for(int i=0; i<relevantNodes.length; i++)
				if( relevantNodes[i].subspace.enabled ){
					relevantNodes[i].subspace.enabled = false;
					numOfSearchedSubspaces++;
					if( input.printTheSubspaceThatIsCurrentlyBeingSearched )
						System.out.println(input.problemID+" - ****** with DP's help, IP avoided the subspace: "+Arrays.toString(relevantNodes[i].integerPartition.partsSortedAscendingly));
				}
		}
		return( numOfSearchedSubspaces );
	}
	
	//******************************************************************************************************
	
	private void searchUsingSampling( Input input, Output output, Result result )
	{
		//Checking the special case where the size of the coalition structure is either "1" or "numOfAgents"
		if(( integers.length == 1 )||( integers.length == input.numOfAgents )) {
			searchFirstOrLastLevel( integers,input,output,result );
			return;
		}		
		//Initialization
		int numOfSamples = Math.min( 100000, (int)Math.round(sizeOfSubspace*0.1));
		RandomPermutation randomPermutation = new RandomPermutation( input.numOfAgents );
		
		//for every sample
		for(int i=0; i<numOfSamples; i++)
		{
			int[] permutation = randomPermutation.get();
			double coalitionStructureValue=0;
			int[] coalitionStructure = new int[integers.length];
			int indexInPermutation=0;
			//for every size in the integer partition
			for(int j=integers.length-1; j>=0; j--)
			{
				int size = integers[j];
				int currentCoalition = 0;
				//Compute the current coalition
				for(int k=0; k<size; k++){					
					currentCoalition += ( 1 << (permutation[ indexInPermutation ]) ); //add agent "permutation[ indexInPermutation ]" to "currentCoalition"
					indexInPermutation++;
				}
				coalitionStructure[j] = currentCoalition;
				
				//update the value of the current coalition structure
				coalitionStructureValue += input.getCoalitionValue( currentCoalition );
				//coalitionStructureValue += localBranchAndBoundObject.getLowerBoundForCurrentAgents( currentCoalition );
			}				
			//If this value is greater than the best value found so far
			if( result.get_ipValueOfBestCSFound() < coalitionStructureValue )
			{
				int[][] CSInByteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat( coalitionStructure, input.numOfAgents, integers);
				result.updateIPSolution( CSInByteFormat, coalitionStructureValue);
			}
		}
		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
	}
	
	//******************************************************************************************************
	
	private void searchUsingSamplingInGreedyFashion( Input input, Output output, Result result, double acceptableValue )
	{
		//Checking the special case where the size of the coalition structure is either "1" or "numOfAgents"
		if(( integers.length == 1 )||( integers.length == input.numOfAgents )) {
			searchFirstOrLastLevel( integers,input,output,result );
			return;
		}
		//e.g., numOfRemainingAgents[i] is the number of agents that did not appear in coalitions: CS[0],...,CS[i-1]
		int[] numOfRemainingAgents = new int[ integers.length ];
		numOfRemainingAgents[0] = input.numOfAgents;
		for(int j=1; j<integers.length; j++)
			numOfRemainingAgents[j] = numOfRemainingAgents[j-1] - integers[j-1];

		//Compute the number of possible coalitions, given the size, and the number of remaining agents
		long[] numOfPossibleCoalitions = new long[ integers.length ];
		for(int j=0; j<integers.length; j++)
			numOfPossibleCoalitions[j] = Combinations.binomialCoefficient( numOfRemainingAgents[j], integers[j]);
		
		//While constructing a coalition structure (CS), the algorithm will take a
		//number of samples of every coalition size, and select the best one seen. 
		int[] numOfCoalitionSamplesPerSizePerCS = new int[ integers.length ];
		for(int j=0; j<integers.length; j++)
			numOfCoalitionSamplesPerSizePerCS[j] = Math.min( 100000, (int)Math.ceil( numOfPossibleCoalitions[j] * 0.1 ) );
		
		//The number of coalition structures (CS) that the algorithm will construct
		int numOfCoalitionStructureSamples = Math.min( 10000, (int)Math.ceil( sizeOfSubspace * 0.1 ));

		//This is the coalition structure that will be constructed by the algorithm
		int[] CS = new int[ integers.length ];
		
		//For every CS that the algorithm will construct
		for(int i=0; i<numOfCoalitionStructureSamples; i++)
		{			
			//initialize the set of remaining agents to be the grand coalition
			int setOfRemainingAgentsInBitFormat = (1 << input.numOfAgents) - 1;
			int[] setOfRemainingAgentsInByteFormat = Combinations.convertCombinationFromBitToByteFormat( setOfRemainingAgentsInBitFormat, input.numOfAgents);
			
			for(int j=0; j<integers.length - 1; j++) //For every size
			{
				RandomSubsetOfGivenSet samplingObject = new RandomSubsetOfGivenSet( setOfRemainingAgentsInByteFormat, numOfRemainingAgents[j], integers[j]);
				double bestValueOfCurrentSize = Double.MIN_VALUE;				
				//For every sample of the current size
				for(int k=0; k < numOfCoalitionSamplesPerSizePerCS[j]; k++){
					int C = samplingObject.getSubsetInBitFormat();
					//Update the best coalition seen of the current size
					double currentCoalitionValue = input.getCoalitionValue( C );
					if( bestValueOfCurrentSize < currentCoalitionValue ){
						bestValueOfCurrentSize = currentCoalitionValue ;
						CS[j] = C;
					}
				}
				//the set of agents not in coalitions: CS[0],...,CS[i-1]
				setOfRemainingAgentsInBitFormat -= CS[j];
				setOfRemainingAgentsInByteFormat = Combinations.convertCombinationFromBitToByteFormat( setOfRemainingAgentsInBitFormat, input.numOfAgents, numOfRemainingAgents[j]);
			}
			//Add the last coalition in CS
			CS[ integers.length - 1 ] = setOfRemainingAgentsInBitFormat;
			
			//Update the currest best CS found
			if( result.get_ipValueOfBestCSFound() < input.getCoalitionStructureValue(CS) ){
				int[][] CSInByteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat(CS,input.numOfAgents,integers);
				result.updateIPSolution( CSInByteFormat, input.getCoalitionStructureValue(CS));
				if( result.get_ipValueOfBestCSFound() >= acceptableValue ){ //If the value is within the acceptable ratio				{
					output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
					return;
				}
			}
		}
		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
	}
	
	//******************************************************************************************************
	
	/**
	 * Order the parts of the integer partition. Here, I did it such that the first part is the one that has the
	 * maximum difference between its upper bound and its average. This ended up exactly as the descending order.
	 */
	public int[] orderParts( int[] integers, double[] avgValueForEachSize, double[][] maxValueForEachSize, Input input )
	{
		//Get (1) the underlying set of the integer partition, and (2) the multiplicity of each part.
		int[] underlyingSet = General.getUnderlyingSet( integers );
		int[] multiplicity = new int[ input.numOfAgents ];
		for(int i=0; i<input.numOfAgents; i++)
			multiplicity[i] = 0;
		for(int i=0; i<integers.length; i++)
			multiplicity[ integers[i]-1 ]++;

		//Initialize the new ordering of the underlying set
		int[] newUnderlyingSet = new int[ underlyingSet.length ];
		for(int i=0; i<newUnderlyingSet.length; i++){
			newUnderlyingSet[i] = 0;
		}
		//Compute the new ordering of the underlying set
		for(int i=0; i<newUnderlyingSet.length; i++)
		{
			double biggestDifference=-1;
			int sizeWithMaxDifference=-1;
			for(int j=0; j<underlyingSet.length; j++)
			{
				int curSize = underlyingSet[j];

				//if we already put this size in the list "newOrderingOfSizes", then continue skip
				boolean sizeAlreadyAddedToList = false;
				for(int k=0; k<i; k++)
					if( newUnderlyingSet[k] == curSize ){
						sizeAlreadyAddedToList = true;
						break;
					}
				if( sizeAlreadyAddedToList ) continue;

				//Check if the current size has the biggest difference between Max and Avg
				if( biggestDifference < (maxValueForEachSize[ curSize-1 ][0] - avgValueForEachSize[ curSize-1 ]) ){
					biggestDifference = (maxValueForEachSize[ curSize-1 ][0] - avgValueForEachSize[ curSize-1 ]) ;
					sizeWithMaxDifference = curSize;
				}			
			}
			newUnderlyingSet[i] = sizeWithMaxDifference;
		}
		//constructing the new Integer partition
		int[] newIntegers = new int[ integers.length ];
		int index=0;
		for(int i=0; i<newUnderlyingSet.length; i++){
			int curSize = newUnderlyingSet[i];
			for(int j=0; j<multiplicity[ curSize-1 ]; j++){
				newIntegers[ index ] = curSize;
				index++;
			}
		}
		return( newIntegers );
	}

	//******************************************************************************************************

	/**
	 * Computes the size of the subspace (i.e., the number of coalition structures in it).
	 * I HIGHLY RECOMMEND reading the comments that are at the beginning of the method: "search_usingBranchAndBound".
	 */ 
	public long computeNumOfCSInSubspace( int[] integers )
	{
		//Calculate the number of agents from the given integers...
		int numOfAgents=0;
		for(int i=0; i<integers.length; i++) numOfAgents += integers[i];

		//Check the special case where the size of the integer partition equals 1 or "numOfAgents"
		if(( integers.length==1 )||( integers.length==numOfAgents ))
			return( 1 );

		//Initialization
		int[]     length_of_A               =  init_length_of_A( numOfAgents, integers );
		int[]     max_first_member_of_M     =  init_max_first_member_of_M( integers, length_of_A, false );
		long[][]  numOfCombinations         =  init_numOfCombinations( integers, length_of_A, max_first_member_of_M );
		long[]    sumOf_numOfCombinations   =  init_sumOf_numOfCombinations( numOfCombinations, integers, length_of_A, max_first_member_of_M );
		long[]    numOfRemovedCombinations  =  init_numOfRemovedCombinations( integers, length_of_A, max_first_member_of_M);
		long[][]  increment                 =  init_increment( integers, numOfCombinations, sumOf_numOfCombinations, max_first_member_of_M, false);

		//Calculating size of the subspace, i.e., the total number of coalition structures in it:
		long sizeOfSubspace = 0;
		if( numOfRemovedCombinations[0]==0 ) { //then the list has a single increment value
			sizeOfSubspace = increment[0][0]*sumOf_numOfCombinations[0];
		}
		else { //then the list has different increment values
			for(int i=0; i<=max_first_member_of_M[0]-1; i++)
				sizeOfSubspace += increment[0][i]*numOfCombinations[0][i] ;
		}		
		return( sizeOfSubspace );
		
		 // An alternative way for computing the number of CS in a subspace (by using the equation). However, for
		 // large numbers of agents, the factorial method might overflow. Therefore, we do not use the equation.
/*		
		int[] underlyingSet = General.getUnderlyingSet(integers);

		long num1 = Combinations.binomialCoefficient( numOfAgents , integers[0] );
	    int x=numOfAgents;
	    for(int j=1; j<=integers.length-2; j++)
	    {
	        x=(int)(x-integers[j-1]);
	        num1 = num1 * Combinations.binomialCoefficient( x, integers[j] );
	    }
	    long num2=1;
	    for(int j=0; j<=underlyingSet.length-1; j++)
	        num2 = num2 * General.factorial( General.multiplicity(underlyingSet[j],integers) );

	    return( (long)num1/num2 );
 */  }	

	//******************************************************************************************************

	/**
	 * Computes the total number of expansions in each search space. Observe that this differs
	 * according to whether the integer partition is ordered ascendingly or descendingly.
	 * This method is thoroughly tested, and is guaranteed to work.
	 */
	public static long computeTotalNumOfExpansionsInSubspace( int[] integers, Input input)
	{
		//Compute the number of agents
		int numOfAgents = 0;
		for( int i=0; i<integers.length; i++)
			numOfAgents += integers[i];
		
		//Sort the parts in the integer partition
		int[] sortedIntegers = General.sortArray( integers, input.orderIntegerPartitionsAscendingly );

		int[] alpha = new int[ sortedIntegers.length ];
		long[][] gamma = new long[ sortedIntegers.length ][];
		for( int j=0; j<sortedIntegers.length; j++ )
		{
			//compute the maximum index (in the integer partition) at which the current integer appears
			int maxIndex=0;
			for( int k=0; k<sortedIntegers.length; k++)
				if( sortedIntegers[k] == sortedIntegers[j] )
					maxIndex = k;
			
			//compute alpha for the current integer
			alpha[j] = numOfAgents + 1;
			for( int k=0; k<=maxIndex; k++)
				alpha[j] -= sortedIntegers[k];
			
			//compute gamma for the current integer
			gamma[j] = new long[alpha[j]];
			for( int beta=0; beta<alpha[j]; beta++ )
			{
				int sumOfPreviousIntegers = 0;
				for( int k=0; k<j; k++)
					sumOfPreviousIntegers += sortedIntegers[k];
				
				if( j==0 )
					gamma[j][beta] = Combinations.binomialCoefficient( numOfAgents-sumOfPreviousIntegers-(beta+1) , sortedIntegers[j]-1 );
				else{
					int lambda;
					if( sortedIntegers[j] == sortedIntegers[j-1] )
						lambda = beta;
					else
						lambda = alpha[j-1] - 1;

					long sum = 0;
					for( int k=0; k<=lambda; k++ )
						sum += gamma[j-1][k];
					gamma[j][beta] = sum * Combinations.binomialCoefficient( numOfAgents-sumOfPreviousIntegers-(beta+1) , sortedIntegers[j]-1 );
				}
			}
		}
		long numOfExpansionsInSubspace=0;
		for( int j=0; j<sortedIntegers.length; j++ )
			for( int beta=0; beta<alpha[j]; beta++ )
				numOfExpansionsInSubspace += gamma[j][beta];
		
		return( numOfExpansionsInSubspace );
	}

	//******************************************************************************************************

	/**
		//Description of the main variables used (it is HIGHLY RECOMMENDED to read these comments before reading the method).
		//For more detail, see Section 4.3, and figure 3 in the paper: "Near-optimal anytime coalition structure generation"
		/*_______________________________________________________________________________________________________________________________
		 *                           |
		 * length_of_A[i]:           |  A coalition structure consists of a number of coalitions (e.g. c[1],c[2],...).
		 *                           |  length_of_A[i] represents THE NUMBER of agents from which we can select members of c[i]
		 *                           |
		 *                           |  (In figure 3, "length_of_A" contains the values: 7,5,3)
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     |   
		 * A[i]:                     |  A coalition structure consists of a number of coalitions (e.g. c[1],c[2],...).
		 *                           |  A[i] represents THE SET of agents from which we can select members of c[i].
		 *                           |  Note:
		 *                           |      Every time you update c[i], you would have to update A[j]:j>i
		 *                           |
		 *                           |  (In figure 3, "A" contains the values: {1,2,3,4,5,6,7}, {2,3,4,5,7}, {2,3,7} )
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     |
		 * M[i]:                     |  (In figure 3, "M" contains the values: {1,6},{3,4},{1,2,3})
 		 * 		                     |___________________________________________________________________________________________________
		 * 		                     | 
		 * max_first_member_of_M[i]  |  Represents the maximum value that the first member of "M[i]" can take.
		 *                           |
		 *                           |  (In figure 3, "max_first_member_of_M" contains the values: 4,4,1)  
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     | 
		 * index_of_M[i]	         |  The index of "M" in the list of potential combinations for "M"
		 *                           | 
		 *                           |  (In figure 3, "index_of_M" contains the values: 17,3,1)
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     | 
		 * numOfCombinations:        |  Represents the number of possible combinations for M[s]
		 *                           |
		 *                           |  Note(1)
		 *                   		 |      If( integers[s] != integers[s+1] ), then numOfCombinations[s] would actually
		 *                           |      be the number of possible combinations of size integers[s] out of the set A[s]. 
		 *                   		 |  Note(2)
		 *               			 |      If( integers[s] == integers[s+1] ), then the set of possible coalitions would 
		 *                           |       be A SUBSET of the possible combinations of size integers[s] out of the set A[s].
		 *                           |  
		 *                           |      In more detail, it would be those coalitions containing 1, and those not containing
		 *                           |      1 but containing 2, and those not containing 1,2 but containing 3, and so on, until
		 *                           |      those not containing: 1,...,(length_of_A[s+1]-integers[s+1]+1)-1, but containing:
		 *                           |      (length_of_A[s+1]-integers[s+1]+1).
		 *                           |
		 *                           |      numOfCombinations[s][0] contains the number of coalitions containing 1.
		 *                           |      numOfCombinations[s][1] contains the number of coalitions not containing 1, but containing 2
		 *                           |      numOfCombinations[s][2] contains the number of coalitions not containing 1,2, but containing 3
		 *                           |      and so on...
		 *                           |
		 *                           |  (In figure 3, "numOfCombinations[0]" contains the values: 6,5,4,3
		 *                           |                "numOfCombinations[1]" contains the values: 4,3,2,1
		 *                           |                "numOfCombinations[2]" contains the value : 1       ) 
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     |
		 * sumOf_numOfCombinations[i]|  Contains the sum of: numOfCombinations[i][j] : j = 0,1,2,...
		 *                           |
		 *                           |  (In figure 3, "sumOf_numOfCombinations" contains the values: 18,10,1)
		 * 		                     |___________________________________________________________________________________________________
		 * 		                     | 
		 * numOfRemovedCombinations: |  As mentioned above:
		 *                           |     if( integers[s] != integers[s+1] ) then the set of possible combinations for M[s] 
		 *                           |     would be the set of possible combinations of size integers[s] out of the set A[s]. In
		 *                           |     this case, "numOfRemovedCombinations" would be 0.
		 *                           |
		 *                           |     if( integers[s] == integers[s+1] ) then the set of possible combinaitons for M[s] 
		 *                           |     would only be a subset of that set. In this case "numOfRemovedCombinations" would be the size of
		 *                           |     the set minus the size of the subset.
		 *                           |
		 *                           |  (In figure 3, "numOfRemovedCombinations" contains the values: 3,0,0)
		 *                           |___________________________________________________________________________________________________
		 * 		                     |  
		 * indexToStartAt[i]         | The index at which M[i] starts. It equals: sumOf_numOfCombinations[i] + numOfRemovedCombinations[i]
		 * 		                     |
		 *                           |  (In figure 3, "indexToStartAt" contains the values: 21, 10, 1)   
		 *                           |___________________________________________________________________________________________________
		 * 		                     |  
		 * indexToStopAt[i]          | The index at which M[i] stops. It equals: numOfRemovedCombinations[i] + 1 
		 * 		                     |
		 *                           |  (In figure 3, "indexToStopAt" contains the values: 4, 1, 1)   
		 *                           |___________________________________________________________________________________________________
		 * 		                     |  
		 * CS:                       |  CS[i][j] = A[ M[i][j] ]. In other words, "CS" represents the actual coalition,
		 *                           |  while "M" only represents indices that point to the actual agents
		 *                           | 
		 *                           |  (In figure 3, "CS" contains the values: {1,6},{4,5},{2,3,7})
		 * 		                     |___________________________________________________________________________________________________
		 *                           |
		 * increment[s]:             |  Every time we move up one step in the list of combinations for M[s], we would have finished
		 *                           |  scanning a particular number of coalition structures. This number is "increment[s]"
		 *                           |
		 *                           |  Note that if( integers[s] == integers[s+1] ), then this number would not be equal
		 *                           |  for every combination in the list.
		 *                           |
		 *                           |  (In figure 3, increment[0] contains the values: 10,6,3,1
		 *                           |                increment[1] contains the value : 1
		 *                           |                increment[2] contains the value : 1)
		 * 		                     |___________________________________________________________________________________________________
		 *                           |
		 * sumOf_values              |  Every time we calculate the value of a coalition structure, we sum the values of its constituent
		 *                           |  coalitions.
		 *                           |
		 *                           |  Now, due to the way we scan the possible CS, every time we move M[s] one step,
		 *                           |  (M[i] : i>s) must be updated, while (M[j] : j<s) remain unchanged. This implies that: (CS[i] : i>s)
		 *                           |  must be updated, while (CS[j] : j<s) remains unchanged. Therefore, if we knew the summation of the
		 *                           |  values of coalitions (CS[j] : j<s), then we would only need to add to it the value of coalition
		 *                           |  CS[s], plus the values of coalitions (CS[i] : i>s).
		 *                           |   
		 *                           |  sumOf_values[1] = the value of the first coalition (i.e. v(CS[0]))
		 *                           |  sumOf_values[2] = the sum of the values of the first 2 coalitions (i.e. v(CS[0]) + v(CS[1]))
		 *                           |  and so on...
		 *                           |
		 *                           |  IMPORTANT NOTE:
		 *                           |      sumOf_values actually contains: "numOfAgents+1" values, where sumOf_values[0] is always equal
		 *                           |      to 0. This way, the value at location i would be: sumOf_values[i], and not: sumOf_values[i-1],
		 *                           |      which makes accessing the value slightly faster. Moreover, we can now say:
		 *                           |      sumOf_values[i] = sumOf_values[i-1] + getCoalitionValue(...), even if i=1, because we know that
		 *                           |      sumOf_values[0] will always be equal to zero.
		 * 		                     |___________________________________________________________________________________________________
		 *                           |
		 * sumOf_agents              |  sumOf_agents[s] = the agents that belong to coalitions (CS[j] : j<=s), represented as a bit string
		 *                           |__________________________________________________________________________________________________
		 *                           |
		 * sumOf_parts               |  sumOf_parts[s] = the sum of the following parts in the integer partition: 0, 1, ..., s
		 *                           |
		 *                           |  (In figure 3, "sumOf_parts" contains the values: 2, 4, 7
		 *___________________________|__________________________________________________________________________________________________
		 */

	private void search_useBranchAndBound( Input input,Output output,Result result,double acceptableValue,double[] sumOfMax, int numOfIntegersToSplit)
	{
		//Checking the special case where the size of the coalition structure is either "1" or "numOfAgents"
		if(( integers.length == 1 )||( integers.length == input.numOfAgents )) {
			searchFirstOrLastLevel( integers,input,output,result );
			return;
		}
		//Initialization
		final int numOfIntegers = integers.length;
		final long ipNumOfSearchedCoalitions_beforeSearchingThisSubspace  = result.ipNumOfExpansions;
		final int numOfAgents = input.numOfAgents;
		final int numOfIntsToSplit = numOfIntegersToSplit;
		final boolean ipUsesBranchAndBound = input.useBranchAndBound;
		final boolean constraintsExist;
		boolean this_CS_is_useless;
		double valueOfCS=0;
		if( input.feasibleCoalitions == null ) constraintsExist = false;
		else constraintsExist = true;
		final boolean ODPIsHelpingIP;
		if( input.solverName == SolverNames.ODPIP ) ODPIsHelpingIP = true;
		else ODPIsHelpingIP = false;

		
		//Initialization (in the right order)			
		final int[]     bit                      =  init_bit( numOfAgents );
		final int[]     length_of_A              =  init_length_of_A( numOfAgents, integers );
		final int[]     max_first_member_of_M    =  init_max_first_member_of_M( integers,length_of_A, ODPIsHelpingIP );
		final long[][]  numOfCombinations        =  init_numOfCombinations( integers, length_of_A, max_first_member_of_M );
		final long[]    sumOf_numOfCombinations  =  init_sumOf_numOfCombinations( numOfCombinations, integers, length_of_A, max_first_member_of_M );
		final long[]    numOfRemovedCombinations =  init_numOfRemovedCombinations( integers, length_of_A, max_first_member_of_M);
		final long[][]  increment                =  init_increment( integers, numOfCombinations, sumOf_numOfCombinations, max_first_member_of_M, ODPIsHelpingIP);
		final long[]    indexToStartAt           =  init_indexToStartAt(numOfIntegers, numOfRemovedCombinations, sumOf_numOfCombinations);
		final long[]    indexToStopAt            =  init_indexToStopAt(numOfIntegers, numOfRemovedCombinations);
		long[]    index_of_M   =  init_index_of_M( 1,integers,increment,max_first_member_of_M,numOfCombinations,numOfRemovedCombinations,sumOf_numOfCombinations);
		int[][]   M            =  init_M( index_of_M, integers, length_of_A, numOfAgents );
		int[][]   A            =  init_A( numOfAgents, integers, M, length_of_A );
		int[]     CS           =  init_CS_hashTableVersion( M, A, length_of_A, bit, numOfIntegers );
		int[]     sumOf_agents =  init_sumOf_agents_hashTableVersion( numOfIntegers, CS );
		double[]  sumOf_values =  init_sumOf_values_hashTableVersion( numOfIntegers, CS, input );
		result.ipNumOfExpansions  += integers.length-2;
		IDPSolver_whenRunning_ODPIP localBranchAndBoundObject = result.idpSolver_whenRunning_ODPIP;

		main_loop: while(true)
		{
			//In the following loop, we fix the coalitions 1,2,...,numOfIntegers-3, and try all
			//the possibilities for the last two coalitions in the integer partition.
			do{ 
				setTheLastTwoCoalitionsInCS( CS, M[numOfIntegers-2], A, numOfIntegers, bit);

				//If there are constraints, then check them on the last two coalitions
				this_CS_is_useless = false;
				if(( constraintsExist )&&( checkIfLastTwoCoalitionsSatisfyConstraints( CS,input.feasibleCoalitions )==false ))
					this_CS_is_useless = true;

				//If the current coalition structure satisfies the constraints, then...
				if( this_CS_is_useless == false )
				{
					//Calculate the value of the current coalition structure
					switch( numOfIntsToSplit ){
					case 0:  valueOfCS = sumOf_values[ numOfIntegers-2 ] + input.getCoalitionValue(CS[numOfIntegers-2])    + input.getCoalitionValue(CS[numOfIntegers-1]); break;
					case 1:  valueOfCS = sumOf_values[ numOfIntegers-2 ] + input.getCoalitionValue(CS[numOfIntegers-2])    + localBranchAndBoundObject.getValueOfBestPartitionFound( CS[numOfIntegers-1] ); break;
					default: valueOfCS = sumOf_values[ numOfIntegers-2 ] + localBranchAndBoundObject.getValueOfBestPartitionFound(CS[numOfIntegers-2]) + localBranchAndBoundObject.getValueOfBestPartitionFound( CS[numOfIntegers-1] );
					}
					//If this value is greater than the best value found so far
					if( result.get_ipValueOfBestCSFound() < valueOfCS )
					{
						int[][] CSInByteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat(CS,numOfAgents,integers);
						result.updateIPSolution( CSInByteFormat, valueOfCS);

						if( result.get_ipValueOfBestCSFound() >= acceptableValue ) //If the value is within the acceptable ratio
						{
							output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
							numOfSearchedCoalitionsInThisSubspace = result.ipNumOfExpansions - ipNumOfSearchedCoalitions_beforeSearchingThisSubspace;
							return;
						}
					}
				}
				index_of_M[ numOfIntegers-2 ]--;
				Combinations.getPreviousCombination( length_of_A[numOfIntegers-2], integers[numOfIntegers-2], M[numOfIntegers-2]);
			}
			while( index_of_M[ numOfIntegers-2 ] >= indexToStopAt [ numOfIntegers-2 ] );

			/* In the following loop, we keep changing the coalitions 1,2,...,numOfIntegers-3 until we find
			 * a combinations of them that is potentially useful (i.e., until the sum of their values, plus
			 * the maximum possible value of the remaining coalitions, is greater than the best value found so far)
			 */
			int s1 = numOfIntegers-3;
			sub_loop: while(s1>=0)
			{
				if( index_of_M[s1] > indexToStopAt[s1] ) //If you have NOT finished scannig this column:
				{
					if( s1==0 ){
						output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
						if( ODPIsHelpingIP ){
							if( result.get_dpMaxSizeThatWasComputedSoFar() >= ((int)Math.floor(2*input.numOfAgents/(double)3)) ){  
								numOfSearchedCoalitionsInThisSubspace = result.ipNumOfExpansions - ipNumOfSearchedCoalitions_beforeSearchingThisSubspace;
								return;						
							}
						}
					}
					for(int s2=s1; s2<=numOfIntegers-3; s2++)
					{
						boolean firstTime = true;
						do{
							result.ipNumOfExpansions++;  //update the number of searched coalitions

							if(( firstTime )&&( s2 > s1 ) ) {
								//Set the index of M to be the last index in the column
								set_M_and_index_of_M( M, index_of_M, length_of_A, indexToStartAt, s2 );
								firstTime = false;
							}else{
								//move M one step upwards, and update the index of M
								Combinations.getPreviousCombination(length_of_A[s2], integers[s2], M[s2]);
								index_of_M[s2]--;								
							}
							//Set CS, given the new M
							int temp3=0;
							for(int j1=integers[s2]-1; j1>=0; j1--)
								temp3 |= bit[ A[ s2 ][ M[ s2 ][j1]-1 ] ];
							CS[ s2 ]=temp3;

							//If there exists constraints that we need to deal with, then check them
							this_CS_is_useless=false;
							if( constraintsExist ){
								if( input.feasibleCoalitions.contains(new Integer(CS[s2])) == false )
									this_CS_is_useless = true;
							}
							//If all the possible values that can be found by fixing this coalition (along
							//with the ones before it) can never be greater than the value we found so far, then...
							if(( ipUsesBranchAndBound )&&( this_CS_is_useless == false ))
							{
								//Recalculate sumOf_values[s2+1] and sumOf_agents[s2+1], given the new CS
								int newCoalition = CS[s2];
								double valueOfNewCoalition;
								if( s2 >= numOfIntegers-numOfIntsToSplit ){
									valueOfNewCoalition = localBranchAndBoundObject.getValueOfBestPartitionFound( CS[s2] );
								}else{
									valueOfNewCoalition = input.getCoalitionValue( CS[s2] );
								}
								sumOf_values[s2+1] = sumOf_values[s2] + valueOfNewCoalition;
								sumOf_agents[s2+1] = sumOf_agents[s2] + CS[s2];
								
								//Use branch and bound to see if the current coalitions have potential
								double upperBoundForRemainingAgents = sumOfMax[s2+2];
								//if( ( (sumOf_values[s2+1]+upperBoundForRemainingAgents) <= result.get_ipValueOfBestCSFound() )
								if( ( (sumOf_values[s2+1]+upperBoundForRemainingAgents) - result.get_ipValueOfBestCSFound() <-0.00000000005)
										||( (ODPIsHelpingIP) && (useLocalBranchAndBound( input,localBranchAndBoundObject,sumOf_values,sumOf_agents,s2,newCoalition,valueOfNewCoalition)) ))
									this_CS_is_useless = true;
							}
							//If the current combination of coalitions has been found useless...	
							if( this_CS_is_useless == false ) break;	
						}
						while( index_of_M[s2] > indexToStopAt[s2] );

						if( this_CS_is_useless ) { //If we kept moving upwards, until we reached the top:
							s1 = s2-1;
							continue sub_loop;
						}
						update_A( A[s2+1], A[s2], length_of_A[s2], M[s2], integers[s2]);
					}
					//Set M, given its new index
					int s2 = numOfIntegers-2;
					set_M_and_index_of_M( M, index_of_M, length_of_A, indexToStartAt, s2 );
					
					continue main_loop;
				}
				s1--;
			}
			break main_loop;
		}
		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
		numOfSearchedCoalitionsInThisSubspace = result.ipNumOfExpansions - ipNumOfSearchedCoalitions_beforeSearchingThisSubspace;
	}
	
	//******************************************************************************************************
	
	/**
	 * This method is called for both the integer partition of size 1, and that of size numOfAgents. This is because
	 * each of the corresponding subspaces contain a single coalition structure, and searching it can be done instantly
	 */
	private void searchFirstOrLastLevel(int[] integers, Input input, Output output, Result result)
	{
		//Initialization
		int numOfAgents = input.numOfAgents; int[][] curCS;
		
		if( integers.length==1 )
		{
			curCS = new int[1][numOfAgents];
			for(int i=0; i<=(int)(numOfAgents-1); i++)
				curCS[0][i]=(int)(i+1);
		}
		else {
			curCS = new int[numOfAgents][1];
			for(int i=0; i<=numOfAgents-1; i++)
				curCS[i][0]=(int)(i+1);
		}
		double valueOfCurCS = input.getCoalitionStructureValue( curCS );

		result.updateIPSolution( curCS, valueOfCurCS);

		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );
	}
	
	//******************************************************************************************************

	/**
	 * Check if the last two coalitions in the coalition structure satisfy the constraints
	 * (i.e., chech whether they appear in the list of feasible coalitions).
	 */
	private boolean checkIfLastTwoCoalitionsSatisfyConstraints( int[] CS, TreeSet<Integer> feasibleCoalitions)
	{
		if( feasibleCoalitions.contains(new Integer(CS[CS.length-1])) == false )
			return(false);
		if( feasibleCoalitions.contains(new Integer(CS[CS.length-2])) == false )
			return(false);
		return(true);
	}
	
	//******************************************************************************************************
	
    /**
     * bit[i] is the bit representing agent a_i (e.g. given 4 agents, bit[2]=2=0010, bit[3]=4=0100, etc.)
     */
	private int[] init_bit( int numOfAgents )
	{
		int[] bit = new int[numOfAgents+1];
		for(int i=0; i<numOfAgents; i++)
			bit[i+1] = 1 << i;
		return( bit );
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes indexToStopAt. For more details see the comments at the beginning of "search_useBranchAndBound"
	 */ 
	private long[] init_indexToStartAt( int numOfIntegers, long[] numOfRemovedCombinations, long[] sumOf_numOfCombinations)
	{
		long[] indexToStartAt = new long[ numOfIntegers ];
		for(int i=0; i<numOfIntegers; i++)
		{
			indexToStartAt[i] = sumOf_numOfCombinations[i] + numOfRemovedCombinations[i];
		}
		return( indexToStartAt );
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes indexToStopAt. For more details see the comments at the beginning of "search_useBranchAndBound"
	 */	
	private long[] init_indexToStopAt( int numOfIntegers, long[] numOfRemovedCombinations )
	{
		long[] indexToStopAt  = new long[ numOfIntegers ];
		for(int i=0; i<numOfIntegers; i++)
		{
			indexToStopAt[i] = numOfRemovedCombinations[i] + 1;
		}
		return( indexToStopAt );
	}
	
	//******************************************************************************************************	
	
	/**
	 * This method initializes "init_max_first_member_of_M". For more details see the comments at the beginning of "search_useBranchAndBound"
	 */
	private int[] init_max_first_member_of_M( int[] integers, int[] length_of_A, boolean ipUsesLocalBranchAndBound )
	{
		int[] max_first_member_of_M = new int[ integers.length ];
		int i=integers.length-1;		
		
		if(( ipUsesLocalBranchAndBound )&&( relevantNodes != null )&&( integers.length > 2 )){
			max_first_member_of_M[i] = (int)( length_of_A[i]-integers[i]+1 );
			i--;
		}
		while(i>=0)
		{
			max_first_member_of_M[i] = (int)( length_of_A[i]-integers[i]+1 );
			i--;
			while(( i>=0 )&&( integers[i]==integers[i+1] ))
			{
				max_first_member_of_M[i] = max_first_member_of_M[i+1];
				i--;
			}
		}
		return( max_first_member_of_M );
	}
	
	//******************************************************************************************************	
	
	/**
	 * This method initializes "numOfCombinatinos". For more details see the comments at the beginning of "search_useBranchAndBound"
	 */
	private long[][] init_numOfCombinations( int[] integers, int[] length_of_A, int[] max_first_member_of_M)
	{	
		long[][] numOfCombinations = new long[ integers.length ][];
		for(int i=0; i<=integers.length-1; i++ )
		{
			if( length_of_A[i]==integers[i] )
			{
				numOfCombinations[i]=new long[1];
				numOfCombinations[i][0]=1;
			}
			else
			{
				numOfCombinations[i] = new long[max_first_member_of_M[i]];
				for(int j=0; j<=max_first_member_of_M[i]-1; j++)
				{
					numOfCombinations[i][j] = Combinations.binomialCoefficient( (int)(length_of_A[i]-(j+1)) , (int)(integers[i]-1) );
				}
			}
		}
		return( numOfCombinations );
	}
	
	//******************************************************************************************************	
	
	/**
	 * This method initializes "sumOf_numOfCombinations". For more details see the comments at the beginning of "search_useBranchAndBound"
	 */
	private long[] init_sumOf_numOfCombinations( long[][] numOfCombinations, int[] integers, int[] length_of_A, int[] max_first_member_of_M)
	{	
		long[] sumOf_numOfCombinations = new long[ integers.length ];
		
		for(int i=0; i<=integers.length-1; i++ )
		{
			if( length_of_A[i]==integers[i] )
			{
				sumOf_numOfCombinations[i]=1;
			}
			else
			{
				sumOf_numOfCombinations[i]=0;
				for(int j=0; j<=max_first_member_of_M[i]-1; j++)
				{
					sumOf_numOfCombinations[i] = sumOf_numOfCombinations[i] + numOfCombinations[i][j];
				}
			}
		}
		return( sumOf_numOfCombinations );
	}
	
	//******************************************************************************************************	

	/**
	 * This method initializes: numOfRemovedCombinations.
	 * (For more details on: "numOfRemovedCombinations", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private long[] init_numOfRemovedCombinations( int[] integers, int[] length_of_A, int[] max_first_member_of_M)
	{	
		long[] numOfRemovedCombinations = new long[ integers.length ];
		
		for(int i=0; i<=integers.length-1; i++ )
		{
			if( length_of_A[i]==integers[i] )
			{
				numOfRemovedCombinations[i]=0;
			}
			else
			{
				numOfRemovedCombinations[i]=0;
				for(int j=max_first_member_of_M[i]; j<=length_of_A[i]-integers[i]; j++)
				{
					numOfRemovedCombinations[i] = numOfRemovedCombinations[i] + Combinations.binomialCoefficient( (int)(length_of_A[i]-(j+1)) , (int)(integers[i]-1) );
				}
			}
		}
		return( numOfRemovedCombinations );
	}	
	
	//******************************************************************************************************
	
	/**
	 * This method initializes increment
	 * (For more details on: "increment", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private long[][] init_increment( int[] integers, long[][] numOfCombinations, long[] sumOf_numOfCombinations, int[] max_first_member_of_M, boolean ipUsesLocalBranchAndBound )
	{
		long[][] increment = new long[ integers.length ][];
		increment[ integers.length-1 ] = new long[1];
		increment[ integers.length-1 ][0]=1;
		
		int s=integers.length-2;
		while(s>=0) //make sure to define s as integer
		{
			if(( integers[s]!=integers[s+1] )||( (ipUsesLocalBranchAndBound) && (s==integers.length-2) && (integers.length>2) ))					
			{
				//For the possible combinations of M[s], the increment would be equal to the number of possible
				//combinations for M[s+1], multiplied by the increment of each of these combinations.				
				increment[s]=new long[1];
				increment[s][0]=sumOf_numOfCombinations[s+1] * increment[s+1][0];
				s--;
			}
			else
			{
				/* Calculate the increment for the possible combinations of M[s].
				 * 
				 * Here, we calculate the increment for the combinations that start with j, where: j = 1,2,...,
				 * max_first_member_of_M[s]), although they will all have the same increment, which is the number
				 * of possible combinations of M[s+1], multiplied by the increment of each of these combinations.
				 */
				increment[s]=new long[max_first_member_of_M[s]];
				for(int i=0; i<=max_first_member_of_M[s]-1; i++)
				{					
					increment[s][i]=0;
					for(int j=i; j<=max_first_member_of_M[s]-1; j++)
						increment[s][i] += ( numOfCombinations[s+1][j] * increment[s+1][ 0 ] );
				}
				s--;
				
				/* If there exists (M[i] : i<s) such that (M[i]=M[i+1]), then calculate the increment for the
				 * possible combinations of M[i].
				 * 
				 * For the possible combinations of M[i] that start with j (j=1,2,...,max_first_member_of_M[i]),
				 * the increment is equal to the number of possible combinations of M[i+1] that start with j, 
				 * multiplied by the increment of each of these combinations.
				 */
				while(( s>=0 )&&( integers[s]==integers[s+1] ))
				{
					increment[s]=new long[max_first_member_of_M[s]];
					for(int i=0; i<=max_first_member_of_M[s]-1; i++)
					{
						increment[s][i]=0;
						for(int j=i; j<=max_first_member_of_M[s]-1; j++)
							increment[s][i] += ( numOfCombinations[s+1][j] * increment[s+1][ j ] );
					}
					s--;
				}
				
				/* Now, knowing that M[s]!=M[s+1], we calculate the increment for the possible combinations of M[s]
				 * 
				 * Note that these would all have the same increment, which is the summation of the following:
				 *    the number of possible combinations for M[s+1] that start with j, multiplied by the
				 *    increment of each of these combinations, where: j=1,2,...,max_first_member_of_M[s+1]
				 */ 
				if( s>=0 )
				{
					increment[s]=new long[1];
					increment[s][0]=0;
					for(int j=0; j<=max_first_member_of_M[s+1]-1; j++)
						increment[s][0] += ( numOfCombinations[s+1][j] * increment[s+1][j] );
					s--;
				}
			}
		}
		return( increment );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method sets M[i] to be the combination located at: index_of_M[i]
	 */
	private int[][] init_M( long[] index_of_M, int[] integers, int[] length_of_A, int numOfAgents )
	{
		long[][] pascalMatrix = Combinations.initPascalMatrix( numOfAgents+1, numOfAgents+1 );
		
		//Memory allocation:
		int[][] M = new int[ integers.length ][];
		for(int s=0; s<=integers.length-1; s++)
		{
			M[s] = new int[integers[s]];
		}
		
		//Setting M[0], M[1], ..., M[integers.length-1]:
		for(int i=0; i<=integers.length-1; i++)
		{
			/*1*/int j=1; long index=index_of_M[i]; int s1=integers[i];
			
			boolean done=false;
			do
			{
				//Check the values: pascalMatrix[s1,1],pascalMatrix[s1,2],...
				/*2*/ int x=1; while( pascalMatrix[s1-1][x-1] < index ) x++;
				
				/*3*/ M[i][j-1]=(int)( (length_of_A[i]-s1+1)-x+1 );
				
				/*4*/ if( pascalMatrix[s1-1][x-1]==index )
				{
					//Set the rest of the coalition as follows:
					for(int k=j; k<=integers[i]-1; k++) M[i][k]=(int)(M[i][k-1]+1);
					done=true;
				}
				//Otherwise:
				else
				{
					j=j+1;  index=index-pascalMatrix[s1-1][x-2];  s1=s1-1;
				}
			}
			while(done==false);
		}
		return(M);
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes index_of_M  (Converts the index into a list of indices for every list)
	 * (For more details on: "index_of_M", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private long[] init_index_of_M( long index, int[] integers, long[][] increment, int[] max_first_member_of_M, long[][] numOfCombinations, long[] numOfRemovedCombinations, long[] sumOf_numOfCombinations)
	{
		/*
		 * Here, we will use the variables: counter1, counter2.
		 *       counter1 will be used to count the coalition structures that were scanned before reaching the
		 *                current one (i.e., before reaching the coalition structure that is located at: "index").
		 *       counter2 will be used to count the combinations that were scanned before reaching the current M
		 * 
		 * As an example, I will refer to figure 3 in the paper: "Near-optimal anytime coalition structure generation".
		 * 
		 * In figure 3, in the list of possible combinations of M[0], we will consider the index of
		 * combination: "6,7" to be: 1, and the index of combination: "5,7" to be: 2, and so on.
		 * 
		 * In figure 3, we have:
		 * 
		 * 	     "integers" contains the values: 2,2,3
		 * 
		 *       "numOfCombinations[0]" contains the values: 6,5,4,3
		 *       "numOfCombinations[1]" contains the values: 4,3,2,1
		 *       "numOfCombinations[2]" contains the value : 1
		 *    
		 *       "sumOf_numOfCombinations" contains the values: 18,10,1
		 * 
		 *       "numOfRemovedCombinations" contains the values: 3,0,0
		 *    
		 *       "increment[0]" contains the values: 10,6,3,1
		 *       "increment[1]" contains the value : 1
		 *       "increment[2]" contains the value : 1
		 *    
		 *       "max_first_member_of_M" contains the values: 4,4,1
		 *       
		 *       "counter1" contains the value: 48
		 *       "counter2" contains the value: 5 (when searching for M[0])
		 *                  contians the value: 8 (when searching for M[1])
		 */
		
		long counter1=0;
		long counter2=1;
		long[] index_of_M=new long[ integers.length ];
		
		//Setting index_of_M[integers.length-1]:
		index_of_M[integers.length-1]=1;		
		
		//Setting index_of_M[0], index_of_M[1], ..., index_of_M[integers.length-2]:		
		int min_first_member_of_M=0;
		for(int i=0; i<=integers.length-2; i++)
		{
			if( sumOf_numOfCombinations[i]==1 ) //If the list of possible combiantions of M[i] contains one combinations...
			{
				index_of_M[i]=1;
			}
			else
				if( increment[i].length == 1 ) //i.e., if all the possible combinations of M[i] had equal increments...
				{
					counter1=0;
					counter2=1;
					if( min_first_member_of_M>0 )
						for(int j=0; j<=min_first_member_of_M-1; j++)
							counter2 += numOfCombinations[i][j];
					
					long steps = (long) ( Math.ceil( index / (double)increment[i][0] ) -1);
					counter1 += steps*increment[i][0]; 
					counter2 += steps;
					
					index_of_M[i] = counter2;
					index        -= counter1;
					
					if(( i>=integers.length-1 )||( integers[i]!=integers[i+1] )) min_first_member_of_M=0;
				}
				else
					//i.e., if the possible combinations of M[i] had different increments. (In this case, all the combinations
					//that start with j, where j=1,2,...,max_first_member_of_M[i], would have the increment = "increment[i][j]") 
				{
					counter1=0;
					counter2=1;
					if( min_first_member_of_M > 0 )
						for(int j=0; j<min_first_member_of_M; j++)
							counter2 += numOfCombinations[i][j];
					
					/*
					 * Note that counter1 is initially equal to 0, and counter2 is initially equal to 1.
					 * Now, in figure 3, we have the following:
					 *
					 *     In the list of possible combinations of M[0], we have 6 combinations that start with 1, each 
					 *     of which the increment is 10. Therefore, if index <= counter1+60, then M[0] must start with 1.
					 *     Otherwise:
					 *         (1) we add 60 to counter1,   (2) we add 6 to counter2,   (3) we move to the next setp.
					 *     
					 *     In the list of possible combinations of M[0], we have 5 combinations that start with 2, each
					 *     of which the increment is  6. Therefore, if index <= counter1+30, then M[0] must start with 2. 
					 *     Otherwise:
                     *         (1) we add 30 to counter1,   (2) we add 5 to counter2,   (3) we move to the next setp.
					 *     
					 *     This is done until we find the required first element of M[0], let us denote it by "x". Now,
					 *     we need to know which of these combinations (that start with "x") is the required one.
					 *     
					 *     We count the steps that we take within the coalitions that start with "x". For example,
					 *     if the required combination was 80, then M[0] would be one of the combinations that start with
					 *     2, and the number of steps that we take within these combinations is 3. 
					 */
					for(int j=min_first_member_of_M; j<max_first_member_of_M[i]; j++)
					{
						if( index <= counter1+(numOfCombinations[i][j]*increment[i][j]) )
						{
							long steps = (long) Math.ceil( (index-counter1) / (double)increment[i][j] ) - 1;
							counter1 += steps*increment[i][j];
							counter2 += steps;

							index_of_M[i] = counter2;
							index        -= counter1;
							
							if(( i<integers.length-1 )&&( integers[i]==integers[i+1] ))
								min_first_member_of_M = j;
							else
								min_first_member_of_M = 0;
							
							break;
						}
						else
						{
							long steps = numOfCombinations[i][j];
							counter1 += steps*increment[i][j];
							counter2 += steps;
						}
					}
				}
		}			

		//When calculating a combination, based on its index, we assume 1,2,..,n to be the last combination
		//(instead of the first). Therefore, we flip the indices (i.e., the index: i becomes: n-i)
		for(int i=0; i<=index_of_M.length-1; i++)
			index_of_M[i] = (sumOf_numOfCombinations[i]+numOfRemovedCombinations[i]) - index_of_M[i] + 1;
		
		return( index_of_M );
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes A.
	 * (For more details on: "A", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private int[][] init_A( int numOfAgents, int[] integers, int[][] M, int[] length_of_A )
	{
		/*
		 * Note that if we have a subspace [n_1, n_2, ..., n_s], then we only need: 
		 * A[1], A[2], ... A[s-1]. Because
		 * A[s] would simply contain all of the remaining agents.
		 */
		int[][] A = new int[ integers.length-1 ][];
		for(int s=0; s<=integers.length-2; s++)
		{
			A[s]=new int[length_of_A[s]];
			if( s==0 )
			{
				for(int j1=0; j1<=numOfAgents-1; j1++)
				{
					A[s][j1]=(int)(j1+1);
				}
			}
			else
			{
				int j1=0;int j2=0;
				for(int j3=0; j3<=length_of_A[s-1]-1; j3++)
				{
					if(( j1>=M[s-1].length )||( j3+1 != M[s-1][j1] ))
					{
						A[s][j2]=A[s-1][j3];
						j2++;
					}
					else j1++;
				}
			}
		}
		return( A );
	}
	
	//******************************************************************************************************

	/**
	 * This method initializes "length_of_A". For more details see the comments at the beginning of "search_useBranchAndBound"
	 */
	private int[] init_length_of_A( int numOfAgents, int[] integers)
	{
		int[] length_of_A = new int[ integers.length ];
		
		length_of_A[0]=numOfAgents;
		if( integers.length > 1 )
			for(int s=1; s<=integers.length-1; s++)
				length_of_A[s]=(int)(length_of_A[s-1]-integers[s-1]);

		return( length_of_A );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method is similar to the method: "init_CS", except that it is used with CS where the agents are represented
	 * as bits, rather than ints.
	 */
	private int[] init_CS_hashTableVersion( int[][] M,int[][] A,int[] length_of_A,int[] bit,int numOfIntegers)
	{
		int[] CS = new int[ integers.length ];
		
		for(int s=0; s<=integers.length-2; s++)
		{
			CS[s]=0;
			for(int j1=0; j1<M[s].length; j1++)
			{
				CS[s] |= bit[ A[ s ][ M[s][j1]-1 ] ];
			}
		}
		setTheLastTwoCoalitionsInCS(CS, M[numOfIntegers-2], A, numOfIntegers, bit);
		return( CS );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method initializes sumOf_values.
	 * (For more details on: "sumOf_values", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private double[] init_sumOf_values_hashTableVersion( int numOfIntegers, int[] CS, Input input )
	{
		double[] sumOf_values = new double[ numOfIntegers+1 ];		
		
		sumOf_values[0] = 0;
		for(int i=1; i<=numOfIntegers; i++)
		{
			sumOf_values[i] = sumOf_values[i-1] + input.getCoalitionValue( CS[i-1] );
		}

		return( sumOf_values );
	}	

	//******************************************************************************************************
	
	/**
	 * This method initializes sumOf_agents.
	 * (For more details on: "sumOf_agents", read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private int[] init_sumOf_agents( int numOfIntegers, int[][] CS, int[] bit )
	{
		int[] sumOf_agents = new int[ numOfIntegers+1 ];		
		
		sumOf_agents[0] = 0;
		
		for(int i=1; i<=numOfIntegers; i++)
		{			
			sumOf_agents[i] = sumOf_agents[i-1];
			for(int j=0; j<integers[i-1]; j++)
			{
				sumOf_agents[i] += bit[ CS[i-1][j] ];
			}
		}

		return( sumOf_agents );
	}	
	
	//******************************************************************************************************
	
	/**
	 * This method is similar to the method: "init_sumOf_agents", except that it is
	 * used with CS where the agents are represented as bits, rather than ints.
	 */
	private int[] init_sumOf_agents_hashTableVersion( int numOfIntegers, int[] CS )
	{
		int[] sumOf_agents = new int[ numOfIntegers+1 ];		
		
		sumOf_agents[0] = 0;
		
		for(int i=1; i<=numOfIntegers; i++)
		{
			sumOf_agents[i] = sumOf_agents[i-1] + CS[i-1];
		}

		return( sumOf_agents );
	}
	
	//******************************************************************************************************

	/**
	 * This method is only used in the subspace search methods. It sets "M" and "index_of_M"
	 * (For more details, read the comments that are in the beginning of method: "search_useBranchAndBound")
	 */
	private void set_M_and_index_of_M(int[][] M, long[] index_of_M, 
			final int[] length_of_A, final long[] indexToStartAt, int s2)
	{
		//Set the index of M to be the last index in the column
		index_of_M[s2] = indexToStartAt[s2];

		//Set M, given its new index
		if( integers[s2]==integers[s2-1] ){
			if( M[s2-1][0]>1 )
				for(int j=1; j<M[s2-1][0]; j++)
					index_of_M[s2]= index_of_M[s2] - Combinations.binomialCoefficient( length_of_A[s2]-j, integers[s2]-1);

			for(int j1=0; j1<integers[s2]; j1++)
				M[s2][j1]=(int)(M[s2-1][0]+j1);
		}
		else
			for(int j1=0; j1<integers[s2]; j1++)
				M[s2][j1]=(int)(1+j1);
	}
	
	//******************************************************************************************************
	
	/**
	 * Given: A[s-1], M[s-1], it sets the elements of: A[s].
	 * These are simply the emelents that are in A[s-1] that were not pointed at by M[s-1].
	 */
	private void update_A(int[] A1, int[] A2, final int numOfAgents, int[] M, final int length_of_M )
	{
		int j1=0;
		int j2=0;
		for(int j3=0; j3<A2.length; j3++)
		{
			if(( j1>=length_of_M )||( (j3+1)!=M[j1] ))
			{
				A1[j2]=A2[j3];
				j2++;
			}
			else j1++;
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * Note that we need both M[i] and A[i] in order to find CS[i]. (for more detail on:
	 * M, A, CS, see the comments at the beginning of method: "search_useBranchAndBound"). However, this does
	 * not apply to the last coalition (i.e., CS[integers.length-1]). This is because M[integers.length-2] and
	 * A[integers.length-2] are enough to find both: CS[integers.length-2] and CS[integers.length-1]. This
	 * is because the elements of: A[integers.length-2] that were not pointed at by M[integers.length-2]
	 * must belong to CS[integers.length-1].
	 */
	private void setTheLastTwoCoalitionsInCS( int[][] CS, int[][] M,
			int[][] A, final int[] length_of_A, final int numOfIntegers ) 
	{
    	int j1=0;
		int j2=0;		
		for(int j3=0; j3<length_of_A[ numOfIntegers-2 ]; j3++)
		{
			if(( j1>=integers[ numOfIntegers-2 ] )||( (j3+1)!=M[ numOfIntegers-2 ][j1] ))
			{
				CS[ numOfIntegers-1 ][j2] = A[ numOfIntegers-2 ][j3];
				j2++;
			}
			else
			{
				CS[ numOfIntegers-2 ][j1] = A[ numOfIntegers-2 ][j3];
				j1++;
			}
		}
	}
	
	//******************************************************************************************************

	/**
	 * This method is similar to the method: "setTheLastTwoCoalitionsInCS" that is mentioned above, except that it
	 * is used with CS where the agents are represented as bits, rather than ints.
	 */
	private void setTheLastTwoCoalitionsInCS( int[] CS, int[] M,
			int[][] A, final int numOfIntegers, final int[] bit)
	{
		int result1=0;
		int result2=0;		
		int m=integers[ numOfIntegers-2 ]-1;
		int a=A[ numOfIntegers-2 ].length-1;
		do
		{
			if( a == M[m]-1 )
			{
				result1 += bit[ A[ numOfIntegers-2 ][a] ];
				if( m==0 )
				{
					a--;
					break;
				}
				m--;
			}
			else
				result2 += bit[ A[ numOfIntegers-2 ][a] ];
			
			a--;
		}
		while( a>=0 );
		
		while( a>=0 )
		{
			result2 += bit[ A[ numOfIntegers-2 ][a] ];
			a--;
		}		
		CS[ numOfIntegers-2 ] = result1;
		CS[ numOfIntegers-1 ] = result2;
	}
	
	//******************************************************************************************************
	
	/**
	 * This method sets CS to the coalition structure located at the given index.
	 */
	private void set_CS_given_its_index( final int numOfAgents, int[][] CS, final long index,
			final int[] length_of_A, final long[][] increment, final int[] max_first_member_of_M,
			final long[][] numOfCombinations, final long[] numOfRemovedCombinations, final long[] sumOf_numOfCombinations)
	{
		//Convert the index into a list of indices for every list
		long[] index_of_M = init_index_of_M( index, integers, increment, max_first_member_of_M, numOfCombinations, numOfRemovedCombinations, sumOf_numOfCombinations);
		
		//Set M[i] to be the coalition located at: index_of_M[i]
		int[][] M = init_M( index_of_M, integers, length_of_A, numOfAgents );
		
		//Set A
		int[][] A = init_A( numOfAgents,integers, M, length_of_A );

		//Finding the actual CS...
		for(int s=0; s<=integers.length-3; s++)
			for(int i=0; i<=M[s].length-1; i++)
				CS[s][i]=A[ s ][ M[s][i]-1 ];		
		setTheLastTwoCoalitionsInCS( CS, M, A, length_of_A, integers.length );
	}	
	
	//******************************************************************************************************
	
	/**
	 * This method is similar to the method: "set_CS_given_its_index" that is mentioned above, except
	 * that it is used with CS where the agents are represented as bits, rather than ints.
	 */
	private void set_CS_given_its_index( final int numOfAgents, int[] CS, final long index, final int[] length_of_A, final long[][] increment,
			final int[] max_first_member_of_M,	final long[][] numOfCombinations, final long[] numOfRemovedCombinations, final long[] sumOf_numOfCombinations, final int[] bit)
	{
		//Convert the index into a list of indices for every list
		long[] index_of_M = init_index_of_M( index, integers, increment, max_first_member_of_M, numOfCombinations, numOfRemovedCombinations, sumOf_numOfCombinations);
		
		//Set M[i] to be the coalition located at: index_of_M[i]
		int[][] M = init_M( index_of_M, integers, length_of_A, numOfAgents );
		
		//Set A
		int[][]  A =  init_A( numOfAgents, integers, M, length_of_A );
		
		//Finding the actual CS...
		for(int s=0; s<=integers.length-3; s++)
		{
			CS[s]=0;
			for(int j1=0; j1<=M[s].length-1; j1++)
				CS[s] |= bit[ A[ s ][ M[s][j1]-1 ] ];
		}
		
		int remainingAgents = 0;
		for(int i=length_of_A[ integers.length-2 ]-1; i>=0; i--)
		{
		   	remainingAgents |= bit[ A[ integers.length-2 ][i] ];
		}		
		setTheLastTwoCoalitionsInCS(CS,M[integers.length-2], A, integers.length, bit);		
	}
	
	//******************************************************************************************************
	
	/**
	 * This method uses the technique called: "local branch and bound".
	 * In more detail, For every subset of agents S, we record the best value found so far, and that is regardless
	 * of the way the agents in S are partitioned.
	 * 
	 * For example, given S={a,b,c,d}, and while we are cycling through coalition structures in subspace [2,2,4,4], we
	 * we might come across the following set of coalitions: {{a,b},{c,d},...}, and suppose that v({2,3})+v({5,9})=100
	 * then we store in the table the value: 100.
	 * 
	 * After that, while cycling through coalition structures in subspace [1,1,2,4,4,4], suppose that we come across
	 * the following set of coalitions: {{a},{c},{b,d},...}, then:
	 *   * if( v({a})+v({c})+v({b,d}=80 ), then there is no need to continue searching the tree of coalitions that
	 *     comes with {{a},{c},{b,d},...}
	 *   * if( v({a})+v({c})+v({b,d}=120 ), then the table is updated, and the value 120 is stored instead of 100.
	 */
	private boolean useLocalBranchAndBound( Input input,IDPSolver_whenRunning_ODPIP localBranchAndBoundObject,double[] sumOf_values, int[] sumOf_agents, int s2, int newCoalition, double valueOfNewCoalition)
	{
		boolean result = false;
		
		//if(( localBranchAndBoundObject.getLowerBoundForCurrentAgents(sumOf_agents[s2+1]) > sumOf_values[s2+1] )||( localBranchAndBoundObject.getLowerBoundForCurrentAgents(newCoalition) > valueOfNewCoalition ))
		if(( localBranchAndBoundObject.getValueOfBestPartitionFound(sumOf_agents[s2+1]) - sumOf_values[s2+1] >0.00000000005 )||( localBranchAndBoundObject.getValueOfBestPartitionFound(newCoalition) - valueOfNewCoalition >0.00000000005 ))
			result = true;

		//Update the local-branch-and-bound table if necessary
		//if( localBranchAndBoundObject.getLowerBoundForCurrentAgents(sumOf_agents[s2+1]) < sumOf_values[s2+1] )
		if( localBranchAndBoundObject.getValueOfBestPartitionFound(sumOf_agents[s2+1]) - sumOf_values[s2+1] <-0.00000000005 )
			localBranchAndBoundObject.updateValueOfBestPartitionFound( sumOf_agents[s2+1], sumOf_values[s2+1] );
		
		//if( localBranchAndBoundObject.getLowerBoundForCurrentAgents( newCoalition ) < valueOfNewCoalition )
		if( localBranchAndBoundObject.getValueOfBestPartitionFound( newCoalition ) - valueOfNewCoalition <-0.00000000005 )
			localBranchAndBoundObject.updateValueOfBestPartitionFound( newCoalition, valueOfNewCoalition );
		
		return( result );
	}
}