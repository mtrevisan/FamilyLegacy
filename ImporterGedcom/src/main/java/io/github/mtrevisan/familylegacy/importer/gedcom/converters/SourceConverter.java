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
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Converts a GEDCOM SOURCE_RECORD (SOUR) into an FLEF SourceRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>TITL → title; ABBR → title of type {@code abbreviated}</li>
 *   <li>AUTH → author; DATA.AGNC (the responsible agency) → author</li>
 *   <li>PUBL → publisher</li>
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

	/**
	 * Converts a GEDCOM SOUR node into an FLEF SourceRecord.
	 *
	 * @param sourNode the GEDCOM node with tag "SOUR"
	 */
	public void convert(GEDCOMNode sourNode){
		String xref = sourNode.getXrefId();
		if(xref == null)
			return;

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
			source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_PUBLISHER, publication));

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
			final boolean dateFree = source.getChildren().stream().noneMatch(child -> SourceReader.TAG_DATE.equals(child.getTag()));
			if(dateFree)
				GEDCOMRecordBuilder.attachDate(source, SourceReader.TAG_DATE, period);
			if(period != null && (!dateFree || severalEntries))
				sb.append(", ").append(period);
			if(placNode != null && StringUtils.isNotBlank(placNode.getValue())){
				final boolean placeFree = source.getChildren().stream().noneMatch(child -> SourceReader.TAG_PLACE.equals(child.getTag()));
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
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(source, model, noteNode, noteRawMap);
		}
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(dataNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(source, model, noteNode, noteRawMap);
		}
		if(!eventsRecorded.isEmpty())
			source.addChild(GEDCOMRecordBuilder.createNoteStruct("Events recorded", String.join(StringUtils.LF, eventsRecorded), dataNode));
		source.addChild(GEDCOMRecordBuilder.createNoteStruct("Text from source",
			GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, "TEXT")), sourNode));

		// ---- What has no field in FLEF is kept as notes ----
		KeptNotes.keepIdentifiers(source, sourNode, StringUtils.EMPTY, sourNode);
		KeptNotes.keepUnmapped(source, sourNode, GEDCOMMapper.SOURCE_CONSUMED_TAGS, StringUtils.EMPTY, sourNode);

		// ---- Privacy (RESN) ----
		GEDCOMRecordBuilder.attachRestriction(source, sourNode);

		// ---- Audit ----
		source.addChild(AuditBuilder.build(sourNode));

		model.addRecord(source);
	}

}
