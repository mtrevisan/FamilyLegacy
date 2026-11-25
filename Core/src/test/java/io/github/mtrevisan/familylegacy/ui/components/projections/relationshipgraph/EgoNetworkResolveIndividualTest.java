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
package io.github.mtrevisan.familylegacy.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeLayout;
import io.github.mtrevisan.familylegacy.ui.components.projections.groupnetwork.GroupNetworkTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** Switching from the ego network to the family tree: a selected group must become an individual, never an empty person. */
class EgoNetworkResolveIndividualTest{

	private static EgoNetworkPanel panel(){
		final FLEFModel model = GroupNetworkTest.model();
		return new EgoNetworkPanel(TreeLayout.VERTICAL, new GenealogyRepository(new String[0], model), model);
	}


	@Test
	void anIndividualStaysAsItIs(){
		Assertions.assertEquals("I2", panel().resolveIndividualId("I2"));
	}

	@Test
	void aGroupBecomesItsFirstMember(){
		//the Club has Anna and Bruno as members
		Assertions.assertEquals("I1", panel().resolveIndividualId("G2"));
	}

	@Test
	void aGroupWithoutIndividualsGivesNothing(){
		//the Federation only contains the Club: the caller keeps the previous root
		Assertions.assertNull(panel().resolveIndividualId("G1"));
	}

	@Test
	void anUnknownOrMissingIdGivesNothing(){
		final EgoNetworkPanel panel = panel();
		Assertions.assertNull(panel.resolveIndividualId(null));
		Assertions.assertNull(panel.resolveIndividualId("X99"));
	}

}
