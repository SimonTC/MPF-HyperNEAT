package com.ojcoleman.ahni.util;

public class SuperPoint extends Point {
	private double[] coordinates;

	public SuperPoint() {
	}
	
	/**
	 * Creates a super point with the given coordinates. The three first values has to be x,y,z coordinates.
	 * @param coordinates
	 */
	public SuperPoint(double[] coordinates) {
		super(coordinates[0], coordinates[1], coordinates[2]);
		this.coordinates = coordinates;
		
	}

	/**
	 * @return true iff this Point and the given Point have exactly the same coordinates.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof SuperPoint) {
			SuperPoint p = (SuperPoint) o;
			double[] otherCoordinates = p.getCoordinates();
			for (int i = 0; i < coordinates.length; i++){
				if (coordinates[i] != otherCoordinates[i]) return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Calculates a hash code based on the sum of this points coordinates. The coordinates are assumed to lie in the
	 * range [-1, 1].
	 */
	@Override
	public int hashCode() {
		double sum = 0;
		for (double d : coordinates) sum += d;
		return (int) (sum * (Integer.MAX_VALUE / coordinates.length));
	}

	@Override
	public String toString() {
		String s = "(";
		for (double d : coordinates) s += (float) d + ",";
		s = s.substring(0, s.length()-1);
		s+= ")";
		return s;
	}
	
	public void setCoordinates(double[] coordinates) { 
		this.coordinates = coordinates;
	}
	
	public double[] getCoordinates(){
		return coordinates;
	}
}