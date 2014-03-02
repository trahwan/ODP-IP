package general;

import java.util.Arrays;

public class SubsetEnumerator
{
	/**
	 * Object used to enumerate all subsets of size k out of n elements. 
	 * Every subset is represented in bit format (i.e., bit mask), e.g.:
	 *     - given 10 elements, the first subset is {1,2,3}, represented as: 0000000111
	 *     
	 * @param n: The total number of elements
	 * @param s: The size of the subsets that will be enumerated
	 */
	final private int n;
	final private int s;
	private int currentSubset;
	
	//*********************************************************************************************************
	
	/**
	 * The constructor...	
	 */
	public SubsetEnumerator( int n, int s ){
		this.n = n;
		this.s = s;
	}

	//*********************************************************************************************************
	
	/**
	 * An example of how to use this class (just for testing)...
	 */
	public static void main(String[] args)
	{
		int n=6;
		System.out.println(" Testing the subset enumerator object \n The total number of elements is: "+n);
		//enumerate all subsets of size = 1, 2, ... n
		for(int s=1; s<=n; s++)
		{
			System.out.println("Current subset size is: "+s);
			SubsetEnumerator subsetEnumerator = new SubsetEnumerator(n, s);
			int subset = subsetEnumerator.getFirstSubset();   //this will return the first subset in the list
			while(subset < (1<<n)){
				//Put here the code that needs to deal with "subset", e.g., printing it as below:
				System.out.println( "the current subset is "+Arrays.toString(Combinations.convertCombinationFromBitToByteFormat( subset, n, s)));
				//generate the next subset
				subset = subsetEnumerator.getNextSubset();
			}
		}
	}
	
	//*********************************************************************************************************

	/**
	 * Given a subset, C, of size "s", this method returns the subset C' that is located just
	 * after C in the list of subsets of size s. If there is no such subset (i.e., if
	 * we have reached the end of the list), then the method returns "1<<n".
	 */
	public int getFirstSubset()
	{
		currentSubset=0;
		for(int i=0; i<s; i++)
			currentSubset += (1<<i);
		return( currentSubset );
	}
	
	//*********************************************************************************************************
	
	/**
	 * If (currentSubset = C), this method sets (currentSubset = C') where C' is located just
	 * after C in the list of subsets of size s. If there is no such subset (i.e., if
	 * we have reached the end of the list), then the method returns "1<<n".
	 */
	public int getNextSubset()
	{
		currentSubset = Combinations.getPreviousCombinationInBitFormat2(n, s, currentSubset);
		return(currentSubset);
	}
}