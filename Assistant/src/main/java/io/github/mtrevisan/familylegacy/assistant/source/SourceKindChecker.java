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
package io.github.mtrevisan.familylegacy.assistant.source;

import io.github.mtrevisan.familylegacy.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Turns the kind of the source the user is looking at ({@code SourceRecord.kind}: what the source is, not its physical form) into a finding
 * of the Research Assistant: what a source of that kind is good for (a church register for births, marriages and burials, a census for
 * age, residence, household and occupation, a military record for the service timeline...). It only suggests: nothing is created or changed.
 * <p>
 * It is about one source, the selected one, and only if it has a kind with something to say. A kind written as free text is not advised on.
 * </p>
 */
public final class SourceKindChecker{

	/** The kinds of the vocabulary the assistant has advice for (each one has its text in the messages: {@code source_kind.advice.<kind>}). */
	public static final List<String> ADVISED = List.of(SourceReader.ENUM_KIND_CIVIL_REGISTRATION, SourceReader.ENUM_KIND_CHURCH_REGISTER,
		SourceReader.ENUM_KIND_CENSUS, SourceReader.ENUM_KIND_DEED, SourceReader.ENUM_KIND_WILL_PROBATE, SourceReader.ENUM_KIND_COURT_RECORD,
		SourceReader.ENUM_KIND_MILITARY_RECORD, SourceReader.ENUM_KIND_SCHOOL_RECORD, SourceReader.ENUM_KIND_IMMIGRATION,
		SourceReader.ENUM_KIND_TAX_ROLL, SourceReader.ENUM_KIND_DIRECTORY, SourceReader.ENUM_KIND_NEWSPAPER,
		SourceReader.ENUM_KIND_CEMETERY_RECORD, SourceReader.ENUM_KIND_CORRESPONDENCE, SourceReader.ENUM_KIND_ORAL,
		SourceReader.ENUM_KIND_COMPILATION);

	private static final String KIND_PREFIX = "source_kind.advice.";


	private SourceKindChecker(){}


	/**
	 * @param model    the model
	 * @param sourceId the selected record; can be {@code null} or not a source
	 * @return the findings about the source, none if nothing is selected, the source has no kind or there is nothing to suggest for it
	 */
	public static List<Diagnostic> findings(final FLEFModel model, final String sourceId){
		if(model == null || sourceId == null)
			return List.of();

		final FLEFRecord source = model.getRecordById(sourceId);
		if(source == null || !SourceReader.isOfType(source))
			return List.of();

		final String kind = StringUtils.trimToNull(SourceReader.extractKind(source));
		if(kind == null || !ADVISED.contains(kind))
			return List.of();
		//a compilation that already says what it comes from has nothing more to look for
		if(SourceReader.ENUM_KIND_COMPILATION.equals(kind) && !SourceReader.extractDerivedFromIds(source).isEmpty())
			return List.of();

		final String name = SourceReader.displayText(source);
		final String kindLabel = I18N.t("enum.source.kind." + kind, kind);
		return List.of(new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.SOURCES,
			I18N.tf("source_kind.title", name, kindLabel), I18N.t(KIND_PREFIX + kind),
			List.of(sourceId), null, DiagnosticKeys.of("source_kind." + kind, List.of(sourceId))));
	}

}
