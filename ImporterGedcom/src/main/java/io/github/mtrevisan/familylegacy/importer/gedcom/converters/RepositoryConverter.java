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
import org.apache.commons.lang3.StringUtils;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.Map;


/**
 * Converts GEDCOM REPO records to FLEF RepositoryRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Name (NAME) → name+: NameStructure</li>
 *   <li>Address structure (ADDR, PHON, EMAIL, FAX, WWW) → contact*: ContactStructure</li>
 *   <li>Place (PLAC, an extension of some programs) → place: PlaceCitation</li>
 *   <li>Notes (NOTE) → inline NoteStructure (with audit)</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 * REFN and RIN have no counterpart.
 */
public class RepositoryConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> repositoryMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final PlaceCache placeCache;

	/**
	 * Constructor.
	 *
	 * @param repositoryMap map of repository IDs to FLEF records
	 */
	public RepositoryConverter(FLEFModel model, Map<String, FLEFRecord> repositoryMap, Map<String, GEDCOMNode> noteRawMap,
			PlaceCache placeCache){
		this.model = model;
		this.repositoryMap = repositoryMap;
		this.noteRawMap = noteRawMap;
		this.placeCache = placeCache;
	}

	/**
	 * Converts a GEDCOM REPO node into an FLEF RepositoryRecord.
	 *
	 * @param repoNode the GEDCOM node with the tag "REPO"
	 */
	public void convert(GEDCOMNode repoNode){
		String xref = repoNode.getXrefId();
		if(xref == null)
			return;

		IDGenerator.registerExistingId(xref);

		FLEFRecord repository = FLEFRecord.createMainRecord(xref, RepositoryReader.TYPE);
		repositoryMap.put(xref, repository);
		GEDCOMImportContext.setRecord(xref);

		// ---- 1. NAME (name+: NameStructure) ----
		final String name = GEDCOMHelper.findFirstValue(repoNode, GEDCOMMapper.TAG_NAME);
		repository.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, (name != null? name: "Repository " + xref))));

		// ---- 2. PLAC (place: PlaceCitation) ----
		GEDCOMRecordBuilder.attachPlaceCitation(repository, model, GEDCOMHelper.findFirstChild(repoNode, GEDCOMMapper.TAG_PLACE), null, noteRawMap,
			placeCache);

		// ---- 3. ADDRESS_STRUCTURE (ADDR, PHON, EMAIL, FAX, WWW) ----
		GEDCOMRecordBuilder.attachContacts(repository, repoNode, null);

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(repository, model, noteNode, noteRawMap);
		}

		// ---- What has no field in FLEF is kept as notes ----
		KeptNotes.keepIdentifiers(repository, repoNode, StringUtils.EMPTY, repoNode);
		KeptNotes.keepUnmapped(repository, repoNode, GEDCOMMapper.REPOSITORY_CONSUMED_TAGS, StringUtils.EMPTY, repoNode);

		// ---- Privacy (RESN, an extension of some programs) ----
		GEDCOMRecordBuilder.attachRestriction(repository, repoNode);

		// ---- Audit (required) ----
		repository.addChild(AuditBuilder.build(repoNode));

		model.addRecord(repository);
	}

}
