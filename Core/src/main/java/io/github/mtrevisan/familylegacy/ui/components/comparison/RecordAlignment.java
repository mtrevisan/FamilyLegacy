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
package io.github.mtrevisan.familylegacy.ui.components.comparison;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;


/**
 * How two records are put side by side to be compared, the same for every window that compares records (the review of a merge, the
 * comparison of two records): <b>a block of a side faces at most one block of the other</b>, at every depth, from the line that opens
 * it to the one that closes it, and is never interrupted by a second one.
 * <ul>
 *   <li>The sub-records are shown in the same order in both columns, whatever order they had (the order of the right column).</li>
 *   <li>A sub-record that both have and differs faces its counterpart; one that only a side has stands whole, facing empty lines.</li>
 *   <li>What a block cites weighs the most: blocks that cite the same record face each other, blocks that cite different records of a
 *       kind and none in common never do (two citations of two sources are two things).</li>
 *   <li>A reference to another record is a stronger difference than another text ({@link DiffKind#REFERENCE}).</li>
 * </ul>
 * It is only how it is shown: nothing here changes a record.
 */
public final class RecordAlignment{

	private RecordAlignment(){}


	/**
	 * The two records side by side, one row for each line.
	 *
	 * @param left	The record shown on the left, or {@code null} if it has none.
	 * @param right	The record shown on the right, or {@code null}.
	 * @param model	The file the records are in, to know what a reference is ({@code null}: there are no references).
	 */
	public static Rows align(final FLEFRecord left, final FLEFRecord right, final FLEFModel model){
		final Rows rows = new Rows();
		if(left != null && right != null)
			alignBlocks(left, right, 0, rows, model, singularFieldsOf(left.getTag()));
		else
			sideBySide(left, right, 0, rows);
		return rows;
	}

	/** No field is known to hold a single value. */
	private static final Predicate<String> NO_SINGULAR_FIELD = tag -> false;

	/**
	 * The fields of a kind of record that hold a single value, as the grammar says: of two records of the kind, the two values of such
	 * a field are the two versions of it (the preferred image), whatever they cite, and not two things.
	 *
	 * @param recordKind	The tag of the kind of record ({@code individual}).
	 * @return	Whether a field of the kind, by its tag, holds a single value.
	 */
	public static Predicate<String> singularFieldsOf(final String recordKind){
		final FLEFGrammar grammar = MergeGrammar.instance();
		if(grammar == null || recordKind == null)
			return NO_SINGULAR_FIELD;

		final RecordAbsorber absorber = new RecordAbsorber(grammar);
		return tag -> (absorber.fieldRole(recordKind, tag) == RecordAbsorber.FieldRole.SINGULAR);
	}

	/** The text of a block, a line each ended by a line feed: two blocks that are the same have the same text. */
	public static String serialize(final FLEFRecord record){
		final List<String> lines = new ArrayList<>();
		blockLines(record, 0, lines);
		return String.join(StringUtils.LF, lines) + StringUtils.LF;
	}

	/** The line that only opens or closes a group (<code>name &#123;</code> or <code>&#125;</code>): it is the shape of the text, not a difference. */
	public static boolean isDelimiter(final String line){
		final String trimmed = line.strip();
		return (trimmed.equals("}") || trimmed.endsWith("{"));
	}

	/** The lines of the text, the empty ones too, so that the lines of a column are the rows of the grid. */
	public static String[] linesOf(final String text){
		return (StringUtils.isEmpty(text)
			? new String[0]
			: StringUtils.splitPreserveAllTokens(StringUtils.removeEnd(text, StringUtils.LF), '\n'));
	}

	/** The mark of a line: an empty line (a {@code null}) is a filler, the lines that only open or close a group are never marked. */
	public static DiffKind markOf(final String line, final DiffKind kind){
		if(line == null)
			return DiffKind.FILLER;

		return (line.isBlank() || isDelimiter(line)? null: kind);
	}

	/**
	 * The two sides of a section, one row for each line, as they are shown side by side: a {@code null} line is an empty one, where a
	 * side has none, and the kinds say what each line is (a difference, a filler, or {@code null} for the one that is the same).
	 */
	public static final class Rows{

		public final List<String> source = new ArrayList<>();
		public final List<String> target = new ArrayList<>();
		public final List<DiffKind> sourceKind = new ArrayList<>();
		public final List<DiffKind> targetKind = new ArrayList<>();
		/** The blocks that differ, each as the first row, the last and the depth: what the buttons of a block act on. */
		public final List<int[]> spans = new ArrayList<>();


		public void add(final String sourceLine, final DiffKind sourceMark, final String targetLine, final DiffKind targetMark){
			source.add(sourceLine);
			target.add(targetLine);
			sourceKind.add(sourceMark);
			targetKind.add(targetMark);
		}

		public int size(){
			return source.size();
		}

	}

	/** The lines of the block, as {@link #serialize(FLEFRecord)} writes them, from the indentation of the depth. */
	public static void blockLines(final FLEFRecord record, final int depth, final List<String> lines){
		final String prefix = "  ".repeat(depth);
		final StringBuilder header = new StringBuilder(prefix).append(record.getTag() != null? record.getTag(): "?");
		if(record.getId() != null)
			header.append(' ').append(record.getId());
		if(record.getValue() != null)
			header.append(' ').append(record.getValue());
		if(!record.hasChildren()){
			lines.add(header.toString());
			return;
		}

		lines.add(header.append(" {").toString());
		for(final FLEFRecord child : record.getChildren())
			blockLines(child, depth + 1, lines);
		lines.add(prefix + "}");
	}

	/**
	 * A block of a side, or two, one beside the other, each whole and from its first line, the shorter with empty lines at the
	 * bottom: it is the one block that faces the other. Two are a conflict, one is from its side.
	 */
	public static void sideBySide(final FLEFRecord source, final FLEFRecord target, final int depth, final Rows rows){
		final int start = rows.size();
		final List<String> sourceLines = new ArrayList<>();
		if(source != null)
			blockLines(source, depth, sourceLines);
		final List<String> targetLines = new ArrayList<>();
		if(target != null)
			blockLines(target, depth, targetLines);

		final DiffKind sourceKind = (target != null? DiffKind.CONFLICT: DiffKind.FROM_SOURCE);
		final DiffKind targetKind = (source != null? DiffKind.CONFLICT: DiffKind.FROM_TARGET);
		for(int k = 0; k < Math.max(sourceLines.size(), targetLines.size()); k ++){
			final String sourceLine = (k < sourceLines.size()? sourceLines.get(k): null);
			final String targetLine = (k < targetLines.size()? targetLines.get(k): null);
			rows.add(sourceLine, markOf(sourceLine, sourceKind), targetLine, markOf(targetLine, targetKind));
		}
		if(depth > 0 && rows.size() > start)
			rows.spans.add(new int[]{start, rows.size() - 1, depth});
	}

	/**
	 * Aligns two blocks that are not the same, one against the other: a block of a side faces <b>at most one</b> block of the other,
	 * from the line that opens it to the line that closes it. Its sub-records are aligned in the same way: the ones that are the same
	 * are on the same rows, the ones that both have and differ face each other, and the ones that only a side has stand whole, facing
	 * empty lines. No block is ever interrupted by a block of the other side.
	 */
	public static void alignBlocks(final FLEFRecord source, final FLEFRecord target, final int depth, final Rows rows,
			final FLEFModel model){
		alignBlocks(source, target, depth, rows, model, NO_SINGULAR_FIELD);
	}

	/**
	 * As {@link #alignBlocks(FLEFRecord, FLEFRecord, int, Rows, FLEFModel)}, knowing which fields of the blocks hold a single value
	 * ({@code singular}, by tag): they are the two versions of one, whatever they cite.
	 */
	public static void alignBlocks(final FLEFRecord source, final FLEFRecord target, final int depth, final Rows rows,
			final FLEFModel model, final Predicate<String> singular){
		final int start = rows.size();
		final List<String> sourceLines = new ArrayList<>();
		blockLines(source, depth, sourceLines);
		final List<String> targetLines = new ArrayList<>();
		blockLines(target, depth, targetLines);

		if(!source.hasChildren() && !target.hasChildren()){
			final boolean same = sourceLines.get(0).equals(targetLines.get(0));
			//a reference to another record is a stronger difference than another text
			final DiffKind kind = (isReference(source, model) && isReference(target, model)? DiffKind.REFERENCE: DiffKind.CONFLICT);
			rows.add(sourceLines.get(0), (same? null: markOf(sourceLines.get(0), kind)), targetLines.get(0),
				(same? null: markOf(targetLines.get(0), kind)));
			if(!same && depth > 0)
				rows.spans.add(new int[]{start, start, depth});
			return;
		}
		if(source.hasChildren() != target.hasChildren()){
			sideBySide(source, target, depth, rows);
			return;
		}

		rows.add(sourceLines.get(0), null, targetLines.get(0), null);
		alignChildren(source.getChildren(), target.getChildren(), depth + 1, rows, model, singular);
		final String closing = "  ".repeat(depth) + "}";
		rows.add(closing, null, closing, null);
		if(depth > 0)
			rows.spans.add(new int[]{start, rows.size() - 1, depth});
	}

	/**
	 * The sub-records of two blocks, shown in the same order whatever their order was: the order of the target, with what only the
	 * source has after the sub-record that precedes it in the source. The sub-records that are the same, or are the same thing, face
	 * each other wherever they were. It is only how they are shown: the order the file keeps is not the one of the screen.
	 */
	private static void alignChildren(final List<FLEFRecord> source, final List<FLEFRecord> target, final int depth, final Rows rows,
			final FLEFModel model, final Predicate<String> singular){
		final int[] pairing = pairBlocks(source, target, model, false, singular);
		for(final int[] step : displayOrder(pairing, target.size())){
			final FLEFRecord sourceChild = (step[0] >= 0? source.get(step[0]): null);
			final FLEFRecord targetChild = (step[1] >= 0? target.get(step[1]): null);
			if(sourceChild == null || targetChild == null){
				sideBySide(sourceChild, targetChild, depth, rows);
				continue;
			}

			if(serialize(sourceChild).equals(serialize(targetChild))){
				final List<String> lines = new ArrayList<>();
				blockLines(sourceChild, depth, lines);
				for(final String line : lines)
					rows.add(line, null, line, null);
			}
			else
				alignBlocks(sourceChild, targetChild, depth, rows, model);
		}
	}

	/** Whether two blocks can be the same thing: they have the same tag and the same shape (a line, or a group). */
	private static boolean sameKind(final FLEFRecord first, final FLEFRecord second){
		return (Objects.equals(first.getTag(), second.getTag()) && first.hasChildren() == second.hasChildren());
	}

	/**
	 * Pairs the blocks of the source with the ones of the target, <b>whatever their order</b>: first the ones that are the same, then
	 * the ones that are the same thing (the ones that cite the same records, or have the most lines in common), the best first, each
	 * block once. With {@code any}, the ones that are left and that can be the same thing are paired in order, as the versions of one
	 * (the occurrences of a field).
	 * <p>
	 * The only block of its tag on each side is the same thing as the one of the other side: there is room for one, so they are the two
	 * versions of it (the crop of an image, the preferred image). Unless they cite different records, as two citations: those are two
	 * things, but not if the field has a single value ({@code singular}), where they are alternatives whatever they cite.
	 *
	 * @return	For each block of the source, the index of the one of the target, or -1.
	 */
	public static int[] pairBlocks(final List<FLEFRecord> source, final List<FLEFRecord> target, final FLEFModel model,
			final boolean any, final Predicate<String> singular){
		final int[] pairing = new int[source.size()];
		Arrays.fill(pairing, -1);
		final boolean[] used = new boolean[target.size()];

		final String[] targetText = new String[target.size()];
		for(int j = 0; j < targetText.length; j ++)
			targetText[j] = serialize(target.get(j));
		for(int i = 0; i < pairing.length; i ++){
			final String text = serialize(source.get(i));
			for(int j = 0; j < targetText.length; j ++)
				if(!used[j] && targetText[j].equals(text)){
					pairing[i] = j;
					used[j] = true;
					break;
				}
		}

		final List<int[]> candidates = new ArrayList<>();
		for(int i = 0; i < pairing.length; i ++)
			if(pairing[i] < 0)
				for(int j = 0; j < targetText.length; j ++)
					if(!used[j] && sameKind(source.get(i), target.get(j))){
						final int score = similarity(source.get(i), target.get(j), model);
						if(score > 0)
							candidates.add(new int[]{i, j, score});
					}
		candidates.sort((first, second) -> (first[2] != second[2]
			? Integer.compare(second[2], first[2])
			: (first[0] != second[0]? Integer.compare(first[0], second[0]): Integer.compare(first[1], second[1]))));
		for(final int[] candidate : candidates)
			if(pairing[candidate[0]] < 0 && !used[candidate[1]]){
				pairing[candidate[0]] = candidate[1];
				used[candidate[1]] = true;
			}

		if(any)
			for(int i = 0; i < pairing.length; i ++)
				if(pairing[i] < 0)
					for(int j = 0; j < targetText.length; j ++)
						if(!used[j] && sameKind(source.get(i), target.get(j)) && similarity(source.get(i), target.get(j), model) >= 0){
							pairing[i] = j;
							used[j] = true;
							break;
						}

		for(int i = 0; i < pairing.length; i ++){
			if(pairing[i] >= 0)
				continue;

			int sameInSource = 0;
			for(final FLEFRecord block : source)
				if(sameKind(block, source.get(i)))
					sameInSource ++;
			int sameInTarget = 0;
			int found = -1;
			for(int j = 0; j < targetText.length; j ++)
				if(sameKind(source.get(i), target.get(j))){
					sameInTarget ++;
					if(!used[j])
						found = j;
				}
			if(sameInSource == 1 && sameInTarget == 1 && found >= 0
					&& (singular.test(source.get(i).getTag())
					|| similarity(source.get(i), target.get(found), model) >= 0)){
				pairing[i] = found;
				used[found] = true;
			}
		}
		return pairing;
	}

	/**
	 * The order the blocks are shown in: the one of the target, a block of the source beside the one of the target it is paired with,
	 * and the blocks of the source that have no pair after the block of the source that precedes them (the ones before the first
	 * paired block come first).
	 *
	 * @return	The steps, as the index of the block of the source and the one of the target, or -1.
	 */
	public static List<int[]> displayOrder(final int[] pairing, final int targetCount){
		final int[] pairedWith = new int[targetCount];
		Arrays.fill(pairedWith, -1);
		final List<Integer> leading = new ArrayList<>();
		final Map<Integer, List<Integer>> following = new HashMap<>();
		int anchor = -1;
		for(int i = 0; i < pairing.length; i ++){
			if(pairing[i] >= 0){
				pairedWith[pairing[i]] = i;
				anchor = pairing[i];
			}
			else if(anchor < 0)
				leading.add(i);
			else
				following.computeIfAbsent(anchor, key -> new ArrayList<>()).add(i);
		}

		final List<int[]> steps = new ArrayList<>();
		for(final int i : leading)
			steps.add(new int[]{i, -1});
		for(int j = 0; j < targetCount; j ++){
			steps.add(new int[]{pairedWith[j], j});
			for(final int i : following.getOrDefault(j, List.of()))
				steps.add(new int[]{i, -1});
		}
		return steps;
	}

	/** What a reference outweighs a line of text: two blocks that cite the same record are the same thing, whatever else they say. */
	public static final int REFERENCE_WEIGHT = 100;

	/**
	 * Whether the line (a block with no sub-records) is a reference to a record of the file: its tag is the one of the record it names
	 * ({@code place P3}, {@code source S1}), as the grammar writes a reference.
	 */
	public static boolean isReference(final FLEFRecord record, final FLEFModel model){
		if(model == null || record.hasChildren() || record.getValue() == null)
			return false;

		final FLEFRecord referenced = model.getRecordById(record.getValue());
		return (referenced != null && Objects.equals(referenced.getTag(), record.getTag()));
	}

	/** The records a block cites, anywhere inside it, by the tag they are cited with. */
	private static Map<String, Set<String>> referencesOf(final FLEFRecord record, final FLEFModel model){
		final Map<String, Set<String>> references = new HashMap<>();
		collectReferences(record, model, references);
		return references;
	}

	private static void collectReferences(final FLEFRecord record, final FLEFModel model, final Map<String, Set<String>> references){
		if(isReference(record, model)){
			references.computeIfAbsent(record.getTag(), tag -> new HashSet<>()).add(record.getValue());
			return;
		}

		for(final FLEFRecord child : record.getChildren())
			collectReferences(child, model, references);
	}

	/**
	 * How much two blocks have in common, or -1 if they cannot be the same thing.
	 * <p>
	 * A record that both cite is worth much more than any text: two citations of the same source are the same citation even if the
	 * page differs. Two blocks that cite different records of a kind and no record in common are two different things, whatever the
	 * text they share (two citations of two sources are not the two versions of one). Otherwise it is the lines they share, apart
	 * from the ones that open or close a group. A line (a block with no sub-records) is a counterpart on its own.
	 */
	public static int similarity(final FLEFRecord first, final FLEFRecord second, final FLEFModel model){
		if(!first.hasChildren())
			return 1;

		final Map<String, Set<String>> firstReferences = referencesOf(first, model);
		final Map<String, Set<String>> secondReferences = referencesOf(second, model);
		int sharedReferences = 0;
		boolean different = false;
		for(final Map.Entry<String, Set<String>> entry : firstReferences.entrySet()){
			final Set<String> other = secondReferences.get(entry.getKey());
			if(other == null)
				continue;

			final Set<String> common = new HashSet<>(entry.getValue());
			common.retainAll(other);
			if(common.isEmpty())
				different = true;
			else
				sharedReferences += common.size();
		}
		if(different && sharedReferences == 0)
			return -1;

		final List<String> firstLines = new ArrayList<>();
		blockLines(first, 0, firstLines);
		final List<String> secondLines = new ArrayList<>();
		blockLines(second, 0, secondLines);
		final Map<String, Integer> available = new HashMap<>();
		for(final String line : secondLines)
			if(!isDelimiter(line))
				available.merge(line, 1, Integer::sum);

		int shared = 0;
		for(final String line : firstLines){
			final Integer left = available.get(line);
			if(!isDelimiter(line) && left != null && left > 0){
				available.put(line, left - 1);
				shared ++;
			}
		}
		return shared + REFERENCE_WEIGHT * sharedReferences;
	}

}
