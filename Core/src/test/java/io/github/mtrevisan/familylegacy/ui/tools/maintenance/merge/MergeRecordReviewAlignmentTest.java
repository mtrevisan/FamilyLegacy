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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * The three columns of a section are aligned so that a block, at any depth, faces at most one block of the other column, from the
 * line that opens it to the one that closes it: no block is interrupted by a second one.
 */
class MergeRecordReviewAlignmentTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	private static FLEFRecord firstName(final FLEFModel model, final String id){
		for(final FLEFRecord child : model.getRecordById(id).getChildren())
			if("name".equals(child.getTag()))
				return child;
		throw new IllegalStateException(id);
	}

	private static String[] lines(final String text){
		final List<String> lines = new ArrayList<>();
		for(final String line : text.split("\n"))
			if(!line.isBlank())
				lines.add(line);
		return lines.toArray(new String[0]);
	}

	/** For each row, the blocks that contain it, from the outermost: the line that opens a group, its lines, and the one that closes it. */
	private static List<List<Integer>> chains(final List<String> column){
		final List<List<Integer>> chains = new ArrayList<>();
		final List<Integer> open = new ArrayList<>();
		int sequence = 0;
		for(final String line : column){
			if(line == null || line.isBlank()){
				chains.add(List.of());
				continue;
			}

			final String stripped = line.strip();
			final List<Integer> chain = new ArrayList<>(open);
			if(stripped.equals("}")){
				chains.add(chain);
				if(!open.isEmpty())
					open.remove(open.size() - 1);
			}
			else if(stripped.endsWith("{")){
				open.add(++ sequence);
				chains.add(new ArrayList<>(open));
			}
			else{
				chain.add(++ sequence);
				chains.add(chain);
			}
		}
		return chains;
	}

	/** Whether every block of the column faces, at its own depth, at most one block of the other one. */
	private static void assertFacesAtMostOne(final List<String> column, final List<String> other, final String what){
		final List<List<Integer>> chains = chains(column);
		final List<List<Integer>> otherChains = chains(other);
		final Set<Integer> done = new HashSet<>();
		for(final List<Integer> chain : chains)
			for(int depth = 0; depth < chain.size(); depth ++){
				final int block = chain.get(depth);
				if(!done.add(block))
					continue;

				int first = Integer.MAX_VALUE;
				int last = -1;
				for(int row = 0; row < chains.size(); row ++)
					if(chains.get(row).size() > depth && chains.get(row).get(depth) == block){
						first = Math.min(first, row);
						last = Math.max(last, row);
					}
				final Set<Integer> faced = new HashSet<>();
				for(int row = first; row <= last; row ++)
					if(otherChains.get(row).size() > depth)
						faced.add(otherChains.get(row).get(depth));
				Assertions.assertTrue(faced.size() <= 1,
					what + ": the block of the rows " + first + "-" + last + " (depth " + depth + ") faces " + faced.size() + " blocks");
			}
	}

	private static void check(final String sourceName, final String targetName, final String... results){
		final FLEFModel model = new FLEFParser().parse(HEADER + "individual I1 {\n" + targetName + "\n}\nindividual I2 {\n" + sourceName
			+ "\n}\n");
		final FLEFRecord source = firstName(model, "I2");
		final FLEFRecord target = firstName(model, "I1");
		final String sourceText = MergeFragments.serialize(source);
		final String targetText = MergeFragments.serialize(target);
		final List<String[]> variants = new ArrayList<>();
		variants.add(lines(targetText));
		variants.add(lines(sourceText));
		variants.add(lines(targetText + sourceText));
		variants.add(lines(sourceText + targetText));
		variants.add(new String[0]);
		for(final String result : results)
			variants.add(lines(result));

		for(final String[] result : variants){
			final List<List<String>> columns = MergeRecordReviewDialog.alignedColumns(source, target, result, null);
			Assertions.assertEquals(columns.get(0).size(), columns.get(1).size());
			Assertions.assertEquals(columns.get(0).size(), columns.get(2).size());
			assertFacesAtMostOne(columns.get(0), columns.get(2), "source|target");
			assertFacesAtMostOne(columns.get(2), columns.get(0), "target|source");
			assertFacesAtMostOne(columns.get(0), columns.get(1), "source|result");
			assertFacesAtMostOne(columns.get(1), columns.get(0), "result|source");
			assertFacesAtMostOne(columns.get(2), columns.get(1), "target|result");
			assertFacesAtMostOne(columns.get(1), columns.get(2), "result|target");
		}
	}

	private static String name(final String... parts){
		final StringBuilder sb = new StringBuilder("name {\n");
		for(final String part : parts)
			sb.append("part {\n").append(part).append("\n}\n");
		return sb.append("}").toString();
	}

	private static final String SOURCES = """
		source S1 {
			title A
		}
		source S2 {
			title B
		}
		""";

	/** A holder with a citation of a source, on each side of a merge, in a file where both sources exist. */
	private static List<List<String>> citations(final String sourceCitation, final String targetCitation){
		final FLEFModel model = new FLEFParser().parse(HEADER + SOURCES + "individual I1 {\nholder {\n" + targetCitation + "\n}\n}\n"
			+ "individual I2 {\nholder {\n" + sourceCitation + "\n}\n}\n");
		final FLEFRecord source = model.getRecordById("I2").getChildren().get(0);
		final FLEFRecord target = model.getRecordById("I1").getChildren().get(0);
		return MergeRecordReviewDialog.alignedColumns(source, target, new String[0], model);
	}

	private static boolean sameRow(final List<List<String>> columns, final String sourceFragment, final String targetFragment){
		for(int row = 0; row < columns.get(0).size(); row ++){
			final String sourceLine = columns.get(0).get(row);
			final String targetLine = columns.get(2).get(row);
			if(sourceLine != null && sourceLine.contains(sourceFragment) && targetLine != null && targetLine.contains(targetFragment))
				return true;
		}
		return false;
	}

	@Test
	void twoCitationsOfTheSameSourceFaceEachOtherWhateverElseTheySay(){
		final List<List<String>> columns = citations("citation {\nsource S1\npage 9\n}", "citation {\nsource S1\npage 3\n}");

		Assertions.assertTrue(sameRow(columns, "source S1", "source S1"), columns.toString());
		Assertions.assertTrue(sameRow(columns, "page 9", "page 3"), columns.toString());
	}

	@Test
	void twoCitationsOfDifferentSourcesAreTwoThingsAndNeverFaceEachOther(){
		final List<List<String>> columns = citations("citation {\nsource S1\npage 3\n}", "citation {\nsource S2\npage 3\n}");

		Assertions.assertFalse(sameRow(columns, "source S1", "source S2"), columns.toString());
		Assertions.assertFalse(sameRow(columns, "page 3", "page 3"), columns.toString());
		//each one stands whole: the source's citation faces empty lines, and so does the target's
		Assertions.assertTrue(columns.get(2).get(columns.get(0).indexOf("    source S1")) == null, columns.toString());
	}

	@Test
	void aSourceThatDiffersIsMoreThanATextThatDiffers(){
		//the same citation of a source and of another one: one leaf against another is a conflict of records, not of text
		final FLEFModel model = new FLEFParser().parse(HEADER + SOURCES + "individual I1 {\nholder {\nsource S2\n}\n}\n"
			+ "individual I2 {\nholder {\nsource S1\n}\n}\n");
		final FLEFRecord source = model.getRecordById("I2").getChildren().get(0);
		final FLEFRecord target = model.getRecordById("I1").getChildren().get(0);

		Assertions.assertEquals(1, MergeRecordReviewDialog.referenceConflicts(source, target, model));
		Assertions.assertEquals(0, MergeRecordReviewDialog.referenceConflicts(source, target, null));
	}

	/** Two holders with these blocks inside, one on each side of a merge, in a file where the two sources exist. */
	private static List<List<String>> holders(final String sourceBlocks, final String targetBlocks){
		final FLEFModel model = new FLEFParser().parse(HEADER + SOURCES + "individual I1 {\nholder {\n" + targetBlocks + "\n}\n}\n"
			+ "individual I2 {\nholder {\n" + sourceBlocks + "\n}\n}\n");
		final FLEFRecord source = model.getRecordById("I2").getChildren().get(0);
		final FLEFRecord target = model.getRecordById("I1").getChildren().get(0);
		return MergeRecordReviewDialog.alignedColumns(source, target, new String[0], model);
	}

	private static int rowOf(final List<String> column, final String fragment){
		for(int row = 0; row < column.size(); row ++)
			if(column.get(row) != null && column.get(row).contains(fragment))
				return row;
		return -1;
	}

	@Test
	void theSubBlocksAreShownInTheSameOrderWhateverTheOrderTheyHad(){
		//the source has the parts the other way round: they are shown as the target has them, each beside its own
		final List<List<String>> columns = holders("part {\ntype family\nvalue Rossa\n}\npart {\ntype given\nvalue Giovanni\n}",
			"part {\ntype given\nvalue Giovanni\n}\npart {\ntype family\nvalue Rossi\n}");

		Assertions.assertTrue(sameRow(columns, "value Giovanni", "value Giovanni"), columns.toString());
		Assertions.assertTrue(sameRow(columns, "value Rossa", "value Rossi"), columns.toString());
		Assertions.assertTrue(rowOf(columns.get(0), "type given") < rowOf(columns.get(0), "type family"), columns.get(0).toString());
		//nothing is shown twice: the opening line, two parts of four lines each, the closing line
		Assertions.assertEquals(10, columns.get(0).size(), columns.toString());
	}

	@Test
	void whatOnlyTheSourceHasStaysAfterTheBlockThatPrecedesItInTheSource(){
		final List<List<String>> columns = holders("part {\ntype given\nvalue A\n}\npart {\ntype nickname\nvalue X\n}\npart {\ntype family\nvalue B\n}",
			"part {\ntype family\nvalue B\n}\npart {\ntype given\nvalue A\n}");

		//in the order of the target (family, given), the nickname after the given that precedes it in the source
		Assertions.assertTrue(rowOf(columns.get(0), "value B") < rowOf(columns.get(0), "value A"), columns.get(0).toString());
		Assertions.assertTrue(rowOf(columns.get(0), "value A") < rowOf(columns.get(0), "value X"), columns.get(0).toString());
		Assertions.assertTrue(sameRow(columns, "value A", "value A"), columns.toString());
		Assertions.assertTrue(sameRow(columns, "value B", "value B"), columns.toString());
		Assertions.assertNull(columns.get(2).get(rowOf(columns.get(0), "value X")), columns.toString());
	}

	@Test
	void citationsOfTheSameSourcesFaceEachOtherInAnyOrder(){
		final List<List<String>> columns = holders("citation {\nsource S2\npage 8\n}\ncitation {\nsource S1\npage 7\n}",
			"citation {\nsource S1\npage 3\n}\ncitation {\nsource S2\npage 4\n}");

		Assertions.assertTrue(sameRow(columns, "page 7", "page 3"), columns.toString());
		Assertions.assertTrue(sameRow(columns, "page 8", "page 4"), columns.toString());
	}

	@Test
	void theOnlyBlockOfATagOnEachSideFacesTheOtherBlock(){
		//the crop of an image: one on each side, with nothing in common but its shape, they are the two versions of one
		final List<List<String>> columns = holders("crop {\nx 3\ny 4\n}", "crop {\nx 1\ny 2\n}");

		Assertions.assertTrue(sameRow(columns, "x 3", "x 1"), columns.toString());
		Assertions.assertTrue(sameRow(columns, "y 4", "y 2"), columns.toString());
		Assertions.assertEquals(6, columns.get(0).size(), columns.toString());
	}

	@Test
	void theOnlyCitationOfEachSideIsStillTwoThingsIfTheyCiteDifferentSources(){
		final List<List<String>> columns = holders("citation {\nsource S1\npage 3\n}", "citation {\nsource S2\npage 3\n}");

		Assertions.assertFalse(sameRow(columns, "source S1", "source S2"), columns.toString());
	}

	@Test
	void aPartThatOnlyTheSourceHasStandsFacingEmptyLines(){
		check(name("type given\nvalue Giovanni", "type nickname\nvalue Gianni", "type family\nvalue Rossa"),
			name("type given\nvalue Giovanni", "type family\nvalue Rossi"),
			//what a user may write: a value changed, a part dropped, a part added, a block that is not a block
			name("type given\nvalue Giovanni", "type nickname\nvalue Gianni", "type family\nvalue Rossone"),
			name("type family\nvalue Rossa"), name("type given\nvalue Giovanni", "type given\nvalue Gianni", "type family\nvalue Rossa", "type title\nvalue Sir"),
			"name {\npart {\ntype given", "name {\npart {\ntype given\nvalue X\n}\n}\nname {\npart {\ntype family\nvalue Y\n}\n}");
	}

	@Test
	void partsInAnotherOrderFaceEachOneOnlyOnce(){
		check(name("type family\nvalue Rossa", "type given\nvalue Giovanni"), name("type given\nvalue Giovanni", "type family\nvalue Rossi"));
	}

	@Test
	void severalPartsOnOneSideAndFewOnTheOther(){
		check(name("type given\nvalue A", "type given\nvalue B", "type given\nvalue C", "type family\nvalue Z"),
			name("type given\nvalue B", "type family\nvalue Y"), name("type given\nvalue Q\nvalue R", "type family\nvalue Y"));
	}

	@Test
	void deeperSubBlocksAreAlignedInTheSameWay(){
		check("name {\npart {\ntype given\nvalue A\nextra {\nk 1\nj 2\n}\nextra {\nk 3\n}\n}\npart {\ntype family\nvalue B\n}\n}",
			"name {\npart {\ntype given\nvalue A\nextra {\nk 3\n}\nextra {\nk 9\nj 8\n}\n}\n}",
			"name {\npart {\ntype given\nvalue A\nextra {\nk 3\nj 1\n}\nextra {\nk 7\n}\nextra {\nk 9\n}\n}\n}");
	}

	@Test
	void aBlockAgainstALineAndALineAgainstABlock(){
		check("name {\npart {\ntype given\nvalue A\n}\nnote hello\n}", "name {\npart given\nnote {\ntext hello\n}\n}");
	}

}
