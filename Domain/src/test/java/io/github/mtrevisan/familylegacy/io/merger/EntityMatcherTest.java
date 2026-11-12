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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


class EntityMatcherTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = EntityMatcherTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void anIndividualWithoutGivenNameStillHasBlockingKeys(){
		final EntityMatcher matcher = matcher(person("I1", "Giovanni", "Rossi") + birth("E1", "P1", "I1", "1850-06-15")
			+ person("I2", null, "Rossi") + birth("E2", "P2", "I2", "1851-03-02")
			+ person("I3", null, "Rossi")
			+ person("I4", null, "Rossi"));
		

		final Set<String> withoutGiven = matcher.blockingKeys(model.getRecordById("I2"));
		Assertions.assertFalse(withoutGiven.isEmpty());
		//it meets the individual with a given name (same surname, same birth decade)
		final Set<String> common = new HashSet<>(withoutGiven);
		common.retainAll(matcher.blockingKeys(model.getRecordById("I1")));
		Assertions.assertFalse(common.isEmpty());

		//without any date the surname alone is the key, shared by the records in the same condition
		final Set<String> undated = matcher.blockingKeys(model.getRecordById("I3"));
		Assertions.assertFalse(undated.isEmpty());
		Assertions.assertTrue(undated.containsAll(matcher.blockingKeys(model.getRecordById("I4"))));
	}


	private FLEFModel model;

	private EntityMatcher matcher(final String records){
		model = new FLEFParser().parse("header {\n\tdate 2026-01-01T00:00:00Z\n}\n" + records);
		final GrammarSupport support = new GrammarSupport(grammar);
		return new EntityMatcher(new ModelIndex(model.getRecords(), new ReferenceRewriter(support)), new RecordSignature(support), false);
	}

	private static String person(final String id, final String given, final String family){
		return "individual " + id + " {\n"
			+ "\tname {\n"
			+ (given != null? "\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + given + "\n\t\t}\n": "")
			+ "\t\tpart {\n\t\t\ttype family\n\t\t\tvalue " + family + "\n\t\t}\n"
			+ "\t}\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String birth(final String eventId, final String participationId, final String individualId, final String date){
		return "event " + eventId + " {\n"
			+ "\ttype birth\n"
			+ "\tdate.value.point {\n\t\tvalue.full_date " + date + "\n\t\tcalendar gregorian\n\t}\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n"
			+ "event_participation " + participationId + " {\n"
			+ "\tparticipant.individual " + individualId + "\n"
			+ "\tevent " + eventId + "\n"
			+ "\trole principal\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

}
