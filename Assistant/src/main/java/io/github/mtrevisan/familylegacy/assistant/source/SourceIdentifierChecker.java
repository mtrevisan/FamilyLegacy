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
import io.github.mtrevisan.familylegacy.assistant.Sentences;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Turns the identifiers of the source the user is looking at into findings of the Research Assistant, keeping apart what the data model
 * keeps apart: the identifiers of the source itself (ISBN, ISSN, DOI, URL, microfilm number), the identifiers of its repository
 * citations (shelf mark, call number, classification number) and the locator (the position inside the source).
 * <ul>
 *   <li><b>an identifier that fails the check of its scheme</b> (the check digit of an ISBN or an ISSN, the syntax of a URL): a warning,
 *   as in the validation of the file;</li>
 *   <li><b>a value that looks like another scheme</b> (a DOI written as a URL, an ISBN with a free-text scheme): information;</li>
 *   <li><b>a locator that holds an identifier</b> (a shelf mark, a call number or a film number written in the locator of the repository
 *   citation, where only the page, the entry, the folio or the frame belong): information;</li>
 *   <li><b>a source of a kind that is normally published, without any identifier</b>; <b>a repository citation without an identifier</b>
 *   (how to find the source in the repository);</li>
 *   <li><b>the microfilms of FamilySearch</b>: how to look a film up, and a hint to record the film number when the source is kept at
 *   FamilySearch.</li>
 * </ul>
 * It only suggests: nothing is created or changed.
 */
public final class SourceIdentifierChecker{

	/** The kinds of source that normally have an identifier of their own when they are published. */
	private static final Set<String> PUBLISHED_KINDS = Set.of(SourceReader.ENUM_KIND_BOOK, SourceReader.ENUM_KIND_PERIODICAL,
		SourceReader.ENUM_KIND_COMPILATION, SourceReader.ENUM_KIND_WEBSITE);
	/** The kinds of source that are kept as registers, and often filmed. */
	private static final Set<String> REGISTER_KINDS = Set.of(SourceReader.ENUM_KIND_CHURCH_REGISTER, SourceReader.ENUM_KIND_CIVIL_REGISTRATION,
		SourceReader.ENUM_KIND_CENSUS, SourceReader.ENUM_KIND_DEED, SourceReader.ENUM_KIND_WILL_PROBATE, SourceReader.ENUM_KIND_COURT_RECORD,
		SourceReader.ENUM_KIND_MILITARY_RECORD, SourceReader.ENUM_KIND_SCHOOL_RECORD, SourceReader.ENUM_KIND_IMMIGRATION,
		SourceReader.ENUM_KIND_TAX_ROLL);
	/** The schemes that a value is recognized as, when it is written with another. */
	private static final Set<String> RECOGNIZED_SCHEMES = Set.of(IdentifierReader.ENUM_SCHEME_ISBN, IdentifierReader.ENUM_SCHEME_ISSN,
		IdentifierReader.ENUM_SCHEME_DOI);
	private static final String[] FAMILYSEARCH_NAMES = {"familysearch", "family history library", "genealogical society of utah"};
	/** What separates the parts of a locator. */
	private static final String LOCATOR_SEPARATORS = ",;";


	private SourceIdentifierChecker(){}


	/**
	 * @param model    the model
	 * @param sourceId the selected record; can be {@code null} or not a source
	 * @return the findings about the source, none if nothing is selected or there is nothing to suggest
	 */
	public static List<Diagnostic> findings(final FLEFModel model, final String sourceId){
		if(model == null || sourceId == null)
			return List.of();

		final FLEFRecord source = model.getRecordById(sourceId);
		if(source == null || !SourceReader.isOfType(source))
			return List.of();

		final String name = SourceReader.displayText(source);
		final List<Diagnostic> findings = new ArrayList<>(0);
		final List<FLEFRecord> sourceIdentifiers = IdentifierReader.extractIdentifiers(source);
		for(final FLEFRecord identifier : sourceIdentifiers)
			checkIdentifier(findings, sourceId, name, identifier, false);

		boolean repositoryIdentifiers = false;
		boolean filmRecorded = hasScheme(sourceIdentifiers, IdentifierReader.ENUM_SCHEME_FHL_FILM)
			|| hasScheme(sourceIdentifiers, IdentifierReader.ENUM_SCHEME_FHL_DIGITAL_FOLDER);
		String familySearchRepository = null;
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_REPOSITORY)){
			final List<FLEFRecord> identifiers = RepositoryCitationReader.extractIdentifiers(citation);
			repositoryIdentifiers |= !identifiers.isEmpty();
			for(final FLEFRecord identifier : identifiers)
				checkIdentifier(findings, sourceId, name, identifier, true);
			filmRecorded |= hasScheme(identifiers, IdentifierReader.ENUM_SCHEME_FHL_FILM)
				|| hasScheme(identifiers, IdentifierReader.ENUM_SCHEME_FHL_DIGITAL_FOLDER);

			final String repositoryName = repositoryName(model, citation);
			if(identifiers.isEmpty())
				findings.add(finding(Diagnostic.Severity.INFORMATION, "source_identifier.repository_without_identifier", sourceId,
					List.of(name, repositoryName), repositoryName));
			checkLocator(findings, sourceId, name, citation);
			if(familySearchRepository == null && isFamilySearch(repositoryName))
				familySearchRepository = repositoryName;
		}

		checkFilms(findings, sourceId, name, source);
		final String kind = StringUtils.trimToEmpty(SourceReader.extractKind(source)).toLowerCase(Locale.ROOT);
		if(PUBLISHED_KINDS.contains(kind) && sourceIdentifiers.isEmpty() && !repositoryIdentifiers)
			findings.add(finding(Diagnostic.Severity.INFORMATION,
				(SourceReader.ENUM_KIND_WEBSITE.equals(kind)? "source_identifier.missing.website": "source_identifier.missing"), sourceId,
				List.of(name, I18N.t("enum.source.kind." + kind, kind)), kind));
		if(REGISTER_KINDS.contains(kind) && familySearchRepository != null && !filmRecorded)
			findings.add(finding(Diagnostic.Severity.INFORMATION, "source_identifier.fhl_missing", sourceId,
				List.of(name, familySearchRepository), familySearchRepository));
		return findings;
	}


	private static void checkIdentifier(final List<Diagnostic> findings, final String sourceId, final String name,
			final FLEFRecord identifier, final boolean inRepository){
		final String scheme = StringUtils.trimToEmpty(IdentifierReader.extractScheme(identifier));
		final String value = StringUtils.trimToEmpty(IdentifierReader.extractValue(identifier));
		if(value.isEmpty())
			return;

		final String schemeLabel = I18N.t("enum.identifier.scheme." + scheme.toLowerCase(Locale.ROOT), scheme);
		if(IdentifierReader.validity(scheme, value) == IdentifierReader.Validity.INVALID){
			findings.add(finding(Diagnostic.Severity.WARNING, "source_identifier.invalid", sourceId, List.of(name, schemeLabel, value,
				I18N.t(IdentifierReader.ENUM_SCHEME_URL.equalsIgnoreCase(scheme)? "text.identifier.invalid.url"
					: "text.identifier.invalid.check_digit")), scheme, value));
			return;
		}

		final String inferred = IdentifierReader.inferScheme(value);
		if(inferred != null && RECOGNIZED_SCHEMES.contains(inferred) && !inferred.equalsIgnoreCase(scheme))
			findings.add(finding(Diagnostic.Severity.INFORMATION, "source_identifier.scheme_mismatch" + (inRepository? ".repository": ""),
				sourceId, List.of(name, schemeLabel, I18N.t("enum.identifier.scheme." + inferred, inferred), value), scheme, value));
	}

	/**
	 * A locator is a position inside the source: when what it holds is a shelf mark, a call number or a film number, it identifies the
	 * item in the repository, and is an identifier of the repository citation.
	 */
	private static void checkLocator(final List<Diagnostic> findings, final String sourceId, final String name, final FLEFRecord citation){
		final String locator = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR));
		if(locator == null)
			return;

		final List<String> identifiers = new ArrayList<>(0);
		for(final String part : StringUtils.split(locator, LOCATOR_SEPARATORS)){
			final int colon = part.indexOf(':');
			if(colon > 0 && IdentifierReader.schemeOfLabel(part.substring(0, colon)) != null)
				identifiers.add(part.strip());
		}
		if(!identifiers.isEmpty())
			findings.add(finding(Diagnostic.Severity.INFORMATION, "source_identifier.locator_holds_identifier", sourceId,
				List.of(name, Sentences.list(identifiers)), locator));
	}

	/** The microfilms of FamilySearch: how to look the film up. */
	private static void checkFilms(final List<Diagnostic> findings, final String sourceId, final String name,
			final FLEFRecord source){
		final List<String> films = new ArrayList<>(IdentifierReader.extractValues(source, IdentifierReader.ENUM_SCHEME_FHL_FILM));
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_REPOSITORY))
			for(final String film : IdentifierReader.extractValues(citation, IdentifierReader.ENUM_SCHEME_FHL_FILM))
				if(!films.contains(film))
					films.add(film);
		for(final String film : films)
			findings.add(finding(Diagnostic.Severity.INFORMATION, "source_identifier.film", sourceId, List.of(name, film), film));
	}

	private static boolean hasScheme(final List<FLEFRecord> identifiers, final String scheme){
		for(final FLEFRecord identifier : identifiers)
			if(scheme.equalsIgnoreCase(StringUtils.trim(IdentifierReader.extractScheme(identifier)))
					&& StringUtils.isNotBlank(IdentifierReader.extractValue(identifier)))
				return true;
		return false;
	}

	private static String repositoryName(final FLEFModel model, final FLEFRecord citation){
		final String repositoryId = RepositoryCitationReader.extractRepository(citation);
		final FLEFRecord repository = (repositoryId != null? model.getRecordById(repositoryId): null);
		final List<String> names = (repository != null? RepositoryReader.extractNames(repository): List.of());
		return (!names.isEmpty()? names.getFirst(): StringUtils.defaultString(repositoryId));
	}

	private static boolean isFamilySearch(final String repositoryName){
		final String lower = repositoryName.toLowerCase(Locale.ROOT);
		for(final String marker : FAMILYSEARCH_NAMES)
			if(lower.contains(marker))
				return true;
		return false;
	}

	/** @return the finding: its title and text are the messages {@code <kind>.title} and {@code <kind>.text} with the arguments */
	private static Diagnostic finding(final Diagnostic.Severity severity, final String kind, final String sourceId,
			final List<String> arguments, final Object... keyData){
		final String messageKind = StringUtils.removeEnd(StringUtils.removeEnd(kind, ".repository"), ".website");
		final Object[] data = arguments.toArray();
		return new Diagnostic(severity, AssistantCategory.SOURCES, I18N.tf(messageKind + ".title", data), I18N.tf(kind + ".text", data),
			List.of(sourceId), null, DiagnosticKeys.of(messageKind, List.of(sourceId), keyData));
	}

}
