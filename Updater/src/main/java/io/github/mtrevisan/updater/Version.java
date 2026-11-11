/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.updater;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * A version number as it appears in the pom and in the tag of a release: {@code 1.2.3}, {@code v1.2.3}, {@code 1.2.3-rc1}.
 * <p>
 * The numbers are compared one by one (a missing one is zero); a version with a qualifier introduced by a hyphen
 * ({@code -rc1}, {@code -SNAPSHOT}) comes before the same version without it. Anything after a space, such as the
 * {@code (development)} of the old "2.0.0 (development)", is ignored.
 * </p>
 */
public final class Version implements Comparable<Version>{

	private final List<Integer> numbers;
	private final String qualifier;


	private Version(final List<Integer> numbers, final String qualifier){
		this.numbers = numbers;
		this.qualifier = qualifier;
	}

	/**
	 * @param text	The text of the version, with or without a leading {@code v}.
	 * @return	The version, or empty if the text does not start with a number.
	 */
	public static Optional<Version> parse(final String text){
		if(text == null)
			return Optional.empty();

		final String trimmed = text.strip();
		int index = (!trimmed.isEmpty() && (trimmed.charAt(0) == 'v' || trimmed.charAt(0) == 'V')? 1: 0);

		final List<Integer> numbers = new ArrayList<>(3);
		while(index < trimmed.length() && Character.isDigit(trimmed.charAt(index))){
			long value = 0;
			while(index < trimmed.length() && Character.isDigit(trimmed.charAt(index))){
				//(a runaway number must not overflow)
				value = Math.min(value * 10 + Character.digit(trimmed.charAt(index), 10), Integer.MAX_VALUE);
				index ++;
			}
			numbers.add((int)value);

			if(index + 1 < trimmed.length() && trimmed.charAt(index) == '.' && Character.isDigit(trimmed.charAt(index + 1)))
				index ++;
			else
				break;
		}
		if(numbers.isEmpty())
			return Optional.empty();

		String qualifier = "";
		if(index < trimmed.length() && trimmed.charAt(index) == '-'){
			int end = index + 1;
			while(end < trimmed.length() && !Character.isWhitespace(trimmed.charAt(end)))
				end ++;
			qualifier = trimmed.substring(index + 1, end).toLowerCase(Locale.ROOT);
			//(these say that it is the final version)
			if(qualifier.equals("final") || qualifier.equals("release") || qualifier.equals("ga"))
				qualifier = "";
		}
		return Optional.of(new Version(List.copyOf(numbers), qualifier));
	}

	/** @return	Whether this is a version that precedes the final one ({@code -rc1}, {@code -SNAPSHOT}, ...). */
	public boolean isPreRelease(){
		return !qualifier.isEmpty();
	}

	@Override
	public int compareTo(final Version other){
		final int size = Math.max(numbers.size(), other.numbers.size());
		for(int i = 0; i < size; i ++){
			final int a = (i < numbers.size()? numbers.get(i): 0);
			final int b = (i < other.numbers.size()? other.numbers.get(i): 0);
			if(a != b)
				return Integer.compare(a, b);
		}
		if(qualifier.equals(other.qualifier))
			return 0;
		if(qualifier.isEmpty())
			return 1;
		if(other.qualifier.isEmpty())
			return -1;
		return compareQualifiers(qualifier, other.qualifier);
	}

	/** Compares token by token ({@code rc10} is {@code rc, 10}): numbers numerically, and before words, words alphabetically. */
	private static int compareQualifiers(final String a, final String b){
		final List<String> left = tokens(a);
		final List<String> right = tokens(b);
		final int size = Math.min(left.size(), right.size());
		for(int i = 0; i < size; i ++){
			final String x = left.get(i);
			final String y = right.get(i);
			final boolean numericX = Character.isDigit(x.charAt(0));
			final boolean numericY = Character.isDigit(y.charAt(0));
			final int result;
			if(numericX && numericY)
				result = new BigInteger(x).compareTo(new BigInteger(y));
			else if(numericX != numericY)
				result = (numericX? -1: 1);
			else
				result = x.compareTo(y);
			if(result != 0)
				return Integer.signum(result);
		}
		return Integer.compare(left.size(), right.size());
	}

	private static List<String> tokens(final String qualifier){
		final List<String> result = new ArrayList<>();
		int start = 0;
		for(int i = 1; i <= qualifier.length(); i ++)
			if(i == qualifier.length() || isSeparator(qualifier.charAt(i)) || isSeparator(qualifier.charAt(i - 1))
					|| Character.isDigit(qualifier.charAt(i)) != Character.isDigit(qualifier.charAt(i - 1))){
				if(!isSeparator(qualifier.charAt(start)))
					result.add(qualifier.substring(start, i));
				start = i;
			}
		return result;
	}

	/** Numbers equal as values have the same form ({@code 01} and {@code 1}). */
	private static String canonical(final String token){
		return (Character.isDigit(token.charAt(0))? new BigInteger(token).toString(): token);
	}

	private static boolean isSeparator(final char c){
		return (c == '.' || c == '-' || c == '_');
	}

	@Override
	public boolean equals(final Object obj){
		return (this == obj || obj instanceof final Version other && compareTo(other) == 0);
	}

	@Override
	public int hashCode(){
		int end = numbers.size();
		while(end > 1 && numbers.get(end - 1) == 0)
			end --;
		return numbers.subList(0, end).hashCode() * 31 + tokens(qualifier).stream().map(Version::canonical).toList().hashCode();
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < numbers.size(); i ++){
			if(i > 0)
				sb.append('.');
			sb.append(numbers.get(i));
		}
		if(!qualifier.isEmpty())
			sb.append('-').append(qualifier);
		return sb.toString();
	}

}
