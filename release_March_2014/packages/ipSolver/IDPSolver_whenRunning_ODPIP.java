package ipSolver;

import mainSolver.Result;
import dpSolver.DPSolver;
import inputOutput.Input;
import inputOutput.SolverNames;

public class IDPSolver_whenRunning_ODPIP extends Thread
{
	private Input inputToIDPSolver;
	private Result result;
	private double[] valueOfBestPartitionFound;
	private boolean stop = false;
	public DPSolver dpSolver;

	public void setStop( boolean value )
	{
		if( dpSolver != null )  dpSolver.setStop(value);
		stop = value;
	}
	
	//*****************************************************************************************************

	/**
	 * The constructor
	 */
	public IDPSolver_whenRunning_ODPIP(  Input input, Result result )
	{
		this.stop = false;
		this.result= result;
		this.valueOfBestPartitionFound = new double[ 1 << input.numOfAgents ];
		
		//Initialize the input to, and the result of, the IDP solver
		this.inputToIDPSolver = getInputToIDPSolver( input );
		
		//Run the IDP solver
		this.dpSolver = new DPSolver( inputToIDPSolver, result );
	}
	
	//*****************************************************************************************************
	
	/**
	 * Synchronized set method
	 */
	public synchronized void updateValueOfBestPartitionFound( int coalition, double value ){
		if( valueOfBestPartitionFound[coalition] < value )
			valueOfBestPartitionFound[coalition] = value ;
	}
	/**
	 * get method
	 */
	public double getValueOfBestPartitionFound( int coalition ){
		return valueOfBestPartitionFound[coalition];
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method uses IDP to compute parts of the table used in the local-branch-and-bound technique
	 */
	public void run()
	{
		this.dpSolver.runDPorIDP();		
	}
	
	//*****************************************************************************************************
	
	/**
	 * Initialize the array "valueOfBestPartitionFound" such that, for every coalition C,
	 * we simply put: valueOfBestPartitionFound[C] = v[C]
	 */
	public void initValueOfBestPartitionFound(Input input, double[][] maxValueForEachSize)
	{
		long startTime = System.currentTimeMillis();
		
		valueOfBestPartitionFound[0]=0;
		
		//initialize result.max_f. Basically, result.max_f[3] is the maximum f value that DP computed for a coalition of size 4
		result.init_max_f( input, maxValueForEachSize );
		
		//Initialize the best partition of each coalition C to be the maximum between
		//its value, and the sum of values of the singletons made of its members.
		for(int coalitionInBitFormat = valueOfBestPartitionFound.length-1; coalitionInBitFormat >= 1; coalitionInBitFormat--)
			valueOfBestPartitionFound[coalitionInBitFormat] = input.getCoalitionValue(coalitionInBitFormat);

		//System.out.println("The time required to initialize the local-branch-and-bound table is "+(System.currentTimeMillis()-startTime)+" millisec");		
	}
	
	//*****************************************************************************************************
	
	/**
	 * Initialize the parameters of the input object to be passed to the DP solver
	 */
	private Input getInputToIDPSolver( Input input )
	{
		Input inputToDPSolver = new Input();
		inputToDPSolver.coalitionValues = input.coalitionValues;
		inputToDPSolver.problemID = input.problemID;
		
		//The DP parameters
		inputToDPSolver.solverName = SolverNames.ODPIP;
		
		//The printing parameters
		inputToDPSolver.storeCoalitionValuesInFile = false;
		inputToDPSolver.printDetailsOfSubspaces = false;
		inputToDPSolver.printNumOfIntegerPartitionsWithRepeatedParts = false;
		inputToDPSolver.printInterimResultsOfIPToFiles = false;
		inputToDPSolver.printTimeTakenByIPForEachSubspace = false;

		//General parameters
		inputToDPSolver.feasibleCoalitions = null;
		inputToDPSolver.numOfAgents = input.numOfAgents;
		inputToDPSolver.numOfRunningTimes = 1;	
		
		return( inputToDPSolver );
	}
}