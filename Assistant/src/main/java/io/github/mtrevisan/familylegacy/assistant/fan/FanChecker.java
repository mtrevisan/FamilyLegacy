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
package io.github.mtrevisan.familylegacy.assistant.fan;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.social.FanEntry;
import io.github.mtrevisan.familylegacy.social.FanResult;
import io.github.mtrevisan.familylegacy.social.FanService;
import io.github.mtrevisan.familylegacy.social.FanSuggestion;
import io.github.mtrevisan.familylegacy.social.FanText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Turns the FAN table (see {@link FanService}) of the individual the user is looking at into findings of the Research Assistant: who
 * to search together with him, and the hints drawn from the people who appear with him. It only suggests: nothing is created or changed.
 * <p>
 * It is about one individual, the selected one: for the whole file it would be a finding for almost everybody who has a witness.
 * </p>
 */
public final class FanChecker{

	/** The most people named in the finding of the people to search together. */
	static final int MAX_NAMED = 6;


	private FanChecker(){}


	/**
	 * @param model        the model
	 * @param individualId the selected individual; can be {@code null} or not an individual
	 * @return the findings about the individual, none if nothing is selected or nobody appears with it
	 */
	static List<Diagnostic> findings(final FLEFModel model, final String individualId){
		if(model == null || individualId == null)
			return List.of();

		final FLEFRecord subject = model.getRecordById(individualId);
		if(subject == null || !IndividualReader.isOfType(subject))
			return List.of();

		final FanResult fan = new FanService(model).build(individualId);
		if(fan.isEmpty())
			return List.of();

		final String subjectName = IndividualReader.displayText(subject);
		final List<Diagnostic> findings = new ArrayList<>();

		final List<FanEntry> together = FanText.toSearchTogether(fan.entries(), MAX_NAMED);
		if(!together.isEmpty()){
			final List<String> recordIds = new ArrayList<>(List.of(individualId));
			together.forEach(entry -> recordIds.add(entry.personId()));
			final String names = together.stream()
				.map(entry -> entry.name() + " (" + FanText.rolePair(entry) + ")")
				.collect(Collectors.joining("; "));
			findings.add(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH,
				I18N.tf("fan.together.title", subjectName), I18N.tf("fan.together.explanation", subjectName, names),
				recordIds, null, DiagnosticKeys.of("fan.together", recordIds, together.size()))
				.withRank(together.get(0).score()));
		}

		for(final FanSuggestion suggestion : fan.suggestions()){
			final List<String> recordIds = new ArrayList<>(List.of(individualId));
			recordIds.addAll(suggestion.personIds());
			final String kind = FanText.kindOf(suggestion);
			findings.add(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RELATIONSHIPS,
				FanText.title(suggestion, subjectName, id -> label(fan, model, id)),
				FanText.explanation(suggestion, subjectName, id -> label(fan, model, id)),
				recordIds, null, DiagnosticKeys.of(kind, recordIds, suggestion.eventIds().size()))
				.withRank(suggestion.eventIds().size()));
		}
		return findings;
	}


	private static String label(final FanResult fan, final FLEFModel model, final String id){
		for(final FanEntry entry : fan.entries())
			if(entry.personId().equals(id))
				return entry.name();

		final FLEFRecord record = model.getRecordById(id);
		return (record != null? IndividualReader.displayText(record): id);
	}

}
