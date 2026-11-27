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
package io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.layout;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.tree.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.Map;


/** The tree takes the columns its ancestors need, not the ones of a complete pedigree. */
class TreeLayoutWidthTest{

	private static TreeNode node(final String id, final int generation){
		return new TreeNode(FLEFRecord.createMainRecord(id, "INDIVIDUAL"), null, generation);
	}

	private static int width(final TreeNode root, final int maxDepth){
		final Map<TreeNode, Integer> widths = new IdentityHashMap<>();
		return TreeLayoutBuilder.width(root, 0, maxDepth, widths);
	}


	@Test
	void aPersonWithNoAncestorsTakesOneColumn(){
		Assertions.assertEquals(1, width(node("I1", 0), 3));
		Assertions.assertEquals(0, TreeLayoutBuilder.width(null, 0, 3, new IdentityHashMap<>()));
	}

	@Test
	void aCompletePedigreeTakesTheColumnsOfItsLastGeneration(){
		//I1 <- (F, M), F <- (FF, FM), M <- (MF, MM): four people in the last generation
		final TreeNode root = node("I1", 0);
		final TreeNode father = node("F", 1);
		final TreeNode mother = node("M", 1);
		root.setFather(father);
		root.setMother(mother);
		father.setFather(node("FF", 2));
		father.setMother(node("FM", 2));
		mother.setFather(node("MF", 2));
		mother.setMother(node("MM", 2));

		Assertions.assertEquals(4, width(root, 2));
	}

	@Test
	void aBranchThatStopsEarlyLeavesNoHole(){
		//the same pedigree, with only the father of the father known: three columns would be 4 in the old layout (2^2)
		final TreeNode root = node("I1", 0);
		final TreeNode father = node("F", 1);
		final TreeNode mother = node("M", 1);
		root.setFather(father);
		root.setMother(mother);
		father.setFather(node("FF", 2));

		//the father takes the column of his father; the mother, with no ancestors, one: two columns, not four
		Assertions.assertEquals(2, width(root, 2));
	}

	@Test
	void theColumnsAreNotMoreThanTheDepthAllows(){
		final TreeNode root = node("I1", 0);
		final TreeNode father = node("F", 1);
		root.setFather(father);
		father.setFather(node("FF", 2));
		father.setMother(node("FM", 2));

		//nothing is drawn beyond the last generation
		Assertions.assertEquals(1, width(root, 1));
		Assertions.assertEquals(2, width(root, 2));
	}

	@Test
	void aSinglePersonWithOneParentIsCenteredOnThatParent(){
		final TreeNode root = node("I1", 0);
		final TreeNode father = node("F", 1);
		root.setFather(father);
		father.setFather(node("FF", 2));
		father.setMother(node("FM", 2));
		father.getFather()
			.setFather(node("FFF", 3));

		//the lone root takes the columns of its only parent: two (FF takes the column of FFF, FM one)
		Assertions.assertEquals(2, width(root, 3));
	}

}
