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
package io.github.mtrevisan.familylegacy.gedcom.converters;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

		FLEFRecord source = FLEFRecord.createMainRecord(xref, SourceHandler.TYPE);
		sourceMap.put(xref, source);

		// ---- 1. TITL and ABBR (title+) ----
		final String title = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, "TITL")));
		final String abbreviation = GEDCOMHelper.findFirstValue(sourNode, "ABBR");
		if(title != null)
			source.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", title)));
		if(abbreviation != null)
			source.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(FLEFRecord.createChildWithTagAndValue("type", "abbreviated"))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", abbreviation)));
		if(title == null && abbreviation == null)
			source.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Source " + xref)));

		// ---- 2. AUTH (author), DATA.AGNC (responsible agency) ----
		final GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(sourNode, "DATA");
		final String author = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, "AUTH")));
		if(author != null)
			source.addChild(FLEFRecord.createChildWithTag("author")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", author)));
		final String agency = GEDCOMHelper.findFirstValue(dataNode, "AGNC");
		if(agency != null)
			source.addChild(FLEFRecord.createChildWithTag("author")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", agency)));

		// ---- 3. PUBL (publication facts) ----
		final String publication = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode,
			"PUBL")));
		if(publication != null)
			source.addChild(FLEFRecord.createChildWithTagAndValue("publisher", publication));

		// ---- 4. DATA.EVEN (events recorded, with the period covered and the jurisdiction) ----
		final List<String> eventsRecorded = new ArrayList<>();
		for(GEDCOMNode evenNode : GEDCOMHelper.findChildren(dataNode, "EVEN")){
			final StringBuilder sb = new StringBuilder(StringUtils.defaultString(StringUtils.trimToNull(evenNode.getValue()),
				"?"));
			final String period = GEDCOMHelper.findFirstValue(evenNode, "DATE");
			final GEDCOMNode placNode = GEDCOMHelper.findFirstChild(evenNode, "PLAC");
			if(source.getChildren().stream().noneMatch(child -> "date".equals(child.getTag())))
				GEDCOMHelper.attachDate(source, "date", period);
			else if(period != null)
				sb.append(", ").append(period);
			if(placNode != null && StringUtils.isNotBlank(placNode.getValue())){
				if(source.getChildren().stream().noneMatch(child -> "place".equals(child.getTag())))
					GEDCOMHelper.attachPlaceCitation(source, model, placNode, null, noteRawMap, placeCache);
				else
					sb.append(", ").append(placNode.getValue().trim());
			}
			eventsRecorded.add(sb.toString());
		}

		// ---- 5. REPO (repository citation) with CALN, MEDI, NOTE ----
		String mediaType = null;
		final List<FLEFRecord> repositoryCitations = new ArrayList<>();
		for(GEDCOMNode repoNode : GEDCOMHelper.findChildren(sourNode, "REPO")){
			String repositoryId = GEDCOMHelper.pointer(repoNode.getValue());
			if(repositoryId == null || !model.hasRecord(repositoryId)){
				// a repository not recorded (or described in the citation itself)
				final String description = (repositoryId == null? StringUtils.trimToNull(repoNode.getValue()): null);
				repositoryId = (repositoryId != null? repositoryId: IDGenerator.nextId(RepositoryHandler.ID_PREFIX));
				model.addRecord(FLEFRecord.createMainRecord(repositoryId, RepositoryHandler.TYPE)
					.addChild(FLEFRecord.createChildWithTag("name")
						.addChild(FLEFRecord.createChildWithTagAndValue("value",
							(description != null? description: "Repository " + repositoryId))))
					.addChild(AuditBuilder.build(sourNode)));
			}
			final FLEFRecord repositoryCitation = FLEFRecord.createChildWithTag("repository")
				.addChild(FLEFRecord.createChildWithTagAndValue("repository", repositoryId));

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
				repositoryCitation.addChild(FLEFRecord.createChildWithTagAndValue("locator", String.join("; ", callNumbers)));

			// 5b. NOTE inside REPO
			for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, "NOTE")){
				final String noteId = GEDCOMHelper.pointer(noteNode.getValue());
				final String text = GEDCOMHelper.extractFullText(noteId != null? noteRawMap.get(noteId): noteNode);
				if(StringUtils.isNotBlank(text))
					repositoryCitation.addChild(FLEFRecord.createChildWithTagAndValue("remark", text.trim()));
			}

			repositoryCitations.add(repositoryCitation);
		}
		mediaType = GEDCOMMapper.toTypeToken(mediaType);
		if(mediaType != null)
			source.addChild(FLEFRecord.createChildWithTagAndValue("media_type", mediaType));
		for(final FLEFRecord repositoryCitation : repositoryCitations)
			source.addChild(repositoryCitation);

		// ---- 6. Multimedia (OBJE) ----
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(sourNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(source, model, multimediaLinkNode, noteRawMap);
		}

		// ---- 7. Notes (NOTE, DATA.NOTE, events recorded, TEXT) – inline structs ----
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, "NOTE")){
			GEDCOMHelper.attachNote(source, noteNode, noteRawMap);
		}
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(dataNode, "NOTE")){
			GEDCOMHelper.attachNote(source, noteNode, noteRawMap);
		}
		if(!eventsRecorded.isEmpty())
			source.addChild(GEDCOMHelper.createNoteStruct("Events recorded", String.join("\n", eventsRecorded), dataNode));
		source.addChild(GEDCOMHelper.createNoteStruct("Text from source",
			GEDCOMHelper.extractFullText(GEDCOMHelper.findFirstChild(sourNode, "TEXT")), sourNode));

		// ---- Privacy (RESN) ----
		GEDCOMHelper.attachRestriction(source, sourNode);

		// ---- Audit ----
		source.addChild(AuditBuilder.build(sourNode));

		model.addRecord(source);
	}

}
