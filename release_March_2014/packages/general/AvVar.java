package general;

import java.lang.Math;
import java.util.*;

/**
 * A class for tracking the average, variance and standard deviation of a sequence of numbers.
 */

/* ==================================
 *  General tips on how to use AvVar
 * ==================================
 * 
 * You will have to change the package to yours.
 * 
 * Although it looks somewhat complicated, it is very easy really. To use, simply create an object e.g. like:
 * AvVar myData = new AvVar()
 * 
 * Every time you have a new data point (has to be a double value), you simply use the add() method.
 * For instance, myData.add(data)
 * 
 * Whenever you want to know the standard dev or variance, just invoke myData.stddev() and myData.variance() respectively.
 * 
 * That's it. It uses very little memory.
 * 
 * ===============================================================================
 *  How to compute the mean, standard error of the mean, and confidence intervals
 * ===============================================================================
 * 
 * (1) The mean (i.e. average) can be computed as follows:  average()
 *  
 * (2) The standard error of the mean is the standard deviation devided by the square
 *     root of the number of values.
 * 
 *     This can be computed as follows:  stddev() / Math.sqrt( num() )
 *  
 * (3) The 95% confidence intervals are computed by adding/subtracting the standard
 *     error of the mean, multiplied by 1.96
 *     The 99% confidence intervals are computed by adding/subtracting the standard
 *     error of the mean, multiplied by 2.58
 * 
 *     This can be computed as follows:  1.96 * (stddev() / Math.sqrt( num() ))
 *                                       2.58 * (stddev() / Math.sqrt( num() ))  
 */
public class AvVar implements Cloneable {

	public boolean checkValidity = true;

	/**
	 * Counts the number of double values in the added sequence
	 * @see chm.math.AvVar#add
	 */
	private long num;
	/**
	 * This variable contain the sum of the added values
	 * @see chm.math.AvVar#add
	 */
	private double av;
	/**
	 * This variable contain the sum of the squares of the added values
	 * @see chm.math.AvVar#add
	 */
	private double sav;
	/**
	 * These variable are used to track the minimal and maximal value
	 * observed in the sequence of added values
	 * @see chm.math.AvVar#add
	 */
	private double min, max;
	private long indexMin, indexMax;
	private boolean doMedian = false;
	ArrayList values = null;
		/*
		return  new String("Aver. = ").concat(
			new String().valueOf(average())).concat(
			new String(" Var. = ")). concat(
			new String().valueOf(variance())); };
		 */
public AvVar() {
	init();
	return;
}
		/*
		return  new String("Aver. = ").concat(
			new String().valueOf(average())).concat(
			new String(" Var. = ")). concat(
			new String().valueOf(variance())); };
		 */
public AvVar(boolean doMedian) {
	init();
	this.doMedian = doMedian;
	if (doMedian)
		values = new ArrayList();
	return;
}
/**
 * adds a double to the sequence
 * @param d  the double value to be added
 */
public void add(double d) {
	num++;
	av += d;
	sav += d*d;
	if (d<min) {
		min = d;
		indexMin = num;
	}
	if (d>max) {
		max = d;
		indexMax = num;
	}

	if (doMedian)
		values.add(new Double(d));
}
/**
 * Calculates the average of the current sequence
 * @return the average
 */
public double average() {
	if (num==0)
		return 0;
	else
		return av/num;
}
/**
 * Clone an <code>AvVar</code> object.
 */
public Object clone() {
	AvVar clone = null;
	try {
		clone = (AvVar) super.clone();
	} catch (CloneNotSupportedException e) {
		System.err.println("CloneNotSupportedException in AvVar.clone: " + e.getMessage());
		System.exit(-1);
	}
	return clone;
}
/**
 * First occurence of the current maximal value (first index)
 */
public long indexMax() {
	return indexMax;
}
/**
 * First occurence of the current minimal value (first index)
 */
public long indexMin() {
	return indexMin;
}
/**
 * Initialize an already created AvVar.
 */
public void init() {
	num = 0;
	av = 0.0;
	sav = 0.0;
	min = Double.MAX_VALUE;
	max = -Double.MAX_VALUE; //MIN_VALUE is minimal positive val
}
/**
 * Determines the maximal value in the current sequence
 * @return the maximal value
 */
public double max() {
	return max;
}
/**
 * Insert the method's description here.
 * Creation date: (4/9/03 16:41:11)
 */
public double median() {
	if (num==0 || !doMedian)
		return 0;
		
	Object[] dvalues = values.toArray();
	Arrays.sort(dvalues);
 	if (num % 2 == 0) // even
 		return (((Double)dvalues[(int)(num/2-1)]).doubleValue()+((Double)dvalues[(int)(num/2)]).doubleValue())/2.0;
 	else // odd
 		return ((Double)dvalues[(int)(num/2)]).doubleValue();
	
}
/**
 * Determines the minimal value in the current sequence
 * @return the minimal value
 */
public double min() {
	return min;
}
/**
 * Determines the number of elements in the current sequence
 * @return the number of elements
 */
public long num() {
	return num;
}
/**
 * Calculates the standard deviation of the current sequence
 * @return the variance
 */
public double stddev() {
	double var = variance();
	double stddev = var;

	if ((var >= 0.0) || (checkValidity)) {
		stddev = Math.sqrt(var);

		if (Double.isNaN(stddev)) {
//			Assert.test(Math.abs(var) < 1e-10, " invalid variance: " + var +
//							   "  num=" + num + "  aver=" + av);
			stddev = 0.0;
		}
	}
	return stddev;
}
/**
 * Returns a string containing the average and the standard deviation
 * of the current sequence
 * @return  a string-representation
 */
public String toString() {
	return "Aver="+average()+" std.="+stddev();
}
/**
 * Calculates the variance of the current sequence
 * @return the variance
 */
public double variance() {
	double res = 0;
	if (num != 0) {
		double d = av/(double)num;
		res = sav/(double)num - d*d;
	}
	return res;
}
}
