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
package io.github.mtrevisan.familylegacy.v2.io.merger;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Provides static methods for computing similarity between strings, dates, and FLEFRecord structures.
 */
public final class SimilarityMetrics{

	private SimilarityMetrics(){}


	// ---------- String similarity ----------

	/**
	 * Computes Jaro‑Winkler similarity between two strings.
	 * The Jaro‑Winkler algorithm is a variant of the Jaro distance that gives higher
	 * weight to common prefixes. It is well‑suited for short strings like names.
	 *
	 * @param s1 first string (may be null)
	 * @param s2 second string (may be null)
	 * @return similarity in [0,1], or 1.0 if both are null or empty
	 */
	public static double jaroWinkler(String s1, String s2){
		if(s1 == null && s2 == null)
			return 1.;
		if(s1 == null || s2 == null)
			return 0.;

		s1 = s1.trim()
			.toLowerCase();
		s2 = s2.trim()
			.toLowerCase();
		if(s1.isEmpty() && s2.isEmpty())
			return 1.;
		if(s1.isEmpty() || s2.isEmpty())
			return 0.;

		// Jaro similarity
		final int len1 = s1.length();
		final int len2 = s2.length();
		int maxDist = Math.max(len1, len2) / 2 - 1;
		if(maxDist < 0)
			maxDist = 0;
		final boolean[] match1 = new boolean[len1];
		final boolean[] match2 = new boolean[len2];
		int matches = 0;
		for(int i = 0; i < len1; i ++){
			final int start = Math.max(0, i - maxDist);
			final int end = Math.min(len2, i + maxDist + 1);
			for(int j = start; j < end; j ++){
				if(!match2[j] && s1.charAt(i) == s2.charAt(j)){
					match1[i] = true;
					match2[j] = true;
					matches ++;

					break;
				}
			}
		}
		if(matches == 0)
			return 0.;

		int transpositions = 0;
		int k = 0;
		for(int i = 0; i < len1; i ++)
			if(match1[i]){
				while(!match2[k])
					k ++;
				if(s1.charAt(i) != s2.charAt(k))
					transpositions ++;
				k ++;
			}
		transpositions /= 2;
		final double jaro = ((double)matches / len1 + (double)matches / len2 +
			(double)(matches - transpositions) / matches) / 3.;

		// Winkler boost (prefix length <=4)
		int prefix = 0;
		for(int i = 0, size = Math.min(4, Math.min(len1, len2)); i < size; i ++){
			if(s1.charAt(i) == s2.charAt(i))
				prefix ++;
			else
				break;
		}
		return jaro + prefix * 0.1 * (1 - jaro);
	}

	/**
	 * Computes Levenshtein distance based similarity.
	 * The Levenshtein distance counts the number of insertions, deletions and substitutions
	 * needed to transform one string into the other. This is normalised by the maximum length.
	 */
	public static double levenshteinSimilarity(String s1, String s2){
		if(s1 == null && s2 == null)
			return 1.;
		if(s1 == null || s2 == null)
			return 0.;
		s1 = s1.trim()
			.toLowerCase();
		s2 = s2.trim()
			.toLowerCase();
		if(s1.isEmpty() && s2.isEmpty())
			return 1.;
		if(s1.isEmpty() || s2.isEmpty())
			return 0.;

		final int maxLen = Math.max(s1.length(), s2.length());
		final int dist = levenshteinDistance(s1, s2);
		return 1. - (double)dist / maxLen;
	}

	private static int levenshteinDistance(final String s1, final String s2){
		final int n = s1.length();
		final int m = s2.length();
		if(n == 0)
			return m;
		if(m == 0)
			return n;

		final int[][] dp = new int[n + 1][m + 1];
		for(int i = 0; i <= n; i ++)
			dp[i][0] = i;
		for(int j = 0; j <= m; j ++)
			dp[0][j] = j;
		for(int i = 1; i <= n; i ++)
			for(int j = 1; j <= m; j ++){
				final int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)? 0: 1);
				dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
			}
		return dp[n][m];
	}

	// ---------- Date similarity ----------

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	/**
	 * Computes similarity between two ISO date strings (YYYY-MM-DD or YYYY).
	 * The similarity is based on the absolute difference in days, mapped to [0,1] with a
	 * decaying function: exact or 1 day apart → 1.0, >10 years → 0.0.
	 */
	public static double dateSimilarity(final String date1, final String date2){
		final LocalDate d1 = parseDate(date1);
		final LocalDate d2 = parseDate(date2);
		if(d1 == null && d2 == null)
			return 1.;
		if(d1 == null || d2 == null)
			return 0.;

		final long days = Math.abs(d1.toEpochDay() - d2.toEpochDay());
		if(days <= 1)
			return 1.;
		if(days <= 7)
			return 0.9;
		if(days <= 30)
			return 0.7;
		if(days <= 365)
			return 0.5;
		if(days <= 3650)
			return 0.3;
		return 0.;
	}

	private static LocalDate parseDate(final String s){
		if(s == null)
			return null;
		try{
			// try full ISO date
			if(s.matches("\\d{4}-\\d{2}-\\d{2}"))
				return LocalDate.parse(s, ISO_DATE);
			// try year only
			if(s.matches("\\d{4}"))
				return LocalDate.of(Integer.parseInt(s), 1, 1);
			// ignore other formats
			return null;
		}
		catch(final Exception e){
			return null;
		}
	}

	// ---------- Structural similarity ----------

	/**
	 * Computes the structural similarity between two FLEFRecord trees.
	 * The comparison considers:
	 * - tag (0.3 weight)
	 * - value (0.3 weight)
	 * - children (0.4 weight) – recursively matched using a greedy best‑match algorithm.
	 */
	public static double structuralSimilarity(final FLEFRecord r1, final FLEFRecord r2){
		if(r1 == null && r2 == null)
			return 1.;
		if(r1 == null || r2 == null)
			return 0.;

		// Tag match: 0.3 weight
		final double tagSim = (Objects.equals(r1.getTag(), r2.getTag())? 1.: 0.);

		// Value match: 0.3 weight
		double valSim;
		final String v1 = r1.getValue();
		final String v2 = r2.getValue();
		if(v1 == null && v2 == null)
			valSim = 1.;
		else if(v1 != null && v2 != null)
			valSim = levenshteinSimilarity(v1, v2);
		else
			valSim = 0.;

		// Children similarity: 0.4 weight
		final double childSim = childrenSimilarity(r1.getChildren(), r2.getChildren());

		return tagSim * 0.3 + valSim * 0.3 + childSim * 0.4;
	}

	private static double childrenSimilarity(final List<FLEFRecord> c1, final List<FLEFRecord> c2){
		if(c1.isEmpty() && c2.isEmpty())
			return 1.;
		if(c1.isEmpty() || c2.isEmpty())
			return 0.;

		// For each child, find best match in the other list (greedy)
		// Use a simple approach: average of best matches
		double sum = 0.;
		final int size = c2.size();
		final boolean[] used2 = new boolean[size];
		for(final FLEFRecord a : c1){
			double best = 0.;
			int bestIdx = -1;
			for(int j = 0; j < size; j ++){
				if(used2[j])
					continue;

				final double sim = structuralSimilarity(a, c2.get(j));
				if(sim > best){
					best = sim;
					bestIdx = j;
				}
			}
			if(bestIdx >= 0){
				sum += best;
				used2[bestIdx] = true;
			}
		}
		// Also consider unmatched children from c2 (if any) – they contribute 0
		// Normalize by max number of children
		final int max = Math.max(c1.size(), c2.size());
		return (max > 0? sum / max: 0.);
	}

	// ---------- Composite similarity (convenience) ----------

	/**
	 * Computes a composite similarity score between two records using
	 * weighted combination of name, place, date, and structural similarities.
	 * The weights can be configured via the map (defaults are used if not specified).
	 */
	public static double compositeSimilarity(final FLEFRecord r1, final FLEFRecord r2,
			final Map<String, Double> weights){
		final double nameSim = computeNameSimilarity(r1, r2);
		final double placeSim = computePlaceSimilarity(r1, r2);
		final double dateSim = computeDateSimilarity(r1, r2);
		final double structSim = structuralSimilarity(r1, r2);

		return nameSim * weights.getOrDefault("name", 0.35) +
			placeSim * weights.getOrDefault("place", 0.20) +
			dateSim * weights.getOrDefault("date", 0.15) +
			structSim * weights.getOrDefault("structural", 0.30);
	}

	static double computeNameSimilarity(final FLEFRecord r1, final FLEFRecord r2){
		// Extract name parts (given, family)
		final String given1 = extractNamePart(r1, "given");
		final String given2 = extractNamePart(r2, "given");
		final String family1 = extractNamePart(r1, "family");
		final String family2 = extractNamePart(r2, "family");

		final double givenSim = jaroWinkler(given1, given2);
		final double familySim = jaroWinkler(family1, family2);
		return givenSim * 0.5 + familySim * 0.5;
	}

	static double computePlaceSimilarity(final FLEFRecord r1, final FLEFRecord r2){
		final String place1 = extractPlace(r1);
		final String place2 = extractPlace(r2);
		return jaroWinkler(place1, place2);
	}

	static double computeDateSimilarity(final FLEFRecord r1, final FLEFRecord r2){
		final String date1 = extractDate(r1);
		final String date2 = extractDate(r2);
		return dateSimilarity(date1, date2);
	}

	/**
	 * Extracts the given name part (e.g., "given", "family") from a record.
	 */
	public static String extractNamePart(final FLEFRecord record, final String partType){
		for(final FLEFRecord name : record.getChildren())
			if("name".equalsIgnoreCase(name.getTag()))
				for(FLEFRecord part : name.getChildren())
					if("part".equalsIgnoreCase(part.getTag())){
						final String type = FLEFRecordHelper.getChildValue(part, "type");
						final String value = FLEFRecordHelper.getChildValue(part, "value");
						if(partType.equalsIgnoreCase(type) && value != null)
							return value;
					}
		return StringUtils.EMPTY;
	}

	private static String extractPlace(final FLEFRecord record){
		final FLEFRecord place = FLEFRecordHelper.findChild(record, "place");
		if(place != null){
			final String ref = place.getValue();
			if(ref != null && !ref.isEmpty())
				return ref;

			final String orig = FLEFRecordHelper.getChildValue(place, "original_text");
			if(orig != null)
				return orig;
		}
		return StringUtils.EMPTY;
	}

	private static String extractDate(final FLEFRecord record){
		final FLEFRecord date = FLEFRecordHelper.findChild(record, "date");
		if(date != null)
			return FLEFRecordHelper.getChildValue(date, "value.point.full_date.value");
		return null;
	}

}
