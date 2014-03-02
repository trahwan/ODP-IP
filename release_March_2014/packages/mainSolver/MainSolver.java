package mainSolver;

import cplexSolver.CplexSolver;
import general.Combinations;
import inputOutput.*;
import dpSolver.*;
import ipSolver.*;

public class MainSolver
{
	/** The main method which calls all solvers that are selected by the user.
	 */	
	public Result solve( Input input )	
	{
		ComputeErrorBars computeErrorBars = new ComputeErrorBars(input); //to compute the error when averaging over multiple runs

		for( int problemID=1; problemID<=input.numOfRunningTimes; problemID++) //for every problem instance
		{			
			input.problemID = (new Integer(problemID)).toString();
			Result result = new Result(input);
			Output output = new Output();
			output.initOutput( input );

			//If required, store the coalition values in a file
			if( input.storeCoalitionValuesInFile )
				input.storeCoalitionValuesInFile( problemID );
			
			//ApproximateShapley approximateShapley = new ApproximateShapley();  approximateShapley.approximateShapley(input);

			if ( input.solverName == SolverNames.CPLEX ){
				CplexSolver cplexSolver = new CplexSolver(); cplexSolver.solve( input, result );
			}
			else if ( (input.solverName == SolverNames.DP) || (input.solverName == SolverNames.IDP) ){
				DPSolver dpSolver = new DPSolver(input, result); dpSolver.runDPorIDP();
			}
			else if ( (input.solverName == SolverNames.ODP) ){
				DPSolver dpSolver = new DPSolver(input, result); dpSolver.runODP();
			}
			else if ( (input.solverName == SolverNames.IP) || (input.solverName == SolverNames.ODPIP) ){
				IPSolver ipSolver = new IPSolver(); ipSolver.solve( input, output, result );
			}
			if( input.numOfRunningTimes == 1 ) {
				return( result );
			}else{
				computeErrorBars.addResults( result );
				if( problemID < input.numOfRunningTimes ){
					if( input.readCoalitionValuesFromFile )
						input.readCoalitionValuesFromFile( problemID+1 );
					else
						input.generateCoalitionValues();
				}
				System.out.println(input.numOfAgents+" agents, "+ValueDistribution.toString(input.valueDistribution)+" distribution. The solver just finished solving "+input.problemID+" problems out of  "+input.numOfRunningTimes);
			}
		}
		Result averageResult = computeErrorBars.setAverageResultAndConfidenceIntervals( input );
		return( averageResult );
	}
}