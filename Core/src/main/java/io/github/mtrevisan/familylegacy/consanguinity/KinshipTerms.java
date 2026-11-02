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
package io.github.mtrevisan.familylegacy.consanguinity;


/** The name of the kinship that two individuals have, from the generations between each of them and their closest common ancestor. */
public final class KinshipTerms{

	private KinshipTerms(){}


	/**
	 * @param stepsFromFirst  the generations between the first individual and the common ancestor
	 * @param stepsFromSecond the generations between the second individual and the common ancestor
	 * @return	The kinship, in English and with no regard to sex: "siblings", "uncle or aunt and niece or nephew", "first cousins",
	 * "second cousins once removed"... The blood relatives in a direct line are "parent and child", "grandparent and grandchild", ...
	 */
	public static String describe(final int stepsFromFirst, final int stepsFromSecond){
		final int nearer = Math.min(stepsFromFirst, stepsFromSecond);
		final int farther = Math.max(stepsFromFirst, stepsFromSecond);
		if(farther == 0)
			return "the same individual";

		//one is the ancestor of the other
		if(nearer == 0)
			return switch(farther){
				case 1 -> "parent and child";
				case 2 -> "grandparent and grandchild";
				case 3 -> "great-grandparent and great-grandchild";
				default -> (farther - 2) + "-times-great-grandparent and descendant";
			};

		if(nearer == 1)
			return switch(farther){
				case 1 -> "siblings";
				case 2 -> "uncle or aunt and niece or nephew";
				case 3 -> "great-uncle or great-aunt and great-niece or great-nephew";
				default -> "collateral relatives, " + (farther - 1) + " generations apart";
			};

		final int degree = nearer - 1;
		final int removed = farther - nearer;
		final String cousins = ordinal(degree) + " cousins";
		return (removed == 0? cousins: cousins + " " + (removed == 1? "once": removed + " times") + " removed");
	}

	private static String ordinal(final int n){
		return switch(n){
			case 1 -> "first";
			case 2 -> "second";
			case 3 -> "third";
			case 4 -> "fourth";
			case 5 -> "fifth";
			case 6 -> "sixth";
			case 7 -> "seventh";
			case 8 -> "eighth";
			case 9 -> "ninth";
			default -> n + "th";
		};
	}

}
