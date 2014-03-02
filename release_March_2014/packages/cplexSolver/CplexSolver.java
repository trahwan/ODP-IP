package cplexSolver;

import ilog.concert.*;
import ilog.cplex.*;
import inputOutput.Input;

import java.util.*;
import mainSolver.Result;

public class CplexSolver
{
	private double[] optimizationResult;	
	private int[][] bestCSFoundByCPLEX;	
	private double objectiveValue;	
	
	//******************************************************************************************************

	/**
	 * The main method in the CPLEX solver
	 */
	public void solve( Input input, Result result )
	{
		try
		{	
			//Solve the optimization problem using CPLEX
			long startTime = System.currentTimeMillis();
			IloCplex cplex = new IloCplex();
			IloNumVar[][] var = new IloNumVar[1][];
			IloRange[][] rng = new IloRange[1][];
			populateMatrix(cplex, var, rng, input.numOfAgents, input.coalitionValuesAsHashTable);
			cplex.setParam(IloCplex.BooleanParam.PreInd, false);
			long timeElapsed = System.currentTimeMillis()-startTime;

			if( cplex.runDPorIDP() ) //If CPLEX has found a solution
			{
				optimizationResult = cplex.getValues(var[0]);
				setBestCSFoundByCPLEX( input.numOfAgents, optimizationResult );
 				System.out.println("CPLEX time (in milliseconds) = "+timeElapsed);
				System.out.println("CPLEX Solution status = " + cplex.getStatus());
				System.out.println("CPLEX Solution value  = " + cplex.getObjValue());
				for (int j = 0; j < optimizationResult.length; ++j) {
					if (optimizationResult[j] != 0)
						System.out.println("Column: " + j + " Value = " + optimizationResult[j]);
				}
			}
			//cplex.exportModel("mipex1.lp");
			cplex.end();
			
			result.cplexTime = timeElapsed;
			result.cplexBestCSFound = bestCSFoundByCPLEX;
			result.cplexValueOfBestCS = objectiveValue;	
		}
		catch (IloException e) {
			System.err.println("Concert exception caught '" + e + "' caught");
			e.printStackTrace(System.out);
		}
	}
	
	//******************************************************************************************************

	private void populateMatrix(IloMPModeler model,	IloRange[][] rng, int numOfAgents, double[] coalitionValuesAsHashTable) throws IloException
	{
		int numOfCoalitions = (int) Math.pow(2, (numOfAgents));
		IloIntVar[] x = model.boolVarArray(numOfCoalitions);
		model.addMaximize(model.scalProd(x, coalitionValuesAsHashTable));

		/* Generating the constraints matrix for the complete set partitioning problem.
		 */
		//IloLPMatrix lp = ((IloMPModeler) model).addLPMatrix("Andrea");
		//int[][] mutuallyExclusive = new int[nbAgents][nbCoalitions];
		IloLinearIntExpr linexr;
		for (int i = numOfAgents - 1; i >= 0; i--)
		{
			int power = (int) Math.pow(2, i);
			int value = power;
			int index = 0;
			linexr = model.linearIntExpr();
			for (int j = 0; j < Math.pow(2, numOfAgents - i - 1); j++) {
				for (int k = 0; k < power; k++)
				{
					index++;
					value = value + 1;
					//writeln(value);
					//System.out.print(value + "," + (nbAgents - i-1)+","+(index)+";");
					//mutuallyExclusive[nbAgents - i-1][index] = value;
					linexr.addTerm(x[value - 1], 1);
				}
				value = value + power;
			}
			model.addEq(1, linexr);
		}
	}
	
	//******************************************************************************************************

	public void setBestCSFoundByCPLEX( int numOfAgents, double[] optimizationResult )
	{
        //bit[i] is the bit representing agent a_i (e.g. given 4 agents, bit[2]=2=0010, bit[3]=4=0100, etc.)
		int[] bit = new int[numOfAgents+1];
		for(int i=0; i<numOfAgents; i++)
			bit[i+1] = 1 << i;
		
		//Count the number of nonZero elements
		int numOfNonZero=0;
		for(int i=0; i<optimizationResult.length; i++ )
			if( optimizationResult[i] != 0 )
				numOfNonZero++;
		
		int coalitionIndex=0;
		bestCSFoundByCPLEX = new int[ numOfNonZero ][];
		for(int i=0; i<optimizationResult.length; i++ )
		{
			//Initialization
			if( optimizationResult[i] == 0 ) continue;
			long coalition = i;
			int indexOfNextMember = 0;
			int[] coalitionAsBytes = new int[numOfAgents]; 

			/* Calculate coalition size, and fill the variable "coalitionAsBytes" with ints representing members
			 * of the coalition. For example, if coalition=[0,1,0,0,1], then coalitionAsBytes would be: [2,5,0,0,0]
			 */			
			int sizeOfCoalition = 0;
			for( int j=1 ; j<=numOfAgents ; j++ ) { // For each agent
				if( (coalition & bit[j]) != 0 ) { // if the agent is a member of the coalition
					sizeOfCoalition++;
					coalitionAsBytes[ indexOfNextMember ] = (int)(j-1);
					indexOfNextMember++;
				}
			}			
			//Remove the zeros from the end of the coalition (for more detail, read the comment above)
			bestCSFoundByCPLEX[ coalitionIndex ] = new int[ indexOfNextMember ];
			for( int j=0; j<bestCSFoundByCPLEX[ coalitionIndex ].length; j++) {
				bestCSFoundByCPLEX[ coalitionIndex ][j] = coalitionAsBytes[j];
			}
			coalitionIndex++;
		}
	}
}