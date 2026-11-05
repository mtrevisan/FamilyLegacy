package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.chronomap.ChronomapIndex.GeoAnchor;
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
		Assertions.assertEquals(List.of(BIRTH, RESIDENCE), ChronomapOverlayPainter.filterAnchors(ANCHORS, Set.of("birth")));
		Assertions.assertEquals(List.of(DEATH, RESIDENCE), ChronomapOverlayPainter.filterAnchors(ANCHORS, Set.of("death")));
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
