package inputOutput;

import general.Combinations;
import general.General;
import mainSolver.Result;
import ipSolver.Node;
import ipSolver.Subspace;

public class Output
{	
	private StringBuffer time_numOfRemainingCoalitions;
	private StringBuffer time_solutionQuality;
	private StringBuffer time_boundQuality;
	private StringBuffer numOfSearchedCoalitions_solutionQuality;
	private StringBuffer numOfSearchedCoalitions_boundQuality;
	private double[] prevValues_time_numOfRemainingCoalitions;
	private double[] prevValues_time_solutionQuality;
	private double[] prevValues_time_boundQuality;
	private double[] prevValues_numOfSearchedCoalitions_solutionQuality;
	private double[] prevValues_numOfSearchedCoalitions_boundQuality;

	//******************************************************************************************************

	/**
	 * Initialization
	 */
	public void initOutput( Input input )
	{
		prevValues_time_solutionQuality = new double[2];
		prevValues_time_boundQuality = new double[2];
		prevValues_numOfSearchedCoalitions_boundQuality = new double[2];
		prevValues_time_numOfRemainingCoalitions = new double[2];
		prevValues_numOfSearchedCoalitions_solutionQuality = new double[2];
		
		prevValues_time_solutionQuality[0] = 0;
		prevValues_time_solutionQuality[1] = 0;
		prevValues_time_boundQuality[0] = 0;
		prevValues_time_boundQuality[1] = 0;
		prevValues_numOfSearchedCoalitions_boundQuality[0] = 0;
		prevValues_numOfSearchedCoalitions_boundQuality[1] = 0;
		prevValues_time_numOfRemainingCoalitions[0] = 0;
		prevValues_time_numOfRemainingCoalitions[1] = 0;
		prevValues_numOfSearchedCoalitions_solutionQuality[0] = 0;
		prevValues_numOfSearchedCoalitions_solutionQuality[1]= 0;
		
		time_numOfRemainingCoalitions = new StringBuffer();
		time_solutionQuality = new StringBuffer();
		time_boundQuality = new StringBuffer();
		numOfSearchedCoalitions_solutionQuality = new StringBuffer();
		numOfSearchedCoalitions_boundQuality = new StringBuffer();
	}
	
	//******************************************************************************************************
	
	/**
	 * Empty the contents of the String-Buffers to the relevant output files.
	 */
	public void emptyStringBufferContentsIntoOutputFiles( Input input )
	{
		if( input.printInterimResultsOfIPToFiles )
		{
			General.createFolder( input.outputFolder );			
			String tempStr = input.outputFolder + "/" + "("+input.problemID+")_";
			
			if( time_numOfRemainingCoalitions.length() != 0 ) {
				General.printToFile( tempStr+"time_numOfRemainingCoalitions.txt", time_numOfRemainingCoalitions.toString(),false);
				time_numOfRemainingCoalitions.setLength(0);
			}
			if( time_solutionQuality.length() != 0 ) {
				General.printToFile( tempStr+"time_solutionQuality.txt", time_solutionQuality.toString()    ,false);
				time_solutionQuality.setLength(0);
			}
			if( time_boundQuality.length() != 0 ) {
				General.printToFile( tempStr+"time_boundQuality.txt", time_boundQuality.toString()    ,false);
				time_boundQuality.setLength(0);
			}
			if( numOfSearchedCoalitions_solutionQuality.length() != 0 ) {
				General.printToFile( tempStr+"numOfSearchedCoalitions_solutionQuality.txt" , numOfSearchedCoalitions_solutionQuality.toString() ,false);
				numOfSearchedCoalitions_solutionQuality.setLength(0);
			}
			if( numOfSearchedCoalitions_boundQuality.length() != 0 ) {
				General.printToFile( tempStr+"numOfSearchedCoalitions_boundQuality.txt"    , numOfSearchedCoalitions_boundQuality.toString()    ,false);
				numOfSearchedCoalitions_boundQuality.setLength(0);
			}
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * Print the details of IP (or ODP-IP) to string buffers.
	 */
	public void printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( Input input, Result result )
	{
		if( input.printInterimResultsOfIPToFiles )
		{
			long timeThatElapsedSinceIPStarted = System.currentTimeMillis()-result.ipStartTime;
			double boundQuality = result.ipUpperBoundOnOptimalValue / result.get_ipValueOfBestCSFound();

			if(( prevValues_time_solutionQuality[0] != timeThatElapsedSinceIPStarted )
					&&( prevValues_time_solutionQuality[1] != result.get_ipValueOfBestCSFound() ))
			{
				time_solutionQuality.append(timeThatElapsedSinceIPStarted+", "+result.get_ipValueOfBestCSFound()+"\n");
				prevValues_time_solutionQuality[0] = timeThatElapsedSinceIPStarted;
				prevValues_time_solutionQuality[1] = result.get_ipValueOfBestCSFound();
			}
			if(( prevValues_time_boundQuality[0] != timeThatElapsedSinceIPStarted )
					&&( prevValues_time_boundQuality[1] != boundQuality ))
			{
				time_boundQuality.append(timeThatElapsedSinceIPStarted+", "+boundQuality+"\n");
				prevValues_time_boundQuality[0] = timeThatElapsedSinceIPStarted;
				prevValues_time_boundQuality[1] = boundQuality;
			}
			if(( prevValues_numOfSearchedCoalitions_boundQuality[0] != result.ipNumOfExpansions )
					&&( prevValues_numOfSearchedCoalitions_boundQuality[1] != boundQuality ))
			{
				numOfSearchedCoalitions_boundQuality.append(result.ipNumOfExpansions+", "+boundQuality+"\n");
				prevValues_numOfSearchedCoalitions_boundQuality[0] = result.ipNumOfExpansions;
				prevValues_numOfSearchedCoalitions_boundQuality[1] = boundQuality;
			}

			if(( prevValues_numOfSearchedCoalitions_solutionQuality[0] != result.ipNumOfExpansions )
					&&( prevValues_numOfSearchedCoalitions_solutionQuality[1] != result.get_ipValueOfBestCSFound() ))
			{
				numOfSearchedCoalitions_solutionQuality.append(result.ipNumOfExpansions+", "+result.get_ipValueOfBestCSFound()+"\n");
				prevValues_numOfSearchedCoalitions_solutionQuality[0] = result.ipNumOfExpansions;
				prevValues_numOfSearchedCoalitions_solutionQuality[1] = result.get_ipValueOfBestCSFound();
			}			
		}
	}

	//******************************************************************************************************

	/**
	 * print the current results of IP to the relevant output files
	 */
	public void printFinalResultsOfIPToFiles( Input input, Result result )
	{
		if( input.printInterimResultsOfIPToFiles )
		{
			General.createFolder( input.outputFolder );

			//Get the optimal coalition structure, and convert it to string
			String bestCSFoundByIP_asString;
			if( result.get_ipBestCSFound() == null )
				bestCSFoundByIP_asString = "null";
			else
				bestCSFoundByIP_asString = General.convertArrayToString(result.get_ipBestCSFound());
				
			//Get the level of the optimal coalition structure, and convert it to string
			String levelOfBestCSFoundByIP_asString;
			if( result.get_ipBestCSFound() == null )
				levelOfBestCSFoundByIP_asString = "null";
			else
				levelOfBestCSFoundByIP_asString = (new Integer( (result.get_ipBestCSFound()).length )).toString();
			
			//Get the integer partition of the optimal coailtion structure, and convert it to string
			String integerPartitionOfBestCSFoundByIP_asString;
			if( result.get_ipBestCSFound() == null )
				integerPartitionOfBestCSFoundByIP_asString = "null";
			else{
				int[][] ipBestCSFound = result.get_ipBestCSFound();
				int[] integerPartitionOfBestCSFoundByIP = new int[ ipBestCSFound.length ];
				for(int i=0; i<ipBestCSFound.length; i++)
					integerPartitionOfBestCSFoundByIP[i] = ipBestCSFound[i].length;
				integerPartitionOfBestCSFoundByIP_asString = General.convertArrayToString(integerPartitionOfBestCSFoundByIP);
			}

			//Print the results			
			String tempStr = input.outputFolder + "/";
			General.printToFile( tempStr+"bestCSFoundByIP.txt", bestCSFoundByIP_asString+"\n", false);
			General.printToFile( tempStr+"integerPartitionOfBestCSFoundByIP.txt", integerPartitionOfBestCSFoundByIP_asString+"\n", false);
			General.printToFile( tempStr+"levelOfBestCSFoundByIP.txt", levelOfBestCSFoundByIP_asString+"\n", false);
			General.printToFile( tempStr+"valueOfBestCSFoundByIP.txt", result.get_ipValueOfBestCSFound()+"\n", false);

			tempStr += "("+input.problemID+")_";			
			long timeThatElapsedSinceIPStarted = System.currentTimeMillis() - result.ipStartTime;
			double boundQuality = 1;//result.ipUpperBoundOnOptimalValue / result.ipValueOfBestCSFound;

			General.printToFile( tempStr+"time_solutionQuality.txt", timeThatElapsedSinceIPStarted+", "+result.get_ipValueOfBestCSFound()+"\n", false);
			General.printToFile( tempStr+"time_boundQuality.txt", timeThatElapsedSinceIPStarted+", "+boundQuality+"\n", false);
			General.printToFile( tempStr+"numOfSearchedCoalitions_boundQuality.txt", result.ipNumOfExpansions+", "+boundQuality+"\n", false);
			General.printToFile( tempStr+"numOfSearchedCoalitions_solutionQuality.txt", result.ipNumOfExpansions+", "+result.get_ipValueOfBestCSFound()+"\n", false);
		}
	}

	//******************************************************************************************************

	/**
	 * Print the size, upperBound, and lowerBound for each subspace
	 */
	public void printDetailsOfSubspaces( Input input, Result result )
	{
		if( input.printDetailsOfSubspaces )
		{
			// Count the number of possible coalition structures
			long numOfCS = Combinations.getNumOfCS( input.numOfAgents );

			Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
			double[] percentagePerLevel = new double[ nodes.length ];
			double maxPercentage=0;
			int ii=0;
			int jj=0;
			for(int i=0; i<nodes.length; i++) 
			{
				System.out.println("");
				percentagePerLevel[i]=0;

				for(int j=0; j<nodes[i].length; j++)
				{
					Subspace curSubspace = nodes[i][j].subspace;

					double percentagePerSubspace = (double) (curSubspace.sizeOfSubspace * 100) / numOfCS;
					if (maxPercentage < percentagePerSubspace) {
						maxPercentage = percentagePerSubspace;
						ii = i; //Store the location of the biggest subspace
						jj = j; //Store the location of the biggest subspace
					}

					//Update the percentage for the current level
					percentagePerLevel[i] += percentagePerSubspace;

					//Printing the details
					General.printArray("subspace["+i+"]["+j+"] = ", curSubspace.integers);
					System.out.println(",   size = "+curSubspace.sizeOfSubspace+" ("
							+General.setDecimalPrecision( percentagePerSubspace, 5 )+"%),"
							+"  UB = "+curSubspace.UB+",  LB = "+curSubspace.LB+",  AVG = "+curSubspace.Avg);				
				}
			}
			//Print the percentage per level
			System.out.println("");
			for (int i = 0; i < nodes.length; i++)
				System.out.println("level"+(i+1)+": "+General.setDecimalPrecision( percentagePerLevel[i], 5 ) + "%\n");

			// Printing the biggest subspace:
			Subspace biggestSubspace = nodes[ii][jj].subspace;
			General.printArray("The biggest subspace is:",biggestSubspace.integers);
			System.out.println(" ("+maxPercentage+"%)\n");
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * Prints to a file the time required to search a given subspace
	 */
	public void printTimeRequiredForEachSubspace( Subspace subspace, Input input )
	{
		//Print the time required to search this sub-space
		if( input.printTimeTakenByIPForEachSubspace )
		{
			General.createFolder( input.outputFolder );			
			String integerPartitionAsString = General.convertArrayToString( subspace.integers );
			System.out.println("Searching "+integerPartitionAsString+" took "+subspace.timeToSearchThisSubspace+" ms.");
			General.printToFile( input.outputFolder + "/" + "("+input.problemID+")_" + "ipTimeForEachSubspace.txt", 
					"Searching "+integerPartitionAsString+" took "+subspace.timeToSearchThisSubspace+"\n", false);
		}
	}
}