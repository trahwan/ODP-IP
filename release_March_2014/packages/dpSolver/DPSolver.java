/**
 * The dynamic programming solvers. This code is used for either DP, IDP, ODP, or the IDP part of ODP-IP
 * @author Talal Rahwan
 */
package dpSolver;

import mainSolver.Result;
import inputOutput.*;
import general.Combinations;
import general.General;
import general.SubsetEnumerator;

public class DPSolver {
	int[] t; // t[i] contains the first half of the best split of coalition i (where the coalition is represented in bit format)
	//this is only used by DP, i.e., it is not used by IDP, ODP, or the IDP part of ODP-IP
    
    private double[] f;
    
    private boolean stop = false;
    
    private Input input;
    
    private Result result;
    
    public DPSolver( Input input, Result result ){
    	this.input = input;
    	this.result = result;
    }
    
    //*********************************************************************************************************
    
    public void set_f( int index, double value ){
    	if( input.solverName == SolverNames.ODPIP )
    		result.idpSolver_whenRunning_ODPIP.updateValueOfBestPartitionFound( index, value);
    	else    		
    		f[ index ] = value;
    }
    public double get_f( int index ){
    	if( input.solverName == SolverNames.ODPIP )
    		return result.idpSolver_whenRunning_ODPIP.getValueOfBestPartitionFound( index );
    	else
    		return f[ index ];
    }
    
    //*********************************************************************************************************
    
	public void setStop( boolean value )
	{
		stop = value;
	}
	public boolean getStop()
	{
		return stop;
	}
		
	//*********************************************************************************************************
	
    /**
     * This is the optimal dynamic programming algorithm, ODP
     */
	public void runODP()
    {
		int n = input.numOfAgents;

   		f = new double[ input.coalitionValues.length ];
   		for(int i=0; i<f.length; i++)
   			set_f(i, input.coalitionValues[i]); //Initialize every f(C) to be equal to v(C)
    	set_f(0,0); //just in case, set the f value of the empty set to 0

		/* This commented code is needed for ODP to set the f table that only has entries for coalitions containing {1,2}.
		   However, I do not use it. Instead, I create an f table for all coalitions, and only set those containing {1,2}.
		   This way, every coalition becomes the index to its f entry. Besides, the code below is not 100% tested.
		   */ 
    	//int numOfCoalitionsThatContain12 = (1<< (n-2));
   		//f = new double[ numOfCoalitionsThatContain12 ]; //because {1,2} is implicitly in each coalition
   		//for(int C=0; C < numOfCoalitionsThatContain12; C++)
   		//	set_f( C , input.coalitionValues[ (C<<2) + 3 ] ); // + 3 means: U {1,2}

    	//initialization
    	long[] requiredTimeForEachSize = new long[ n+1 ];
    	requiredTimeForEachSize[1] = 0;
    	long[] startTimeForEachSize = new long[ n+1 ];
    	int A = (1 << n) - 1; // the set of agents, each "true" bit in A represents an agent.
    	int bestHalfOfGrandCoalition=-1;
        long startTime = System.currentTimeMillis();
        result.set_dpMaxSizeThatWasComputedSoFar(1);

        //Step 2 (evaluate the possible splits of every coalition of size 2, 3, 4, ...)
        for (int s = 0; s <= n-2; s++)//For every size
        {        	
        	startTimeForEachSize[ s+2 ] = System.currentTimeMillis();
        	if( s == 0 ){
        		evaluateSplitsOf12();
        	}else{
        		if( s < n-2 ){        			
        			//Evaluate the possible splits of the current coalition
        			SubsetEnumerator subsetEnumerator = new SubsetEnumerator(n-2, s);
        			int C = subsetEnumerator.getFirstSubset() << 2;  //set C to be the "first" coalition in the list (where {1,2} not in C)
        			int markThatListHasFinished = (1<<(n-2)) << 2;
        			while(C < markThatListHasFinished){  //while C has not gone outside the list
        				evaluateSplitsOptimally( C, s, A, n );
        				C = subsetEnumerator.getNextSubset() << 2;  //set C to be the "next" coalition in the list (where {1,2} not in C)
        			}
        		}else{
        			bestHalfOfGrandCoalition = evaluateSplitsOfGrandCoalition();
        		}
        	}
        	//Update the value of the best CS found so far by ODP
        	if( s < n-2 ){
   				bestHalfOfGrandCoalition = evaluateSplitsOfGrandCoalition();
   				int[] bestCSFoundSoFar = getOptimalSplit( A, bestHalfOfGrandCoalition);
   				int[][] bestCSFoundSoFar_byteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat( bestCSFoundSoFar, n );
   				result.updateDPSolution( bestCSFoundSoFar_byteFormat , input.getCoalitionStructureValue( bestCSFoundSoFar_byteFormat ) );
   			}
    		//Print the time that was taken to perform this step 
    		requiredTimeForEachSize[ s+2 ] = System.currentTimeMillis() - startTimeForEachSize[ s+2 ];
    		System.out.print("    The time for ODP to finish evaluating the splittings of coalitions of size "+(s+2)+" is: "+requiredTimeForEachSize[s+2]);
    		System.out.println(".  The best CS found so far: "+General.convertArrayToString(result.get_dpBestCSFound())+" , its value is: "+result.get_dpValueOfBestCSFound());

    		//Update the maximum size that DP has finished searching
   			result.set_dpMaxSizeThatWasComputedSoFar( s+2 );
        }        
        //Initialize the result
        result.dpTimeForEachSize = requiredTimeForEachSize;

        //Step 3 (divide the grand coalition in the optimal way using the f table)
        int[] bestCSFound = getOptimalSplit( A, bestHalfOfGrandCoalition);
        
        //Set the final result
        result.dpTime = System.currentTimeMillis() - startTime;
        int[][] dpBestCSInByteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat( bestCSFound, n );
        result.updateDPSolution( dpBestCSInByteFormat , input.getCoalitionStructureValue( dpBestCSInByteFormat ) );
    }
	
    //*********************************************************************************************************

	/**
     * Run DP or IDP, or the IDP part of ODP-IP.
     * To specify which algorithm to run, set input.solverName to either SolverNames.DP or SolverNames.IDP, or SolverNames.ODPIP
     */
    public void runDPorIDP()
    {
    	if( input.solverName != SolverNames.ODPIP ){
    		f = new double[ input.coalitionValues.length ];
    		for(int i=0; i<f.length; i++)
    			set_f( i, input.coalitionValues[i] );
    	}    	
    	int numOfAgents = input.numOfAgents;
    	set_f(0,0); //just in case, set the value of the empty set to 0

    	//initialization
    	long[] requiredTimeForEachSize = new long[ numOfAgents+1 ];
    	requiredTimeForEachSize[1] = 0;
    	long[] startTimeForEachSize = new long[ numOfAgents+1 ];
    	int grandCoalition = (1 << numOfAgents) - 1;
    	int numOfCoalitions = 1 << numOfAgents;
    	int bestHalfOfGrandCoalition=-1;
        long startTime = System.currentTimeMillis();
        result.set_dpMaxSizeThatWasComputedSoFar(1);

        //Step 1 (initialization for step 2). Basically, initialize the t table.
        if( input.solverName == SolverNames.DP ){ //Initialize the best half of every coalition to be the coalition itself.
        	t = new int[numOfCoalitions];
        	for (int coalition = 0; coalition < numOfCoalitions; coalition++)
        		t[coalition] = coalition;
        }        
        //Step 2 (evaluate the possible splits of every coalition of size 2, 3, 4, ...)
        for (int curSize = 2; curSize <= numOfAgents; curSize++)//For every size
        {
        	if( (input.solverName == SolverNames.IDP)||(input.solverName == SolverNames.ODPIP) )
        		if(( (int)(Math.floor((2*numOfAgents)/(double)3)) < curSize )&&( curSize<numOfAgents )) continue;
        	
        	//check if all splits of this size will be evaluated
        	boolean allSplitsOfCurSizeWillBeEvaluated = true;
        	for(int sizeOfFirstHalf=(int)Math.ceil(curSize/(double)2); sizeOfFirstHalf<curSize; sizeOfFirstHalf++)
        		if( (input.solverName == SolverNames.IDP)||(input.solverName == SolverNames.ODPIP) )
        			if(( sizeOfFirstHalf > numOfAgents-curSize )&&( curSize!=numOfAgents )){
        				allSplitsOfCurSizeWillBeEvaluated = false;
        				break;
        			}
        	startTimeForEachSize[ curSize ] = System.currentTimeMillis();        	
        	
    		//If the coalition happens to be the grand coalition, then deal with this case saperately
        	if( curSize < numOfAgents )
        	{
        		int numOfCoalitionsOfCurSize = (int)Combinations.binomialCoefficient(numOfAgents, curSize);

        		if( allSplitsOfCurSizeWillBeEvaluated ) //in this case, the evaluation of the splits can be done more efficiently
        		{
        			//Compute the number of possible splits of any coalition of size = "curSize"
        			int numOfPossibleSplits = 1 << curSize;
        			//Evaluate the possible splits of the current coalition
        			int[] curCoalition = Combinations.getCombinationAtGivenIndex( curSize, numOfCoalitionsOfCurSize-1, numOfAgents);
        			evaluateSplitsEfficiently( curCoalition, curSize, numOfPossibleSplits );
        			for(int i=1; i<numOfCoalitionsOfCurSize; i++ ) {
        				Combinations.getPreviousCombination( numOfAgents, curSize, curCoalition );
        				evaluateSplitsEfficiently( curCoalition, curSize, numOfPossibleSplits );
        				if( getStop() ) break;
        			}
        		}
        		else
        		{
        			//Compute the number of possible splits of any coalition of size = "curSize"
        			int numOfPossibleSplits = 1 << curSize;
        			//Compute the number of possible splits for every partition of "curSize" into "sizeOfFirstHalf" + "sizeOfSecondHalf"
        			long[] numOfPossibleSplitsBasedOnSizeOfFirstHalf = computeNumOfPossibleSplitsBasedOnSizeOfFirstHalf( curSize );
        			//Evaluate the possible splits of the current coalition
        			int[] curCoalition = Combinations.getCombinationAtGivenIndex( curSize, numOfCoalitionsOfCurSize-1, numOfAgents);
        			for(int i=1; i<numOfCoalitionsOfCurSize; i++ ) {
        				Combinations.getPreviousCombination( numOfAgents, curSize, curCoalition );
        				if( (input.solverName == SolverNames.DP) || (input.useEfficientImplementationOfIDP) )
            				evaluateSplitsEfficiently( curCoalition, curSize, numOfPossibleSplits );        				
        				else
        					evaluateSplits( curCoalition, curSize, numOfPossibleSplitsBasedOnSizeOfFirstHalf );
        				
        				if( getStop() ) break;
        			}
        		}
        		if( getStop() ) break;
        	}else
        		bestHalfOfGrandCoalition = evaluateSplitsOfGrandCoalition();

        	//Update the value of the best CS found so far by IDP
        	if( curSize < numOfAgents ){
   				bestHalfOfGrandCoalition = evaluateSplitsOfGrandCoalition();
   				int[] bestCSFoundSoFar = getOptimalSplit( grandCoalition, bestHalfOfGrandCoalition);
   				int[][] bestCSFoundSoFar_byteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat( bestCSFoundSoFar, numOfAgents );
   				result.updateDPSolution( bestCSFoundSoFar_byteFormat , +input.getCoalitionStructureValue( bestCSFoundSoFar_byteFormat ) );
   				if( input.solverName == SolverNames.ODPIP )
   					result.updateIPSolution( bestCSFoundSoFar_byteFormat , input.getCoalitionStructureValue( bestCSFoundSoFar_byteFormat ) );
   			}
    		//Print the time that was taken to perform this step 
    		requiredTimeForEachSize[ curSize ] = System.currentTimeMillis() - startTimeForEachSize[curSize];
    		if( input.solverName == SolverNames.DP ){
    			System.out.println("   The time for DP to finish evaluating the splittings of coalitions of size "+curSize+" is: "+requiredTimeForEachSize[curSize]);
    		}else{ //i.e., if it is IDP or ODP-IP
    			System.out.print("   The time for IDP to finish evaluating the splittings of coalitions of size "+curSize+" is: "+requiredTimeForEachSize[curSize]);
    			System.out.println(".  The best CS found so far by IDP is : "+General.convertArrayToString(result.get_dpBestCSFound())+" , its value is: "+result.get_dpValueOfBestCSFound());
    		}
    		//Update the maximum size that DP has finished searching
   			result.set_dpMaxSizeThatWasComputedSoFar( curSize );
        }
        if( getStop() ) return;
        
        //Initialize the result
        result.dpTimeForEachSize = requiredTimeForEachSize;

        //Step 3 (divide the grand coalition in the optimal way using the f table)
        int[] bestCSFound = getOptimalSplit( grandCoalition, bestHalfOfGrandCoalition);
        
        //Set the final result
        result.dpTime = System.currentTimeMillis() - startTime;
        int[][] dpBestCSInByteFormat = Combinations.convertSetOfCombinationsFromBitToByteFormat( bestCSFound, numOfAgents );
        result.updateDPSolution( dpBestCSInByteFormat , input.getCoalitionStructureValue( dpBestCSInByteFormat ) );
        if( input.solverName == SolverNames.ODPIP )
        	result.updateIPSolution( dpBestCSInByteFormat , input.getCoalitionStructureValue( dpBestCSInByteFormat ) );
        
        //Print the distribution of f if required
        if( input.printDistributionOfThefTable )
        	printDistributionOfThefTable();
        
        if( input.printPercentageOf_v_equals_f )
        	printPercentageOf_v_equals_f();
    }
	
    //*********************************************************************************************************

    /**
     * Given a size, we can split it into [size-1,1], and [size-2,2],and so on. Now suppose that there are 1000 possible
     * splits that match [size-1,1], and 8700 splits that match [size-2,2], and so on. This method computes these numbers
     * of possible splits.
     * 
     * numOfPossibleSplitsBasedOnSizeOfFirstHalf[ 3 ] is the number of possible splits, where the first half is equals 3
     */
    private long[] computeNumOfPossibleSplitsBasedOnSizeOfFirstHalf( int size )
    {
    	long[] numOfPossibleSplitsBasedOnSizeOfFirstHalf = new long[size];
    	for(int sizeOfFirstHalf=(int)Math.ceil(size/(double)2); sizeOfFirstHalf<size; sizeOfFirstHalf++){
    		int sizeOfSecondHalf = (int)(size - sizeOfFirstHalf);
    		if(( (size % 2) == 0 )&&( sizeOfFirstHalf == sizeOfSecondHalf ))
    			numOfPossibleSplitsBasedOnSizeOfFirstHalf[ sizeOfFirstHalf ] = Combinations.binomialCoefficient(size, sizeOfFirstHalf )/2;
    		else
    			numOfPossibleSplitsBasedOnSizeOfFirstHalf[ sizeOfFirstHalf ] = Combinations.binomialCoefficient(size, sizeOfFirstHalf );    		
    	}
    	return( numOfPossibleSplitsBasedOnSizeOfFirstHalf );
    }    
    
    //*********************************************************************************************************
    
    /**
     * Splits the given coalition in an optimal way using the f table (i.e., it makes the optimal
     * movements in the coalition structure graph). Here, coalitions are represented IN BIT FORMAT.
     */
    private int[] getOptimalSplit( int coalitionInBitFormat, int bestHalfOfCoalition )
    {
    	int[] optimalSplit;
    	if( bestHalfOfCoalition == coalitionInBitFormat )
    	{
    		optimalSplit = new int[1];
    		optimalSplit[0] = coalitionInBitFormat;
    	}
    	else
    	{
    		//Initialization
			int[] arrayOfBestHalf = new int[2];
			int[][] arrayOfOptimalSplit = new int[2][];
    		int[] arrayOfCoalitionInBitFormat = new int[2]; 

    		//Set the two halves
    		arrayOfCoalitionInBitFormat[0] = bestHalfOfCoalition;
    		arrayOfCoalitionInBitFormat[1] = coalitionInBitFormat - bestHalfOfCoalition;
    		
    		//For each one of the two halves
    		for( int i=0; i<2; i++ )
    		{
    			if( (input.solverName == SolverNames.DP) ) 
    				arrayOfBestHalf[i] = t[ arrayOfCoalitionInBitFormat[i] ];
    			else
    				arrayOfBestHalf[i] = getBestHalf( arrayOfCoalitionInBitFormat[i] ); //We need to recompute the best half
    			
    			arrayOfOptimalSplit[i] = getOptimalSplit( arrayOfCoalitionInBitFormat[i], arrayOfBestHalf[i] );
    		}
        	//Set "optimalSplit" by combining "arrayOfOptimalSplit[0]" with "arrayOfOptimalSplit[1]" 
        	optimalSplit = new int[ arrayOfOptimalSplit[0].length + arrayOfOptimalSplit[1].length ];
        	int k=0;
        	for( int i=0; i<2; i++ )
        		for( int j=0; j<arrayOfOptimalSplit[i].length; j++ )
        		{
        			optimalSplit[k] = arrayOfOptimalSplit[i][j];
        			k++;
        		}
    	}
    	return( optimalSplit );
    }
    
    //*********************************************************************************************************    
    
    /**
     * Split each coalition in the given structure in an optimal way using the f table (i.e., it makes the
     * optimal movements in the coalition structure graph). Here, coalitions are represented IN BYTE FORMAT. 
     */
    public int[][] getOptimalSplit( int[][] CS )
    {   
    	//Initialization
    	int[][] optimalSplit = new int[ CS.length ][]; 
    	int numOfCoalitionsInFinalResult=0;
    	
    	//For every coalition CS[i] in CS, find the optimal split of CS[i], and store it in "optimalSplit[i]"
    	for( int i=0; i<CS.length; i++ )
    	{
   			int coalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( CS[i] );
   			int bestHalfOfCoalitionInBitFormat = getBestHalf( coalitionInBitFormat );
   			optimalSplit[i] = getOptimalSplit( coalitionInBitFormat, bestHalfOfCoalitionInBitFormat );
    		numOfCoalitionsInFinalResult += optimalSplit[i].length;
    	}
    	//Group the optimal splits of different coalitions into "finalResult"
    	int[][] finalResult = new int[ numOfCoalitionsInFinalResult ][];
    	int k=0;
    	for( int i=0; i<CS.length; i++ ) //For every coalition CS[i] in CS
    	{
    		for( int j=0; j<optimalSplit[i].length; j++ ) //For every coalition in the optimal split of CS[i]
    		{
    			finalResult[k] = Combinations.convertCombinationFromBitToByteFormat( optimalSplit[i][j] , input.numOfAgents );
    			k++;
    		}
    	}
    	return( finalResult );
    }
    
    //*********************************************************************************************************
    
    /**
     * Evaluate the possible splittings of the grand coalition. The way this is done 
     * is similar to the way IP searches the second layer while scanning the input.
     */
    private int evaluateSplitsOfGrandCoalition()
    {    	
    	//Initialization
    	double curValue=-1;
    	double bestValue=-1;
    	int bestHalfOfGrandCoalitionInBitFormat=-1;
    	int numOfCoalitions = 1 << input.numOfAgents;
    	int grandCoalition = (1 << input.numOfAgents) - 1;

    	//Check the possible ways of splitting the grand coalition into two (non-empty) coalitions
    	for (int firstHalfOfGrandCoalition=(numOfCoalitions/2)-1; firstHalfOfGrandCoalition<numOfCoalitions; firstHalfOfGrandCoalition++)
    	{
    		int secondHalfOfGrandCoalition = numOfCoalitions-1-firstHalfOfGrandCoalition;
    		curValue = get_f(firstHalfOfGrandCoalition) + get_f(secondHalfOfGrandCoalition); 
    		if( curValue > bestValue )
    		{
    			bestValue = curValue;
    			bestHalfOfGrandCoalitionInBitFormat = firstHalfOfGrandCoalition;
    		}
    	}    	
    	//Deal with the case where the first half is the grand coalition itself
    	int firstHalfOfGrandCoalition = grandCoalition;
    	curValue = get_f( firstHalfOfGrandCoalition );
		if( curValue > bestValue )
		{
			bestValue = curValue;
			bestHalfOfGrandCoalitionInBitFormat = firstHalfOfGrandCoalition;
		}    	
    	//Set t and f
		set_f( grandCoalition, bestValue);
		if( input.solverName == SolverNames.DP ) //Then use the t table
        	t[ grandCoalition ] = bestHalfOfGrandCoalitionInBitFormat;
		
    	return( bestHalfOfGrandCoalitionInBitFormat );
    }
    
    //*********************************************************************************************************
    
    /**
     * Given a coalition of a particular size, this method evaluates the possible splits of this
     * coalition into two. Here, the evaluation is based on f (not v) of each of the two halves.
     * 
     * NOTE: this will be called when running DP or IDP, but not when running ODP!!
     */
    private void evaluateSplits( int[] coalitionInByteFormat, int coalitionSize, long[] numOfPossibleSplitsBasedOnSizeOfFirstHalf )
    {
    	double curValue=-1;
    	double bestValue=-1;
    	int bestHalfOfCoalitionInBitFormat=-1;
    	int numOfAgents = input.numOfAgents;
    	int coalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( coalitionInByteFormat );

    	//bit[i] is the bit representing agent a_i (e.g. given 4 agents, bit[2]=2=0010, bit[3]=4=0100, etc.)
		int[] bit = new int[numOfAgents+1];
		for(int i=0; i<numOfAgents; i++)
			bit[i+1] = 1 << i;
		
    	//Check the possible ways of splitting the coalition into two (non-empty) coalitions
    	for(int sizeOfFirstHalf=(int)Math.ceil(coalitionSize/(double)2); sizeOfFirstHalf<coalitionSize; sizeOfFirstHalf++)
    	{
    		//int sizeOfSecondHalf = (int)(coalitionSize - sizeOfFirstHalf);
    		
    		//If we want to evaluate only the useful splits, and the size of the biggest half (which, here, happens to
    		//be the first half) is greater than the number of agents minus the size of the coalition, then continue.
    		if(( (input.solverName == SolverNames.IDP)||(input.solverName == SolverNames.ODPIP) )&&( sizeOfFirstHalf > numOfAgents-coalitionSize )
    				&&( coalitionSize!=numOfAgents )) continue;
    		
        	//Set the initial indices of the members of the first half
    		int[] indicesOfMembersOfFirstHalf = new int[ sizeOfFirstHalf ];
    		for(int i=0; i<sizeOfFirstHalf; i++)
    			indicesOfMembersOfFirstHalf[i] = (int)(i+1);

    		//Compute the first half (in bit format)
    		int firstHalfInBitFormat=0;
        	for(int i=0; i<sizeOfFirstHalf; i++)
        		firstHalfInBitFormat += bit[ coalitionInByteFormat[ indicesOfMembersOfFirstHalf[i]-1 ] ];
        	
        	//Compute the second half (in bit format)
        	int secondHalfInBitFormat = (coalitionInBitFormat-firstHalfInBitFormat);        	
        	
        	//Update the functions t and f    	
            curValue = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );
            if( bestValue < curValue ){
                bestValue = curValue;
                if( input.solverName == SolverNames.DP )
                	bestHalfOfCoalitionInBitFormat = firstHalfInBitFormat;
            }            
        	//Do the same for the remaining possibilities of the first and second halves
            for(int j=1; j<numOfPossibleSplitsBasedOnSizeOfFirstHalf[ sizeOfFirstHalf ]; j++)
            {
            	/**/
    			Combinations.getPreviousCombination( coalitionSize, sizeOfFirstHalf, indicesOfMembersOfFirstHalf );
            	
            	//Compute the first half (in bit format)
            	firstHalfInBitFormat=0;
            	for(int i=0; i<sizeOfFirstHalf; i++)
            		firstHalfInBitFormat += bit[ coalitionInByteFormat[ indicesOfMembersOfFirstHalf[i]-1 ] ];
            	/**/
            	//Combinations.getPreviousCombinationInBitFormat2(numOfAgents, sizeOfFirstHalf, firstHalfInBitFormat);

            	//Compute the second half (in bit format)
            	secondHalfInBitFormat = (coalitionInBitFormat-firstHalfInBitFormat);           	
            	
            	//Update the functions t and f    	
            	curValue = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );
            	if( bestValue < curValue ) {
            		bestValue = curValue;
            		if( input.solverName == SolverNames.DP )
            			bestHalfOfCoalitionInBitFormat = firstHalfInBitFormat;
            	}
            }            
    	}    	
        //Deal with the case where the first half is the coalition itself
		int firstHalfInBitFormat = coalitionInBitFormat;
		curValue = get_f( firstHalfInBitFormat );
        if( bestValue < curValue ){
            bestValue = curValue;
            if( input.solverName == SolverNames.DP )
            	bestHalfOfCoalitionInBitFormat = firstHalfInBitFormat;
        }    	

        //Update the maximum value of f for the size that is equal to "coalitionSize"
        if( input.solverName == SolverNames.ODPIP )
        	if( result.get_max_f( coalitionSize-1 ) < bestValue )
        		result.set_max_f( coalitionSize-1 ,   bestValue );
        
        //Finalizing
        set_f( coalitionInBitFormat, bestValue );
        if( input.solverName == SolverNames.DP )
        	t[ coalitionInBitFormat ] = bestHalfOfCoalitionInBitFormat;
    }
    
    //*********************************************************************************************************
    
    /**
     * Given a coalition of a particular size, this method evaluates the possible splits of this
     * coalition into two. Here, the evaluation is based on f (not v) of each of the two halves.
     * NOTE: here, the iteration over all splits is done efficiently  
     */
    private void evaluateSplitsEfficiently( int[] coalitionInByteFormat, int coalitionSize, int numOfPossibilities )
    {
    	double curValue=-1;
    	double bestValue=-1;
    	int bestHalfOfCoalitionInBitFormat=-1;
    	int coalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( coalitionInByteFormat );

    	bestValue=input.getCoalitionValue(coalitionInBitFormat);
    	bestHalfOfCoalitionInBitFormat=coalitionInBitFormat;

    	//Check the possible ways of splitting the grand coalition into two (non-empty) coalitions
    	for (int firstHalfInBitFormat = coalitionInBitFormat-1 & coalitionInBitFormat; /*firstHalfInBitFormat > 0*/; firstHalfInBitFormat = firstHalfInBitFormat-1 & coalitionInBitFormat)
    	{
    		int secondHalfInBitFormat = coalitionInBitFormat^firstHalfInBitFormat;

    		//Update the functions t and f    	
    		curValue = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );

    		if(bestValue<=curValue)
    		{
    			bestValue = curValue;
    			if( input.solverName == SolverNames.DP ){ //i.e., if we are using the t table
    				if( Integer.bitCount(firstHalfInBitFormat) > Integer.bitCount(secondHalfInBitFormat) )
    					bestHalfOfCoalitionInBitFormat = firstHalfInBitFormat;
    				else
    					bestHalfOfCoalitionInBitFormat = secondHalfInBitFormat;
    			}
    		}
    		if((firstHalfInBitFormat & (firstHalfInBitFormat-1))==0)break;
    	}
    	//Update the maximum value of f for the size that is equal to "coalitionSize"
    	if( input.solverName == SolverNames.ODPIP )
    		if( result.get_max_f( coalitionSize-1 ) < bestValue )
    			result.set_max_f( coalitionSize-1 ,   bestValue );

    	//Finalizing
    	set_f( coalitionInBitFormat, bestValue );
    	if( input.solverName == SolverNames.DP )
    		t[ coalitionInBitFormat ] = bestHalfOfCoalitionInBitFormat;
    }

    //*********************************************************************************************************
    
    /**
     * Given a non-empty coalition C \subset A\{1,2}, this method evaluates the possible splits of this
     * coalition into two coalitions, C1 and C2 (including the split {\emptyset,C}). For every such split,
     * compute: f(C1 U {1}) + f(C2 U {2})
     *          f(C1 U {2}) + f(C2 U {1})
     *          if( min agents in C1 < min agent in A \ (C U {1,2}) )
     *              f(C2 U {1,2}) + f(C1)
     *          if( min agents in C2 < min agent in A \ (C U {1,2}) )
     *              f(C1 U {1,2}) + f(C2)
     */
    private void evaluateSplitsOptimally( int C, int sizeOfC, int grandCoalition, int numOfAgents )
    {
    	double bestValue=input.getCoalitionValue( C + 3 ); //curValue = v( C U {1,2} )
    	
    	//Compute the minimum agent in: "remainingAgents", which is: A \ ( C U {1,2} )
    	int remainingAgents = grandCoalition - C - 3;
    	int minAgent =0; //here, I put "=0" just to stop the compiler from complaining 
    	for(int i=2; i<numOfAgents; i++)
			if ((remainingAgents & (1<<i)) != 0){ //If agent "i+1" is a member of "remainingAgents"
				minAgent = i+1;
				break;
			}
    	//set "acceptableMinAgents" to be the set of all agents from 1 to minAgent-1
    	int acceptableMinAgents = (1 << (minAgent-1)) - 1;
    	
    	//Check all splits of C into two coalitions, including the split {C,\emptyset}
    	double value;
    	for (int C1 = C & C; /* C1>0 */; C1 = C1-1 & C)
    	{
    		int C2 = C-C1;

    		value = input.coalitionValues[ C1+1 ] + input.coalitionValues[ C2+2 ];
    		if( bestValue < value ) bestValue = value;

    		value = input.coalitionValues[ C1+2 ] + input.coalitionValues[  C2+1 ];
    		if( bestValue < value ) bestValue = value;

    		if( (C1 & acceptableMinAgents)!=0 ){
    			value = input.coalitionValues[ C1 ] + get_f( C2+3 );
    			if( bestValue < value ) bestValue = value;
    		}
    		if( (C2 & acceptableMinAgents)!=0 ){
    			value = input.coalitionValues[ C2 ] + get_f( C1+3 );
    			if( bestValue < value ) bestValue = value;
    		}
    		if((C1 & (C1-1))==0) break; // check if we have seen all splits of C into two non-empty coalitions, {C1,C2}
    	}
    	set_f( C + 3, bestValue ); //set f[ C U {1,2}] = bestValue
    	
    	//Update the maximum value of f for the size that is equal to "sizeOfC + 2"
    	if( input.solverName == SolverNames.ODPIP )
    		if( result.get_max_f( (sizeOfC+2)-1 ) < bestValue ) //we use: "sizeOfC+2" because we want: C U {1,2}
    			result.set_max_f( (sizeOfC+2)-1 ,   bestValue );
    }
    
    //	`*********************************************************************************************************
    
    /**
     * Compute the optimal split of coalition {1,2}  
     */
    private void evaluateSplitsOf12()
    {
    	int coalitionSize = 2;
    	int coalitionInBitFormat = 3;
    	int firstHalfInBitFormat = 1;
    	int secondHalfInBitFormat = 2;    	
    	double valueOfCoalition = input.getCoalitionValue(coalitionInBitFormat);
    	double valueOfSplit = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );
    	double bestValue = Math.max( valueOfCoalition, valueOfSplit );
    	set_f( coalitionInBitFormat, bestValue );
        if( input.solverName == SolverNames.ODPIP )
       		result.set_max_f( coalitionSize-1, bestValue );
    }
    
    //*********************************************************************************************************
    
    /**
     * Given a coalition, and given the value of the best split, find the best split, and return the first half
     */
    private int getBestHalf( int coalitionInBitFormat )
    {
    	double valueOfBestSplit = input.getCoalitionValue( coalitionInBitFormat );
    	int best_firstHalfInBitFormat = coalitionInBitFormat;
    	
        //bit[i] is the bit representing agent a_i (e.g. given 4 agents, bit[2]=2=0010, bit[3]=4=0100, etc.)
		int[] bit = new int[input.numOfAgents+1];
		for(int i=0; i<input.numOfAgents; i++)
			bit[i+1] = 1 << i;
    	
    	//Convert the original coalition from bit format to int format
		int[] coalitionInByteFormat = Combinations.convertCombinationFromBitToByteFormat(coalitionInBitFormat, input.numOfAgents);
    	
    	//Check the possible ways of splitting the coalition into two (non-empty) coalitions
		int coalitionSize = (int)coalitionInByteFormat.length;
		for(int sizeOfFirstHalf=(int)Math.ceil(coalitionSize/(double)2); sizeOfFirstHalf<coalitionSize; sizeOfFirstHalf++)
    	{
    		int sizeOfSecondHalf = (int)(coalitionSize - sizeOfFirstHalf);

    		//Compute the number of possible splits
    		long numOfPossibleSplits;
    		if(( (coalitionSize % 2) == 0 )&&( sizeOfFirstHalf == sizeOfSecondHalf ))
    			numOfPossibleSplits = Combinations.binomialCoefficient(coalitionSize, sizeOfFirstHalf )/2;
    		else
				numOfPossibleSplits = Combinations.binomialCoefficient(coalitionSize, sizeOfFirstHalf );    		
    		
        	//Set the initial indices of the members of the first half
    		int[] indicesOfMembersOfFirstHalf = new int[ sizeOfFirstHalf ];
    		for(int i=0; i<sizeOfFirstHalf; i++)
    			indicesOfMembersOfFirstHalf[i] = (int)(i+1);

    		//Compute the first half (in bit format)
    		int firstHalfInBitFormat=0;
        	for(int i=0; i<sizeOfFirstHalf; i++)
        		firstHalfInBitFormat += bit[ coalitionInByteFormat[ indicesOfMembersOfFirstHalf[i]-1 ] ];

        	//Compute the second half (in bit format)
        	int secondHalfInBitFormat = (coalitionInBitFormat-firstHalfInBitFormat);
        	
        	//Check if we have found a half that gives the value of the best split
        	if( get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat ) > valueOfBestSplit ){
            	best_firstHalfInBitFormat = firstHalfInBitFormat;
            	valueOfBestSplit = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );
        	}
        	//Do the same for the remaining possibilities of the first and second halves
            for(int j=1; j<numOfPossibleSplits; j++)
            {
    			Combinations.getPreviousCombination( coalitionSize, sizeOfFirstHalf, indicesOfMembersOfFirstHalf );

            	//Compute the first half (in bit format)
            	firstHalfInBitFormat=0;
            	for(int i=0; i<sizeOfFirstHalf; i++)
            		firstHalfInBitFormat += bit[ coalitionInByteFormat[ indicesOfMembersOfFirstHalf[i]-1 ] ];

            	//Compute the second half (in bit format)
            	secondHalfInBitFormat = (coalitionInBitFormat-firstHalfInBitFormat);

            	//Check if we have found a half that gives the value of the best split    	
            	if( get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat ) > valueOfBestSplit ){
                	best_firstHalfInBitFormat = firstHalfInBitFormat;
                	valueOfBestSplit = get_f( firstHalfInBitFormat ) + get_f( secondHalfInBitFormat );
            	}
            }
    	}
		//If we haven't found any split of which the value is equal to "valueOfBestSplit", then:
    	return( best_firstHalfInBitFormat );	
    }
    
    //******************************************************************************************************
	
    /**
     * Print the percentage of coalitions, C, for which: f(C) = v(C).
     * Should be used with DP, not IDP or ODP
     */
    private void printPercentageOf_v_equals_f()
    {
    	int numOfAgents = input.numOfAgents;
    	int totalNumOfCoalitions = 1 << numOfAgents;
    	int totalCounter=0;
    	long[] numOfCoalitionsOfParticularSize = new long[numOfAgents+1];
    	int[] counter = new int[numOfAgents+1];
    	for(int i=1; i<numOfAgents+1; i++){
    		counter[i]=0;
    		numOfCoalitionsOfParticularSize[i] = Combinations.binomialCoefficient(numOfAgents, i);
    	}
    	for(int i=1; i<totalNumOfCoalitions; i++)
    		if(input.getCoalitionValue(i)==get_f(i)){
    			counter[ Integer.bitCount(i) ]++;
    			totalCounter++;
    		}
    	System.out.println("percentage of all coalitions of that are optimal partitions of themselves is: "+((double)totalCounter/totalNumOfCoalitions));
    	for(int i=2; i<=numOfAgents; i++){
    		System.out.println("size: "+i+"  percentage: "+((double)counter[i]/numOfCoalitionsOfParticularSize[i]));
    		//System.out.println("        counter = "+counter[i]);
    		//System.out.println("numOfCoalitions = "+numOfCoalitionsOfParticularSize[i]);
    	}
    }
    
	//******************************************************************************************************
	
	/**
	 * Given the f values, get the "weighted" distribution. Meaning that every f(C) value is divided by the size of C
	 */
	private void printDistributionOfThefTable()
	{
		//Initialization
		int totalNumOfCoalitions = 1 << input.numOfAgents;
		int[] counter = new int[40];
		for(int i=0; i<counter.length; i++){
			counter[i]=0;
		}		
		//get the minimum and maximum values
		long min = Integer.MAX_VALUE;
		long max = Integer.MIN_VALUE;
		for(int i=1; i<totalNumOfCoalitions; i++){
			long currentWeightedValue = (long)Math.round( get_f(i) / Integer.bitCount(i) );
			if( min > currentWeightedValue )
				min = currentWeightedValue ;
			if( max < currentWeightedValue )
				max = currentWeightedValue ;
		}
		System.out.println("The maximum weighted f value is  "+max+"  and the minimum one is  "+min);
		
		//get the distribution		       
		for(int i=1; i<totalNumOfCoalitions; i++){
			long currentWeightedValue = (long)Math.round( get_f(i) / Integer.bitCount(i) );
			int percentageOfMax = (int)Math.round( (currentWeightedValue-min) * (counter.length-1) / (max-min) );
			counter[percentageOfMax]++;
		}
		//Printing
		System.out.println("The distribution of the weighted coalition values:");
		System.out.print(ValueDistribution.toString(input.valueDistribution)+"_f = [");
		for(int i=0; i<counter.length; i++)
			System.out.print(counter[i]+" ");
		System.out.println("]");
	}
}