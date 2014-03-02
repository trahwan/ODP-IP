package general;

public class IntegerPartition
{
	public int[] partsSortedAscendingly;
	public int[] sortedMultiplicity;
	public int[] sortedUnderlyingSet;
	public int   sortedUnderlyingSetInBitFormat;
	public int   numOfAgents;
	public ElementOfMultiset[] sortedMultiset;
	public int[] tempIntegersThatResultedFromASplit; //I use it occationally to store the two integers that were the result of a split

	//*****************************************************************************************************

	/**
	 * Constructor
	 */
	public IntegerPartition( int[] parts )
	{
		numOfAgents=0;
		for(int i=0; i<parts.length; i++)
			numOfAgents += parts[i];
		partsSortedAscendingly = General.sortArray( parts , true );
		sortedUnderlyingSet = General.getUnderlyingSet( partsSortedAscendingly );
		sortedMultiplicity = new int[ sortedUnderlyingSet.length ];
		int indexInMultiplicity = 0;
		sortedMultiplicity[ indexInMultiplicity ]=1;
		for(int i=1; i<partsSortedAscendingly.length; i++){
			if( partsSortedAscendingly[i] == partsSortedAscendingly[i-1] )
				sortedMultiplicity[ indexInMultiplicity ]++;
			else{
				indexInMultiplicity++;
				sortedMultiplicity[ indexInMultiplicity ]=1;
			}					
		}
		sortedUnderlyingSetInBitFormat = Combinations.convertCombinationFromByteToBitFormat( sortedUnderlyingSet );
		
		//The integer partition represented concisely as a multiset
		sortedMultiset = new ElementOfMultiset[ sortedMultiplicity.length ];
		for(int i=0; i<sortedMultiset.length; i++){
			sortedMultiset[i] = new ElementOfMultiset( sortedUnderlyingSet[i], sortedMultiplicity[i]);
		}
	}

	//*****************************************************************************************************

	/**
	 * Returns the NUMBER of Integer partitions that are directly connected to this one
	 * (which are all in the above level of the integer partition graph).
	 */
	public int getNumOfDirectedlyConnectedIntegerPartitions( int largestIntegerBeingSplit, int prev_largestIntegerBeingSplit )
	{
		//Check the special case where the node is the bottom one in the graph...
		if( sortedUnderlyingSet[0] == numOfAgents)
			return( (int)Math.floor( numOfAgents / (double)2) );
			
		int counter=0;
		for(int i=0; i< sortedUnderlyingSet.length; i++)
		{
			if(( sortedUnderlyingSet[i] > largestIntegerBeingSplit )||( sortedUnderlyingSet[i] <= prev_largestIntegerBeingSplit )){
				continue;
			}
			//for every possible way of splitting "underlyingSet[i]" into two integers
			for(int j=1; j<=(int)Math.floor( sortedUnderlyingSet[i]/(double)2 ); j++)
			{
				int smallHalf = (int)j;
				int largeHalf = (int)(sortedUnderlyingSet[i] - j);
				if( largeHalf > numOfAgents-smallHalf-largeHalf ){
					continue;
				}
				counter++;
			}
		}
		return(counter);
	}

	//*****************************************************************************************************

	/**
	 * Returns the LIST of Integer partitions that are directly connected to this one
	 * (which are all in the above level of the integer partition graph).
	 */
	public IntegerPartition[] getListOfDirectedlyConnectedIntegerPartitions( int largestIntegerBeingSplit, int prev_largestIntegerBeingSplit )
	{
		//Allocate memory for the list of directly connected integer partitions
		int counter = getNumOfDirectedlyConnectedIntegerPartitions( largestIntegerBeingSplit, prev_largestIntegerBeingSplit );
		if( counter == 0 ){
			return( null );
		}
		IntegerPartition[] listOfDirectlyConnectedIntegerPartitions = new IntegerPartition[ counter ];

		//Check the special case where the node is the bottom one in the graph
		if( sortedUnderlyingSet[0] == numOfAgents )
		{
			int index=0;
			for(int i=1; i<= (int)Math.floor( numOfAgents/(double)2); i++)
			{
				int[] newSortedParts = new int[2];
				newSortedParts[0] = i;
				newSortedParts[1] = numOfAgents-i;				
				listOfDirectlyConnectedIntegerPartitions[index] = new IntegerPartition( newSortedParts );
				listOfDirectlyConnectedIntegerPartitions[index].tempIntegersThatResultedFromASplit = new int[]{ i, numOfAgents-i };
				index++;
			}
		}
		else{
			//Compute the list of directly connected integer partitions
			int index=0;
			for(int i=0; i< sortedUnderlyingSet.length; i++)
			{
				final int curPart = sortedUnderlyingSet[i];
				if(( curPart > largestIntegerBeingSplit )||( curPart <= prev_largestIntegerBeingSplit )){
					continue;
				}
				//for every possible way of splitting "curPart" into two integers
				for(int j=1; j<=(int)Math.floor( curPart/(double)2 ); j++)
				{
					int smallHalf = (int)j;
					int largeHalf = (int)(curPart - j);
					if( largeHalf > numOfAgents-smallHalf-largeHalf ){
						continue;
					}
					int[] newSortedParts = new int[ partsSortedAscendingly.length + 1 ];
					int i1=0;
					int i2=0;
					while( partsSortedAscendingly[i1] < smallHalf ){
						newSortedParts[i2] = partsSortedAscendingly[i1];
						i1++;
						i2++;
					}
					newSortedParts[i2] = smallHalf;
					i2++;
					while( partsSortedAscendingly[i1] < largeHalf ){
						newSortedParts[i2] = partsSortedAscendingly[i1];
						i1++;
						i2++;
					}
					newSortedParts[i2] = largeHalf;
					i2++;
					boolean curPartHasBeenSeen = false;
					while( i1 < partsSortedAscendingly.length ){
						if(( partsSortedAscendingly[i1] == curPart )&&( curPartHasBeenSeen == false )){
							curPartHasBeenSeen = true;
							i1++;
						}else{
							newSortedParts[i2] = partsSortedAscendingly[i1];
							i1++;
							i2++;
						}
					}
					listOfDirectlyConnectedIntegerPartitions[index] = new IntegerPartition( newSortedParts );
					listOfDirectlyConnectedIntegerPartitions[index].tempIntegersThatResultedFromASplit = new int[]{ smallHalf, largeHalf };
					index++;
				}
			}
		}
		return( listOfDirectlyConnectedIntegerPartitions );
	}
	
	//*****************************************************************************************************

	/**
	 * This method returns the number of integer partitions of "n"
	 */
	public static int getNumOfIntegerPartitions( int n )
	{
		int numOfIntegerPartitions = 0;
		for(int level=1; level<=n; level++)
		{
			numOfIntegerPartitions +=  IntegerPartition.getNumOfIntegerPartitionsInLevel( n, level );
		}
		return( numOfIntegerPartitions );
	}
	
	//******************************************************************************************************
	
	/**
	 * This method returns the number of integer partitions of "n" that are in level = "level"
	 * of the integer partition graph (i.e. those of size = "level")
	 */
	public static int getNumOfIntegerPartitionsInLevel( int n, int level)
	{
		return( getNumOfIntegerPartitionsInLevel_additionalParameter( n, level, (int)(n-level+1) ));
	}
	private static int getNumOfIntegerPartitionsInLevel_additionalParameter( int n, int level, int M )
	{
		if(( level==1 )||( level==n ))
		{
			return(1);
		}		
		int sum=0;
		for(int M1=(int)Math.ceil( n/(double)level ); M1<=Math.min(n-level+1,M); M1++)
		{
			sum += getNumOfIntegerPartitionsInLevel_additionalParameter( (int)(n-M1), (int)(level-1), M1 );
		}
		return(sum);
	}

	//******************************************************************************************************	
	
	/**
	 * This method returns the number of integer partitions of "n" that are in level = "level" of
	 * of the integer partition graph (i.e. those of size = "level"), where the smallest part in
	 * each partition is smaller than, or equal to, "smallestPart".
	 */
	public static int getNumOfIntegerPartitionsInLevel( int n, int level, int smallestPart )
	{
		return( getNumOfIntegerPartitionsInLevel_additionalParameter( n, level, smallestPart, (int)(n-level+1) ));
	}
	private static int getNumOfIntegerPartitionsInLevel_additionalParameter( int n,int level,int smallestPart,int M )
	{
		if( smallestPart==1 ) {
			return( getNumOfIntegerPartitionsInLevel( n, level ) );
		}
		else {			
			if(( n<smallestPart )||( level==n )){
				return(0);
			}
			if( level==1 ) {
				return(1);
			}		
			int sum=0;
			for(int M1=(int)Math.ceil( n/(double)level ); M1<=Math.min(n-level+1,M); M1++)
			{
				sum += getNumOfIntegerPartitionsInLevel_additionalParameter((int)(n-M1),(int)(level-1),smallestPart,M1);
			}
			return(sum);
		}
	}


	//******************************************************************************************************	

	/**
	 * This method generates the integer partitions of n; it uses the algorithm SZ1. For more details, see: 
	 * "Fast algorithms for generating integer partitions", by Antoine Zoghbi and Ivan Stojmenovic, 1998
	 */
    public static int[][] getIntegerPartitionsOrderedLexicographically( int n, boolean orderIntegerPartitionsAscendingly )
    {
    	int index=0;
    	
    	//Memory allocation
    	int[][] listOfIntegerPartitions = new int[ IntegerPartition.getNumOfIntegerPartitions(n) ][];
    	
    	//Initialize "indexOfNewPartition". Here, "indexOfNewPartition[i]" represents the
    	//index at which any new partition with "i+1" parts must be added to integerPartitions[i].
    	int[] indexOfNewPartition = new int[n];
    	for(int i=0; i<n; i++)
    		indexOfNewPartition[i]=0;
    	
    	//We will set x to one integer partition, and then to another, and so on, until we
    	//are done with all integer partitions.
    	//NOTE: I have made x contain n+1 element. This way, if we ignore the first element
    	//      (i.e. x[0]) then the first element in x becomes x[1] just as in the paper.
    	//
    	int[] x = new int[n+1];
    	
    	//Setting x to the integer partition containing n parts, all of which have a value of 1.
    	for(int i=1; i<=n; i++) x[i]=1;

    	//Initialization
    	x[1]=n; int m=1; int h=1;

    	//put the first integer partition in the list.
    	listOfIntegerPartitions[ index ] = new int[x.length];
    	for(int i=0; i<x.length; i++)
    		listOfIntegerPartitions[ index ][i] = x[i];
    	index++;
    	
    	//Setting x to the other integer partitions
    	while( x[1]!=1 )
    	{
    		if( x[h]==2 )
    		{
    			m=m+1; x[h]=1; h=h-1;     			
    		}
    		else
    		{
    			int r=x[h]-1; int t=m-h+1; x[h]=r;
    			while( t>=r ){ h=h+1; x[h]=r; t=t-r; }
    			if( t==0 ){ m=h; }
    			else
    			{ 
    				m=h+1;
    				if( t>1 ){ h=h+1; x[h]=t; }
    			}
    		}
        	//put the current integer partition in the list.
    		listOfIntegerPartitions[ index ] = new int[x.length];
    		for(int i=0; i<x.length; i++)
    			listOfIntegerPartitions[ index ][i] = x[i];
        	index++;   	
    	}
    	return( listOfIntegerPartitions );
    }
	
	//******************************************************************************************************	

	/**
	 * This method generates the integer partitions of n; it uses the algorithm SZ1. For more details, see: 
	 * "Fast algorithms for generating integer partitions", by Antoine Zoghbi and Ivan Stojmenovic, 1998
	 */
    public static int[][][] getIntegerPartitions( int n, boolean orderIntegerPartitionsAscendingly )
    {
    	//Memory allocation
    	int[][][] integerPartitions = allocateMemoryForIntegerPartitions(n);
    	
    	//Initialize "indexOfNewPartition". Here, "indexOfNewPartition[i]" represents the
    	//index at which any new partition with "i+1" parts must be added to integerPartitions[i].
    	int[] indexOfNewPartition = new int[n];
    	for(int i=0; i<n; i++)
    		indexOfNewPartition[i]=0;
    	
    	//We will set x to one integer partition, and then to another, and so on, until we
    	//are done with all integer partitions.
    	//NOTE: I have made x contain n+1 element. This way, if we ignore the first element
    	//      (i.e. x[0]) then the first element in x becomes x[1] just as in the paper.
    	//
    	int[] x = new int[n+1];
    	
    	//Setting x to the integer partition containing n parts, all of which have a value of 1.
    	for(int i=1; i<=n; i++) x[i]=1;

    	//Initialization
    	x[1]=n; int m=1; int h=1; fill_x_in_partitions( x, integerPartitions, m, orderIntegerPartitionsAscendingly, indexOfNewPartition);
    	
    	//Setting x to the other integer partitions
    	while( x[1]!=1 )
    	{
    		if( x[h]==2 )
    		{
    			m=m+1; x[h]=1; h=h-1;     			
    		}
    		else
    		{
    			int r=x[h]-1; int t=m-h+1; x[h]=r;
    			while( t>=r ){ h=h+1; x[h]=r; t=t-r; }
    			if( t==0 ){ m=h; }
    			else
    			{ 
    				m=h+1;
    				if( t>1 ){ h=h+1; x[h]=t; }
    			}
    		}
    		//This method fills x in the array "integerPartitions"
    		fill_x_in_partitions( x, integerPartitions, m, orderIntegerPartitionsAscendingly, indexOfNewPartition );   	
    	}
    	return( integerPartitions );
    }

    //******************************************************************************************************

	/**
	 * This method generates all integer partitions of m in which every part is greater than, or equal to, l1.
	 * I also adds these partitions to the relevant layers in the array "prevPartitions" (based on the number
	 * of parts in each partition).
	 * 
	 * This method uses the algorithm parta. For more details, see; "Algorithm 29 Efficient Algorithms for Double
	 * and Multiply Restricted Partitions", by W. Riha and K. R. James, 1974.
	 * 
	 * Note the following differences between my code and the code of the original algorithm "parta"
	 * 
	 *    * I set parameter "z" to 0 (to avoid generating any partition that contains repeated parts, set z to 1).
	 *    * I removed "num" since I won't be using it.    
	 *    * I added the parameter "carryOn" to avoid the use of "goto"
	 *    * I replaced "proc" (which prints x) with "fill_x_in_partitions" (which adds x to "prevPartitions").     
	 */	
    public static int[][][] getRestrictedIntegerPartitions( int m,int l1,boolean ascending )
    {
    	//Memory allocation...
    	int[][][] integerPartitions = allocateMemoryForIntegerPartitions( (int)m, (int)l1 );
    	
    	//Initialize "indexOfNewPartition". Here, "indexOfNewPartition[i]" represents the
    	//index at which any new partition with "i+1" parts must be added to integerPartitionss[i].
    	int[] indexOfNewPartition = new int[m];
    	for(int i=0; i<integerPartitions.length; i++)
    		indexOfNewPartition[i]=0;
    	
    	//Store the original value of m (this is because "parta" changes the value of "m" while running)
    	int original_m = m;
   	
    	integerPartitions[0][0][0] = (int)m;
    	for(int level=2; level<=integerPartitions.length; level++)
    	{
    		m = original_m; //Set "m" to its original value.
    		int n=level; //Set the number of parts.
    		int l2 = m; //Set the maximum value that a part in this level can get.
    		int z=0;
    		
    		//********************************************
    		//***                                      ***
    		//***         The "parta" algorithm        ***
    		//***                                      ***
    		//********************************************

    		int i,j; int[] x=new int[n+1]; int[] y=new int[n+1];
    		j=z*n*(n-1); m=m-n*l1-j/2; l2=l2-l1;
    		if(( m>=0 )&( m<=n*l2-j ))
    		{
    			for(i=1; i<=n; i++)
    			{
    				x[i]=l1+z*(n-i); y[i]=l1+z*(n-i);
    			}
    			i=1; l2=l2-z*(n-1);

    			boolean carryOn=true;
    			while( carryOn )
    			{
    				carryOn=false;
    				while( m>l2 )
    				{
    					m=m-l2; x[i]=y[i]+l2;
    					i=i+1;
    				}
    				x[i]=y[i]+m; fill_x_in_partitions( x, integerPartitions, n, ascending, indexOfNewPartition );
    				if(( i<n )&( m>1 ))
    				{
    					m=1; x[i]=x[i]-1;
    					i=i+1; x[i]=y[i]+1;
    					fill_x_in_partitions( x, integerPartitions, n, ascending, indexOfNewPartition );
    				}
    				for( j=i-1; j>=1; j-- )
    				{
    					l2=x[j]-y[j]-1; m=m+1;
    					if( m<=(n-j)*l2 )
    					{
    						x[j]=y[j]+l2;
    						carryOn=true; break;
    					}
    					m=m+l2; x[i]=y[i]; i=j;    				
    				}
    			}    		
    		}
    	}
    	return( integerPartitions );
    }
    
    //******************************************************************************************************
    
	/**
	 * This is only used in the method "getIntegerPartitions".
	 * It allocates the memory required for the array of integer partitions.
	 */
    private static int[][][] allocateMemoryForIntegerPartitions( int n )
	{
		//Count the number of integerPartitionss in each level
    	int[] numOfIntegerPartitionsInLevel = new int[n];
		for(int level=1; level<=n; level++)
			numOfIntegerPartitionsInLevel[level-1] = getNumOfIntegerPartitionsInLevel( n, level );
		
    	int[][][] integerPartitions = new int[n][][];		
		for(int level=1; level<=n; level++)
		{
			integerPartitions[level-1] = new int[ numOfIntegerPartitionsInLevel[level-1] ][];
			for(int i=0; i<numOfIntegerPartitionsInLevel[level-1]; i++)
			{
				integerPartitions[level-1][i] = new int[ level ];    			
			}
		}
		return( integerPartitions );
	}
    
	//******************************************************************************************************

	/**
	 * This is only used in the method "generateRestrictedIntegerPartitions".
	 * It allocates the memory required for the array of integer partitions
	 * of "n" in which the parts are greater than, or equal to, "smallestPart".
	 */
    private static int[][][] allocateMemoryForIntegerPartitions( int n, int smallestPart )
	{
		//Count the number of integer partitions in each level, as well as the number of non-empty levels.
    	int numOfNonEmptyLevels=0;
    	int[] numOfIntegerPartitions = new int[n];
		for(int level=1; level<=n; level++)
		{
			numOfIntegerPartitions[level-1] = getNumOfIntegerPartitionsInLevel( n, level, smallestPart );
			if( numOfIntegerPartitions[level-1]>0 )
			{
				numOfNonEmptyLevels++;
			}
		}
    		
		int[][][] integerPartitions = new int[numOfNonEmptyLevels][][];		
		for(int level=1; level<=numOfNonEmptyLevels; level++)
		{
			integerPartitions[level-1] = new int[ numOfIntegerPartitions[level-1] ][];
			for(int i=0; i<numOfIntegerPartitions[level-1]; i++)
			{
				integerPartitions[level-1][i] = new int[ level ];    			
			}
		}
		return( integerPartitions );
	}
    
	//******************************************************************************************************
    
    /**
     * This is only used in the methods: "getIntegerPartitions" (which uses the SZ1 algorithm) and
     * the method: "generateRestrictedIntegerPartitions" (which uses the parta algorithm).
     * 
     * It fills x (i.e. the current integer partitoin) in the array "integerPartitions".
     */
    private static void fill_x_in_partitions( int x[], int[][][] integerPartitions, int m, boolean ascending, int[] indexOfNewPartition )
    {
    	//Note that the integer partition in x is ordered in a descending order. However, we
    	//might want to fill in "integerPartitions" in an ascending order.
    	if( ascending==false )
    	{
    		for(int i=1; i<=m; i++)
    			integerPartitions[ m-1 ][ indexOfNewPartition[m-1] ][ i-1 ] = (int)x[i];
    		indexOfNewPartition[m-1]++;
    	}
    	else //fill x in "integerPartitions" such that the elements in "integerPartitions" are ordered ascendingly
    	{
    		for(int i=1; i<=m; i++)
    			integerPartitions[ m-1 ][ indexOfNewPartition[m-1] ][ i-1 ] = (int)x[m-i+1];
    		indexOfNewPartition[m-1]++;    		
    	}
    }
    
    //******************************************************************************************************
    
    /**
     * This method sorts the integer partitions lexicographically.
     * For example, [3,3,2,2,1], [3,3,2,1,1,1], [3,3,3,2], [4,1] will be sorted as follows:
     *    [4,1]
     *    [3,3,3,2]
     *    [3,3,2,2,1]
     *    [3,3,2,1,1,1]
     * Also: [1,2,2,3,3], [1,1,1,2,3,3], [2,3,3,3], [1,4] will be sorted as follows:
     *    [2,3,3,3]
     *    [1,4]
     *    [1,2,2,3,3]
     *    [1,1,1,2,3,3]
     * @return list of indices of the new order. In example 1, it returns [2,3,1,0]. In example 2, it returns [2,3,0,1]
     */
    public static int[] sortListOfIntegerPartitionsLexicographically( int[][] listOfIntegerPartitions, boolean eachIntegerPartitionIsOrderedAscendingly )
    {
    	int[] sortedIndices = new int[ listOfIntegerPartitions.length ];
    	
    	return( sortedIndices );
    }
    
    //******************************************************************************************************
    
    /**
     * returns true if the integer partition contains the given multiset. Otherwise, it returns false
     */
    public boolean contains( ElementOfMultiset[] multiset )
    {
    	if( sortedMultiset.length < multiset.length ) return( false );
    	
    	for( int i=0; i<multiset.length; i++ ){
    		boolean found = false;
    		for( int j=0; j<this.sortedMultiset.length; j++ ){
    			if( this.sortedMultiset[j].element == multiset[i].element ){
    				if( this.sortedMultiset[j].repetition < multiset[i].repetition ){
    					return( false );
    				}
    				found = true;
    				break;
    			}
    		}
    		if( found == false ){
    			return( false );
    		}
    	}
   		return(true);
    }
}