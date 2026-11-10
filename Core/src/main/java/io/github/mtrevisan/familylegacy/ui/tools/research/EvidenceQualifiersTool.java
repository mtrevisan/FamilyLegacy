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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Report on the distribution of {@code EvidenceQualifiers} across the
 * model.
 * <p>
 * Three dimensions are counted, as defined by the protocol:
 * <ul>
 *   <li>{@code source_type} — original or derived;</li>
 *   <li>{@code information_type} — primary, secondary, or
 *       undetermined;</li>
 *   <li>{@code evidence_type} — direct, indirect, or negative.</li>
 * </ul>
 * The report also lists the records that carry no evidence qualifiers at
 * all, which is the most common gap in a model that has not been fully
 * assessed.
 * <p>
 * The scan is done in a single pass over every record type that can
 * carry an {@code evidence} block, and the qualifiers are read from the
 * block once per record. No O(n²) lookup is involved.
 */
public final class EvidenceQualifiersTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.research.evidence_qualifiers");
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();

		final Map<String, Integer> sourceTypes = new LinkedHashMap<>();
		final Map<String, Integer> informationTypes = new LinkedHashMap<>();
		final Map<String, Integer> evidenceTypes = new LinkedHashMap<>();
		int withQualifiers = 0;
		int withoutQualifiers = 0;
		final List<FLEFRecord> missing = new ArrayList<>();

		//the qualifiers belong to the citations (the source children of the records), not to the records themselves
		for(final String type : SourceHelper.allSourceCitingTypes()){
			for(final FLEFRecord record : PrivacyService.visible(model, model.getRecordsByType(type))){
				boolean unassessed = false;
				for(final FLEFRecord citation : record.getChildren()){
					if(!SourceReader.isOfType(citation))
						continue;

					final FLEFRecord qualifiers = FLEFRecordHelper.findChild(citation, EvidenceQualifiersReader.TAG_EVIDENCE);
					if(qualifiers == null){
						withoutQualifiers ++;
						unassessed = true;
						continue;
					}
					withQualifiers ++;
					merge(sourceTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_SOURCE_TYPE));
					merge(informationTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_INFORMATION_TYPE));
					merge(evidenceTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE));
				}
				if(unassessed)
					missing.add(record);
			}
		}

		new EvidenceQualifiersDialog(context.owner(), model, sourceTypes, informationTypes, evidenceTypes, withQualifiers, withoutQualifiers, missing)
			.setVisible(true);
	}


	private static void merge(final Map<String, Integer> map, final String value){
		final String key = (value != null && !value.isBlank()? value: I18N.t("text.evidence_qualifiers.not_set"));
		map.merge(key, 1, Integer::sum);
	}

}
