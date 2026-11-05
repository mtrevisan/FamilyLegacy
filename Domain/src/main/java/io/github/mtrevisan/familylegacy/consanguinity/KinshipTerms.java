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

import io.github.mtrevisan.familylegacy.i18n.I18N;


/**
 * The name of the kinship that two individuals have, from the generations between each of them and their closest common ancestor.
 * The words are the ones of the language of the application ({@code kinship.*} in the messages): a language orders the words of a
 * sentence in its own way, so the sentences are messages with the names in their places, not pieces put one after the other.
 */
public final class KinshipTerms{

	private KinshipTerms(){}


	/**
	 * @param stepsFromFirst  the generations between the first individual and the common ancestor
	 * @param stepsFromSecond the generations between the second individual and the common ancestor
	 * @return	The kinship, with no regard to sex: "siblings", "uncle or aunt and niece or nephew", "first cousins",
	 * "second cousins once removed"... The blood relatives in a direct line are "parent and child", "grandparent and grandchild", ...
	 */
	public static String describe(final int stepsFromFirst, final int stepsFromSecond){
		final int nearer = Math.min(stepsFromFirst, stepsFromSecond);
		final int farther = Math.max(stepsFromFirst, stepsFromSecond);
		if(farther == 0)
			return I18N.t("kinship.terms.same_individual");

		//one is the ancestor of the other
		if(nearer == 0)
			return switch(farther){
				case 1 -> I18N.t("kinship.terms.parent_child");
				case 2 -> I18N.t("kinship.terms.grandparent_grandchild");
				case 3 -> I18N.t("kinship.terms.great_grandparent_great_grandchild");
				default -> I18N.tf("kinship.terms.remote_ancestor_descendant", farther - 2);
			};

		if(nearer == 1)
			return switch(farther){
				case 1 -> I18N.t("kinship.terms.siblings");
				case 2 -> I18N.t("kinship.terms.uncle_nephew");
				case 3 -> I18N.t("kinship.terms.great_uncle_great_nephew");
				default -> I18N.tf("kinship.terms.collateral", farther - 1);
			};

		return cousins(nearer - 1, farther - nearer);
	}

	/**
	 * @param degree	The degree of the cousins (1 for first cousins).
	 * @param removed	The generations between the two (0 for cousins of the same generation).
	 * @return	"first cousins", "second cousins once removed", "third cousins 2 times removed"...
	 */
	public static String cousins(final int degree, final int removed){
		return cousins(KinshipVocabulary.ui(), degree, removed);
	}

	/** The same as {@link #cousins(int, int)}, in the language of the vocabulary. */
	public static String cousins(final KinshipVocabulary vocabulary, final int degree, final int removed){
		return vocabulary.text((removed == 0? "kinship.cousins": (removed == 1? "kinship.cousins.once_removed": "kinship.cousins.times_removed")),
			ordinal(vocabulary, degree, false), removed);
	}

	/** The same as {@link #cousins(int, int)}, for one of the two: "first cousin", "second cousin once removed"... */
	public static String cousin(final int degree, final int removed){
		return cousin(KinshipVocabulary.ui(), degree, removed, false);
	}

	/**
	 * @param vocabulary	Where the words come from.
	 * @param degree	The degree of the cousin (1 for a first cousin).
	 * @param removed	The generations between the two (0 for cousins of the same generation).
	 * @param female	Whether the cousin is a woman (a language can have a word for each sex).
	 * @return	"first cousin", "second cousin once removed"... in the language of the vocabulary.
	 */
	public static String cousin(final KinshipVocabulary vocabulary, final int degree, final int removed, final boolean female){
		final String sex = (female? "female": "male");
		return vocabulary.text("kinship.cousin." + sex + (removed == 0? "": (removed == 1? ".once_removed": ".times_removed")),
			ordinal(vocabulary, degree, female), removed, degree);
	}

	/** @return	"first", "second", ... "ninth", and "10th", "11th"... for the larger ones. */
	public static String ordinal(final int n){
		return ordinal(KinshipVocabulary.ui(), n, false);
	}

	/**
	 * @param vocabulary	Where the words come from.
	 * @param n	The number.
	 * @param female	Whether the word is for a feminine noun (a language can have two forms).
	 * @return	"first", "second", ... "ninth", and "10th", "11th"... for the larger ones.
	 */
	public static String ordinal(final KinshipVocabulary vocabulary, final int n, final boolean female){
		return (n >= 1 && n <= 9
			? vocabulary.text("kinship.ordinal." + (female? "f.": "") + n)
			: vocabulary.text("kinship.ordinal.other", n));
	}

}
