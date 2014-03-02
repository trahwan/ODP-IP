package general;

import java.util.Arrays;
import java.util.Random;

public class SubsetsOfMultiset
{
	final private ElementOfMultiset[] multiset; //The multiset from which we want to take subsets
	
	final private int sizeOfSubsets; //the size of the desired subsets
	
	private int totalNumOfElementsInMultiset; //the total number of elements (including repetitions)
	
	private ElementOfMultiset[] multisetWithIncrementalElements; //for example, if
	//multiset = [2,2,2,5,5,6], then multisetWithIncrementalElements = [1,1,1,2,2,3]
	
	private boolean currentSubsetIsFirstSubset; //if "true", then the method "getNextSubset" would
	//return the first subset in the list of subsets of "multisetWithIncrementalElements"

	private ElementOfMultiset[] currentSubset; //current subset in the list of subsets of "multisetWithIncrementalElements"
	
	private int numOfUniqueElementsInCurrentSubset; //the number of unique elements in the current subset. this
	//should be updated every time "currentSubset" is changed
	
	private ElementOfMultiset[] lastSubset; //if multisetWithIncrementalElements = [1,1,1,2,2,3], and sizeOfSubset=4, then
	//then lastSubset = [1,2,2,3]
	
	private int[] numOfInstancesOutsideSubset; //For every element in the current subset, this array contains the number of
	//copies of that element that did not appear in the subset. For example, if multiset = [1,1,1,1,2,2,2,3,3,3,4,4,4], and
	//the current subset is [1,1,3,3,4], then this method returns [2,1,2]. This is because the numbers of instances outside
	//the current subset are: 2 for element "1", 1 for element "3", and 2 for element "4"
	
	private final boolean keepTrackOfNumOfInstancesOutsideSubset; //if "true" then for every element "e" in the current
	//subset, the "SubsetOfMultiset" object will keep track of how many the object will keep track of how many
	//instances of "e" were left out of the current subset.
	
	//************************************************************************************************
	
	/**
	 * The constructor
	 * @param	multiset	the multiset
	 * @param	sizeOfSubset	the sizes of the required subsets
	 * @param	keepTrackOfNumOfInstancesOutsideSubset	if "true" then for every element "e" in the current
	 * subset, the "SubsetOfMultiset" object will keep track of how many the object will keep track of how
	 * many instances of "e" were left out of the current subset.
	 */
	public SubsetsOfMultiset( ElementOfMultiset[] multiset, int sizeOfSubsets, boolean keepTrackOfNumOfInstancesOutsideSubset )
	{
		//Initialise
		this.multiset = multiset;
		this.sizeOfSubsets = sizeOfSubsets;
		this.keepTrackOfNumOfInstancesOutsideSubset = keepTrackOfNumOfInstancesOutsideSubset;
		resetParameters();
	}
	
	//*********************************************************************************************************
	
	/**
	 * resets all parameters (used mainly in the constructor)
	 */
	public void resetParameters()
	{
		currentSubsetIsFirstSubset = true;
		
		multisetWithIncrementalElements = new ElementOfMultiset[ multiset.length ];
		for(int i=0; i<multiset.length; i++){
			multisetWithIncrementalElements[i] = new ElementOfMultiset( i+1, multiset[i].repetition );
		}
		setLastSubset();
		currentSubset = new ElementOfMultiset[ multiset.length ];
		
		//Compute the total number of agents
		totalNumOfElementsInMultiset = 0;
		for( int i=0; i<multiset.length; i++){
			totalNumOfElementsInMultiset += multiset[i].repetition;
		}
	}

	//*********************************************************************************************************
	
	/**
	 * An example of how to use this class (just for testing)...
	 */
	public static void main(String[] args)
	{
		//Create a test case, which is multiset = [4,4,4,5,5,6]
		ElementOfMultiset[] multiset = new ElementOfMultiset[ 3 ];
		multiset[0] = new ElementOfMultiset( 4, 3 );
		multiset[1] = new ElementOfMultiset( 5, 2 );
		multiset[2] = new ElementOfMultiset( 6, 1 );
		
		//enumerate all subsets of size = 1, 2, 3, ...
		for(int size=1; size<=3; size++)
		{
			System.out.println("Current size is "+size);
			SubsetsOfMultiset subsetsOfMultiset = new SubsetsOfMultiset( multiset, size, true );
			ElementOfMultiset[] subset = subsetsOfMultiset.getNextSubset();
			while(subset != null){
				//Put here the code that needs to deal with "subset", e.g., printing it as below:
				System.out.println( "the current subset is "+General.convertMultisetToString( subset )+
						" , and numOfInstancesOutsideSubset = "+Arrays.toString( subsetsOfMultiset.getNumOfInstancesOutsideSubset() ) );
				subset = subsetsOfMultiset.getNextSubset();
			}
		}
	}
	
	//*********************************************************************************************************
	
	/**
	 * Given a subset S of a multiset, this method returns the subset S' that is located just before S in the
	 * list of subsets of the same size. Here, the list is ordered lexicographically.
	 */
	public ElementOfMultiset[] getNextSubset()
	{
		if( currentSubsetIsFirstSubset )
		{
			setCurrentSubsetToFirstSubset();
			currentSubsetIsFirstSubset = false;
			return( prepareResult() );
		}
		else //If the required subset is not the first subset
		{
			int totalNumberOfElementsSeenSoFar = 0;
			int indexInLastSubset = lastSubset.length-1;
			for(int indexInCurrentSubset = numOfUniqueElementsInCurrentSubset-1; indexInCurrentSubset>=0; indexInCurrentSubset--)
			{	
				if( currentSubset[ indexInCurrentSubset ].element != lastSubset[ indexInLastSubset ].element )
				{
					//remove one copy from the CURRENT unique agent, and replace it with a copy of the NEXT unique agent
					if( currentSubset[ indexInCurrentSubset ].repetition > 1 )
					{
						currentSubset[ indexInCurrentSubset ].repetition--;
						currentSubset[ indexInCurrentSubset+1 ].element = currentSubset[ indexInCurrentSubset ].element+1;
						currentSubset[ indexInCurrentSubset+1 ].repetition = 1;
						numOfUniqueElementsInCurrentSubset++;
						fillRemainingAgents( totalNumberOfElementsSeenSoFar, indexInCurrentSubset+1 );
					}else{
						currentSubset[ indexInCurrentSubset ].element++;
						fillRemainingAgents( totalNumberOfElementsSeenSoFar, indexInCurrentSubset );
					}
					return( prepareResult() );
				}else{
					if( currentSubset[ indexInCurrentSubset ].repetition < lastSubset[ indexInLastSubset ].repetition )
					{
						totalNumberOfElementsSeenSoFar  += currentSubset[indexInCurrentSubset].repetition;
						numOfUniqueElementsInCurrentSubset--;
						indexInCurrentSubset--;
						
						//remove one copy from the CURRENT unique agent, and replace it with a copy of the NEXT unique agent
						if( currentSubset[ indexInCurrentSubset ].repetition > 1 )
						{
							currentSubset[ indexInCurrentSubset ].repetition--;
							currentSubset[ indexInCurrentSubset+1 ].element = currentSubset[ indexInCurrentSubset ].element+1;
							currentSubset[ indexInCurrentSubset+1 ].repetition = 1;
							numOfUniqueElementsInCurrentSubset++;
							fillRemainingAgents( totalNumberOfElementsSeenSoFar, indexInCurrentSubset+1 );
						}else{
							currentSubset[ indexInCurrentSubset ].element++;
							fillRemainingAgents( totalNumberOfElementsSeenSoFar, indexInCurrentSubset );
						}
						return( prepareResult() );
					}else{
						totalNumberOfElementsSeenSoFar  += currentSubset[indexInCurrentSubset].repetition;
						indexInLastSubset--;
						numOfUniqueElementsInCurrentSubset--;
					}
				}
			}
			return(null);
		}
	}
	
	//************************************************************************************************

	/**
	 * fills the remaining agents in a sequence, e.g., if we have 5 copies of agent 2, 5 copies of agent 3, and
	 * 3 copies of agent 4, and we want to fill 8 agents, then we put 5 copies of agent 2, and 3 copies of agent 3
	 */
	private void fillRemainingAgents( int totalNumOfAgentsToBeAdded, int indexAtWhichToStartFilling )
	{
		if( totalNumOfAgentsToBeAdded == 0 ){
			return;
		}
		int firstUniqueAgentToBeAdded = currentSubset[ indexAtWhichToStartFilling ].element;

		//deal with the first index at which we need to fill elements
		int max = multisetWithIncrementalElements[ firstUniqueAgentToBeAdded-1 ].repetition - currentSubset[ indexAtWhichToStartFilling ].repetition;
		if( max > 0 ){
			if( totalNumOfAgentsToBeAdded <= max ){
				currentSubset[ indexAtWhichToStartFilling ].repetition += totalNumOfAgentsToBeAdded;
				return;
			}else{
				currentSubset[ indexAtWhichToStartFilling ].repetition += max;
				totalNumOfAgentsToBeAdded -= max;
			}
		}
		//deal with the remaining indices at which we need to fill the remaining elements
		int k=1;
		do{
			numOfUniqueElementsInCurrentSubset++;
			if( totalNumOfAgentsToBeAdded <= multisetWithIncrementalElements[ firstUniqueAgentToBeAdded+k-1 ].repetition ){
				currentSubset[ k + indexAtWhichToStartFilling ] = new ElementOfMultiset( firstUniqueAgentToBeAdded + k, totalNumOfAgentsToBeAdded);
				break;
			}else{
				currentSubset[ k + indexAtWhichToStartFilling ] = new ElementOfMultiset( firstUniqueAgentToBeAdded + k, multisetWithIncrementalElements[ firstUniqueAgentToBeAdded+k-1 ].repetition);
				totalNumOfAgentsToBeAdded -= multisetWithIncrementalElements[ firstUniqueAgentToBeAdded+k-1 ].repetition;
				k++;
			}
		}
		while(true);
	}
	
	//************************************************************************************************

	/**
	 * If multiset = [2,2,2,5,5,6], then multisetWithIncrementalElements = [1,1,1,2,2,3]. We generate
	 * subsets of [1,1,1,2,2,3] and not [2,2,2,5,5,6]. Then, before returning each subset, we convert
	 * it to the original elements. For example, if subset=[1,2,2], then after calling "prepareResult"
	 * we have subsetWithOriginalElements = [2,5,5].
	 */
	private ElementOfMultiset[] prepareResult()
	{
		//Initialization
		ElementOfMultiset[] subsetWithOriginalElements = new ElementOfMultiset[ numOfUniqueElementsInCurrentSubset ];
		if( keepTrackOfNumOfInstancesOutsideSubset ){
			numOfInstancesOutsideSubset = new int[ numOfUniqueElementsInCurrentSubset ];
		}
		
		for(int i=0; i < numOfUniqueElementsInCurrentSubset; i++)
		{
			ElementOfMultiset originalElement = multiset[ currentSubset[i].element - 1 ]; 
			subsetWithOriginalElements[i] = new ElementOfMultiset( originalElement.element, currentSubset[i].repetition );
		
			if( keepTrackOfNumOfInstancesOutsideSubset ){
				numOfInstancesOutsideSubset[i] = originalElement.repetition - currentSubset[i].repetition;
			}
		}
		return( subsetWithOriginalElements );
	}
	
	//************************************************************************************************
	
	/**
	 * Returns the first subset in the list, e.g., for multisetWithIncrementalElements=[1,1,1,2,2,3],
	 * the first subset of size 4 is [1,1,1,2] 
	 */
	private void setCurrentSubsetToFirstSubset()
	{
		//Initialize the size of the first subset to the size of the multiset, because the
		//number of unique elements in first subset is AT MOST equal to the size of the multiset.
		currentSubset = new ElementOfMultiset[ multisetWithIncrementalElements.length ];
		for(int i=0; i<currentSubset.length; i++){
			currentSubset[i] = new ElementOfMultiset( 0 , 0 );
		}
		//Initialize "numOfInstancesOutsideSubset" if necessary
		if( keepTrackOfNumOfInstancesOutsideSubset )
			numOfInstancesOutsideSubset = new int[ multisetWithIncrementalElements.length ];

		int totalNumOfAgentsToBeAdded = sizeOfSubsets;
		int i=0;
		for(int j=0; j<multisetWithIncrementalElements.length; j++)
		{
			if( totalNumOfAgentsToBeAdded <= multisetWithIncrementalElements[j].repetition ){
				currentSubset[i] = new ElementOfMultiset( multisetWithIncrementalElements[j].element, totalNumOfAgentsToBeAdded);
				//Update "numOfInstancesOutsideSubset" if necessary
				if( keepTrackOfNumOfInstancesOutsideSubset ){
					numOfInstancesOutsideSubset[i] = multisetWithIncrementalElements[j].repetition - totalNumOfAgentsToBeAdded;
				}
				break;
			}else{
				currentSubset[i] = new ElementOfMultiset( multisetWithIncrementalElements[j].element, multisetWithIncrementalElements[j].repetition);
				totalNumOfAgentsToBeAdded -= multisetWithIncrementalElements[j].repetition;
				i++;
				//Update "numOfInstancesOutsideSubset" if necessary
				if( keepTrackOfNumOfInstancesOutsideSubset ){
					numOfInstancesOutsideSubset[i] = 0;
				}
			}
		}
		numOfUniqueElementsInCurrentSubset = i+1;
	}
	
	//************************************************************************************************
	
	/**
	 * Returns the first subset in the list, e.g., for multisetWithIncrementalElements=[1,1,1,2,2,3],
	 * the first subset of size 4 is [1,1,1,2] 
	 */
	private void setLastSubset()
	{
		//Initialize the size of the last subset to "sizeOfSubsets", because the
		//number of unique elements in last subset is AT MOST "sizeOfSubsets".
		ElementOfMultiset[] temp = new ElementOfMultiset[ multisetWithIncrementalElements.length ];
		for(int i=0; i<temp.length; i++)
			temp[i] = new ElementOfMultiset( 0 , 0 );
		
		int totalNumOfAgentsToBeAdded = sizeOfSubsets;
		int i=temp.length-1;
		for(int j=multisetWithIncrementalElements.length-1; j>=0; j--)
		{
			if( totalNumOfAgentsToBeAdded <= multisetWithIncrementalElements[j].repetition ){
				temp[i] = new ElementOfMultiset( multisetWithIncrementalElements[j].element, totalNumOfAgentsToBeAdded);
				break;
			}else{
				temp[i] = new ElementOfMultiset( multisetWithIncrementalElements[j].element, multisetWithIncrementalElements[j].repetition);
				totalNumOfAgentsToBeAdded -= multisetWithIncrementalElements[j].repetition;
				i--;
			}
		}
		//fill the elements of "temp" in "lastSubset"
		lastSubset = new ElementOfMultiset[ multisetWithIncrementalElements.length - i ];
		for(int j=lastSubset.length-1; j>=0; j--)
			lastSubset[j] = temp[ temp.length - lastSubset.length + j ];
	}
	
	//*********************************************************************************************************
	
	/**
	 * For every element in the current subset, this method returns the number of copies of
	 * that element that did not appear in the subset
	 * 
	 * For example, if multiset = [1,1,1,1,2,2,2,3,3,3,4,4,4], and the current subset is
	 * [1,1,3,3,4], then this method returns [2,1,2]. This is because the numbers of instances
	 * outside the current subset are: 2 for element "1", 1 for element "3", and 2 for element "4" 
	 */
	public int[] getNumOfInstancesOutsideSubset()
	{
		if( keepTrackOfNumOfInstancesOutsideSubset )
			return( numOfInstancesOutsideSubset );
		else{
			System.err.println("the method getNumOfInstancesOutsideSubset was called while the" +
					"parameter keepTrackOfNumOfInstancesOutsideSubset was set to false");
			return(null);
		}
	}
	
	//************************************************************************************************
	
	/**
	 * Returns a random subset of the multiset
	 * @param	multiset	the multiset from which we want to select the random subset
	 * @param	totalNumOfElementsInMultiset	the total number of elements (including repetition) in the multiset
	 * @param	sizeOfSubset	the size of the required random subset (including repetitions)
	 */
	public ElementOfMultiset[] getRandomSubset()
	{
		//deal with the case where the input is incorrect.
		if(( sizeOfSubsets <= 0 )||( sizeOfSubsets >= totalNumOfElementsInMultiset )){
			System.err.println("ERROR in method: getRandomSubset, the size of the desired subset must be between 1 and totalNumOfElements-1");
			return(null);
		}
		
		Random randomIndex = new Random();
		Random randomRepetition = new Random();
		
		//To improve efficiency, if the size of the required subset is greater than "multiset.length/2", then
		//we will generate a random subset of size "multiset.length - sizeOfSubset" instead of "sizeOfSubset"
		int sizeThatWeWillConsider;
		if( sizeOfSubsets <= totalNumOfElementsInMultiset/2 )
			sizeThatWeWillConsider = sizeOfSubsets;
		else
			sizeThatWeWillConsider = totalNumOfElementsInMultiset - sizeOfSubsets;
			
		//this array specifies the indices of unique elements that can still be added to the subset
		int[] indicesOfElementsThatCanBeAdded = new int[ multiset.length ];
		
		//the number of times each unique element is repeated in the subset
		int[] numOfRepetitionsInSubset = new int[ multiset.length ];
		
		//this specifies the maximum index that we can deal with in the array "indicesOfElementsThatCanBeAdded",
		//i.e., we will only deal with: "indicesOfElementsThatCanBeAdded[ i ]" for all i < maxIndex. 
		int maxIndex = multiset.length;
		
		//initialization		
		for(int i=multiset.length-1; i>=0; i--)
		{
			indicesOfElementsThatCanBeAdded[i] = i;
			numOfRepetitionsInSubset[i] = 0;
		}
		//create the random subset of size = "sizeThatWeWillConsider"
		int sizeOfSubsetSoFar=0;
		int numOfUniqueElementsInSubset=0;
		do{
			//choose a random unique element
			int i = randomIndex.nextInt( maxIndex );
			int j = indicesOfElementsThatCanBeAdded[ i ];
			
			//choose a random number of repetitions for the unique element (here, the random number will exclude zero)
			//int k = 1 + randomRepetition.nextInt( Math.min( sizeThatWeWillConsider - sizeOfSubsetSoFar , multiset[j].repetition - numOfRepetitionsInSubset[j] ) );
			int k = 1; /*To be Unbiased!*/

			//Check if this is a new unique element in the subset
			if( numOfRepetitionsInSubset[j] == 0 )
				numOfUniqueElementsInSubset++;
			
			//update the number of repetitions in the subset
			numOfRepetitionsInSubset[j] += k;
			
			//update the size of the subset so far
			sizeOfSubsetSoFar += k;
			
			//if the element cannot be added any more, get rid of it
			if( numOfRepetitionsInSubset[j] == multiset[j].repetition ){
				indicesOfElementsThatCanBeAdded[ i ] = indicesOfElementsThatCanBeAdded[ maxIndex-1 ];
				maxIndex--;
			}
		}
		while( sizeOfSubsetSoFar != sizeThatWeWillConsider );

		//Create the random subset based on "numOfRepetitionsInSubset"
		//
		//Note: As mentioned earlier, to improve efficiency, if the size of the required subset is
		//      greater than "multiset.length/2", then we would have generated a random subset of
		//      size: "multiset.length - sizeOfSubset" instead of "sizeOfSubset"
		//
		if( sizeOfSubsets <= totalNumOfElementsInMultiset/2 )
		{
			//Intialize "numOfInstancesOutsideSubset" if necessary
			if( keepTrackOfNumOfInstancesOutsideSubset )
				numOfInstancesOutsideSubset = new int[ numOfUniqueElementsInSubset ];

			ElementOfMultiset[] subset = new ElementOfMultiset[ numOfUniqueElementsInSubset ];
			int j=0;
			for(int i=0; i < multiset.length; i++)
			{
				if( numOfRepetitionsInSubset[i] > 0 )
				{
					subset[j] = new ElementOfMultiset( multiset[i].element, numOfRepetitionsInSubset[i]);

					//Update "numOfInstancesOutsideSubset" if necessary
					if( keepTrackOfNumOfInstancesOutsideSubset )
						numOfInstancesOutsideSubset[j] = multiset[i].repetition - numOfRepetitionsInSubset[i];
					
					j++;
					if( j == subset.length ){
						break;
					}
				}
			}
			return( subset );
		}else{
			//We initially allocate memory for a number of unique elements that is equal to "multiset.length".
			//This is because the number of unique elements in the subset is at most "multiset.length"
			ElementOfMultiset[] temp_subset = new ElementOfMultiset[ multiset.length ];
			int[] temp_numOfInstancesOutsideSubset = new int[ multiset.length ];
			int j=0;
			for(int i=0; i < multiset.length; i++)
			{
				if( numOfRepetitionsInSubset[i] < multiset[i].repetition ){
					temp_subset[j] = new ElementOfMultiset( multiset[i].element, multiset[i].repetition - numOfRepetitionsInSubset[i]);
					
					//Update "numOfInstancesOutsideSubset" if necessary
					if( keepTrackOfNumOfInstancesOutsideSubset )
						temp_numOfInstancesOutsideSubset[j] = numOfRepetitionsInSubset[i];

					j++;
				}
			}
			//truncate "temp_subset" and "temp_numOfInstanceOutsideSubset" to the desired length
			ElementOfMultiset[] subset = new ElementOfMultiset[ j ];
			numOfInstancesOutsideSubset = new int[ j ];
			for(int i=0; i<j; i++){
				subset[i] = temp_subset[i];
				numOfInstancesOutsideSubset[i] = temp_numOfInstancesOutsideSubset[i];
			}
			return( subset );
		}
	}
}