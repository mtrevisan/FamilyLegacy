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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The partners offered as the other parent of a new child. */
class PartnersOfTest{

	private static String person(final String id){
		return "individual " + id + " {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + id + "\n\t\t}\n\t}\n}\n";
	}

	private static String rel(final String id, final String type, final String subject, final String target){
		return "relationship " + id + " {\n\tsubject.individual " + subject + "\n\ttarget.individual " + target + "\n\ttype " + type + "\n}\n";
	}

	private static FLEFModel model(final String relationships){
		return new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""" + person("A") + person("B") + person("C") + person("D") + person("K") + relationships);
	}


	@Test
	void partnersOfEitherSideOfTheRelationship(){
		final FLEFModel model = model(rel("R1", "spouse", "A", "B") + rel("R2", "spouse", "C", "A"));

		final List<String> ids = AbstractRelativeTool.partnersOf("A", "K", model).stream().map(FLEFRecord::getId).sorted().toList();

		Assertions.assertEquals(List.of("B", "C"), ids);
	}

	@Test
	void noPartnersMeansNothingToAsk(){
		final FLEFModel model = model(rel("R1", "biological_child", "K", "A"));

		Assertions.assertTrue(AbstractRelativeTool.partnersOf("A", "K", model).isEmpty());
	}

	@Test
	void theChildItselfIsNotAnOption(){
		final FLEFModel model = model(rel("R1", "spouse", "A", "K") + rel("R2", "spouse", "A", "D") + rel("R3", "spouse", "D", "A"));

		final List<String> ids = AbstractRelativeTool.partnersOf("A", "K", model).stream().map(FLEFRecord::getId).toList();

		Assertions.assertEquals(List.of("D"), ids);
	}

}
