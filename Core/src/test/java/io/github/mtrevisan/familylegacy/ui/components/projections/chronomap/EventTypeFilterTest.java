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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.chronomap.ChronomapIndex.GeoAnchor;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GeoCoordinate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/** The filter of the chronomap by event type. */
class EventTypeFilterTest{

	private static final GeoAnchor BIRTH = anchor("event:birth");
	private static final GeoAnchor DEATH = anchor("event:death");
	private static final GeoAnchor RESIDENCE = anchor("attribute:residence");
	private static final List<GeoAnchor> ANCHORS = List.of(BIRTH, DEATH, RESIDENCE);


	@Test
	void nullKeepsEverything(){
		Assertions.assertSame(ANCHORS, ChronomapOverlayPainter.filterAnchors(ANCHORS, null));
	}

	@Test
	void onlyTheEnabledEventsAreKept(){
		Assertions.assertEquals(List.of(BIRTH, RESIDENCE), ChronomapOverlayPainter.filterAnchors(ANCHORS, Set.of(EventReader.ENUM_TYPE_BIRTH)));
		Assertions.assertEquals(List.of(DEATH, RESIDENCE), ChronomapOverlayPainter.filterAnchors(ANCHORS, Set.of(EventReader.ENUM_TYPE_DEATH)));
	}

	@Test
	void anEmptySetRemovesEveryEventButNotTheAttributes(){
		Assertions.assertEquals(List.of(RESIDENCE), ChronomapOverlayPainter.filterAnchors(ANCHORS, Set.of()));
	}

	@Test
	void anIndividualWithOnlyDisabledEventsHasNoAnchors(){
		Assertions.assertTrue(ChronomapOverlayPainter.filterAnchors(List.of(BIRTH, DEATH), Set.of()).isEmpty());
	}


	private static GeoAnchor anchor(final String kind){
		return new GeoAnchor("I1", 1_000_000L, 1_000_000L, new GeoCoordinate(45., 12., 10), "Place", kind, null);
	}

}
