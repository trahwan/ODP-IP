package approximateShapley;

import inputOutput.Input;
import general.General;
import general.RandomPermutation;

public class ApproximateShapley
{
	/**
	 * Basically, we want an outcome like this: Based on 1000 samples, we are 95% confidant that the Shapley
	 * value of agent 4 is 4500 +/- 200. Here, "numOfSamples"=1000, "failureProbability"=0.05, and "error"=200. 
	 * 
	 * In more detail, for any agent:
	 * 
	 *     - You need to know either:
	 *          - the range (i.e., max - min) of the marginal contributions of an agent to a coalition, or
	 *          - the variance of the marginal contributions of the agent to a coalition.
	 *         
	 *     - You need to specify the desired "failureProbability". 
	 *
	 *     - After that, you either:
	 *     
	 *          - specify the desired "numOfSamples", and the code will sample, approximate, and compute the error
	 *         
	 *          - specify the desired "error", then the code will determine the required "numOfSamples" in order
	 *            to obtain that error. 
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * This method provides examples of how to use the code.
	 */
	public static void approximateShapley(Input input)
	{
		int numOfAgents = input.numOfAgents;

		double failureProbability = 0.1; // setting this to 0.1 results in 90% confidence

		double error; // the acceptable error when approximating the Shapley value

		int numOfSamples; //the number of samples to be taken when approximating the Shapley value

		for(int agentName=1; agentName <= numOfAgents; agentName++) // for every agent, approximate its Shapley value
		{
			//If you know the range (i.e., max - min) of the marginal contributions of the agent...
			double range;
			range = MethodsForTesting.computeRangeViaBruteForce(input, agentName); //This method is only for testing. You shouldn't use it.
			//This is because if you have time to iterate over all coalition values, you don't need to approximate the Shapley
			//value anymore; you can compute the exact Shapley value.
			
			//If you know the variance of the marginal contributions of the agents...
			double variance;
			variance = MethodsForTesting.computeVarianceViaBruteForce(input, agentName); //This method is only for testing. You shouldn't use it.
			//This is because if you have time to iterate over all coalition values, you don't need to approximate the Shapley
			//value anymore; you can compute the exact Shapley value.

			/* We want an outcome like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
			 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
			 * 
			 * If you specify a certain acceptable error, the methods below will determine the number of required samples (taken
			 * from the space of marginal contributions), and then print the approximated Shapley value based on those samples. 
			 */
			error = 0.1 * ((range/2) + MethodsForTesting.computeMinContributionViaBruteForce(input, agentName));
			ApproximateShapleyGivenErrorAndRange   ( input, agentName, numOfAgents, failureProbability, error, range    );
			ApproximateShapleyGivenErrorAndVariance( input, agentName, numOfAgents, failureProbability, error, variance );
			
			/* We want an outcome like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
			 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
			 * 
			 * If you specify a certain number of samples to be taken (from the space of marginal contributions), then the
			 * methods below will take those samples, and print the approximated Shapley value and the approximation error. 
			 */
			numOfSamples = (int) Math.round( 0.01 * Math.pow( 2, input.numOfAgents-1) );
			ApproximateShapleyGivenNumOfSamplesAndRange   ( input, agentName, numOfAgents, failureProbability, numOfSamples, range    );
			ApproximateShapleyGivenNumOfSamplesAndVariance( input, agentName, numOfAgents, failureProbability, numOfSamples, variance );
		}
	}
	
	//*****************************************************************************************************

	/**
	 * This is a very important method that needs to be specified based on the problem domain. It is the
	 * characteristic function---a function that, given a set of agents, returns a real number that represents
	 * the quality of the outcome of this coalition. It is often assumed that the greater the value the better 
	 */
	public static double characteristicFunction( Input input, int[] coalitionMembers )
	{
		if( coalitionMembers.length == 0 ) return 0;
		return input.getCoalitionValue( General.sortArray(coalitionMembers,true) );
	}
	public static double characteristicFunction( Input input, int coalitionMembersInBitFormat )
	{
		if( coalitionMembersInBitFormat == 0 ) return 0;
		return input.getCoalitionValue( coalitionMembersInBitFormat );
	}

	//*****************************************************************************************************

	/**
	 * This method prints something like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
	 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * @param agentName  P.S., the name of the first agent is 1, not zero
	 * @param numOfAgents
	 * @param failureProbability  i.e., the acceptable confidence about the result, e.g., failureProbability = 0.05 corresponds to 95% confidence
	 * @param error  i.e., to allow us to report: shapley value = something +/- error.
	 * @param range  i.e., the maximum - minimum marginal contributions of the agent
	 */
	public static void ApproximateShapleyGivenErrorAndRange( Input input, int agentName, int numOfAgents, double failureProbability, double error, double range )
	{
		int numOfSamples = (int) Math.ceil(  (Math.log( 2 / failureProbability )*Math.pow( range, 2))  /  (2 * Math.pow(error, 2)) );
		double approximateShapley = getApproximateShapleyValue( input, agentName, numOfAgents, numOfSamples );
		double errorAsPercentageOfShapley = error*100/approximateShapley;
		System.out.println("Based on "+numOfSamples+" samples, we are "+((1-failureProbability)*100)+"% confidant that the Shapley Value of agent "+agentName+" is "+General.setDecimalPrecision(approximateShapley,6)+" +/- "+General.setDecimalPrecision(errorAsPercentageOfShapley,3)+"%");
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method prints something like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
	 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * @param agentName  P.S., the name of the first agent is 1, not zero
	 * @param numOfAgents
	 * @param failureProbability  i.e., the acceptable confidence about the result, e.g., failureProbability = 0.05 corresponds to 95% confidence
	 * @param error  i.e., to allow us to report: shapley value = something +/- error.
	 * @param variance  the variance of the marginal contributions of the agent
	 */
	public static void ApproximateShapleyGivenErrorAndVariance( Input input, int agentName, int numOfAgents, double failureProbability, double error, double variance )
	{
		int numOfSamples = (int) Math.ceil( variance / (failureProbability * Math.pow( error, 2)));
		double approximateShapley = getApproximateShapleyValue( input, agentName, numOfAgents, numOfSamples );
		double errorAsPercentageOfShapley = error*100/approximateShapley;
		System.out.println("Based on "+numOfSamples+" samples, we are "+((1-failureProbability)*100)+"% confidant that the Shapley Value of agent "+agentName+" is "+General.setDecimalPrecision(approximateShapley,6)+" +/- "+General.setDecimalPrecision(errorAsPercentageOfShapley,3)+"%");
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method prints something like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
	 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * @param agentName  P.S., the name of the first agent is 1, not zero
	 * @param numOfAgents
	 * @param failureProbability  i.e., the acceptable confidence about the result, e.g., failureProbability = 0.05 corresponds to 95% confidence
	 * @param numOfSamples  the number of samples to be take from the space of marginal contributions of the agent
	 * @param range  i.e., the maximum - minimum marginal contributions of the agent
	 */
	public static void ApproximateShapleyGivenNumOfSamplesAndRange( Input input, int agentName, int numOfAgents, double failureProbability, int numOfSamples, double range )
	{
		double approximateShapley = getApproximateShapleyValue( input, agentName, numOfAgents, numOfSamples );
		double error = Math.sqrt( (Math.log( 2 / failureProbability )*Math.pow( range, 2)) / (2 * numOfSamples) );
		double errorAsPercentageOfShapley = error*100/approximateShapley;
		System.out.println("Based on "+numOfSamples+" samples, we are "+((1-failureProbability)*100)+"% confidant that the Shapley Value of agent "+agentName+" is "+General.setDecimalPrecision(approximateShapley,6)+" +/- "+General.setDecimalPrecision(errorAsPercentageOfShapley,3)+"%");
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method prints something like: Based on 1000 samples, we are 95% confidant that the Shapley value of agent 4 is 4500 +/- 200.
	 * Here, nameOfAgent = 4, numOfSamples=1000, failureProbability=0.05, and error=200.
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * @param agentName  P.S., the name of the first agent is 1, not zero
	 * @param numOfAgents
	 * @param failureProbability  i.e., the acceptable confidence about the result, e.g., failureProbability = 0.05 corresponds to 95% confidence
	 * @param numOfSamples  the number of samples to be take from the space of marginal contributions of the agent
	 * @param variance  the variance of the marginal contributions of the agent
	 */
	public static void ApproximateShapleyGivenNumOfSamplesAndVariance( Input input, int agentName, int numOfAgents, double failureProbability, int numOfSamples, double variance )
	{
		double approximateShapley = getApproximateShapleyValue( input, agentName, numOfAgents, numOfSamples );
		double error = Math.sqrt( variance / (failureProbability * numOfSamples) );
		double errorAsPercentageOfShapley = error*100/approximateShapley;
		System.out.println("Based on "+numOfSamples+" samples, we are "+((1-failureProbability)*100)+"% confidant that the Shapley Value of agent "+agentName+" is "+General.setDecimalPrecision(approximateShapley,6)+" +/- "+General.setDecimalPrecision(errorAsPercentageOfShapley,3)+"%");
	}
	
	//*****************************************************************************************************
	
	/**
	 * A method that returns the approximated Shapley value (based on sampling), without specifying any error or
	 * confidence; these are determined by the method that calls "approximateShapleyValue"
	 * 
	 * P.S., make sure you specify the method "characteristicFunction" in a way that best suits your problem domain
	 * 
	 * @param agentName  P.S., the name of the first agent is 1, not zero
	 * @param numOfAgents
	 * @param numOfSamples  the number of samples to be take from the space of marginal contributions of the agent
	 * @return  the approximated Shapley value, without specifying any error or confidence; these are determined by
	 * the method that calls "approximateShapleyValue"
	 */
	private static double getApproximateShapleyValue( Input input, int agentName, int numOfAgents, int numOfSamples )
	{
		RandomPermutation permutationGenerator = new RandomPermutation( numOfAgents );
		double sumOfMarginalContributions = 0;

		for(int sample=1; sample <= numOfSamples; sample++)			
		{
			//sample a random permutation of the set: 1, ..., numOfAgents
			int[] randomPermutation = permutationGenerator.get();
			
			//find the index at which "agentName" appears in the permutation
			int index=0;
			while( randomPermutation[ index ] != agentName ){
				index++;
			}			
			//Create two coalitions: The first consists of all agents before "agentName" in the permutation
			//The second is the same as the first, except that it also contains "agentName" 
			int[] coalitionWithoutAgent = new int[ index ];
			int[] coalitionWithAgent = new int[ index + 1 ];
			for(int i=0; i<index; i++){
				coalitionWithoutAgent[i] = randomPermutation[i];
				coalitionWithAgent[i] = randomPermutation[i];
			}
			coalitionWithAgent[index] = agentName;				

			//update the sum of marginal contributions of the agent
			sumOfMarginalContributions += characteristicFunction( input, coalitionWithAgent ) - characteristicFunction( input, coalitionWithoutAgent );
		}
		//divide the sum of marginal contributions by the number of samples taken
		double approximateShapleyValue = sumOfMarginalContributions / numOfSamples;		
		return( approximateShapleyValue );
	}
}
