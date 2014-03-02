package predefinedExperiments;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import general.General;
import inputOutput.Input;
import inputOutput.SolverNames;
import inputOutput.ValueDistribution;
import mainSolver.MainSolver;
import mainSolver.Result;

public class RunDPAndIDPAndODP
{
	private String   str_numOfAgents;
	private String   str_dpTime;
	private String[] str_sizeConsideredByDP;
	private String[] str_numOfEvaluatedSplittings;
	private String[] str_dpTimeForEachSize;
	
	//*****************************************************************************************************

	/**
	 *  Run DP and IDP given different Numbers of agents. For each number, print
	 *  the required time for each size, the total time, the No. of evaluated
	 *  splittings, and the No. of pruned subspaces and Coalition structures.
	 */
	public void run( boolean readCoalitionValuesFromFile )
	{
		//Store the current time and date
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd (HH-mm-ss)" );

		Input input = initializeInput(); //Initialize the input parameters
		
		//We will run three times, once for ODP, once for IDP, and once for DP.
		for(int run=1; run<=3; run++) 
		{
			//The numbers of agents for which we are going to run our experiments
			int minNumOfAgents = 15; int maxNumOfAgents = 23;

			//Initialize the strings in which we're storing results
			initializeStringsOfResults( minNumOfAgents, maxNumOfAgents, run );
			
			//Run the experiment for different numbers of agents
			for(input.numOfAgents=minNumOfAgents; input.numOfAgents<=maxNumOfAgents; input.numOfAgents++)
			{
				if( run == 1) // then we are running ODP
				{
					input.solverName = SolverNames.ODP;
					input.outputFolder = "D:/CSGresults/ODP ";
					System.out.println("Now running ODP for "+input.numOfAgents+" agents");
				}
				if( run == 2) // then we are running IDP
				{
					input.solverName = SolverNames.IDP;
					input.outputFolder = "D:/CSGresults/IDP ";
					System.out.println("Now running IDP for "+input.numOfAgents+" agents");
				}
				if( run == 3 ) // then we are running DP
				{
					input.solverName = SolverNames.DP;
					input.outputFolder = "D:/CSGresults/DP ";
					System.out.println("Now running DP for "+input.numOfAgents+" agents");
				}
				//Add the date and time to the name of the output folder
				input.outputFolder += simpleDateFormat.format(calendar.getTime());
				
				//Create the output folder
				General.createFolder( input.outputFolder );

				//Generate the problem instance randomly, or read them from a file
				if( readCoalitionValuesFromFile ) input.readCoalitionValuesFromFile( 1 );
				else input.generateCoalitionValues();
				
				//Run the CSG algorithm(s)
				Result result = (new MainSolver()).solve( input );
				
				//Update the strings in which we are storing the results
				updateStringsOfResults( input, result );
				
				//Print the current results to a file
				printCurrentResultsToFile( input );
			}			
			//Print the final results to a file
			printFinalResultsToFile( input );
		}
	}
		
	//*****************************************************************************************************
	
	/**
	 * Set the majority of the input parameters
	 */
	private Input initializeInput()
	{
		Input input = new Input();
		
		/* The printing parameters:
		 *   - We set them all to "false" since we do not want the solver(s) to print any results. This is because this class uses
		 *     its own printing method (to produce results in a certain format that is designed to facilitate plotting in Matlab)
		 */ 
		input.storeCoalitionValuesInFile = false;
		input.printDetailsOfSubspaces = false;
		input.printNumOfIntegerPartitionsWithRepeatedParts = false;
		input.printInterimResultsOfIPToFiles = false;
		input.printTimeTakenByIPForEachSubspace = false;

		//General parameters
		input.feasibleCoalitions = null;
		input.numOfRunningTimes = 1; //because, with DP, IDP and ODP, we don't need to take average of multiple runs	
		input.valueDistribution = ValueDistribution.UNIFORM;
		
		return( input );
	}
	
	//*****************************************************************************************************
	
	/**
	 * Initialize the strings in which we are storing the results
	 */
	private void initializeStringsOfResults( int minNumOfAgents, int maxNumOfAgents, int run )
	{
		if( run == 1 ) str_dpTime = "ODP_Time = [";
		if( run == 2 ) str_dpTime = "IDP_Time = [";
		if( run == 3 ) str_dpTime = "DP_Time = [";

		str_numOfAgents = "numOfAgents = [";

		str_sizeConsideredByDP          = new String[ maxNumOfAgents+1 ];
		str_numOfEvaluatedSplittings    = new String[ maxNumOfAgents+1 ];
		str_dpTimeForEachSize           = new String[ maxNumOfAgents+1 ];
		
		for(int numOfAgents=minNumOfAgents; numOfAgents<=maxNumOfAgents; numOfAgents++)
		{
			int dpMaxSize = numOfAgents;
			if( run == 2 )
				dpMaxSize = (int)(Math.floor( (2 * numOfAgents) / (double)3 ));
			
			str_sizeConsideredByDP[numOfAgents]          = "sizeConsideredByDP("+numOfAgents+","+1+":"+(dpMaxSize-1)+") = [";
			str_numOfEvaluatedSplittings[numOfAgents]    = "numOfEvaluatedSplittings("+numOfAgents+","+1+":"+(dpMaxSize-1)+") = [";
			str_dpTimeForEachSize[numOfAgents]           = "dpTimeForEachSize("+numOfAgents+","+1+":"+numOfAgents+") = [";
		}
	}
	
	//*****************************************************************************************************
		
	/**
	 * Update the strings in which we are storing the results
	 */
	private void updateStringsOfResults( Input input, Result result )
	{
		str_numOfAgents += input.numOfAgents+" ";
		str_dpTime += result.dpTime+" ";
		
    	long dpMaxSize = input.numOfAgents;
    	if( input.solverName == SolverNames.IDP )
    		dpMaxSize = (int)(Math.floor( (2 * input.numOfAgents) / (double)3 ));

		for(int size=2; size<=dpMaxSize; size++)
			str_sizeConsideredByDP[ input.numOfAgents ] += size+" ";

		for(int size=1; size<=input.numOfAgents; size++)
			str_dpTimeForEachSize[ input.numOfAgents ] += result.dpTimeForEachSize[size]+" ";
	}
	
	//*****************************************************************************************************

	/**
	 * Print the strings (in which we are storing the results) to a file
	 */
	private void printCurrentResultsToFile( Input input )
	{
		//Create an output folder
		String filePathAndName = input.outputFolder + "/NumOfPrunedSubspacesAndEvaluatedSplits.txt";

		//Print the number of agents
		General.printToFile( filePathAndName, "numOfAgents = "+input.numOfAgents+"\n", false);

		//Print the results for the given number of agents
		str_sizeConsideredByDP[ input.numOfAgents] += "]\n";
		General.printToFile( filePathAndName, str_sizeConsideredByDP[ input.numOfAgents], false);
		str_numOfEvaluatedSplittings[ input.numOfAgents] += "]\n";
		General.printToFile( filePathAndName, str_numOfEvaluatedSplittings[ input.numOfAgents], false);
		str_dpTimeForEachSize[ input.numOfAgents] += "]\n\n";
		General.printToFile( filePathAndName, str_dpTimeForEachSize[ input.numOfAgents], false);
	}
	
	//*****************************************************************************************************
	
	/**
	 * Print the strings (in which we are storing the results) to a file
	 */
	private void printFinalResultsToFile( Input input )
	{
		//Create an output folder
		String filePathAndName = input.outputFolder + "/totalRunTime.txt";

		str_numOfAgents += "]\n";
		General.printToFile( filePathAndName, str_numOfAgents, false);
		str_dpTime += "]\n\n";
		General.printToFile( filePathAndName, str_dpTime, false);
	}
}