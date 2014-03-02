package mainSolver;

import general.AvVar;
import general.IntegerPartition;
import inputOutput.Input;
import inputOutput.SolverNames;
import ipSolver.IntegerPartitionGraph;
import ipSolver.Node;
import ipSolver.Subspace;


/**
 * When taking the average of several runs, we need to store the data in a way that enables calculating 
 * information such as the standard deviation. To enable this, we use an object of "myAvVar".  
 */
public class ComputeErrorBars
{
	public AvVar ipTime, cplexTime, ipValueOfBestCS, cplexValueOfBestCS, ipNumOfSearchedCS,
	ipNumOfSearchedCoalitions, ipTimeForScanningTheInput, ipUpperBoundOnOptimalValue;
	
	public AvVar[][] ipNumOfSearchedCoalitionsInSubspaces;

	//******************************************************************************************************
	
	public ComputeErrorBars( Input input ) {
		ipTime    = new AvVar(); 
		cplexTime = new AvVar();
		ipValueOfBestCS = new AvVar();
		cplexValueOfBestCS = new AvVar();
		ipNumOfSearchedCS  = new AvVar();
		ipNumOfSearchedCoalitions = new AvVar();
		ipTimeForScanningTheInput = new AvVar();
		ipUpperBoundOnOptimalValue= new AvVar();
		int[][][] integerPartitions = IntegerPartition.getIntegerPartitions( input.numOfAgents, input.orderIntegerPartitionsAscendingly);
		ipNumOfSearchedCoalitionsInSubspaces = new AvVar[ integerPartitions.length ][];
		for(int i=0; i<ipNumOfSearchedCoalitionsInSubspaces.length; i++)
		{
			ipNumOfSearchedCoalitionsInSubspaces[i] = new AvVar[ integerPartitions[i].length ];
			for(int j=0; j<ipNumOfSearchedCoalitionsInSubspaces[i].length; j++)
				ipNumOfSearchedCoalitionsInSubspaces[i][j] = new AvVar();
		}
	}
	
	//******************************************************************************************************
	
	public void addResults( Result result )
	{
		cplexTime.add( result.cplexTime );
		cplexValueOfBestCS.add( result.cplexValueOfBestCSFound );
		ipTime.add( result.ipTime );
		ipValueOfBestCS.add( result.get_ipValueOfBestCSFound() );
		ipNumOfSearchedCoalitions.add( result.ipNumOfExpansions );
		ipUpperBoundOnOptimalValue.add( result.ipUpperBoundOnOptimalValue );
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		for(int i=0; i<nodes.length; i++)
			for(int j=0; j<nodes[i].length; j++){
				Subspace curSubspace = nodes[i][j].subspace;
				ipNumOfSearchedCoalitionsInSubspaces[i][j].add( curSubspace.numOfSearchedCoalitionsInThisSubspace );
			}
	}
	
	//******************************************************************************************************
	
	/**
	 * If running multiple times, set the result to be the average of all runs, and set the confidence intervals
	 */
	public Result setAverageResultAndConfidenceIntervals( Input input )
	{
		Result result = new Result(input);
		if( input.solverName == SolverNames.CPLEX )
		{
			//get the average results of CPLEX
			result.cplexTime = (long) cplexTime.average();
			result.cplexValueOfBestCSFound = cplexValueOfBestCS.average();

			//Compute confidence intervals for CPLEX results
			result.cpleXTime_confidenceInterval = 1.96 * (cplexTime.stddev() / Math.sqrt(cplexTime.num()));
			result.cplexValueOfBestCSFound = 1.96 * (cplexValueOfBestCS.stddev() / Math.sqrt(cplexValueOfBestCS.num()));
		}
		
		//Get the average results of IP
		result.ipUpperBoundOnOptimalValue = ipUpperBoundOnOptimalValue.average();
		result.ipTimeForScanningTheInput = (long) ipTimeForScanningTheInput.average();
		result.ipNumOfExpansions = (long) ipNumOfSearchedCoalitions.average();
		result.ipTime = (long) ipTime.average();
		result.set_ipValueOfBestCSFound( ipValueOfBestCS.average() );

		//Create the subspaces
    	int[][][] integers=IntegerPartition.getIntegerPartitions(input.numOfAgents,input.orderIntegerPartitionsAscendingly);
    	Subspace[][] subspaces = new Subspace[ integers.length ][];
		for(int level=0; level<input.numOfAgents; level++){
			subspaces[level]=new Subspace[ integers[level].length ];
			for(int i=0; i<integers[level].length; i++){
				subspaces[level][i] = new Subspace( integers[level][i] );
			}
		}	
		result.ipIntegerPartitionGraph = new IntegerPartitionGraph( subspaces, input.numOfAgents, ((int)Math.floor(2*input.numOfAgents/(double)3)));
		
		//get the average number of expansions in each subspace
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		for(int i=0; i<nodes.length; i++)
			for(int j=0; j<nodes[i].length; j++)
			{
				AvVar temp = ipNumOfSearchedCoalitionsInSubspaces[i][j];
				Subspace curSubspace = nodes[i][j].subspace;
				curSubspace.numOfSearchedCoalitionsInThisSubspace = (long)temp.average();
			}
		
		//Compute confidence intervals for IP results
		result.ipUpperBoundOnOptimalValue_confidenceInterval = 1.96 * ( ipUpperBoundOnOptimalValue.stddev() / Math.sqrt(ipUpperBoundOnOptimalValue.num()) );
		result.ipTimeForScanningTheInput_confidenceInterval = 1.96 * (ipTimeForScanningTheInput.stddev() / Math.sqrt(ipTimeForScanningTheInput.num()));
		result.ipNumOfExpansions_confidenceInterval = 1.96 * ( ipNumOfSearchedCoalitions.stddev() / Math.sqrt(ipNumOfSearchedCoalitions.num()) );		
		result.ipValueOfBestCS_confidenceInterval = 1.96 * ( ipValueOfBestCS.stddev() / Math.sqrt(ipValueOfBestCS.num()) );
		result.ipTime_confidenceInterval = 1.96 * ( ipTime.stddev() / Math.sqrt(ipTime.num()) );
		for(int i=0; i<nodes.length; i++)
			for(int j=0; j<nodes[i].length; j++)
			{	
				AvVar temp = ipNumOfSearchedCoalitionsInSubspaces[i][j];
				Subspace curSubspace = nodes[i][j].subspace;
				curSubspace.numOfSearchedCoalitionsInThisSubspace_confidenceInterval = 1.96 * ( temp.stddev() / Math.sqrt(temp.num()) );
			}
		
		return( result );
	}
}
