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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * What an extract says about the conclusions: the assertions whose citations reference it, those it supports and those it contradicts.
 * It is the way from the source material to the conclusions (extract -> citation -> assertion -> individual), the reverse of the
 * evidence trace of an assertion.
 * <p>
 * It is read from the model and never changes it; the records hidden by the privacy policy are left out.
 *
 * @param extract	The extract.
 * @param source	The source the extract belongs to ({@code null} if it says none or it is not in the model).
 * @param supports	The citations that reference the extract and back their assertion.
 * @param contradicts	The citations that reference the extract and contradict their assertion.
 */
public record ExtractEvidence(FLEFRecord extract, FLEFRecord source, List<SourceWorkspace.Citation> supports,
		List<SourceWorkspace.Citation> contradicts){

	/**
	 * @param model	The model.
	 * @param extractId	The identifier of the extract.
	 * @return	The evidence of the extract; {@code null} if there is no such extract or it is hidden by the privacy policy.
	 */
	public static ExtractEvidence of(final FLEFModel model, final String extractId){
		final FLEFRecord extract = (extractId != null? model.getRecordById(extractId): null);
		if(!ExtractReader.isOfType(extract) || PrivacyService.isHidden(model, extract))
			return null;

		final Map<String, List<String>> participantsOfEvent = SourceWorkspace.participantsOfEvents(model);
		final List<SourceWorkspace.Citation> supports = new ArrayList<>();
		final List<SourceWorkspace.Citation> contradicts = new ArrayList<>();
		for(final String type : SourceHelper.allSourceCitingTypes())
			for(final FLEFRecord owner : PrivacyService.visible(model, model.getRecordsByType(type)))
				for(final FLEFRecord citation : owner.getChildren()){
					if(!SourceReader.isOfType(citation))
						continue;

					final List<FLEFRecord> extracts = ExtractReader.extractsOf(citation, model);
					if(extracts.stream().noneMatch(e -> extractId.equals(e.getId())))
						continue;

					final String stance = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE);
					final SourceWorkspace.Citation entry = new SourceWorkspace.Citation(owner,
						StringUtils.defaultString(FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR)),
						(stance != null? stance: SourceCitationReader.STANCE_SUPPORTS), extracts,
						SourceWorkspace.individualsOf(model, owner, participantsOfEvent));
					(entry.contradicts()? contradicts: supports).add(entry);
				}

		final String sourceId = ExtractReader.extractSource(extract);
		final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);
		return new ExtractEvidence(extract, source, List.copyOf(supports), List.copyOf(contradicts));
	}

}
