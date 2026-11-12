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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class TemporalProjectionLayoutTest{

	@Test
	void equalEntriesShareTheSameLane(){
		final TemporalEntry a = entry(100, 200, "birth");
		final TemporalEntry sameAsA = entry(100, 200, "birth");
		final TemporalEntry overlapping = entry(150, 250, "move");

		final TemporalProjectionLayout.TrackLayout track = layoutOf(a, sameAsA, overlapping);

		//two distinct entries that overlap need two lanes; the duplicate of the first must not add a third
		Assertions.assertEquals(2, track.laneCount());
		Assertions.assertEquals(2, track.entryLane().size());
		Assertions.assertEquals(0, track.entryLane().get(a));
		Assertions.assertEquals(1, track.entryLane().get(overlapping));
	}

	@Test
	void entriesThatDoNotOverlapShareLaneZero(){
		final TemporalEntry first = entry(100, 110, "a");
		final TemporalEntry second = entry(200, 210, "b");

		final TemporalProjectionLayout.TrackLayout track = layoutOf(first, second);

		Assertions.assertEquals(1, track.laneCount());
	}


	private static TemporalProjectionLayout.TrackLayout layoutOf(final TemporalEntry... entries){
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		final TemporalEntityRef entity = TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, individual, "Someone");
		final TemporalTrack track = new TemporalTrack(TemporalTrackType.EVENT, List.of(entries), "events");
		final TemporalRow row = new TemporalRow(entity, List.of(track), 0, false);

		final TemporalProjectionLayout layout = new TemporalProjectionLayout();
		layout.compute(TemporalProjectionModel.of(List.of(row), List.of(), List.of(), List.of()));

		return layout.rows().getFirst().tracks().get(TemporalTrackType.EVENT);
	}

	private static TemporalEntry entry(final long fromJdn, final long toJdn, final String label){
		final NormalizedDate from = NormalizedDate.exact(fromJdn, DatePrecision.DAY);
		final NormalizedDate to = NormalizedDate.exact(toJdn, DatePrecision.DAY);
		return new TemporalEntry(TemporalSpan.spanning(from, to), label, "event", null, null, null);
	}

}
