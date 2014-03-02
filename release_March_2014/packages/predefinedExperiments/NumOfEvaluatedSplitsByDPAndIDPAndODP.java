package predefinedExperiments;

import general.Combinations;

public class NumOfEvaluatedSplitsByDPAndIDPAndODP
{
	/**
	 * Given different numbers of agents, this method computes the number
	 * of splits that are evaluated by DP, IDP and ODP 
	 */
	public void run()
	{
		//The numbers of agents for which we are going to compute the number of evaluations
		int minNumOfAgents = 5; int maxNumOfAgents = 40;

		long[] sumDP = new long[ maxNumOfAgents+1 ];
		long[] sumIDP = new long[ maxNumOfAgents+1 ];
		long[] sumODP = new long[ maxNumOfAgents+1 ];
		for(int numOfAgents=0; numOfAgents<=maxNumOfAgents; numOfAgents++){
			sumDP[numOfAgents]=0;
			sumIDP[numOfAgents]=0;
			sumODP[numOfAgents]=0;
		}
		for(int numOfAgents=minNumOfAgents; numOfAgents<=maxNumOfAgents; numOfAgents++){
			for(int sizeOfCoalitions=2; sizeOfCoalitions<=numOfAgents; sizeOfCoalitions++){
				sumIDP[numOfAgents] += computeNumOfSplitsEvaluatedByIDP(numOfAgents, sizeOfCoalitions ,false);
			}
			sumDP [numOfAgents]=(long)(0.5*(Math.pow(3,numOfAgents  )+1) - Math.pow(2,numOfAgents) );
			sumODP[numOfAgents]=(long)(0.5*(Math.pow(3,numOfAgents-1)-1)                           );
		}
		System.out.print("DP=[");
		for(int numOfAgents=minNumOfAgents; numOfAgents<=maxNumOfAgents; numOfAgents++)
			System.out.print( ((double)sumDP[numOfAgents]/sumDP[numOfAgents])  + " ");
		System.out.println("]");
		System.out.print("IDP=[");
		for(int n=minNumOfAgents; n<=maxNumOfAgents; n++)
			System.out.print( ((double)sumIDP[n]/sumDP[n])  + " ");
		System.out.println("]");
		System.out.print("ODP=[");
		for(int n=minNumOfAgents; n<=maxNumOfAgents; n++)
			System.out.print( ((double)sumODP[n]/sumDP[n])  + " ");
		System.out.println("]");
	}
	
	//*****************************************************************************************************
	
    /**
     * Compute the number of splits evaluated for each size by IDP
     */
    private long computeNumOfSplitsEvaluatedByIDP(int numOfAgents, int sizeOfCoalitions, boolean efficientImplementation)
    {
    	//deal with the special case where the coalition size = numOfAgents
    	if( sizeOfCoalitions == numOfAgents )
    	{
    		long numOfGrandCoalitionSplits = 1;
    		for(int size=1; size<=(numOfAgents-1)-1; size++)
    			numOfGrandCoalitionSplits += Combinations.binomialCoefficient((int)(numOfAgents-1), size);
    		return( numOfGrandCoalitionSplits );
    	}
    	//Compute the maximum size of coalitions that IDP would consider (excluding the grand coalition)
    	long dpMaxSize = (int)(Math.floor( (2 * numOfAgents) / (double)3 ));

    	//Check whether the coalition size means IDP won't evaluate any splitting
    	if(( dpMaxSize < sizeOfCoalitions )&&( sizeOfCoalitions < numOfAgents ))
    		return(0);

    	//deal with any case not considered above
    	if(( 1 < sizeOfCoalitions )&&( sizeOfCoalitions < numOfAgents ))
    	{
    		//Compute the number of possible coaitions whose size equals: "sizeOfCoalitions"
    		long numOfCoalitionsOfCurSize = Combinations.binomialCoefficient((int)numOfAgents, sizeOfCoalitions);

    		//Compute the number of possible splits for every coalition of size: "sizeOfCoalitions" into "sizeOfFirstHalf" + "sizeOfSecondHalf"
    		long[] numOfPossibleSplitsBasedOnSizeOfFirstHalf = computeNumOfPossibleSplitsBasedOnSizeOfFirstHalf( sizeOfCoalitions );
    		long numOfEvaluatedSplitsOfOneCoalition=0;
    		for(int sizeOfFirstHalf=(int)Math.ceil(sizeOfCoalitions/(double)2); sizeOfFirstHalf<sizeOfCoalitions; sizeOfFirstHalf++)
    		{
    			if( efficientImplementation == false )
    				if(( sizeOfFirstHalf > numOfAgents-sizeOfCoalitions )&&( sizeOfCoalitions!=numOfAgents )) continue;

    			numOfEvaluatedSplitsOfOneCoalition += numOfPossibleSplitsBasedOnSizeOfFirstHalf[ sizeOfFirstHalf ];
    		}
    		return( numOfCoalitionsOfCurSize * (numOfEvaluatedSplitsOfOneCoalition) );
    	}else{
    		return(0);
    	}
    }

    //*****************************************************************************************************
    
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
}