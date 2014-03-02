package predefinedExperiments;

import general.Combinations;
import general.General;
import general.IntegerPartition;
import ipSolver.Subspace;

public class NumOfCSandSubspacesPrunedByIDP
{
    /**
     * Prints the number of coalition structures and subspaces that are pruned by IDP at different coalition sizes.
     */
	public void run()
    {
		int minNumOfAgents = 10;
		int maxNumOfAgents = 25;
		for(int numOfAgents = minNumOfAgents; numOfAgents <= maxNumOfAgents; numOfAgents++)
		{
			int dpMaxSize = (int)(Math.floor((2*numOfAgents)/(double)3)); // because we are dealing with IDP

			long[] dpNumOfPrunedSubspaces = new long[ dpMaxSize+1 ];
			long[] dpNumOfPrunedCS = new long[ dpMaxSize+1 ];

			for(int size = 2; size <= dpMaxSize; size++){
				dpNumOfPrunedSubspaces[size] = getNumOfSubSpacesPrunedByIDP( size, numOfAgents );
				dpNumOfPrunedCS[size] = getNumOfCSPrunedByIDP( size, numOfAgents );
			}
			printDetailsOfDPForEverySize(numOfAgents, dpNumOfPrunedSubspaces, dpNumOfPrunedCS);
		}
    }
    
    //*********************************************************************************************************
    
    /**
     * This method returns the number of subSpaces that are pruned by DP at a given coalition size.
     */
    private long getNumOfSubSpacesPrunedByIDP( int dpMaxSize, int numOfAgents )
    {
    	//Since the bottom 2 layers of the integer partition graph are pruned given any value of maxDP...
    	long numOfPrunedSubSpaces = 1 + (long)Math.floor( numOfAgents/(double)2 );
  	
    	//Build the integer partitions
    	int[][][] partitions = IntegerPartition.getIntegerPartitions( numOfAgents, true );
    	
		//Scan the integer partitions starting from the 2nd layer.
    	for(int i=2; i<partitions.length; i++)
		{
			for(int j=0; j<partitions[i].length; j++)
			{
				//If the smallest two numbers are smaller than, or equal to, curSize...
				if(( partitions[i][j][0]+partitions[i][j][1] <= dpMaxSize )||( partitions[i][j][0]+partitions[i][j][1] == numOfAgents ))
					numOfPrunedSubSpaces++;
			}
		}
		return( numOfPrunedSubSpaces );
    }
    
    //*********************************************************************************************************
    
    /**
     * This method returns the number of splittings that are pruned by DP at a given coalition size
     */
    private long getNumOfCSPrunedByIDP( int dpMaxSize, int numOfAgents )
    {
    	//Since the bottom 2 layers of the coalition structure partition graph are pruned given any value of maxDP, then
    	long numOfPrunedCS = (long)Math.pow( 2 , numOfAgents-1 ); 
    	
    	//Build the integer partitions
    	int[][][] integerPartitions = IntegerPartition.getIntegerPartitions( numOfAgents, true );
    	
		//Scan the integer partitions starting from the 2nd layer.		
    	for(int i=2; i<integerPartitions.length; i++)
		{
			for(int j=0; j<integerPartitions[i].length; j++)
			{
				//If the smallest two numbers are smaller than, or equal to, curSize...
				if(( integerPartitions[i][j][0]+integerPartitions[i][j][1] <= dpMaxSize )||( integerPartitions[i][j][0]+integerPartitions[i][j][1] == numOfAgents ))
					numOfPrunedCS += (new Subspace( integerPartitions[i][j] )).sizeOfSubspace;
			}
		}
    	return( numOfPrunedCS );
    }
    
	
	//******************************************************************************************************

    /**
     * Print the number of evaluated splittings, the number of pruned sub-spaces, and
     * the number of pruned coalition structures, and that is for every coalition size.
     */
	public void printDetailsOfDPForEverySize( int numOfAgents, long[] dpNumOfPrunedSubspaces, long[] dpNumOfPrunedCS ) 
	{
		long totalNumOfSubspaces = IntegerPartition.getNumOfIntegerPartitions( numOfAgents );
		long totalNumOfCS = Combinations.getNumOfCS( numOfAgents );

		int dpMaxSize = (int)(Math.floor( (2 * numOfAgents) / (double)3 )); // because we are dealing with IDP

		for(int size = 2; size <= dpMaxSize; size++)
		{
			String percentageOfPrunedSubSpaces= General.setDecimalPrecision( (double)dpNumOfPrunedSubspaces[size]*100/totalNumOfSubspaces, 8);
			String percentageOfPrunedCS = General.setDecimalPrecision( (double)dpNumOfPrunedCS[size]*100/totalNumOfCS , 8 );

			String string = "";
			string += "Once IDP finishes dealing with size = "+size;
			string += ", the number of pruned sub-spaces is "+dpNumOfPrunedSubspaces+" ("+percentageOfPrunedSubSpaces+"%)";
			string += ", the number of pruned CS is "+dpNumOfPrunedCS+" ("+percentageOfPrunedCS+"%)";
			System.out.println( string );
		}
	}
}