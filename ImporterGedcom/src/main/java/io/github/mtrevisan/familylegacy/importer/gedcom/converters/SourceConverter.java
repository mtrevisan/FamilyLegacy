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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts a GEDCOM SOURCE_RECORD (SOUR) into an FLEF SourceRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>TITL → title; ABBR → title of type {@code abbreviated}</li>
 *   <li>AUTH → author; DATA.AGNC (the responsible agency) → author</li>
 *   <li>PUBL → publisher (and place and date, when written as "place : publisher, date")</li>
 *   <li>DATA.EVEN.DATE (the period covered) → date; DATA.EVEN.PLAC (the jurisdiction) → place; the events recorded and
 *   DATA.NOTE → notes</li>
 *   <li>TEXT → note</li>
 *   <li>REPO (CALN → locator, NOTE → remark) → repository; CALN.MEDI → media_type</li>
 *   <li>OBJE → document</li>
 *   <li>NOTE → note; RESN → privacy; CHAN → audit</li>
 * </ul>
 * REFN and RIN have no counterpart.
 * <p>
 * Repositories and multimedia records must be converted before.
 */
public class SourceConverter {

	/** A publication over some years (e.g. "1992-1993", "1992-93"). */
	private static final Pattern YEAR_RANGE = Pattern.compile("^(\\d{3,4})\\s*[-–]\\s*(\\d{1,4})$");
	/** The mark of a copyright or phonogram date (e.g. "c1992", "©1992", "cop. 1992", "p1992"). */
	private static final Pattern COPYRIGHT_MARK = Pattern.compile("^(?:©|℗|cop\\.?|copyright|c|p)\\s*(?=\\d)",
		Pattern.CASE_INSENSITIVE);

	private final FLEFModel model;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, FLEFRecord> sourceMap;
	private final PlaceCache placeCache;

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param sourceMap     map of source IDs to FLEF records
	 */
	public SourceConverter(FLEFModel model,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, FLEFRecord> sourceMap,
		PlaceCache placeCache){
		this.model = model;
		this.noteRawMap = noteRawMap;
		this.sourceMap = sourceMap;
		this.placeCache = placeCache;
	}

	/** What a publication says: where, by whom, and when the source was published (each one may be missing). */
	private record Publication(String place, String publisher, String date){}

	/**
	 * A note of the source that is only a date is the date the source was created (published); one that is "place : date" (or
	 * "date : place") gives its place and its date, and one that is "place : publisher, date", as the catalogues write the
	 * publication, gives its publisher too (see {@link #parsePublication(String, boolean)}).
	 * Nothing is done if the note says anything else, or the source has already a date (or a place), or another publisher.
	 *
	 * @return	Whether the note was used (and so it is not a note of the source any more).
	 */
	private boolean consumeDateAndPlace(final FLEFRecord source, final GEDCOMNode noteNode){
		final String noteId = GEDCOMHelper.pointer(noteNode.getValue());
		final String text = StringUtils.trimToNull(GEDCOMHelper.extractFullText(noteId != null && noteRawMap != null
			? noteRawMap.get(noteId): noteNode));
		final Publication publication = parsePublication(text, false);
		return (publication != null && applyPublication(source, publication, noteNode.getLevel()));
	}

	/**
	 * The publication facts of the source (PUBL): written as the catalogues do, "place : publisher, date" (e.g.
	 * {@code Treviso : Canova, 1992}), they give the place, the publisher and the date of the source; otherwise, or if the source
	 * has already a date or a place, the whole text is the publisher.
	 */
	private void attachPublication(final FLEFRecord source, final String text, final int level){
		final Publication publication = parsePublication(text, true);
		if(publication == null || !applyPublication(source, publication, level))
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_PUBLISHER, text));
	}

	/**
	 * Reads a publication as the catalogues write it: "date", "place : date" (or "date : place"), "place : publisher, date"; for
	 * the field of the publication (PUBL) also "place : publisher" and "publisher, date". The date may be written as the catalogues
	 * do (see {@link #publicationDate(String)}).
	 *
	 * @param text	The text.
	 * @param publicationField	Whether the text is the field of the publication (and so its parts are publication facts).
	 * @return	The publication, or {@code null} if the text is not written so.
	 */
	private static Publication parsePublication(final String text, final boolean publicationField){
		if(StringUtils.isBlank(text))
			return null;

		final String onlyDate = publicationDate(text);
		if(onlyDate != null)
			return new Publication(null, null, onlyDate);

		final int colon = text.indexOf(':');
		if(colon < 0){
			//"publisher, date"
			final Publication publisherAndDate = (publicationField? publisherAndDate(null, text): null);
			return (publisherAndDate != null && publisherAndDate.date() != null? publisherAndDate: null);
		}
		if(colon == 0 || colon != text.lastIndexOf(':'))
			return null;

		final String before = StringUtils.trimToNull(text.substring(0, colon));
		final String after = StringUtils.trimToNull(text.substring(colon + 1));
		if(before == null || after == null)
			return null;

		final String afterDate = publicationDate(after);
		final String beforeDate = publicationDate(before);
		if(afterDate != null && beforeDate == null)
			return new Publication(before, null, afterDate);
		if(beforeDate != null)
			return (afterDate == null? new Publication(after, null, beforeDate): null);

		//"place : publisher, date"
		final Publication publisherAndDate = publisherAndDate(before, after);
		if(publisherAndDate != null && publisherAndDate.date() != null)
			return publisherAndDate;
		//"place : publisher"
		return (publicationField? new Publication(before, after, null): null);
	}

	/** "publisher, date" (the date after the last comma), at the given place; {@code null} if the text is not written so. */
	private static Publication publisherAndDate(final String place, final String text){
		final int comma = text.lastIndexOf(',');
		if(comma < 1)
			return null;

		final String publisher = StringUtils.trimToNull(text.substring(0, comma));
		final String date = publicationDate(text.substring(comma + 1));
		return (publisher != null && date != null && publicationDate(publisher) == null
			? new Publication(place, publisher, date)
			: null);
	}

	/**
	 * Gives the source the place, the publisher and the date of a publication, if it has none of them already (the same publisher
	 * is not a conflict).
	 *
	 * @return	Whether the publication was given to the source.
	 */
	private boolean applyPublication(final FLEFRecord source, final Publication publication, final int level){
		if(publication.date() != null && !hasNoChild(source, SourceReader.TAG_DATE)
				|| publication.place() != null && !hasNoChild(source, SourceReader.TAG_PLACE))
			return false;

		final String existingPublisher = FLEFRecordHelper.getChildValue(source, SourceReader.TAG_PUBLISHER);
		if(publication.publisher() != null && existingPublisher != null && !existingPublisher.equalsIgnoreCase(publication.publisher()))
			return false;

		if(publication.publisher() != null && existingPublisher == null)
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_PUBLISHER, publication.publisher()));
		if(publication.date() != null)
			GEDCOMRecordBuilder.attachDate(source, SourceReader.TAG_DATE, publication.date());
		if(publication.place() != null)
			GEDCOMRecordBuilder.attachPlaceCitation(source, model, new GEDCOMNode(level, GEDCOMMapper.TAG_PLACE, publication.place()), null,
				noteRawMap, placeCache);
		return true;
	}

	private static boolean hasNoChild(final FLEFRecord record, final String tag){
		return record.getChildren().stream()
			.noneMatch(child -> tag.equals(child.getTag()));
	}

	/**
	 * The date of a publication, as GEDCOM writes it, from the way the catalogues write it: a final period is dropped
	 * ({@code 1992.}), a date supplied by the cataloguer is in brackets ({@code [1992]}, {@code [1992?]}), a copyright or phonogram
	 * date has its mark ({@code c1992}, {@code ©1992}, {@code cop. 1992}, {@code p1992}), and a publication over some years is a
	 * range ({@code 1992-1993}, {@code 1992-93}).
	 *
	 * @param text	The text.
	 * @return	The date, or {@code null} if the text is not a date.
	 */
	private static String publicationDate(final String text){
		String date = StringUtils.trimToNull(StringUtils.removeEnd(StringUtils.trimToEmpty(text), "."));
		if(date == null)
			return null;

		if(date.length() > 2 && date.charAt(0) == '[' && date.charAt(date.length() - 1) == ']')
			date = date.substring(1, date.length() - 1)
				.trim();
		date = COPYRIGHT_MARK.matcher(date)
			.replaceFirst(StringUtils.EMPTY);
		final Matcher range = YEAR_RANGE.matcher(date);
		if(range.matches()){
			final String start = range.group(1);
			String end = range.group(2);
			//the end may be written with its last digits only
			if(end.length() < start.length())
				end = start.substring(0, start.length() - end.length()) + end;
			date = "FROM " + start + " TO " + end;
		}
		return (isDate(date)? date: null);
	}

	/** Whether the text is a GEDCOM date: it is understood as such and has a year (a bare small number is not one). */
	private static boolean isDate(final String text){
		int run = 0;
		boolean year = false;
		for(int i = 0, length = text.length(); i < length; i ++){
			run = (Character.isDigit(text.charAt(i))? run + 1: 0);
			year |= (run >= 3);
		}
		return (year && GEDCOMDateConverter.buildDateStructure(SourceReader.TAG_DATE, text) != null);
	}

	/**
	 * Converts a GEDCOM SOUR node into an FLEF SourceRecord.
	 *
	 * @param sourNode the GEDCOM node with tag "SOUR"
	 */
	public void convert(GEDCOMNode sourNode){
		String xref = sourNode.getXrefId();
		if(xref == null){
			GEDCOMImportContext.discardUnidentified(sourNode);
			return;
		}

		IDGenerator.registerExistingId(xref);

		FLEFRecord source = FLEFRecord.createMainRecord(xref, SourceReader.TYPE);
		sourceMap.put(xref, source);
		GEDCOMImportContext.setRecord(xref);

		// ---- 1. TITL and ABBR (title+) ----
		final String title = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, GEDCOMMapper.TAG_TITLE)));
		final String abbreviation = GEDCOMHelper.findFirstValue(sourNode, "ABBR");
		if(title != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, title)));
		if(abbreviation != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_TYPE_ABBREVIATED))
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, abbreviation)));
		if(title == null && abbreviation == null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, "Source " + xref)));

		// ---- 2. AUTH (author), DATA.AGNC (responsible agency) ----
		final GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(sourNode, "DATA");
		final String author = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, "AUTH")));
		if(author != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_AUTHOR)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_TEXT, author)));
		final String agency = GEDCOMHelper.findFirstValue(dataNode, "AGNC");
		if(agency != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_AUTHOR)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_TEXT, agency)));

		// ---- 3. PUBL (publication facts) ----
		final String publication = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode,
			"PUBL")));
		if(publication != null)
			attachPublication(source, publication, sourNode.getLevel() + 1);

		// ---- 4. DATA.EVEN (events recorded, with the period covered and the jurisdiction) ----
		final List<String> eventsRecorded = new ArrayList<>();
		final Set<String> coveredEventTypes = new HashSet<>();
		final List<GEDCOMNode> evenNodes = GEDCOMHelper.findChildren(dataNode, GEDCOMMapper.TAG_GENERIC_EVENT);
		//with more than one entry the date and the place of each one are in the note (the first ones are also the date and the place of
		//the source), to tell which entry they belong to
		final boolean severalEntries = (evenNodes.size() > 1);
		for(GEDCOMNode evenNode : evenNodes){
			final StringBuilder sb = new StringBuilder(Objects.toString(StringUtils.trimToNull(evenNode.getValue()),
				"?"));
			final String period = GEDCOMHelper.findFirstValue(evenNode, GEDCOMMapper.TAG_DATE);
			final GEDCOMNode placNode = GEDCOMHelper.findFirstChild(evenNode, GEDCOMMapper.TAG_PLACE);
			final boolean dateFree = source.getChildren().stream()
				.noneMatch(child -> SourceReader.TAG_DATE.equals(child.getTag()));
			if(dateFree)
				GEDCOMRecordBuilder.attachDate(source, SourceReader.TAG_DATE, period);
			if(period != null && (!dateFree || severalEntries))
				sb.append(", ").append(period);
			if(placNode != null && StringUtils.isNotBlank(placNode.getValue())){
				final boolean placeFree = source.getChildren().stream()
					.noneMatch(child -> SourceReader.TAG_PLACE.equals(child.getTag()));
				if(placeFree)
					GEDCOMRecordBuilder.attachPlaceCitation(source, model, placNode, null, noteRawMap, placeCache);
				if(!placeFree || severalEntries)
					sb.append(", ").append(placNode.getValue().trim());
			}
			eventsRecorded.add(sb.toString());
			coveredEventTypes.addAll(GEDCOMRecordBuilder.mapEventTypes(evenNode.getValue()));
		}

		GEDCOMRecordBuilder.registerSourceEventTypes(xref, coveredEventTypes);

		// ---- 5. REPO (repository citation) with CALN, MEDI, NOTE ----
		String mediaType = null;
		final List<FLEFRecord> repositoryCitations = new ArrayList<>();
		for(GEDCOMNode repoNode : GEDCOMHelper.findChildren(sourNode, GEDCOMMapper.TAG_REPOSITORY)){
			String repositoryId = GEDCOMHelper.pointer(repoNode.getValue());
			if(repositoryId == null || !model.hasRecord(repositoryId)){
				// a repository not recorded (or described in the citation itself)
				final String description = (repositoryId == null? StringUtils.trimToNull(repoNode.getValue()): null);
				repositoryId = (repositoryId != null? repositoryId: IDGenerator.nextId(RepositoryReader.ID_PREFIX));
				model.addRecord(FLEFRecord.createMainRecord(repositoryId, RepositoryReader.TYPE)
					.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
						.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE,
							(description != null? description: "Repository " + repositoryId))))
					.addChild(AuditBuilder.build(sourNode)));
			}
			final FLEFRecord repositoryCitation = FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
				.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REPOSITORY, repositoryId));

			// 5a. CALN (call number) with optional MEDI
			final List<String> callNumbers = new ArrayList<>();
			for(GEDCOMNode calnNode : GEDCOMHelper.findChildren(repoNode, "CALN")){
				if(StringUtils.isNotBlank(calnNode.getValue()))
					callNumbers.add(calnNode.getValue().trim());
				final String media = GEDCOMHelper.findFirstValue(calnNode, "MEDI");
				if(mediaType == null && media != null)
					mediaType = GEDCOMMapper.mapMediaType(media);
			}
			if(!callNumbers.isEmpty())
				repositoryCitation.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_LOCATOR, String.join("; ", callNumbers)));

			// 5b. NOTE inside REPO
			for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, GEDCOMMapper.TAG_NOTE)){
				final String noteId = GEDCOMHelper.pointer(noteNode.getValue());
				final String text = GEDCOMHelper.extractFullText(noteId != null? noteRawMap.get(noteId): noteNode);
				if(StringUtils.isNotBlank(text))
					repositoryCitation.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REMARK, text.trim()));
			}

			repositoryCitations.add(repositoryCitation);
		}
		mediaType = GEDCOMMapper.toTypeToken(mediaType);
		if(mediaType != null)
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA_TYPE, mediaType));
		for(final FLEFRecord repositoryCitation : repositoryCitations)
			source.addChild(repositoryCitation);

		// ---- 6. Multimedia (OBJE) ----
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(sourNode, GEDCOMMapper.TAG_MULTIMEDIA)){
			GEDCOMRecordBuilder.attachMultimediaLink(source, model, multimediaLinkNode, noteRawMap);
		}

		// ---- 7. Notes (NOTE, DATA.NOTE, events recorded, TEXT) – inline structs ----
		//a note that is only a date, or "place : date", is the date (and the place) the source was created; the date may be written
		//in a way GEDCOM does not know (e.g. "15 gennaio 1953"): the parsers of the calendars read it
		final List<GEDCOMNode> noteNodes = new ArrayList<>(GEDCOMHelper.findChildren(sourNode, GEDCOMMapper.TAG_NOTE));
		noteNodes.addAll(GEDCOMHelper.findChildren(dataNode, GEDCOMMapper.TAG_NOTE));
		for(final GEDCOMNode noteNode : noteNodes)
			if(!consumeDateAndPlace(source, noteNode))
				GEDCOMRecordBuilder.attachNote(source, model, noteNode, noteRawMap);
		if(!eventsRecorded.isEmpty())
			source.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.events_recorded"), String.join(StringUtils.LF, eventsRecorded), dataNode));

		// ---- What has no field in FLEF is kept as notes ----
		KeptNotes.keepIdentifiers(source, sourNode, StringUtils.EMPTY, sourNode);
		KeptNotes.keepUnmapped(source, sourNode, GEDCOMMapper.SOURCE_CONSUMED_TAGS, StringUtils.EMPTY, sourNode);

		// ---- Privacy (RESN) ----
		GEDCOMRecordBuilder.attachRestriction(source, sourNode);

		// ---- Audit ----
		source.addChild(AuditBuilder.build(sourNode));

		model.addRecord(source);

		// ---- Text from source (TEXT): a verbatim extract of the source ----
		for(final GEDCOMNode textNode : GEDCOMHelper.findChildren(sourNode, GEDCOMMapper.TAG_TEXT))
			addTextExtract(source.getId(), textNode, sourNode);
	}

	/**
	 * The text of a source (a verbatim copy of what it says) is an extract of the source, that the citations of the source can
	 * reference.
	 */
	private void addTextExtract(final String sourceId, final GEDCOMNode textNode, final GEDCOMNode sourNode){
		final String text = GEDCOMHelper.extractFullText(textNode);
		if(StringUtils.isBlank(text))
			return;

		final FLEFRecord extract = FLEFRecord.createMainRecord(IDGenerator.nextId(ExtractReader.ID_PREFIX), ExtractReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_TEXT, text.trim()))
			.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_TYPE, ExtractReader.ENUM_TYPE_VERBATIM));
		// GEDCOM 7 states the language of the text
		final String locale = GEDCOMHelper.findFirstValue(textNode, GEDCOMMapper.TAG_LANGUAGE);
		if(locale != null)
			extract.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_LOCALE, locale));
		extract.addChild(AuditBuilder.build(sourNode));
		model.addRecord(extract);
	}

}
