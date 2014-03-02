package predefinedExperiments;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import general.General;
import general.IntegerPartition;
import inputOutput.Input;
import inputOutput.SolverNames;
import inputOutput.ValueDistribution;
import ipSolver.Node;
import mainSolver.MainSolver;
import mainSolver.Result;
import ipSolver.Subspace;

public class RunIPorODPIP
{
	private String[][] str_ipTime;
	private String[][] str_ipTime_confidenceInterval;
	private String[][] str_ipNumOfSearchedCoalitions;
	private String[][] str_ipNumOfSearchedCoalitions_confidenceInterval;
	private String[][][] str_ipNumOfSearchedCoalitionsInSubspaces;
	private String[][][] str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval;

	
	//*****************************************************************************************************

	/**
	 * Run ODP-IP for different numbers of agents, and for different distributions. It prints the the average time and average number
	 * of searched coalitions. It also prints results showing how the solution quality (and bound quality) grows during run time.
	 * 
	 * @param readCoalitionValuesFromFile  if this is "true", coalition values will be read from a file. Otherwise, they will be generated randomly.
	 * @param runODPIP  if this is "true", the method runs ODP-IP. Otherwise, it runs IP.
	 */	
	public void run( boolean readCoalitionValuesFromFile, boolean runODPIP  )
	{
		//Initialize the input parameters
		Input input = initializeInput( runODPIP );
		input.readCoalitionValuesFromFile = readCoalitionValuesFromFile;
		
		//Create the main output folder
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd (HH-mm-ss)" );
		String mainOutputFolder;
		if( runODPIP ){
			mainOutputFolder = "D:/CSGresults/ODP_IP "+simpleDateFormat.format(calendar.getTime());
		}else{
			mainOutputFolder = "D:/CSGresults/IP "+simpleDateFormat.format(calendar.getTime());
		}
		General.createFolder( mainOutputFolder );
		
		//The numbers of agents for which we are going to run our experiments
		int minNumOfAgents = 15; int maxNumOfAgents = 25;

		//Set the distributions that are going to be used in the experiments
		ValueDistribution[] valueDistributions = { ValueDistribution.UNIFORM, ValueDistribution.NORMAL, ValueDistribution.NDCS, ValueDistribution.EXPONENTIAL, ValueDistribution.BETA,
				ValueDistribution.GAMMA, ValueDistribution.MODIFIEDUNIFORM, ValueDistribution.MODIFIEDNORMAL, ValueDistribution.AGENTBASEDUNIFORM, ValueDistribution.AGENTBASEDNORMAL};
		
		//Initialize the strings in which we're storing results
		initializeStringsOfResults( minNumOfAgents, maxNumOfAgents, valueDistributions.length );
		
		//For every number of agents
		for(input.numOfAgents=minNumOfAgents; input.numOfAgents<=maxNumOfAgents; input.numOfAgents++)
		{
			int numOfIntegerPartitions = IntegerPartition.getNumOfIntegerPartitions( input.numOfAgents );
			
			//Get the No. of running times based on the number of agents
			input.numOfRunningTimes = (new PredefinedExperiments()).getNumOfRunningTimes( input.numOfAgents );
			
			//for every value distribution
			for(int distributionID=0; distributionID < valueDistributions.length; distributionID++)
			{
				input.valueDistribution = valueDistributions[ distributionID ];

				//Set the name of the output folder for interim results
				setNameOfOutputFolder( mainOutputFolder, input, valueDistributions[ distributionID ] );

				//determine whether to read the coalition values from a file, or generate random ones
				if( input.readCoalitionValuesFromFile ){
					input.folderInWhichCoalitionValuesAreStored = "D:/CSGdata/coalitionValues";
					input.readCoalitionValuesFromFile( 1 );
				}else
					input.generateCoalitionValues();

				Result result = (new MainSolver()).solve( input ); //Run the CSG algorithm(s)
				
				updateStringsOfResults( input, result, distributionID ); //Update the strings in which we are storing the results
				printCurrentResultsToFile(input, mainOutputFolder, valueDistributions, distributionID, numOfIntegerPartitions);
			}
		}
	}
	
	//*****************************************************************************************************
	
	/**
	 * Set the majority of the input parameters
	 */
	private Input initializeInput( boolean runODPIP )
	{
		//Initialization
		Input input = new Input(); //"true" to indicate that IP will be using a hashTable for coalition values

		//Determine whether to run IP or IDP-IP*
		if( runODPIP ) input.solverName = SolverNames.ODPIP;
		else input.solverName = SolverNames.IP;
		
		//General parameters:
		input.feasibleCoalitions = null;
		input.folderInWhichCoalitionValuesAreStored = "D:/CSGdata/coalitionValues";

		//The IP parameters:
		input.acceptableRatio = 100; //since we want to find the optimal CS
		input.orderIntegerPartitionsAscendingly = false; //this way, integer partitions will be ordering descendingly
		input.storeCoalitionValuesInFile  = false; //we do not want to store the instance once we have solved it.
		
		// The printing parameters:
		input.printInterimResultsOfIPToFiles = true;
		//We set the rest to "false" since we do not want the solver(s) to print any results. This is because this class uses
		//its own printing method (to produce results in a certain format that is designed to facilitate plotting in Matlab)
		input.printNumOfIntegerPartitionsWithRepeatedParts = false;
		input.storeCoalitionValuesInFile = false;
		input.printDetailsOfSubspaces = false;
		input.printTimeTakenByIPForEachSubspace = false;
		
		return( input );
	}
	
	//*****************************************************************************************************
	
	/**
	 * Set the name of the output folder for the interim results
	 */
	private void setNameOfOutputFolder( String mainOutputFolder, Input input, ValueDistribution valueDistribution )
	{
		input.outputFolder = mainOutputFolder;
		input.outputFolder += "/individual runs";
		input.outputFolder += "/"+input.numOfAgents+"Agents_";
		input.outputFolder += ValueDistribution.toString(valueDistribution);
	}
	
	//*****************************************************************************************************
	
	/**
	 * Initialize the strings in which we are storing the results
	 */
	private void initializeStringsOfResults( int minNumOfAgents, int maxNumOfAgents, int numOfDistributions )
	{
		str_ipTime = new String[ maxNumOfAgents+1 ][ numOfDistributions ];
		str_ipTime_confidenceInterval = new String[ maxNumOfAgents+1 ][ numOfDistributions ];
		str_ipNumOfSearchedCoalitions = new String[ maxNumOfAgents+1 ][ numOfDistributions ];
		str_ipNumOfSearchedCoalitions_confidenceInterval = new String[ maxNumOfAgents+1 ][ numOfDistributions ];
		str_ipNumOfSearchedCoalitionsInSubspaces = new String[ maxNumOfAgents+1 ][ numOfDistributions ][];
		str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval = new String[ maxNumOfAgents+1 ][ numOfDistributions ][];

		for(int numOfAgents=minNumOfAgents; numOfAgents<=maxNumOfAgents; numOfAgents++) //for every number of agents
			for(int distributionID=0; distributionID<numOfDistributions; distributionID++) //for every distribution
			{
				str_ipTime[numOfAgents][distributionID] = "ipTime("+numOfAgents+") = ";
				str_ipTime_confidenceInterval[numOfAgents][distributionID] = "ipTime_confidenceInterval("+numOfAgents+") = ";
				str_ipNumOfSearchedCoalitions[numOfAgents][distributionID] = "ipNumOfSearchedCoalitions("+numOfAgents+") = ";
				str_ipNumOfSearchedCoalitions_confidenceInterval[numOfAgents][distributionID] = "ipNumOfSearchedCoalitions_confidenceInterval("+numOfAgents+") = ";

				int numOfIntegerPartitions = IntegerPartition.getNumOfIntegerPartitions( (int)numOfAgents );
				str_ipNumOfSearchedCoalitionsInSubspaces[numOfAgents][distributionID] = new String[numOfIntegerPartitions];
				str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval[numOfAgents][distributionID] = new String[numOfIntegerPartitions];
				for(int i=0; i<numOfIntegerPartitions; i++)
				{
					str_ipNumOfSearchedCoalitionsInSubspaces[numOfAgents][distributionID][i] = "ipNumOfSearchedCoalitionsInSubspaces("+numOfAgents+","+(i+1)+") = ";
					str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval[numOfAgents][distributionID][i] = "ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval("+numOfAgents+","+(i+1)+") = ";
				}	
			}
	}

	//*****************************************************************************************************

	/**
	 * Update the strings in which we are storing the results
	 */
	private void updateStringsOfResults( Input input, Result result, int distributionID )
	{
		int numOfAgents = input.numOfAgents;
		str_ipTime[numOfAgents][distributionID] += result.ipTime+" ";
		str_ipTime_confidenceInterval[numOfAgents][distributionID] += result.ipTime_confidenceInterval+" ";
		str_ipNumOfSearchedCoalitions[numOfAgents][distributionID] += result.ipNumOfExpansions+" ";
		str_ipNumOfSearchedCoalitions_confidenceInterval[numOfAgents][distributionID] += result.ipNumOfExpansions_confidenceInterval+" ";
		int k=0;
		Node[][] nodes = result.ipIntegerPartitionGraph.nodes;
		for(int i=0; i<nodes.length; i++)
			for(int j=0; j<nodes[i].length; j++)
			{
				Subspace curSubspace = nodes[i][j].subspace;
				str_ipNumOfSearchedCoalitionsInSubspaces[numOfAgents][distributionID][k] += curSubspace.numOfSearchedCoalitionsInThisSubspace+" ";
				str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval[numOfAgents][distributionID][k] += curSubspace.numOfSearchedCoalitionsInThisSubspace_confidenceInterval+" ";
				k++;
			}
	}

	//*****************************************************************************************************

	/**
	 * Print the strings (in which we are storing the results) to a file
	 */
	private void printCurrentResultsToFile( Input input, String mainOutputFolder, ValueDistribution[] valueDistributions,
			int distributionID, int numOfIntegerPartitions )
	{
		int numOfAgents = input.numOfAgents;
		
		//Set the name and path of the output file
		String filePathAndName = mainOutputFolder + "/"+valueDistributions[distributionID];

		//Print the results
		General.printToFile( filePathAndName+".txt", "numOfAgents = "+numOfAgents+"\n", false);
		str_ipTime[numOfAgents][distributionID] += "\n";
		General.printToFile( filePathAndName+".txt", str_ipTime[numOfAgents][distributionID], false);
		str_ipTime_confidenceInterval[numOfAgents][distributionID] += "\n";
		General.printToFile( filePathAndName+".txt", str_ipTime_confidenceInterval[numOfAgents][distributionID], false);
		str_ipNumOfSearchedCoalitions[numOfAgents][distributionID] += "\n";
		General.printToFile( filePathAndName+".txt", str_ipNumOfSearchedCoalitions[numOfAgents][distributionID], false);
		str_ipNumOfSearchedCoalitions_confidenceInterval[numOfAgents][distributionID] += "\n\n";
		General.printToFile( filePathAndName+".txt", str_ipNumOfSearchedCoalitions_confidenceInterval[numOfAgents][distributionID], false);
		
		filePathAndName += "_detailsPerSubspace.txt";
		for(int i=0; i<numOfIntegerPartitions; i++)
		{
			str_ipNumOfSearchedCoalitionsInSubspaces[numOfAgents][distributionID][i] += "\n";
			General.printToFile( filePathAndName, str_ipNumOfSearchedCoalitionsInSubspaces[numOfAgents][distributionID][i], false);
			str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval[numOfAgents][distributionID][i] += "\n\n";
			General.printToFile( filePathAndName, str_ipNumOfSearchedCoalitionsInSubspaces_confidenceInterval[numOfAgents][distributionID][i], false);
		}
	}
}
