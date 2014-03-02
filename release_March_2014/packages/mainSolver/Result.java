package mainSolver;

import inputOutput.Input;
import general.Combinations;
import general.General;
import ipSolver.IntegerPartitionGraph;
import ipSolver.IDPSolver_whenRunning_ODPIP;

public class Result
{
	public Result( Input input ) //The constructor
	{
		totalNumOfCS = Combinations.getNumOfCS( input.numOfAgents );
		totalNumOfCoalitionsInSearchSpace = Combinations.getNumOfCoalitionsInSearchSpace( input.numOfAgents );
		dpMaxSizeThatWasComputedSoFar = 1;
	}
	public int       numOfAgents; //the number of agents
	
	public long      totalNumOfCS; //The total number of coalition structures in the search space
	
	public long      totalNumOfCoalitionsInSearchSpace; //The number of coailtions in the search space. Here,
	//                                                    for every CS, we count the number of coalitions in it. 

	public long      totalNumOfExpansions; // the total number of expansions in the search space, given the tree-like representation of different integer-partition-based subspaces
	
	public IDPSolver_whenRunning_ODPIP idpSolver_whenRunning_ODPIP;
	
	//***************************************************************************
    //*****                                                                 *****
    //*****                          CPLEX results                          *****
    //*****                                                                 *****	
	//***************************************************************************

	public long     cplexTime; // only relevant when running ILOG's CPLEX
	
	public double   cpleXTime_confidenceInterval; // the error when averaging over multiple instances
	
	public  int[][] cplexBestCSFound; // only relevant when running ILOG's CPLEX
	
	public  double  cplexValueOfBestCSFound; // only relevant when running ILOG's CPLEX
	
	public double   cpleXValueOfBestCSFound_confidenceInterval; // the error when averaging over multiple instances
	
	//***************************************************************************
    //*****                                                                 *****
    //*****          Dynamic Programming (DP, IDP, or ODP) results          *****
    //*****                                                                 *****	
	//***************************************************************************
	
	private  int     dpMaxSizeThatWasComputedSoFar; // only relevant when running DP, IDP, or ODP 

	private  int[][] dpBestCSFound; //The best coalition structure found by DP, IDP, or ODP
	
	private  double  dpValueOfBestCSFound; //The value of the best coalition structure found by DP, IDP, or ODP
	
	public long      dpTime; // The time required for DP, IDP, or ODP
	
	public long[]    dpTimeForEachSize ; //The time required for DP, IDP, or ODP for each size
	
	//***************************************************************************
    //*****                                                                 *****
    //*****                       IP or ODP-IP results                      *****
    //*****                                                                 *****	
	//***************************************************************************

	private  int[][] ipBestCSFound; // only relevant when running IP or ODP-IP
	
	private  double  ipValueOfBestCSFound; // only relevant when running IP or ODP-IP

	public double    ipValueOfBestCS_confidenceInterval; // the error when averaging over multiple runs
	
	public long      ipStartTime; // only relevant when running IP or ODP-IP
	
	public double    ipTimeForScanningTheInput_confidenceInterval; // the error when averaging over multiple runs

	public long      ipTime; // only relevant when running IP or ODP-IP
	
	public double    ipTime_confidenceInterval; // the error when averaging over multiple runs

	public long      ipTimeForScanningTheInput; // only relevant when running IP or ODP-IP
	
	public long      ipNumOfExpansions; // only relevant when running IP or ODP-IP

	public double    ipNumOfExpansions_confidenceInterval; // the error when averaging over multiple runs	
	
	public double    ipUpperBoundOnOptimalValue; // only relevant when running IP or ODP-IP

	public double    ipUpperBoundOnOptimalValue_confidenceInterval; // the error when averaging over multiple runs		

	public double    ipLowerBoundOnOptimalValue; // only relevant when running IP or ODP-IP
	
	public IntegerPartitionGraph ipIntegerPartitionGraph; // only relevant when running IP or ODP-IP 
	
	private double[]  max_f; // e.g., max_f[3] is the maximum f value that the dynamic programming solver has computed for a coalition of size 4

	//*****************************************************************************************************
	
	/**
	 * Initializes the main parameters
	 */	
	public void initialize(){
		ipStartTime=System.currentTimeMillis();
		ipNumOfExpansions=0;
		ipValueOfBestCSFound=-1;
		ipBestCSFound = null;
		totalNumOfExpansions = 0;
	}
	
	//*****************************************************************************************************
	
	/**
	 * Only relevant when running DP, IDP, or ODP
	 * @param CS  the best coalition structure found so far
	 * @param value  the value of the best coalition structure found so far
	 */
	public void updateDPSolution( int[][] CS, double value ){
		if( get_dpValueOfBestCSFound() < value ){
			set_dpValueOfBestCSFound( value );
			set_dpBestCSFound( General.copyArray(CS) );
		}
	}
	/**
	 * Only relevant when running IP or ODP-IP
	 * @param CS  the best coalition structure found so far
	 * @param value  the value of the best coalition structure found so far
	 */
	public synchronized void updateIPSolution( int[][] CS, double value ){
		if( get_ipValueOfBestCSFound() <= value ){
			set_ipValueOfBestCSFound( value );
			set_ipBestCSFound( General.copyArray(CS) );
		}
	}
	
	//*****************************************************************************************************
	
	public synchronized void set_dpMaxSizeThatWasComputedSoFar( int size ){
		dpMaxSizeThatWasComputedSoFar = size;
	}
	public synchronized int get_dpMaxSizeThatWasComputedSoFar(){
		return dpMaxSizeThatWasComputedSoFar;
	}
	public synchronized void set_dpBestCSFound( int[][] CS ){
		dpBestCSFound = General.copyArray( CS );		
	}
	public synchronized int[][] get_dpBestCSFound(){
		return dpBestCSFound;		
	}
	public synchronized void set_dpValueOfBestCSFound(double value){
		dpValueOfBestCSFound = value;				
	}
	public synchronized double get_dpValueOfBestCSFound(){
		return dpValueOfBestCSFound;				
	}
	public /*synchronized*/ void set_ipBestCSFound( int[][] CS ){
		ipBestCSFound = General.copyArray( CS );		
	}
	public /*synchronized*/ int[][] get_ipBestCSFound(){
		return ipBestCSFound;		
	}
	public /*synchronized*/ void set_ipValueOfBestCSFound(double value){
		ipValueOfBestCSFound = value;				
	}
	public /*synchronized*/ double get_ipValueOfBestCSFound(){
		return ipValueOfBestCSFound;				
	}
	public void set_max_f( int index, double value){
		max_f[ index ] = value;
	}
	public double get_max_f( int index ){
		return( max_f[index] );
	}
	public void init_max_f( Input input, double[][] maxValueForEachSize ){
		max_f = new double[input.numOfAgents];
    	for( int i=0; i<input.numOfAgents; i++ )
    		set_max_f( i, 0 );
		for(int i=0; i<input.numOfAgents; i++){
			double value = input.getCoalitionValue( (1<<i) ); //compute max_f for coalitions of size = 1
			if( get_max_f( 0 ) < value )
				set_max_f( 0 ,   value );
		}
		for(int i=1; i<input.numOfAgents; i++) //compute max_f for coalitions of size = i+1
			set_max_f( i, maxValueForEachSize[i][0] );
	}
}