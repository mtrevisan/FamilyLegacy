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
package io.github.mtrevisan.familylegacy.social;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class SocialNetworkServiceTest{

	private static final String AUDIT = "\taudit.creation.date 2026-01-01T00:00:00Z\n";


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = SocialNetworkServiceTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static String individual(final String id){
		return "individual " + id + " {\n" + AUDIT + "}\n";
	}

	private static String event(final String id){
		return "event " + id + " {\n\ttype death\n" + AUDIT + "}\n";
	}

	private static String participation(final String id, final String eventId, final String individualId, final String role){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual " + individualId + "\n\trole " + role
			+ "\n" + AUDIT + "}\n";
	}


	@Test
	void twoParticipantsOfAnEventAreConnectedByOneEdge(){
		//both are principal of a death, hence not living and public
		final FLEFModel model = new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			""" + individual("I1") + individual("I2") + event("E0") + event("E1")
			+ participation("EP0", "E0", "I1", "principal") + participation("EP00", "E0", "I2", "principal")
			+ participation("EP1", "E1", "I1", "witness") + participation("EP2", "E1", "I2", "officiant"));

		final SocialGraph graph = new SocialNetworkService(model).build("I1", SocialFilters.all(2));

		Assertions.assertEquals(2, graph.nodes().size());
		Assertions.assertEquals(1, graph.edgeCount());
		Assertions.assertEquals(1, graph.connectionCount("I1", "I2"));
	}

}
