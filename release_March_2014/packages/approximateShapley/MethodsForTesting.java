package approximateShapley;

import general.General;
import inputOutput.Input;

public class MethodsForTesting
{
	/**
	 * Computes the minimum of the marginal contributions of the agent via brute force
	 */
	public static double computeMinContributionViaBruteForce(Input input, int agentName )
	{
		double min = Double.MAX_VALUE; 
		for(int coalitionInBitFormat=1; coalitionInBitFormat<Math.pow(2, input.numOfAgents); coalitionInBitFormat++){
			if ((coalitionInBitFormat & (1<<(agentName-1))) != 0){ //If the agent is a member of the coalition
				double marginalContribution = ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat ) - ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat - (1<<(agentName-1)) );
				if( min > marginalContribution ) min = marginalContribution;
			}
		}
		return min;
	}
	
	//*****************************************************************************************************
	
	/**
	 * Computes the range of the marginal contributions of the agent via brute force
	 */
	public static double computeRangeViaBruteForce(Input input, int agentName )
	{
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE; 
		for(int coalitionInBitFormat=1; coalitionInBitFormat<Math.pow(2, input.numOfAgents); coalitionInBitFormat++){
			if ((coalitionInBitFormat & (1<<(agentName-1))) != 0){ //If the agent is a member of the coalition
				double marginalContribution = ApproximateShapley.characteristicFunction( input, coalitionInBitFormat ) - ApproximateShapley.characteristicFunction( input, coalitionInBitFormat - (1<<(agentName-1)) );
				if( min > marginalContribution ) min = marginalContribution;
				if( max < marginalContribution ) max = marginalContribution;
			}
		}
		double range = max - min;
		return range;
	}
	
	//*****************************************************************************************************
	
	/**
	 * Computes the variance of the marginal contributions of the agent via brute force
	 */
	public static double computeVarianceViaBruteForce( Input input, int agentName )
	{
		//Compute Shapley's weights. Basically, ShapleyWeight[s] is the weight for a coalition of size s, which is: s!(n-s-1)! / n! 
		double[] ShapleyWeight = new double[ input.numOfAgents+1 ];
		for(int s=0; s<=input.numOfAgents; s++)
			ShapleyWeight[s] = General.factorial(s) * (double) General.factorial( input.numOfAgents-s-1) / General.factorial( input.numOfAgents );
		
		//Compute the average of all marginal contributions of the agent, taking into account Shapley's weights
		double averageMarginalContribution = 0;
		for(int coalitionInBitFormat=1; coalitionInBitFormat<Math.pow(2, input.numOfAgents); coalitionInBitFormat++){
			if ((coalitionInBitFormat & (1<<(agentName-1))) != 0){ //If the agent is a member of the coalition
				double marginalContribution = ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat ) - ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat - (1<<(agentName-1)) );
				averageMarginalContribution += marginalContribution * ShapleyWeight[ Integer.bitCount(coalitionInBitFormat) - 1 ];
			}
		}
		System.out.println("The actual Shapley value agent "+agentName+" is "+General.setDecimalPrecision(averageMarginalContribution,6));

		//Compute the variance of the marginal contributions of the agent, taking into account Shapley's weights
		double variance = 0;
		for(int coalitionInBitFormat=1; coalitionInBitFormat<Math.pow(2, input.numOfAgents); coalitionInBitFormat++){
			if ((coalitionInBitFormat & (1<<(agentName-1))) != 0){ //If the agent is a member of the coalition
				double marginalContribution = ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat ) - ApproximateShapley.characteristicFunction( input,  coalitionInBitFormat - (1<<(agentName-1)) );
				variance += Math.pow( marginalContribution - averageMarginalContribution , 2 )  * ShapleyWeight[ Integer.bitCount(coalitionInBitFormat) - 1 ];
			}
		}
		return variance;
	}
}
