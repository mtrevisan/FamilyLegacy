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

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * The ends of a path are the entities it goes from and to, whatever the orientation of its edges.
 */
class SocialPathTest{

	@Test
	void endsAreNotTheEdgeEndpointsWhenEdgesAreWalkedBackwards(){
		final TemporalEntityRef a = entity("I1");
		final TemporalEntityRef b = entity("I2");
		final TemporalEntityRef c = entity("I3");
		//both edges start from B: the path A - B - C walks the first one from its target
		final SocialPath path = SocialPath.of(List.of(edge("R1", b, a), edge("R2", b, c)));

		Assertions.assertEquals(a, path.start());
		Assertions.assertEquals(c, path.end());
		Assertions.assertEquals(List.of(a, b, c), path.nodes());
	}


	private static TemporalEntityRef entity(final String id){
		return TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, FLEFRecord.createMainRecord(id, IndividualReader.TYPE), id);
	}

	private static SocialEdgeRef edge(final String id, final TemporalEntityRef source, final TemporalEntityRef target){
		return new SocialEdgeRef(source, target, "friend", null, SocialRelationCategory.OTHER, SocialEdgeDirection.UNDIRECTED,
			TemporalSpan.unbounded(), FLEFRecord.createMainRecord(id, "relationship"));
	}

}
