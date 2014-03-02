package ipSolver;

import java.util.TreeSet;

import general.Combinations;
import inputOutput.Input;
import inputOutput.Output;
import mainSolver.Result;

public class MethodsForScanningTheInput
{
	//This class is only used while scanning the input, and that is only if there are constraints.
	protected static class CheckFeasibilityOfCoalitions
	{
		//The data members
		public boolean firstCoalitionIsFeasible =true;  public boolean firstCoalitionAsSingletonsIsFeasible =true;
		public boolean secondCoalitionIsFeasible=true;  public boolean secondCoalitionAsSingletonsIsFeasible=true;		
		/**
		 * This method fills the data members. Here, "firstCoalition" and "secondCoalition" are provided as int arrays
		 * In more detail, this method determines whether:
		 *   (1) "firstCoalition"  is feasible
		 *   (2) "secondCoalition" is feasible
		 *   (3) the singletons that can be formed from "firstCoalition"  are all feasible
		 *   (4) the singletons that can be formed from "secondCoalition" are all feasible
		 */
		public void setDataMembers( int numOfAgents, int[] firstCoalition, int[] secondCoalition, TreeSet<Integer> feasibleCoalitions  )
		{
			//If there are no constraints to be dealt with, then do nothing
			if( feasibleCoalitions == null ) return;
			int firstCoalitionInBitFormat  = Combinations.convertCombinationFromByteToBitFormat( firstCoalition );
			int secondCoalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( secondCoalition );
			setDataMembers( numOfAgents,firstCoalitionInBitFormat,secondCoalitionInBitFormat,feasibleCoalitions );
		}		
		/**
		 * This method fills the data members. Here, "firstCoalition" and "secondCoalition" are provided in bit format (i.e., as masks)
		 * In more detail, this method determines whether:
		 *   (1) "firstCoalition"  is feasible
		 *   (2) "secondCoalition" is feasible
		 *   (3) the singletons that can be formed from "firstCoalition"  are all feasible
		 *   (4) the singletons that can be formed from "secondCoalition" are all feasible
		 */
		public void setDataMembers( int numOfAgents,int firstCoalition,int secondCoalition,TreeSet<Integer> feasibleCoalitions )
		{
			//If there are no constraints to be dealt with, then do nothing
			if( feasibleCoalitions == null ) return;
			
			firstCoalitionIsFeasible  = feasibleCoalitions.contains(new Integer(firstCoalition ));
			secondCoalitionIsFeasible = feasibleCoalitions.contains(new Integer(secondCoalition));

			firstCoalitionAsSingletonsIsFeasible = true;
			for(int i=0; i<numOfAgents; i++){
				if ((firstCoalition & (1<<i)) != 0){ //If agent "i+1" is a member of "firstCoalition"
					if( feasibleCoalitions.contains(new Integer((1<<i))) == false ){
						firstCoalitionAsSingletonsIsFeasible = false;
						break;
					}
				}
			}
			secondCoalitionAsSingletonsIsFeasible = true;
			for(int i=0; i<numOfAgents; i++){
				if ((secondCoalition & (1<<i)) != 0){ //If agent "i+1" is a member of "secondCoalition"
					if( feasibleCoalitions.contains(new Integer((1<<i))) == false ){
						secondCoalitionAsSingletonsIsFeasible = false;
						break;
					}
				}
			}
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * (1) Calculates the average of all the values of sizes s=1,...,numOfAgents.
	 * (2) Searches the second level of the sie graph
	 */
	public static void scanTheInputAndComputeAverage( Input input, Output output, Result result, double[] avgValueForEachSize)
	{
		//initialization
		long startTime = System.currentTimeMillis();
		int numOfAgents = input.numOfAgents;
		int totalNumOfCoalitions = (int)Math.pow(2,numOfAgents);
		double bestValue=result.get_ipValueOfBestCSFound();
		int bestCoalition1=0; int bestCoalition2=0;
		double[] sumOfValues = new double[ numOfAgents ];
		for(int size=1; size<=numOfAgents; size++)
			sumOfValues[size-1]=0;

		//Initialize the variables that are only used if there are constraints
		boolean coalition1IsFeasible = true;
		boolean coalition2IsFeasible = true;
		final boolean constraintsExist;
		if( input.feasibleCoalitions == null )
			constraintsExist = false;
		else
			constraintsExist = true;

		for(int coalition1=totalNumOfCoalitions/2; coalition1<totalNumOfCoalitions-1; coalition1++)
		{
			int sizeOfCoalition1 = Combinations.getSizeOfCombinationInBitFormat( coalition1, numOfAgents );
			
			int coalition2 = totalNumOfCoalitions - 1 - coalition1;
			int sizeOfCoalition2 = numOfAgents - sizeOfCoalition1;

			sumOfValues[ sizeOfCoalition1-1 ] += input.getCoalitionValue( coalition1 );
			sumOfValues[ sizeOfCoalition2-1 ] += input.getCoalitionValue( coalition2 );
			
			//Check whether "coalition1" is feasible and whether "coalition2" is feasible
			if( constraintsExist ){
				coalition1IsFeasible = input.feasibleCoalitions.contains(new Integer(coalition1));
				coalition2IsFeasible = input.feasibleCoalitions.contains(new Integer(coalition2));
			}			
			//Compute the value of the current pair of coalitions
			double value = 0;
			if(( constraintsExist == false )||( (coalition1IsFeasible)&&(coalition2IsFeasible) )){
				value = input.getCoalitionValue( coalition1 ) + input.getCoalitionValue( coalition2 );
			}						
			//if this value is greater than best_value, then update best_value...
			if( bestValue < value ) { bestCoalition1=coalition1; bestCoalition2=coalition2; bestValue = value; }
		}
		
		//If this search has improved the value of the best CS found...
		if( bestValue > result.get_ipValueOfBestCSFound() )
		{
			int[][] bestCS = new int[2][];
			bestCS[0] = Combinations.convertCombinationFromBitToByteFormat(bestCoalition1, numOfAgents);
			bestCS[1] = Combinations.convertCombinationFromBitToByteFormat(bestCoalition2, numOfAgents);
			result.updateIPSolution( bestCS, bestValue );
		}
		output.printCurrentResultsOfIPToStringBuffer_ifPrevResultsAreDifferent( input, result );

		//For every coalition size, calculate the average coalition value
		for(int size=1; size<=numOfAgents; size++)
			avgValueForEachSize[ size-1 ] = (double) sumOfValues[ size-1 ] / Combinations.binomialCoefficient(numOfAgents,size); 

		//Update the number of coalitions, and coalition structures, that were searched after scanning the input
		updateNumOfSearchedAndRemainingCoalitionsAndCoalitionStructures( input, result );
		
		//Calculate the time required to scan the input
		result.ipTimeForScanningTheInput = System.currentTimeMillis() - startTime;
	}

	//******************************************************************************************************
	
	/**
	 * Update the number of coalitions, and coalition structures, that were searched after scanning the input
	 */
	private static void updateNumOfSearchedAndRemainingCoalitionsAndCoalitionStructures( Input input, Result result )
	{
		for(int size1=1; size1<=Math.floor(input.numOfAgents/(double)2); size1++)
		{
			//From size1, calculate size2 (e.g., for 7 agents, if size1=1 then size2=6, and if size1=2 then size2=5)
			int size2=(int)(input.numOfAgents-size1);
			
			//Compute the No. of coalitions of size "size1" (p.s., this is the same as the No. of coalitions of size "size2")
			int numOfCombinationsOfSize1 = (int) Combinations.binomialCoefficient( input.numOfAgents, size1 );

			//Compute the number of coalitions of size "size1" that IP scanned
			//(p.s., this is the same as the number of coalitions of size "size2" that IP scanned)
			int temp; 
			if( size1 != size2 ) 
				temp = numOfCombinationsOfSize1;
			else
				temp =(numOfCombinationsOfSize1/2);

			result.ipNumOfExpansions  += 2*temp;;
		}
	}
}