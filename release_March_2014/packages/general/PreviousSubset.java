package general;

public class PreviousSubset {
	
	int h;
	int m;
	final int n;
	final int k;
	public PreviousSubset(int n, int k){
		this.h=k;
		this.m=0;
		this.n=n;
		this.k=k;
	}
	public void getPreviousCombination(int[] a)
	{
		//See page 26 of: http://www.math.upenn.edu/~wilf/website/CombinatorialAlgorithms.pdf	
		if( m < n-h)
			h=1;
		else
			h++;
		m = a[k-h];

		int x=k-h-1;
		
		for(int j=1; j<=h; j++)
			a[x+j]=m+j;
	}
	//I TESTED THIS METHOD USING THE FOLLOWING CODE, BUT IT WAS SLOWER THAN MINE
	/*
	int n=5;
	for(int k=2; k<=n-1; k++)
	{
		int numOfCoalitionsOfCurSize = (int)Combinations.binomialCoefficient(n, k);
		PreviousSubset previousSubset = new PreviousSubset(n,k);

		long time1=System.currentTimeMillis();
		int[] curCombinationInByteFormat1 = Combinations.getCombinationAtGivenIndex(k, numOfCoalitionsOfCurSize-1, n);
		for(int i=1; i<numOfCoalitionsOfCurSize; i++){
			previousSubset.getPreviousCombination(curCombinationInByteFormat1);
			//System.out.println(General.convertArrayToString(curCombinationInByteFormat1));
		}
		time1 = System.currentTimeMillis()-time1;

		long time2=System.currentTimeMillis();
		int[] curCombinationInByteFormat2 = Combinations.getCombinationAtGivenIndex(k, numOfCoalitionsOfCurSize-1, n);
		for(int i=1; i<numOfCoalitionsOfCurSize; i++){
			Combinations.getPreviousCombination(n, k, curCombinationInByteFormat2);
			//System.out.println(General.convertArrayToString(curCombinationInByteFormat2));
		}
		time2 = System.currentTimeMillis()-time2;
		
		System.out.println("size = "+k+"   saving = "+(time2-time1));
	}
	*/
}
