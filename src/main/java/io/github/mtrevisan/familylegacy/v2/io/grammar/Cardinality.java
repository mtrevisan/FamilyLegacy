package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.regex.Pattern;


/**
 * Represents the cardinality of a tag or structure occurrence.
 * Format: {min:max} where min and max are integers, or "M" for unlimited.
 * Examples: {1:1}, {0:M}, {1:M}, {0:1}
 */
public final class Cardinality{

	private static final Pattern PATTERN = Pattern.compile("\\{(\\d+):(\\d+|M)\\}");

	private final int min;
	private final int max; // Integer.MAX_VALUE for unlimited

	private Cardinality(int min, int max){
		if(min < 0) throw new IllegalArgumentException("min cannot be negative");
		if(max < min && max != Integer.MAX_VALUE) throw new IllegalArgumentException("max must be >= min");
		this.min = min;
		this.max = max;
	}

	/**
	 * Parses a cardinality string like "{1:1}" or "{0:M}".
	 *
	 * @param s the string to parse
	 * @return the Cardinality object
	 * @throws IllegalArgumentException if the string is malformed
	 */
	public static Cardinality parse(String s){
		if(s == null || s.isEmpty()){
			throw new IllegalArgumentException("Cardinality string cannot be null or empty");
		}
		var matcher = PATTERN.matcher(s.trim());
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid cardinality format: " + s);
		}
		int min = Integer.parseInt(matcher.group(1));
		String maxStr = matcher.group(2);
		int max = "M".equals(maxStr)? Integer.MAX_VALUE: Integer.parseInt(maxStr);
		return new Cardinality(min, max);
	}

	public int getMin(){
		return min;
	}

	public int getMax(){
		return max;
	}

	public boolean isUnlimited(){
		return max == Integer.MAX_VALUE;
	}

	/**
	 * Checks if a given count satisfies this cardinality.
	 */
	public boolean isValidCount(int count){
		return count >= min && count <= max;
	}

	@Override
	public String toString(){
		return "{" + min + ":" + (max == Integer.MAX_VALUE? "M": max) + "}";
	}

}
