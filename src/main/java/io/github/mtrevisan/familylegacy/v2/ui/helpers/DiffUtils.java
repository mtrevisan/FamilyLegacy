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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple diff utility that computes line‑based differences using the Myers algorithm.
 */
public final class DiffUtils{

	/** Two lines with the same tag, or very similar content, are MODIFIED. */
	private static final double SIMILARITY_THRESHOLD = 0.7;


	public enum Operation{
		EQUAL,
		INSERT,
		DELETE,
		MODIFIED
	}

	public record DiffEntry(Operation operation, String leftLine, String rightLine){}


	private DiffUtils(){}


	/**
	 *
	 * Computes the diff between two lists of strings (lines).
	 * Uses the Myers O(ND) algorithm.
	 * <p>
	 * Steps:
	 * 1. Build the LCS table between the two sequences.
	 * 2. Backtrack the table iteratively to produce a raw sequence of
	 *    EQUAL / DELETE / INSERT entries.
	 * 3. Merge adjacent DELETE/INSERT runs into MODIFIED entries where
	 *    a line was effectively replaced rather than purely added/removed.
	 */
	public static List<DiffEntry> computeDiff(final List<String> leftLines, final List<String> rightLines){
		// Convert to arrays for easier access
		final String[] a = leftLines.toArray(new String[0]);
		final String[] b = rightLines.toArray(new String[0]);

		// Myers diff: find the shortest edit script
		// We'll use the classic approach with a forward-backward algorithm.
		// For simplicity, we use the greedy algorithm that returns the LCS and then
		// we derive the diff.
		// But we'll implement a simpler version using the "midpoint" algorithm.
		// However, to keep the code manageable, we'll implement the basic LCS-based diff.
		// For a real implementation, the Myers algorithm is more efficient, but for
		// typical genealogical text files it's fine.

		// We'll compute the LCS (Longest Common Subsequence) first.
		final int[][] lcs = computeLCS(a, b);
		// Then backtrack to generate diff operations.
		final List<DiffEntry> rawDiff = backtrack(a, b, lcs);
		return mergeModifications(rawDiff);
	}

	private static int[][] computeLCS(final String[] a, final String[] b){
		final int n = a.length;
		final int m = b.length;
		final int[][] dp = new int[n + 1][m + 1];
		for(int i = 1; i <= n; i ++)
			for(int j = 1; j <= m; j ++){
				if(a[i - 1].equals(b[j - 1]))
					dp[i][j] = dp[i - 1][j - 1] + 1;
				else
					dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
			}
		return dp;
	}

	/**
	 * Walks the LCS table from (n, m) down to (0, 0), pushing entries into
	 * a list that is built in reverse order and then reversed once at the end.
	 */
	private static List<DiffEntry> backtrack(final String[] a, final String[] b, final int[][] lcs){
		final List<DiffEntry> reversed = new ArrayList<>();
		int i = a.length;
		int j = b.length;
		while(i > 0 || j > 0){
			if(i > 0 && j > 0 && a[i - 1].equals(b[j - 1])){
				reversed.add(new DiffEntry(Operation.EQUAL, a[i - 1], b[j - 1]));
				i --;
				j --;
			}
			else if(i > 0 && (j == 0 || lcs[i][j] == lcs[i - 1][j])){
				reversed.add(new DiffEntry(Operation.DELETE, a[i - 1], null));
				i --;
			}
			else{
				reversed.add(new DiffEntry(Operation.INSERT, null, b[j - 1]));
				j --;
			}
		}

		// Entries were collected walking backwards, so reverse to restore
		// the original left-to-right / top-to-bottom order.
		final List<DiffEntry> result = new ArrayList<>(reversed.size());
		for(int k = reversed.size() - 1; k >= 0; k --)
			result.add(reversed.get(k));
		return result;
	}

	/**
	 * Scans the raw diff and collapses adjacent DELETE/INSERT runs into
	 * MODIFIED entries.
	 * <p>
	 * Rationale: a maximal contiguous "hunk" of non-EQUAL entries usually
	 * represents lines that were edited in place, not independently removed
	 * and added — regardless of whether the DELETE and INSERT entries inside
	 * the hunk happen to come out delete-first or insert-first from the
	 * backtrack (both orderings occur depending on how ties are broken while
	 * walking the LCS table). Within a hunk, DELETE entries are always in
	 * ascending left-line order and INSERT entries are always in ascending
	 * right-line order (each subsequence walks its own array left to right),
	 * so pairing the k-th DELETE with the k-th INSERT is always a valid,
	 * order-preserving choice regardless of how they were interleaved in the
	 * raw diff. Any surplus lines on either side remain as plain DELETE or
	 * INSERT entries.
	 */
	private static List<DiffEntry> mergeModifications(final List<DiffEntry> rawDiff){
		final List<DiffEntry> merged = new ArrayList<>(rawDiff.size());
		int index = 0;
		final int size = rawDiff.size();
		while(index < size){
			final DiffEntry current = rawDiff.get(index);

			if(current.operation() == Operation.EQUAL){
				merged.add(current);
				index ++;

				continue;
			}

			// Collect the maximal contiguous run of non-EQUAL entries.
			int hunkEnd = index;
			while(hunkEnd < size && rawDiff.get(hunkEnd).operation() != Operation.EQUAL)
				hunkEnd ++;

			final List<DiffEntry> deletes = new ArrayList<>();
			final List<DiffEntry> inserts = new ArrayList<>();
			for(int k = index; k < hunkEnd; k ++){
				final DiffEntry entry = rawDiff.get(k);
				if(entry.operation() == Operation.DELETE)
					deletes.add(entry);
				else
					inserts.add(entry);
			}

			// Pair DELETE[k] with INSERT[k]. Keep them as MODIFIED only when
			// they look like two versions of the same line; otherwise keep
			// them as a plain deletion and a plain insertion.
			final int pairedCount = Math.min(deletes.size(), inserts.size());
			int k = 0;
			for(; k < pairedCount; k ++){
				final String leftLine = deletes.get(k).leftLine();
				final String rightLine = inserts.get(k).rightLine();
				if(areModifiedVersions(leftLine, rightLine))
					merged.add(new DiffEntry(Operation.MODIFIED, leftLine, rightLine));
				else{
					merged.add(deletes.get(k));
					merged.add(inserts.get(k));
				}
			}

			// Any leftover deletes / inserts stay on their own.
			for(int d = k; d < deletes.size(); d ++)
				merged.add(deletes.get(d));
			for(int i = k; i < inserts.size(); i ++)
				merged.add(inserts.get(i));

			index = hunkEnd;
		}
		return merged;
	}

	/**
	 * Decides whether two lines should be shown as MODIFIED rather than as
	 * a deletion plus an insertion.
	 * <p>
	 * Two heuristics are combined:
	 * <ol>
	 *   <li>same tag: the first non-whitespace token is identical. This is
	 *       the strong signal for FLEF data, where the tag is the field
	 *       name and a change of tag means a different field, not a
	 *       rewritten one;</li>
	 *   <li>content similarity: the character-level LCS ratio is above
	 *       {@link #SIMILARITY_THRESHOLD}. This catches tags that are
	 *       slightly reworded but whose content is clearly the same line.</li>
	 * </ol>
	 */
	private static boolean areModifiedVersions(final String leftLine, final String rightLine){
		if(leftLine == null || rightLine == null)
			return false;

		final String leftTag = firstToken(leftLine);
		final String rightTag = firstToken(rightLine);
		if(!leftTag.isEmpty() && leftTag.equals(rightTag))
			return true;

		return (similarity(leftLine, rightLine) >= SIMILARITY_THRESHOLD);
	}

	/** First run of non-whitespace characters, or the empty string. */
	private static String firstToken(final String line){
		int i = 0;
		while(i < line.length() && Character.isWhitespace(line.charAt(i)))
			i ++;
		final int start = i;
		while(i < line.length() && !Character.isWhitespace(line.charAt(i)))
			i ++;
		return line.substring(start, i);
	}

	/** Character-level similarity in [0, 1]: 1 = identical, 0 = disjoint. */
	private static double similarity(final String a, final String b){
		if(a.isEmpty() && b.isEmpty())
			return 1.;

		final int lcs = lcsLength(a, b);
		return 2. * lcs / (a.length() + b.length());
	}

	/** Length of the longest common subsequence of two strings. */
	private static int lcsLength(final String a, final String b){
		final int n = a.length(), m = b.length();
		int[] prev = new int[m + 1];
		int[] curr = new int[m + 1];
		for(int i = 1; i <= n; i ++){
			for(int j = 1; j <= m; j ++){
				if(a.charAt(i - 1) == b.charAt(j - 1))
					curr[j] = prev[j - 1] + 1;
				else
					curr[j] = Math.max(prev[j], curr[j - 1]);
			}
			final int[] tmp = prev;
			prev = curr;
			curr = tmp;
			java.util.Arrays.fill(curr, 0);
		}
		return prev[m];
	}

}
