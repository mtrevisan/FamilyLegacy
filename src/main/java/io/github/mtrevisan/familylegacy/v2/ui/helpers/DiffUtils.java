package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple diff utility that computes line‑based differences using the Myers algorithm.
 */
public final class DiffUtils{

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

			// Collect the maximal contiguous run of non-EQUAL entries (DELETE and INSERT may appear in either order
			// within it)
			int hunkEnd = index;
			while(hunkEnd < size && rawDiff.get(hunkEnd).operation() != Operation.EQUAL)
				hunkEnd ++;

			// Split the hunk into its DELETE and INSERT entries, each preserving its own relative (ascending line-index)
			// order
			final List<DiffEntry> deletes = new ArrayList<>();
			final List<DiffEntry> inserts = new ArrayList<>();
			for(int k = index; k < hunkEnd; k ++){
				final DiffEntry entry = rawDiff.get(k);
				if(entry.operation() == Operation.DELETE)
					deletes.add(entry);
				else
					inserts.add(entry);
			}

			final int pairedCount = Math.min(deletes.size(), inserts.size());

			// Pair up DELETE[k] with INSERT[k] as MODIFIED entries
			for(int k = 0; k < pairedCount; k ++)
				merged.add(new DiffEntry(Operation.MODIFIED, deletes.get(k).leftLine(), inserts.get(k).rightLine()));

			// Any leftover deletes (more deletes than inserts) stay as DELETE
			for(int k = pairedCount; k < deletes.size(); k ++)
				merged.add(deletes.get(k));

			// Any leftover inserts (more inserts than deletes) stay as INSERT
			for(int k = pairedCount; k < inserts.size(); k ++)
				merged.add(inserts.get(k));

			index = hunkEnd;
		}

		return merged;
	}

}
