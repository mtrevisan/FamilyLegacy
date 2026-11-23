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
package io.github.mtrevisan.familylegacy.assistant.tags;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * What the free tags of the record the user is looking at say to the Research Assistant: for each tag of the record, the other records that
 * carry it, so that the researcher can go from one to the next (all the records of a "Brick Wall" or of a "DNA Candidate" cluster).
 * <p>
 * The tags are of the application, not of the file (see {@link RecordTags}); the assistant only reads them.
 */
public final class TagsChecker{

	/** How many of the other records are listed in a finding, at most. */
	static final int MAX_RECORDS = 50;
	static final String KIND = "tags.shared";


	private TagsChecker(){}


	/**
	 * @param model	The model, with its tags.
	 * @param recordId	The selected record; can be {@code null} or a record with no tags.
	 * @return	A finding for each tag of the record that other records carry too, none if nothing is selected.
	 */
	public static List<Diagnostic> findings(final FLEFModel model, final String recordId){
		if(model == null || recordId == null || !model.hasRecord(recordId))
			return List.of();

		final RecordTags tags = model.tags();
		final List<Diagnostic> findings = new ArrayList<>();
		for(final String tag : tags.get(recordId)){
			final List<String> others = tags.idsWithAll(List.of(tag))
				.stream()
				.filter(id -> !id.equals(recordId) && model.hasRecord(id))
				.sorted()
				.toList();
			if(others.isEmpty())
				continue;

			final List<String> shown = others.subList(0, Math.min(others.size(), MAX_RECORDS));
			findings.add(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
				I18N.tf("tags.shared.title", tag, others.size()), I18N.tf("tags.shared.explanation", tag, others.size()),
				shown, null, DiagnosticKeys.of(KIND, List.of(recordId), tag.toLowerCase(Locale.ROOT), others.size())));
		}
		return findings;
	}

}
