/**
 * Copyright (c) 2020 Mauro Trevisan
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
package ch.thn;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Utility methods which handle various number operations and functions related to numbers.<br />
 * <br />
 * It contains operations which can be applied to any type of number (see
 * {@link #add(Number, Number)}, {@link #subtract(Number, Number)}, etc.)
 *
 * @author Thomas Naeff (github.com/thnaeff)
 */
public class NumberUtil{

	// Hierarchy according to: http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2.4
	private enum OperationType{
		SHORT, INTEGER, LONG, FLOAT, DOUBLE;
	}

	// Number types for easier if/switch etc. usage (so that instanceof does not need to be used)
	private static final Map<Class<?>, OperationType> types = new HashMap<Class<?>, OperationType>();

	static{
		types.put(Short.class, OperationType.SHORT);
		types.put(Integer.class, OperationType.INTEGER);
		types.put(Long.class, OperationType.LONG);
		types.put(Float.class, OperationType.FLOAT);
		types.put(Double.class, OperationType.DOUBLE);
	}

	// Shared decimal patterns
	private static final HashMap<Integer, DecimalFormat> roundDecimals = new HashMap<Integer, DecimalFormat>();

	static{
		roundDecimals.put(1, new DecimalFormat("#.#"));
		roundDecimals.put(2, new DecimalFormat("#.##"));
		roundDecimals.put(3, new DecimalFormat("#.###"));
		roundDecimals.put(4, new DecimalFormat("#.####"));
		roundDecimals.put(5, new DecimalFormat("#.#####"));
	}

	private static int MAX_DIGIT_PATTERN = 6;

	// Some predefined digit patterns
	private static final HashMap<Integer, String[]> digitPattern = new HashMap<Integer, String[]>();

	static{
		digitPattern.put(1, new String[]{"#", "0"});
		digitPattern.put(2, new String[]{"##", "00"});
		digitPattern.put(3, new String[]{"###", "000"});
		digitPattern.put(4, new String[]{"####", "0000"});
		digitPattern.put(5, new String[]{"#####", "00000"});
		digitPattern.put(MAX_DIGIT_PATTERN, new String[]{"######", "000000"});
	}


	/**
	 * Checks if a integer is odd.
	 *
	 * @param num The number to test
	 * @return True if the integer is odd, false if the number is even
	 */
	public static boolean isOdd(int num){
		return num % 2 != 0;
	}

	/**
	 * Checks if a integer is even.
	 *
	 * @param num The number to test
	 * @return True if the integer is even, false if the number is even
	 */
	public static boolean isEven(int num){
		return num % 2 == 0;
	}

	/**
	 * Rounds to the given number of decimals for double values.<br>
	 * Example with two decimals: 1.22565... -> 1.23
	 *
	 * @param num              The number to round
	 * @param numberOfDecimals The number of decimals to show
	 * @return The rounded number
	 */
	public static double roundDecimals(double num, int numberOfDecimals){
		if(roundDecimals.containsKey(numberOfDecimals)){
			return Double.valueOf(roundDecimals.get(numberOfDecimals).format(num));
		}
		else{
			return Double.valueOf(createDecimalFormat(1, numberOfDecimals, false, false, false, false, false).format(num));
		}
	}

	/**
	 * Rounds to the given number of decimals for float values.<br>
	 * Example with two decimals: 1.22565... -> 1.23
	 *
	 * @param num              The number to round
	 * @param numberOfDecimals The number of decimals to show
	 * @return The rounded number
	 */
	public static float roundDecimals(float num, int numberOfDecimals){
		if(roundDecimals.containsKey(numberOfDecimals)){
			return Float.valueOf(roundDecimals.get(numberOfDecimals).format(num));
		}
		else{
			return Float.valueOf(createDecimalFormat(1, numberOfDecimals, false, false, false, false, false).format(num));
		}
	}

	/**
	 * This method provides an easy way to create a {@link DecimalFormat} object.<br>
	 * <br>
	 * See the javadoc for more information about the formatting possibilities.
	 *
	 * @param minNumberOfDigits     The number of digits to show
	 * @param maxNumberOfDecimals   The number of decimals to show
	 * @param currency              Adds the currency symbol
	 * @param internationalCurrency Adds the international currency symbol
	 * @param leadingZeroes         Shows leading zeroes, filled up to the number of digits to show
	 * @param percentage            Adds the percentage sign
	 * @param grouping              Groups the numbers by thousands (e.g. shows 10,000 instead of 10000)
	 * @return The decimal format
	 */
	public static DecimalFormat createDecimalFormat(int minNumberOfDigits, int maxNumberOfDecimals, boolean currency, boolean internationalCurrency, boolean leadingZeroes, boolean percentage, boolean grouping){
		if(minNumberOfDigits <= 0){
			throw new IllegalArgumentException("Minimum number of digits has to be > 0");
		}

		if(maxNumberOfDecimals < 0){
			throw new IllegalArgumentException("Maximum number of decimals has to be >= 0");
		}

		StringBuilder sb = new StringBuilder(minNumberOfDigits + maxNumberOfDecimals);

		if(currency || internationalCurrency){
			// Will be replaced by the currency symbol
			sb.append("\u00A4");

			if(internationalCurrency){
				// Two currency symbols are replaced with the international currency symbol
				sb.append("\u00A4");
			}
		}

		sb.append(getDigitPattern(minNumberOfDigits, leadingZeroes, grouping));

		sb.append(".");

		sb.append(getDigitPattern(maxNumberOfDecimals, false, false));

		if(percentage){
			sb.append("%");
		}

		return new DecimalFormat(sb.toString());
	}

	/**
	 * Returns the digit pattern with the given number of digits.
	 *
	 * @param numberOfDigits The number of digits
	 * @param zeroes         If <code>true</code>, missing digits will be shown as zeroes
	 * @param grouping       Groups by thousands
	 * @return The digit pattern
	 */
	private static String getDigitPattern(int numberOfDigits, boolean zeroes, boolean grouping){
		if(numberOfDigits < 0){
			throw new IllegalArgumentException("Number of digits has to be >= 0");
		}

		StringBuilder sb = new StringBuilder(numberOfDigits);

		if(digitPattern.containsKey(numberOfDigits)){
			sb.append(digitPattern.get(numberOfDigits)[zeroes? 1: 0]);
		}
		else{

			sb.append(digitPattern.get(MAX_DIGIT_PATTERN)[zeroes? 1: 0]);

			if(zeroes){
				for(int i = MAX_DIGIT_PATTERN; i < numberOfDigits; i++){
					sb.append("0");
				}
			}
			else{
				for(int i = MAX_DIGIT_PATTERN; i < numberOfDigits; i++){
					sb.append("#");
				}
			}

		}

		if(grouping && sb.length() > 3){
			// Add the grouping separator if requested
			sb.insert(sb.length() - 3, ',');
		}

		return sb.toString();
	}

	/**
	 * Formats a number according to the given parameters.
	 *
	 * @param num                   The number to format
	 * @param minNumberOfDigits     The number of digits to show
	 * @param maxNumberOfDecimals   The maximal number of decimals to show
	 * @param currency              Whether or not to add the currency sign
	 * @param internationalCurrency Whether or not to show the currency sign in international format
	 * @param leadingZeroes         Whether or not to fill missing the digits with zeroes
	 * @param percentage            Whether or not to add a percentage sign
	 * @param grouping              Enables grouping (grouped by thousands, e.g. 10,000)
	 * @return The formatted number
	 */
	public static String formatNumber(Number num, int minNumberOfDigits, int maxNumberOfDecimals, boolean currency, boolean internationalCurrency, boolean leadingZeroes, boolean percentage, boolean grouping){
		return createDecimalFormat(minNumberOfDigits, maxNumberOfDecimals, currency, internationalCurrency, leadingZeroes, percentage, grouping).format(num);
	}

	/**
	 * Formats a number according to the given parameters.
	 *
	 * @param num                   The number to format
	 * @param currency              Whether or not to add the currency sign
	 * @param internationalCurrency Whether or not to show the currency sign in international format
	 * @param percentage            Whether or not to add a percentage sign
	 * @param grouping              Enables grouping (grouped by thousands, e.g. 10,000)
	 * @return The formatted number
	 */
	public static String formatNumber(Number num, boolean currency, boolean internationalCurrency, boolean percentage, boolean grouping){
		// Use at least 4 digits to make the grouping work
		return createDecimalFormat(4, 0, currency, internationalCurrency, false, percentage, grouping).format(num);
	}

	/**
	 * Formats a number according to the given parameters.
	 *
	 * @param num                 The number to format
	 * @param minNumberOfDigits   The number of digits to show
	 * @param maxNumberOfDecimals The maximal number of decimals to show
	 * @param leadingZeroes       Whether or not to fill missing the digits with zeroes
	 * @param grouping            Enables grouping (grouped by thousands, e.g. 10,000)
	 * @return The formatted number
	 */
	public static String formatNumber(Number num, int minNumberOfDigits, int maxNumberOfDecimals, boolean leadingZeroes, boolean grouping){
		return createDecimalFormat(minNumberOfDigits, maxNumberOfDecimals, false, false, leadingZeroes, false, grouping).format(num);
	}

	/**
	 * Formats a number according to the given parameters.
	 *
	 * @param num      The number to format
	 * @param grouping Enables grouping (grouped by thousands, e.g. 10,000)
	 * @return The formatted number
	 */
	public static String formatNumber(Number num, boolean grouping){
		// Use at least 4 digits to make the grouping work
		return createDecimalFormat(4, 0, false, false, false, false, grouping).format(num);
	}


	/**
	 * Adds n1 + n2 for any number type.<br>
	 * The returned number result has the type of the input number with the higher "precision" (for
	 * example, a float value added to an integer value results in a floating point operation,
	 * returning a float type).
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static Number add(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() + num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() + num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() + num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() + num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() + num2.doubleValue();
		}

		throw new IllegalArgumentException("Number addition error");
	}

	/**
	 * Subtracts n1 - n2 for any number type.<br>
	 * The returned number result has the type of the input number with the higher "precision" (for
	 * example, a float value subtracted from an integer value results in a floating point operation,
	 * returning a float type).
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static Number subtract(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() - num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() - num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() - num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() - num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() - num2.doubleValue();
		}

		throw new IllegalArgumentException("Number subtraction error");
	}

	/**
	 * Multiplies n1 * n2 for any number type.<br>
	 * The returned number result has the type of the input number with the higher "precision" (for
	 * example, a float value multiplied by an integer value results in a floating point operation,
	 * returning a float type).
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static Number multiply(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() * num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() * num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() * num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() * num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() * num2.doubleValue();
		}

		throw new IllegalArgumentException("Number multiplication error");
	}


	/**
	 * Divides n1 / n2 for any number type.<br>
	 * The returned number result has the type of the input number with the higher "precision" (for
	 * example, a float value divided by an integer value results in a floating point operation,
	 * returning a float type).
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static Number divide(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() / num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() / num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() / num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() / num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() / num2.doubleValue();
		}

		throw new IllegalArgumentException("Number division error");
	}

	/**
	 * Calculates the remainder n1 % n2 for any number type.<br>
	 * The returned number result has the type of the input number with the higher "precision" (for
	 * example, a float value multiplied by an integer value results in a floating point operation,
	 * returning a float type).
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static Number remainder(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() % num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() % num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() % num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() % num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() % num2.doubleValue();
		}

		throw new IllegalArgumentException("Number remainder error");
	}

	/**
	 * Checks if n1 > n2 for any number type.
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static boolean gt(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() > num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() > num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() > num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() > num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() > num2.doubleValue();
		}

		throw new IllegalArgumentException("Number greater than error");
	}

	/**
	 * Checks if n1 < n2 for any number type.
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The result of the operation
	 */
	public static boolean lt(Number num1, Number num2){
		OperationType type = getOperationType(num1, num2);

		if(type == OperationType.SHORT){
			return num1.shortValue() < num2.shortValue();
		}
		else if(type == OperationType.INTEGER){
			return num1.intValue() < num2.intValue();
		}
		else if(type == OperationType.LONG){
			return num1.longValue() < num2.longValue();
		}
		else if(type == OperationType.FLOAT){
			return num1.floatValue() < num2.floatValue();
		}
		else if(type == OperationType.DOUBLE){
			return num1.doubleValue() < num2.doubleValue();
		}

		throw new IllegalArgumentException("Number less than error");
	}

	/**
	 * Determines the type of the operation. For example, a float value multiplied by an integer value
	 * results in a floating point operation, returning a float type). The floating point operations
	 * are defined in http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2.4
	 *
	 * @param num1 The first number
	 * @param num2 The second number
	 * @return The operation type
	 */
	private static OperationType getOperationType(Number num1, Number num2){
		Class<?> c1 = num1.getClass();
		Class<?> c2 = num2.getClass();

		if(! types.containsKey(c1)){
			throw new IllegalArgumentException("Unknown number class " + c1);
		}

		if(! types.containsKey(c2)){
			throw new IllegalArgumentException("Unknown number class " + c2);
		}

		OperationType type1 = types.get(c1);
		OperationType type2 = types.get(c2);

		return type1.ordinal() > type2.ordinal()? type1: type2;
	}

	/**
	 * Generates a single random number. The type of the resulting numbers is the type of the given
	 * min or max value, whichever has the higher "precision" (short -> int -> long -> float ->
	 * double)<br>
	 * <br>
	 * For multiple random numbers use {@link #generateRandomNumbers(int, Number, Number)}
	 *
	 * @param min The minimal number to generate
	 * @param max The maximum number to generate
	 * @return The random number
	 */
	public static Number generateRandomNumber(Number min, Number max){
		return generateRandomNumbers(1, min, max).get(0);
	}

	/**
	 * Generates random numbers. The type of the resulting numbers is the type of the given min or max
	 * value, whichever has the higher "precision" (short -> int -> long -> float -> double)
	 *
	 * @param count The cound of random numbers to generate
	 * @param min   The minimal number to generate
	 * @param max   The maximum number to generate
	 * @return The random numbers
	 */
	public static List<Number> generateRandomNumbers(int count, Number min, Number max){
		Random random = new Random();
		List<Number> numbers = new ArrayList<Number>(count);

		OperationType type = getOperationType(min, max);

		if(type == OperationType.SHORT){
			// Including the maximum number (from min to max)
			int randomMax = max.intValue() - min.intValue() + 1;
			for(int i = 0; i < count; i++){
				numbers.add(random.nextInt(randomMax) + min.intValue());
			}
		}
		else if(type == OperationType.INTEGER){
			// Including the maximum number (from min to max)
			int randomMax = max.intValue() - min.intValue() + 1;
			for(int i = 0; i < count; i++){
				numbers.add(random.nextInt(randomMax) + min.intValue());
			}
		}
		else if(type == OperationType.LONG){
			// Including the maximum number (from min to max)
			long randomMax = max.longValue() - min.longValue() + 1l;
			for(int i = 0; i < count; i++){
				numbers.add(random.nextLong() * randomMax + min.longValue());
			}
		}
		else if(type == OperationType.FLOAT){
			// Excluding the maximum number (between min and max)
			float randomMax = max.floatValue() - min.floatValue();
			for(int i = 0; i < count; i++){
				numbers.add(random.nextFloat() * randomMax + min.floatValue());
			}
		}
		else if(type == OperationType.DOUBLE){
			// Excluding the maximum number (between min and max)
			double randomMax = max.doubleValue() - min.doubleValue();
			for(int i = 0; i < count; i++){
				numbers.add(random.nextDouble() * randomMax + min.doubleValue());
			}
		}

		return numbers;
	}

}
