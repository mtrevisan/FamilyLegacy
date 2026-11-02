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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;

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

	private static final String TAG_EVIDENCE = "evidence";


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

		for(final String type : SCANNED_TYPES){
			final List<FLEFRecord> records = PrivacyService.visible(model, model.getRecordsByType(type));
			for(final FLEFRecord record : records){
				final FLEFRecord qualifiers = FLEFRecordHelper.findChild(record, TAG_EVIDENCE);
				if(qualifiers == null){
					withoutQualifiers ++;
					continue;
				}
				withQualifiers ++;
				merge(sourceTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_SOURCE_TYPE));
				merge(informationTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_INFORMATION_TYPE));
				merge(evidenceTypes, ResearchHelper.firstTextValue(qualifiers, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE));
			}
		}

		new EvidenceQualifiersDialog(context.owner(), sourceTypes, informationTypes, evidenceTypes, withQualifiers,
			withoutQualifiers)
			.setVisible(true);
	}


	/**
	 * Record types that can carry an {@code evidence} block. The list is
	 * limited to the types where evidence qualifiers are meaningful in
	 * practice.
	 */
	private static final String[] SCANNED_TYPES = {
		"event", "individual_attribute", "group_attribute", "relationship",
		"place", "place_relationship", "event_participation", "source",
		"cultural_norm", "historic_event", "context_impact",
		"identity_hypothesis"
	};

	private static void merge(final Map<String, Integer> map, final String value){
		final String key = (value != null && !value.isBlank()? value: "(not set)");
		map.merge(key, 1, Integer::sum);
	}

}
