package general;

import java.util.Random;

public class RandomSubsetOfGivenSet
{
	final private int[] givenSet;
	final private int numOfElementsInGivenSet;
	final private int sizeOfSubsetToBeSampled;
	final private boolean subsetEqualsGivenSet;
	private int[] availableIndicesToChooseFrom;
	private Random randomGenerator;
	
	//******************************************************************************************************
	
	/**
	 * The constructor:
	 * @param givenSet: The set from which subsets will be sampled
	 * @param numOfElementsInGivenSet: the number of elements in the set from which subsets will be sampled
	 * @param sizeOfSubsetToBeSampled: the size of every subset that will be sampled.
	 */
	public RandomSubsetOfGivenSet(int[] givenSet, int numOfElementsInGivenSet, int sizeOfSubsetToBeSampled ) {
		this.givenSet = givenSet;
		this.numOfElementsInGivenSet = numOfElementsInGivenSet;
		this.sizeOfSubsetToBeSampled = sizeOfSubsetToBeSampled;
		this.randomGenerator = new Random();
		this.availableIndicesToChooseFrom = new int[ numOfElementsInGivenSet ];
		if( sizeOfSubsetToBeSampled == numOfElementsInGivenSet )
			subsetEqualsGivenSet = true;
		else
			subsetEqualsGivenSet = false;
	}
	
	//******************************************************************************************************
	
	/**
	 * Returns random subset of size: "sizeOfSubsetToBeSampled", taken from the set: "givenSet"
	 */
	public int[] getSubsetInByteFormat()
	{
		if( this.subsetEqualsGivenSet ){
			int[] copyOfGivenSet = new int[ numOfElementsInGivenSet ];			
			for(int i=0; i<numOfElementsInGivenSet; i++){
				copyOfGivenSet[i] = givenSet[i];
			}
			return copyOfGivenSet;
		}else{
			//this.randomGenerator = new Random();
			for(int i=numOfElementsInGivenSet-1; i>=0; i--){
				availableIndicesToChooseFrom[i] = i;
			}
			int[] subsetInByteFormat = new int[sizeOfSubsetToBeSampled];
			int indexInSubset = 0;
			for(int i=numOfElementsInGivenSet-1; i>=numOfElementsInGivenSet-sizeOfSubsetToBeSampled; i--)
			{
				int j = this.randomGenerator.nextInt(i+1);
				subsetInByteFormat[indexInSubset] = givenSet[ availableIndicesToChooseFrom[ j ] ];
				indexInSubset++;
				availableIndicesToChooseFrom[ j ] = availableIndicesToChooseFrom[ i ]; 
			}
			return subsetInByteFormat;
		}
	}
	
	//******************************************************************************************************
	
	/**
	 * Returns random subset of size: "sizeOfSubsetToBeSampled", taken from the set: "givenSet"
	 */
	public int getSubsetInBitFormat()
	{
		if( this.subsetEqualsGivenSet ){
			int copyOfGivenSet = 0;			
			for(int i=0; i<numOfElementsInGivenSet; i++){
				//Add agent "givenSet[i]" to "copyOfGivenSet"
				copyOfGivenSet += 1 << (givenSet[i]-1);
			}
			return copyOfGivenSet;
		}else{
			//this.randomGenerator = new Random();
			for(int i=numOfElementsInGivenSet-1; i>=0; i--){
				availableIndicesToChooseFrom[i] = i;
			}
			int subsetInBitFormat = 0;
			int indexInSubset = 0;
			for(int i=numOfElementsInGivenSet-1; i>=numOfElementsInGivenSet-sizeOfSubsetToBeSampled; i--)
			{
				int j = this.randomGenerator.nextInt(i+1);
				//Add agent "givenSet[ availableIndicesToChooseFrom[ j ] ]" to "subsetInBitFormat"
				subsetInBitFormat += 1 << (givenSet[ availableIndicesToChooseFrom[ j ] ] - 1);
				indexInSubset++;
				availableIndicesToChooseFrom[ j ] = availableIndicesToChooseFrom[ i ]; 
			}
			return subsetInBitFormat;
		}
	}
}
