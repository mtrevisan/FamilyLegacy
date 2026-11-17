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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;

import java.util.ArrayList;
import java.util.List;


/** The context bands of a file: one per visible historic event and cultural norm that has a date, drawn behind the rows of a timeline. */
public final class TemporalContextBands{

	private final FLEFModel model;


	/**
	 * @param model	The file.
	 * @return	The bands of the visible historic events and cultural norms that have a date, in any order.
	 */
	public static List<TemporalContextBand> of(final FLEFModel model){
		return new TemporalContextBands(model).build();
	}


	private TemporalContextBands(final FLEFModel model){
		this.model = model;
	}


	private List<TemporalContextBand> build(){
		final List<TemporalContextBand> bands = new ArrayList<>();
		collectHistoricEventBands(bands);
		collectCulturalNormBands(bands);
		return bands;
	}

	private void collectHistoricEventBands(final List<TemporalContextBand> output){
		final List<FLEFRecord> historicEvents = PrivacyService.visible(model, model.getRecordsByType(HistoricEventReader.TYPE));
		for(final FLEFRecord historicEvent : historicEvents){
			final String id = historicEvent.getId();
			if(id == null)
				continue;
			final String title = FLEFRecordHelper.getChildValue(historicEvent, HistoricEventReader.TAG_TITLE);
			final String type = FLEFRecordHelper.getChildValue(historicEvent, HistoricEventReader.TAG_TYPE);
			final TemporalEntityRef ref = new TemporalEntityRef(
				TemporalEntityType.HISTORIC_EVENT, id, historicEvent,
				(title != null? title: type != null? type: id));

			final FLEFRecord date = FLEFRecordHelper.findChild(historicEvent, HistoricEventReader.TAG_DATE);
			final TemporalSpan span = DateNormalizer.normalize(date);
			if(span == null)
				continue;

			final FLEFRecord placeRef = FLEFRecordHelper.findChild(historicEvent, HistoricEventReader.TAG_PLACE);
			final FLEFRecord place = resolvePlace(placeRef);

			output.add(new TemporalContextBand(ref, span, title != null? title: type, type, place));
		}
	}

	private void collectCulturalNormBands(final List<TemporalContextBand> output){
		final List<FLEFRecord> culturalNorms = PrivacyService.visible(model, model.getRecordsByType(CulturalNormReader.TYPE));
		for(final FLEFRecord culturalNorm : culturalNorms){
			final String id = culturalNorm.getId();
			if(id == null)
				continue;

			final String title = FLEFRecordHelper.getChildValue(culturalNorm, CulturalNormReader.TAG_TITLE);
			final String ruleType = FLEFRecordHelper.getChildValue(culturalNorm, CulturalNormReader.TAG_TYPE);
			final TemporalEntityRef ref = new TemporalEntityRef(
				TemporalEntityType.CULTURAL_NORM, id, culturalNorm,
				(title != null? title: ruleType != null? ruleType: id));

			final FLEFRecord from = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_VALID_TO);
			final TemporalSpan span = DateNormalizer.combineBounds(from, to);
			if(span == null)
				continue;

			final FLEFRecord placeRef = FLEFRecordHelper.findChild(culturalNorm, CulturalNormReader.TAG_PLACE);
			final FLEFRecord place = resolvePlace(placeRef);

			output.add(new TemporalContextBand(ref, span, title != null? title: ruleType, ruleType, place));
		}
	}

	private FLEFRecord resolvePlace(final FLEFRecord placeCitation){
		if(placeCitation == null)
			return null;
		final FLEFRecord ref = FLEFRecordHelper.findChild(placeCitation, PlaceCitationReader.TAG_PLACE);
		if(ref == null)
			return null;
		final String id = ref.getValue();
		return (id != null? model.getRecordById(id): null);
	}

}
