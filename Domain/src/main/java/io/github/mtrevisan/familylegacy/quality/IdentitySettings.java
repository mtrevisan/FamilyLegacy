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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;


/**
 * What the Identity Assistant is told by the user: from which similarity a pair of individuals is proposed, how many are, the scores
 * from which the confidence of a hypothesis is high and medium, and the probability from which a pair of the other kinds of record is
 * proposed. They are a presentation choice, kept in the preferences and never in the genealogy; the defaults are conventions that are
 * not calibrated on real data, which is why they can be changed.
 *
 * @param threshold	The similarity score from which a pair of individuals is proposed.
 * @param maxPairs	The most pairs of individuals proposed: the ones with the highest score.
 * @param highScore	The score from which a pair is a strong candidate (a warning) and the confidence proposed is high.
 * @param mediumScore	The score from which the confidence proposed is medium; below it, low.
 * @param otherPercent	The match probability (percent) from which a pair of the other kinds of record is proposed.
 */
public record IdentitySettings(int threshold, int maxPairs, int highScore, int mediumScore, int otherPercent){

	public static final int DEFAULT_THRESHOLD = 60;
	public static final int DEFAULT_MAX_PAIRS = 100;
	public static final int DEFAULT_HIGH_SCORE = 90;
	public static final int DEFAULT_MEDIUM_SCORE = 70;
	public static final int DEFAULT_OTHER_PERCENT = 70;

	/** The ranges the values are brought into. */
	public static final int MIN_THRESHOLD = 1;
	public static final int MAX_THRESHOLD = 300;
	public static final int MIN_MAX_PAIRS = 1;
	public static final int MAX_MAX_PAIRS = 500;
	public static final int MIN_OTHER_PERCENT = 50;
	public static final int MAX_OTHER_PERCENT = 99;

	/** The settings of a user who has not changed any. */
	public static final IdentitySettings DEFAULTS = new IdentitySettings(DEFAULT_THRESHOLD, DEFAULT_MAX_PAIRS, DEFAULT_HIGH_SCORE,
		DEFAULT_MEDIUM_SCORE, DEFAULT_OTHER_PERCENT);


	/**
	 * Brings every value into its range, and the score of the medium confidence under the one of the high confidence, so that a value
	 * that was typed or read wrongly cannot make the assistant meaningless.
	 */
	public IdentitySettings{
		threshold = clamp(threshold, MIN_THRESHOLD, MAX_THRESHOLD);
		maxPairs = clamp(maxPairs, MIN_MAX_PAIRS, MAX_MAX_PAIRS);
		highScore = clamp(highScore, MIN_THRESHOLD, MAX_THRESHOLD);
		mediumScore = Math.min(clamp(mediumScore, MIN_THRESHOLD, MAX_THRESHOLD), highScore);
		otherPercent = clamp(otherPercent, MIN_OTHER_PERCENT, MAX_OTHER_PERCENT);
	}


	/** @return	The settings of the user. */
	public static IdentitySettings load(){
		return load(AppPreferences.getInstance());
	}

	/** @return	The settings kept in the given preferences (the defaults for what is not there). */
	public static IdentitySettings load(final AppPreferences preferences){
		return new IdentitySettings(preferences.intValue(AppPreferences.KEY_IDENTITY_THRESHOLD, DEFAULT_THRESHOLD),
			preferences.intValue(AppPreferences.KEY_IDENTITY_MAX_PAIRS, DEFAULT_MAX_PAIRS),
			preferences.intValue(AppPreferences.KEY_IDENTITY_HIGH_SCORE, DEFAULT_HIGH_SCORE),
			preferences.intValue(AppPreferences.KEY_IDENTITY_MEDIUM_SCORE, DEFAULT_MEDIUM_SCORE),
			preferences.intValue(AppPreferences.KEY_IDENTITY_OTHER_PERCENT, DEFAULT_OTHER_PERCENT));
	}

	/** Keeps the settings in the preferences of the user. */
	public void save(){
		save(AppPreferences.getInstance());
	}

	/** Keeps the settings in the given preferences. */
	public void save(final AppPreferences preferences){
		preferences.setIntValue(AppPreferences.KEY_IDENTITY_THRESHOLD, threshold);
		preferences.setIntValue(AppPreferences.KEY_IDENTITY_MAX_PAIRS, maxPairs);
		preferences.setIntValue(AppPreferences.KEY_IDENTITY_HIGH_SCORE, highScore);
		preferences.setIntValue(AppPreferences.KEY_IDENTITY_MEDIUM_SCORE, mediumScore);
		preferences.setIntValue(AppPreferences.KEY_IDENTITY_OTHER_PERCENT, otherPercent);
	}

	/** @return	The confidence proposed for the hypothesis of a pair with that similarity score: {@code high}, {@code medium} or {@code low}. */
	public String confidence(final int score){
		if(score >= highScore)
			return ConfidenceReader.ENUM_HIGH;
		return (score >= mediumScore? ConfidenceReader.ENUM_MEDIUM: ConfidenceReader.ENUM_LOW);
	}

	/** @return	The probability, from 0 to 1, from which a pair of the other kinds of record is proposed. */
	public double otherProbability(){
		return otherPercent / 100.;
	}


	private static int clamp(final int value, final int min, final int max){
		return Math.max(min, Math.min(max, value));
	}

}
