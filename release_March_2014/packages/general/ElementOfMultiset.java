package general;

public class ElementOfMultiset
{
	public int element;
	
	public int repetition; //the number of times the element is repeated in the multiset
	
	/**
	 * the constructor
	 */
	public ElementOfMultiset( int element, int repetition )
	{
		this.element = element;
		this.repetition = repetition;
	}
}