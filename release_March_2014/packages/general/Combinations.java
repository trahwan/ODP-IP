package general;

import java.math.BigInteger;

/**
 * Class that contains general combinatorial functions such as computing the number of possible
 * combinations of a set of a certain size, generating a list of the possible combinations, and so on...
 * @author Talal Rahwan
 */
public class Combinations
{
	private static long[][] pascalMatrix = null;
	
	//******************************************************************************************************
	
	/**
	 * This method initializes pascal´s Matrix. For more details, see "An Algorithm for 
	 * Distributing Coalitional Value Calculations Among Cooperative Agents" [AIJ, 2007]
	 */
	public static long[][] initPascalMatrix( int numOfLines, int numOfColumns )
	{
		if(( pascalMatrix==null )||( numOfLines > pascalMatrix.length ))
		{
			if( pascalMatrix == null ) //Build the matrix from scratch
			{
				pascalMatrix = new long[numOfLines][numOfColumns];
				for(int i=0; i<numOfLines;   i++) { pascalMatrix[i][0]=1;  }
				for(int j=1; j<numOfColumns; j++) {	pascalMatrix[0][j]=j+1;}

				for(int i=1; i<numOfLines; i++)
					for(int j=1; j<numOfColumns; j++)
						pascalMatrix[i][j]=pascalMatrix[i-1][j]+pascalMatrix[i][j-1];
			}
			else //extend the already-built matrix (only if the previous one was smaller than the current one)
			{
				long[][] prev_pascalMatrix = pascalMatrix;
				int prev_numOfLines   = prev_pascalMatrix.length;
				int prev_numOfColumns = prev_pascalMatrix[0].length; 
				
				pascalMatrix = new long[numOfLines][numOfColumns];
				for(int i=0; i<numOfLines  ; i++) { pascalMatrix[i][0]=1;  }
				for(int j=1; j<numOfColumns; j++) { pascalMatrix[0][j]=j+1;}
				
				for(int i=1; i<prev_numOfLines; i++)
				{
					//Copy the pervious columns of the previous lines
					for(int j=1; j<prev_numOfColumns; j++)
						pascalMatrix[i][j]=prev_pascalMatrix[i][j];
					//Add the new columns to the previous lines
					for(int j=prev_numOfColumns; j<numOfColumns; j++)
						pascalMatrix[i][j]=pascalMatrix[i-1][j]+pascalMatrix[i][j-1];;
				}
				//Build the new lines
				for(int i=prev_numOfLines; i<numOfLines; i++)
					for(int j=1; j<numOfColumns; j++)
						pascalMatrix[i][j]=pascalMatrix[i-1][j]+pascalMatrix[i][j-1];
				
				//Free the previous matrix
				prev_pascalMatrix=null;
			}
		}
		return( pascalMatrix );
	}
	
	//******************************************************************************************************
	
	/**
	 * This function returns the number of combinations of size y out of x agents, In other
	 * words, it returns the following value:  factorial(x) / factorial(x-y)*factorial(y) 
	 */
	public static long binomialCoefficient(int x, int y)
	{
		if( x==y ) return( 1 ); //Deal with this special case

		//initialize pascal matrix (if it hasn't already been initialized)
		initPascalMatrix( x, x );

		//By this, we have: C^x_y = pascalMatrix[x-y-1,y] (with the exception of the case where n = m)
		return( pascalMatrix[x-y-1][y] );
        /*
        //Alternative method...
		int result = 1;
        if( y > x / 2 ) 
        	y = (int)(x - y);
        for (int i = 1; i <= y; i++)
        	result = result * (x + 1 - i) / i;
        return result; */
	}
	
	//******************************************************************************************************
	
    /**
     * Given a combination in bit format, return its size (i.e. the number of ones it contains)
     */
    public static int getSizeOfCombinationInBitFormat( int combinationInBitFormat, int numOfAgents )
    {
    	return( Integer.bitCount( combinationInBitFormat ) );
    	/*
        int result = 0;
        for (int i = 0; i < numOfAgents; i++)
            if ((combinationInBitFormat & (1<<i)) != 0) //if agent "i+1" is a member of "combinationInBitFormat"
                result++;

        return result;
        */
    }	
	
	//******************************************************************************************************
	
    /**
     * Compute the number of possible coalition structures.
     */
    public static long getNumOfCS( int numOfAgents )
    {
    	long numOfCS =0;
    	for(int size=1; size<=numOfAgents; size++)
    		numOfCS += Combinations.getNumOfCSOfCertainSize(numOfAgents,size);
    	return( numOfCS );
    }
    //The following is an alternative approach that uses "BigInteger"
    public static BigInteger getNumOfCS_bitIntegerVersion( int numOfAgents )
    {
    	BigInteger numOfCS = BigInteger.valueOf(0);	
    	for(int size=1; size<=numOfAgents; size++)
    		numOfCS = numOfCS.add(Combinations.getNumOfCSOfCertainSize_bigIntegerVersion(numOfAgents,size));
    	return( numOfCS );
    }
    
    //******************************************************************************************************
    
    /**
     * Compute the number of coalitions in the search space (i.e., in the space of possible coalition
     * structure). Here, for every coalition structure, we count the number of coalitions in it.
     */
    public static long getNumOfCoalitionsInSearchSpace( int numOfAgents )
    {
    	long numOfCoalitionsInSearchSpace =0;
    	for(int size=1; size<=numOfAgents; size++)
    		numOfCoalitionsInSearchSpace += size * Combinations.getNumOfCSOfCertainSize(numOfAgents,size);
    	return( numOfCoalitionsInSearchSpace );
    }
    //The following is an alternative approach that uses "BigInteger"
    public static BigInteger getNumOfCoalitionsInSearchSpace_bitIntegerVersion( int numOfAgents )
    {
    	BigInteger numOfCoalitionsInSearchSpace = BigInteger.valueOf(0);	
    	for(int size=1; size<=numOfAgents; size++) {
    		numOfCoalitionsInSearchSpace = numOfCoalitionsInSearchSpace.add(Combinations.getNumOfCSOfCertainSize_bigIntegerVersion(numOfAgents,size));
    		numOfCoalitionsInSearchSpace = numOfCoalitionsInSearchSpace.multiply( BigInteger.valueOf(size) );
    	}
    	return( numOfCoalitionsInSearchSpace );
    }
    
    //******************************************************************************************************

    /**
	 * Compute the number of possible coalition structures of a particular size.
	 */
	public static long getNumOfCSOfCertainSize( int numOfAgents, int size )
	{
	    if( (size==1)||(size==numOfAgents) )
	    	return(1);
	    else
	    	return(size* getNumOfCSOfCertainSize((int)(numOfAgents-1),size) + getNumOfCSOfCertainSize((int)(numOfAgents-1),(int)(size-1)));
	}	
	//The following is an alternative approach that uses "BigInteger"
	public static BigInteger getNumOfCSOfCertainSize_bigIntegerVersion( int numOfAgents, int size )
	{
	    if( (size==1)||(size==numOfAgents) )
	    	return( BigInteger.valueOf(1) );
	    else
	    {
	    	BigInteger solution = getNumOfCSOfCertainSize_bigIntegerVersion( (int)(numOfAgents-1), size ).multiply( BigInteger.valueOf(size) );
	    	solution = solution.add( getNumOfCSOfCertainSize_bigIntegerVersion( (int)(numOfAgents-1), (int)(size-1) ) );
	    	return( solution );
	    }
	}
	
	//******************************************************************************************************

	/**
	 * Convert a combination (a coalition) from int format to bit format
	 */
	public static int convertCombinationFromByteToBitFormat( int[] combinationInByteFormat )
	{
   		return( convertCombinationFromByteToBitFormat( combinationInByteFormat, combinationInByteFormat.length ) );
	}	
	public static int convertCombinationFromByteToBitFormat( int[] combinationInByteFormat, int combinationSize )
	{
   		int combinationInBitFormat = 0;
   		for(int i=0; i<combinationSize; i++)
   			//Add agent "combinationInByteFormat[i]" to "combinationInBitFormat"
   			combinationInBitFormat += 1 << (combinationInByteFormat[i]-1);

   		return( combinationInBitFormat );
	}
	
	//******************************************************************************************************

	/**
	 * Convert a combination from bit format to int format (e.g. given 4 agents, 0110 becomes {2,3})
	 */
	public static int[] convertCombinationFromBitToByteFormat( int combinationInBitFormat, int numOfAgents, int combinationSize )
	{
		int[] combinationInByteFormat = new int[ combinationSize ];
		int j=0;
		for(int i=0; i<numOfAgents; i++){
			if ((combinationInBitFormat & (1<<i)) != 0){ //if agent "i+1" is a member of "combinationInBitFormat"
				combinationInByteFormat[j]= (int)(i+1);
				j++;
			}
		}
		return( combinationInByteFormat );
	}
	
	//******************************************************************************************************
	
	/**
	 * Convert a combination from bit format to int format (e.g. given 4 agents, 0110 becomes {2,3})
	 */
	public static int[] convertCombinationFromBitToByteFormat( int combinationInBitFormat, int numOfAgents )
	{
		//compute the size of the combination
		int combinationSize = getSizeOfCombinationInBitFormat( combinationInBitFormat, numOfAgents);
		return( convertCombinationFromBitToByteFormat(combinationInBitFormat, numOfAgents, combinationSize) );
	}

	//******************************************************************************************************
	
	/**
	 * Convert a set of combination (a coalition structure) from bit format to int format
	 */
	public static int[][] convertSetOfCombinationsFromBitToByteFormat( int[] setOfCombinationsInBitFormat,int numOfAgents)
	{
    	//Initialization
    	int[] sizeOfEachCombination = new int[ setOfCombinationsInBitFormat.length ];    	
        for(int i=setOfCombinationsInBitFormat.length-1; i>=0; i--)
        	sizeOfEachCombination[i] = (int)getSizeOfCombinationInBitFormat( setOfCombinationsInBitFormat[i] , numOfAgents);

        return( convertSetOfCombinationsFromBitToByteFormat(setOfCombinationsInBitFormat,numOfAgents,sizeOfEachCombination));
        }
	
	//******************************************************************************************************
	
	/**
	 * Convert a set of combination (a coalition structure) from bit to int format, where the length of each coalition
	 * is provided in the array "lengthOfEachCoalition" (i.e., coalition "CS[i]" is of size "lengthOfEachCoalition[i]"). 
	 */
	public static int[][] convertSetOfCombinationsFromBitToByteFormat(
			int[] setOfCombinationsInBitFormat, int numOfAgents, int[] sizeOfEachCombination )
	{
        int[][] setOfCombinationsInByteFormat = new int[ setOfCombinationsInBitFormat.length ][];
    	for(int i=0; i<setOfCombinationsInBitFormat.length; i++)
    		setOfCombinationsInByteFormat[i] = convertCombinationFromBitToByteFormat( setOfCombinationsInBitFormat[i], numOfAgents); 
    			
    	return(setOfCombinationsInByteFormat);		
	}
	
	//******************************************************************************************************
	
	/**
	 * Convert a set of combination (a coalition structure) from int to bit format 
	 */
	public static int[] convertSetOfCombinationsFromByteToBitFormat( int[][] setOfCombinationsInByteFormat )
	{
    	int[] setOfCombinationsInBitFormat = new int[ setOfCombinationsInByteFormat.length ];
    	for(int i=0; i<setOfCombinationsInByteFormat.length; i++)
    		setOfCombinationsInBitFormat[i] = convertCombinationFromByteToBitFormat( setOfCombinationsInByteFormat[i] );
    	return( setOfCombinationsInBitFormat );
    }
	
	//******************************************************************************************************
	
	/**
	 * - This method returns the list of possible combinations of a set of size = "size".
	 * - Here, a coalition is represented using ints (i.e., each agent is represented using a int)
	 */
	public static int[][] getCombinationsOfGivenSize(int numOfAgents, int size )
	/*
	 * Here, we start by generating the combination: "1,2,...,size" which is the last combination
	 * in the list. Then, from the current combination, we find the combination that is located
	 * before it, and so on until we reach the first combination in the list, which is:
	 * "numOfAgents-(size-1), numOfAgents-(size-2), ..., numOfAgents"
	 */
	{
		int[][] list = new int[ (int)binomialCoefficient(numOfAgents, size) ][size];		
		int index = list.length - 1;
		
		//Generate the combination: "1,2,...,size"
		for(int i=1;i<=size;i++)
			list[index][i-1]=i;
		
		//Generate the remaining combinations
		int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		while( index>0 )
		{
			for(int i=size-1; i>=0; i--)
			{
				//If the agent at index "i" is smaller than the largest possible agent that can be at index "i"
				if( list[index][i] < maxPossibleValueForFirstAgent+i )
				{					
					index--;
					
					for(int j=0; j<i; j++)
						list[index][j]=list[index+1][j];
					
					list[index][i]=(int)(list[index+1][i]+1);
					
					for(int j=i+1; j<size; j++)
						list[index][j]=(int)(list[index][j-1]+1);
					
					break;
				}
			}
		}
		return(list);
	}
	
	//******************************************************************************************************
	
	/**
	 * - This method returns the list of possible combinations of a set of size = "size".
	 * - Here, a coalition is represented using bits (i.e., each agent is represented using a bit)
	 */
	public static int[] getCombinationsOfGivenSizeInBitFormat(int numOfAgents, int size)
	/*
	 * Here, we start by generating the combination: "1,2,...,size" which is the LAST combination
	 * in the list. Then, from the current combination, we find the combination that is located
	 * BEFORE it, and so on until we reach the first combination in the list, which is:
	 * "numOfAgents-(size-1), numOfAgents-(size-2), ..., numOfAgents"
	 */
	{
		//set "onesBeforeIndex" such that it contains 1,1,1,1... "k" times, and then contains zeros
		final int[] onesBeforeIndex = new int[ numOfAgents+1 ];
		for(int k=numOfAgents; k>0; k--)
			onesBeforeIndex[k] = (1<<k) - 1;

		int[] list=new int[ (int)binomialCoefficient(numOfAgents, size) ];
		int index = list.length - 1;
		
		//Generate the combination: "1,2,...,size"
		list[index]=0;
		for(int i=1;i<=size;i++)
			list[index] += (1<<(i-1)); //add agent "i" to the coalition "list[index]"
		
		//Generate the remaining combinations
		int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		while( index>0 ) //For every coalition in the list
		{
			//Initializing the index of the agent that we are trying to identify. Here, "size-1"
			int i=size-1; //means that we trying to identify the last agent in the coalition.

			for(int k=numOfAgents; k>0; k--)
			{
				if ((list[index] & (1<<(k-1))) != 0) //If agent "k" is in the coalition "list[index]"
				{
					if( k < maxPossibleValueForFirstAgent+i ) //If agent "k" is smaller than the largest possible agent that can be at index "i"
					{					
						index--;
						
						list[index] = (list[index+1] & onesBeforeIndex[k-1]);

						list[index] += (1<<k); //add agent "k+1" to the coalition "list[index]"

						for(int j=1; j<size-i; j++)
							list[index] += (1<<(k+j)); //add agent "(k+j)+1" to the coalition "list[index]"
						
						i--;
						break;
					}
					i--;
				}
			}
		}
		return(list);
	}
	
	//*********************************************************************************************************
	
	/**
	 * Given a coalition in a list of coalitions that is ordered as in DCVD, this method sets this
	 * coalition to be the coalition located before it. HERE, THE COMBINATION IS GIVEN IN BYTE FORMAT
	 */
	public static void getPreviousCombination( final int numOfAgents, final int size, int[] combination)
	{
		/* maxPossibleValueForFirstAgent: is the maximum value that the first agent in the coalition can have.
		 * For example, if we have coalitions of size 3 out of 9 agents, then, since each combination is ordered
		 * in an ascending order, the first agent cannot be greater than 7, and that is in combination: (7,8,9)
		 */
		final int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		for(int i=size-1; i>=0; i--) {
			if( combination[i] < maxPossibleValueForFirstAgent+i ) {
					combination[i]++;				
				for(int j=i+1; j<size; j++) {
					combination[j]=combination[j-1]+1;
				}
				break;
			}
		}			
	}
	
	//************************************************************************************************
	
	/**
	 * get all the possible subsets of a multiset
	 */
	public static int[][] getCombinationsOfGivenSize_multisetVersion_oldVersion( int[] multiset )
	{
		//initialization
		int[][] subsets;
		int indexInSubsets=0;
		int[] underlyingSet = General.getUnderlyingSet( multiset );
		int sizeOfUnderlyingSet = underlyingSet.length;
		
		//Compute the multiplicity of each member
		int[] multiplicity = new int[ sizeOfUnderlyingSet ];
		for(int i=0; i<sizeOfUnderlyingSet; i++)
			multiplicity[i] = General.getMultiplicity( underlyingSet[i], multiset );

		//compute an upper bound on the number of possible subsets of the multiset
		int[] sortedMultiplicity = General.sortArray( multiplicity, false );
		int upperBoundOnNumOfSubsets = 0;
		for(int curSize=1; curSize<=sizeOfUnderlyingSet; curSize++)
		{
			int x = 1;
			for(int i=0; i<curSize; i++)
				x *= sortedMultiplicity[i];
			upperBoundOnNumOfSubsets += x * Combinations.binomialCoefficient( sizeOfUnderlyingSet, curSize );
		}		
		//Allocate memory for "subspaces"
		subsets = new int[ upperBoundOnNumOfSubsets ][];

		//Generate the possible combinations of { 1, 2, ..., sizeOfUnderlyinSet }
		for(int sizeOfCombination=1; sizeOfCombination<=sizeOfUnderlyingSet; sizeOfCombination++)
		{
			int[] curCombination = new int[sizeOfCombination];
			long numOfCombinationsOfCurSize = Combinations.binomialCoefficient( sizeOfUnderlyingSet, sizeOfCombination );
			for(int i=0; i<numOfCombinationsOfCurSize; i++)
			{
				if( i==0 )
					curCombination = Combinations.getCombinationAtGivenIndex( sizeOfCombination, 0, sizeOfUnderlyingSet );
				else
					Combinations.getNextCombination( sizeOfUnderlyingSet, sizeOfCombination, curCombination );
				
				//for the current combination of { 1, 2, ..., sizeOfUnderlyingSet }: replace "i"
				//with X instances of the i^{th} element of the underlying set, where X=1, and then
				//X=2, and then X=3, and so on, until X equals the multiplicity of the i^{th} element
				indexInSubsets += getSubsetsThatMatchCombination( curCombination, sizeOfCombination, subsets, indexInSubsets, underlyingSet, multiplicity );
			}
		}		
		//rescale "subsets" such that its size equals the number of subsets in it
		int[][] temp = new int[ indexInSubsets ][];
		for(int i=0; i<indexInSubsets; i++)
			temp[i] = subsets[i];
		subsets = temp;
		
		return( subsets );
	}
	
	//************************************************************************************************

	/**
	 * This is only called in method: "getCombinationsOfGivenSize_multisetVersion_oldVersion"
	 * 
	 * for the current combination of { 1, 2, ..., sizeOfUnderlyingSet }: replace "i"
	 * with X instances of the i^{th} element of the underlying set, where X=1, and then
	 * X=2, and then X=3, and so on, until X equals the multiplicity of the i^{th} element
	 * 
	 * Add each of the resulting subsets to "subsets".
	 */
	private static int getSubsetsThatMatchCombination( int[] combination, int sizeOfCombination,
			int[][] subsets, int indexInSubsets, int[] underlyingSet, int[] multiplicity )
	{
		/* In this method, we will use "numOfInstances", which explained in the
		 * following example:
		 *    if numOfInstances = [2,3,5], this this means the number of times we
		 *    need to repeat the first element of the underlying set is 2, and the
		 *    number of times we need to repeat the second element is 3, and so on.
		 */
		
		//Initialization
		int[] numOfRepetitions = new int[sizeOfCombination];
		
		//Compute the number of possible combinations of the numbers of repetition
		int numOfPossibilitiesOfNumOfRepetitions = 1;
		for(int i=0; i<combination.length; i++)
			numOfPossibilitiesOfNumOfRepetitions *= multiplicity[ combination[i]-1 ];
		
		//For each of the remaining possiblities of the numbers of combinations
		for(int i=0; i<numOfPossibilitiesOfNumOfRepetitions; i++)
		{
			if( i==0 )
			{
				//Initializing the numbers of repetition to 1,1,...,1
				for(int j=0; j<sizeOfCombination; j++)
					numOfRepetitions[j] = 1;
			}else
			{
				//get the next numbers of repetition (e.g., if the numbers where: 2,2,4, and the
				//multiplicities were: 3,3,4, then the next numbers of repetitions would be 2,3,1
				for(int j=sizeOfCombination-1; j>=0; j--) {
					if( numOfRepetitions[j] < multiplicity[ combination[j]-1 ] ){
						numOfRepetitions[j] += 1;				
						for(int k=j+1; k<sizeOfCombination; k++)
							numOfRepetitions[k]=1;
						break;
					}
				}
			}
			//Compute the size of the current subset (based on the numbers of repetitions)
			int sizeOfCurSubset = 0;
			for(int j=0; j<sizeOfCombination; j++)
				sizeOfCurSubset += numOfRepetitions[j];
			subsets[ indexInSubsets ] = new int[ sizeOfCurSubset ];

			//Set the current subset based on the current combination and numbers of repetition
			int m=0;
			for(int j=0; j<sizeOfCombination; j++){
				for(int k=0; k<numOfRepetitions[j]; k++){
					subsets[ indexInSubsets ][ m ] = underlyingSet[ combination[j]-1 ];
					m++;			
				}
			}
			//Update the index in the list of subsets
			indexInSubsets++;
		}
		//return the number of subsets that were generated in this method
		return(numOfPossibilitiesOfNumOfRepetitions);
	}

	//******************************************************************************************************
	
	/**
	 * Given a coalition in a list of coalitions that is ordered as in DCVD, this method sets this coalition
	 * to be the coalition located before it. HERE, THE COMBINATION IS GIVEN IN BIT FORMAT
	 */
	public static int getPreviousCombinationInBitFormat( final int numOfAgents, final int size, int combination)
	{
		//Initializing the index of the agent that we are trying to identify. Here, "size-1"
		int i=size-1; //means that we trying to identify the last agent in the coalition.

		/* maxPossibleValueForFirstAgent: is the maximum value that the first agent in the coalition can have.
		 * For example, if we have coalitions of size 3 out of 9 agents, then, since each combination is ordered
		 * in an ascending order, the first agent cannot be greater than 7, and that is in combination: (7,8,9)
		 */
		int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		for(int k=numOfAgents; k>0; k--)
		{
			if ((combination & (1<<(k-1))) != 0) //If agent "k" is in "combination"
			{
				if( k < maxPossibleValueForFirstAgent+i ) //If agent "k" is smaller than the largest possible agent that can be at index "i"
				{
					combination &= (1<<(k-1)) - 1; //copy the part in "combination" that is
					//before "k", and set the remaining part to zeros

					combination += (1<<k); //to replace agent "k" with agent "k+1"

					for(int j=1; j<size-i; j++) //set the remaining agents
						combination += (1<<(k+j)); //add agent "(k+j)+1" to "combination"
					
					i--;
					break;
				}
				i--;
			}
		}
		return( combination );		
	}
	
	//*********************************************************************************************************
	
	/**
	 * Given a coalition in a list of coalitions that is ordered as in DCVD, this method sets this coalition
	 * to be the coalition located after it. HERE, THE COMBINATION IS REPRESENTED IN BYTE FORMAT.
	 */
	public static void getNextCombination( int numOfAgents, int size, int[] combination)
	{
		/* maxPossibleValueForFirstAgent: is the maximum value that the first agent in the coalition can have.
		 * For example, if we have coalitions of size 3 out of 9 agents, then, since each combination is ordered
		 * in an ascending order, the first agent cannot be greater than 7, and that is in combination: (7,8,9)
		 */
		final int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		for(int i=size-1; i>0; i--) {
			if( combination[i] != combination[i-1]+1 ) {
				combination[i]--;				
				for(int j=i+1; j<size; j++) {
					combination[j]=(int)(maxPossibleValueForFirstAgent+j);
				}
				return;
			}
		}
		//If we reach this instruction, it means that we reached a special case
		combination[0]--;
		for(int j=1; j<size; j++) {
			combination[j]=(int)(maxPossibleValueForFirstAgent+j);
		}		
	}
	
	//******************************************************************************************************
	
	/**
	 * More efficient, but not lexicographic order. I got it from the very end of:
	 * http://graphics.stanford.edu/~seander/bithacks.html#NextBitPermutation
	 */
	public static int getPreviousCombinationInBitFormat2( int numOfAgents, int size, int combination)
	{
		int t = (combination | (combination - 1)) + 1;  
		return( t | ((((t & -t) / (combination & -combination)) >> 1) - 1) );
		
		//A DIFFERENT VERSION, SAME EFFICIENCY IT SEEMS

		//int t = combination | (combination - 1);
		//return( (t + 1) | (((~t & -~t) - 1) >> (Integer.numberOfTrailingZeros(combination) + 1)) );
	}
	
	//******************************************************************************************************
	
	/**
	 * Given a coalition in a list of coalitions that is ordered as in DCVD, this method sets this coalition
	 * to be the coalition located after it. HERE, THE COMBINATION IS REPRESENTED IN BIT FORMAT.
	 */
	public static int getNextCombinationInBitFormat( int numOfAgents, int size, int combination)
	{
		int k2=0;
		
		//Initializing the index of the agent that we are trying to identify. Here, "size-1"
		int i=size-1; //means that we trying to identify the last agent in the coalition.
		
		/* maxPossibleValueForFirstAgent: is the maximum value that the first agent in the coalition can have.
		 * For example, if we have coalitions of size 3 out of 9 agents, then, since each combination is ordered
		 * in an ascending order, the first agent cannot be greater than 7, and that is in combination: (7,8,9)
		 */
		final int maxPossibleValueForFirstAgent = (int)(numOfAgents-size+1);
		for(int k=numOfAgents; k>0; k--)
		{
			if((combination & (1<<(k-1))) != 0) //If agent "k" is in the coalition
			{
				if((combination & (1<<(k-2))) == 0) //If agent "k-1" is not in the coalition
				{
					combination &= (1<<(k-1)) - 1; //copy the part in "combination" that is
					//before "k", and set the remaining part to zeros
					
					combination += (1<<(k-2)); //This way, we replace agent "k" with "k-1"				

					for(int j=i+1; j<size; j++) //set the remaining agents
						combination += (1<<(maxPossibleValueForFirstAgent+j - 1));

					return( combination );
				}
				i--;
				if(i==0)
				{
					//Store the first agent in the coalition
					k2 = k-1;
					while( (combination & (1<<(k2-1))) == 0 ) { //while agent "k2" is not in "combination"
						k2--;
					}
					break;
				}
			}
		}
		//If we reach this instruction, it means that we reached a special case
		combination = (1<<(k2-2)); //Initialize the coalition to contain only agent "k2-1"
		for(int j=1; j<size; j++)
			//Add agent "(maxPossibleValueForFirstAgent+j)" to "combination"
			combination += (1 << (int)( (maxPossibleValueForFirstAgent+j) - 1));
		
		return( combination );
	}

	//******************************************************************************************************
	
	/**
	 * This method generates the coalitions located at the given index (assuming that
	 * the coalitions are order as in DCVC).
	 * 
	 * IMPORTANT EXAMPLE:
	 * We have 10 possible coalitions of size 3 out of 5 agents. Now if index=0, then this
	 * method would return "3,4,5", and if index=9, the method would return "1,2,3" 
	 */
	public static int[] getCombinationAtGivenIndex(int size, int index, int numOfAgents)
	{
		//Initialization
		index++;
		initPascalMatrix( numOfAgents+1, numOfAgents+1 );
		int[] M=new int[size];
		boolean done=false;		
	
		/*1*/ int j=1; int s1=size;
		do
		{
			//Check the values: PascalMatrix[s1,1],PascalMatrix[s1,2],...
			/*2*/ int x=1;  while( pascalMatrix[s1-1][x-1] < index )  x++;
	
			/*3*/ M[j-1]=(int)( (numOfAgents-s1+1)-x+1 );
	
			/*4*/ if( pascalMatrix[s1-1][x-1]==index )
			{
				//Set the rest of the coalition as follows:
				for(int k=j; k<=size-1; k++) M[k]=(int)( M[k-1]+1 );
				done=true;
			}
			else //Otherwise
			{
				j++;  index -=pascalMatrix[s1-1][x-2];  s1--;
			}
		}
		while( done==false );
		return(M);
	}

	//*********************************************************************************************************
	
	/**
	 * Given a coalition C : |C|=s, this method returns the index of C in the list of coalitions of size s
	 * IMPORTANT EXAMPLE:
	 * We have 10 possible coalitions of size 3 out of 5 agents. Now if the coalition is "3,4,5",
	 * then this method would return 0, and if the coalition is "1,2,3", the method returns 9.  
	 */	
	public static int getIndexOfCombination( final int[] combination, final int size, final int numOfAgents )
	{
		long indexOfCombination=0;
		if( size==1 )
			indexOfCombination = numOfAgents-combination[0]+1;
		else
		{
			boolean sequence=true;
			for(int i1=size-1; i1>=1; i1--) {
				if( combination[i1]-combination[i1-1]>1 ) {
					indexOfCombination = pascalMatrix[ size-i1-1 ][ (numOfAgents-size+i1)-combination[i1]+1 ];
					for(int i2=i1-1; i2>=0; i2--) {					
						indexOfCombination += pascalMatrix[ size-i2-1 ][ (numOfAgents-size+i2)-combination[i2] ];
					}
					sequence=false;
					break;
				}
			}
			if( sequence )
				indexOfCombination=pascalMatrix[ size-1 ][ numOfAgents-size-combination[0]+1 ];
		}
		return( ((int)indexOfCombination) - 1 );
	}
}