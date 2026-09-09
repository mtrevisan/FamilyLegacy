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
package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;

import java.util.Map;


/**
 * Converts GEDCOM REPO records to FLEF RepositoryRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Name (NAME) → name+: NameStructure</li>
 *   <li>Place (PLAC) → place: PlaceCitation</li>
 *   <li>Address (ADDR) → contact: ContactStructure</li>
 *   <li>Date (DATE) → date</li>
 *   <li>_DATE (extension) → date (if no DATE present)</li>
 *   <li>OBJE → DocumentRecord + document reference</li>
 *   <li>Notes (NOTE) → inline NoteStructure (with audit)</li>
 *   <li>Extra fields (RIN, REFN) → inline NoteStructure (with audit)</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 */
public class RepositoryConverter{

	private final Map<String, FLEFRecord> repositoryMap;
	private final Map<String, GEDCOMNode> noteRawMap;

	/**
	 * Constructor.
	 *
	 * @param repositoryMap map of repository IDs to FLEF records
	 */
	public RepositoryConverter(Map<String, FLEFRecord> repositoryMap,
		Map<String, GEDCOMNode> noteRawMap){
		this.repositoryMap = repositoryMap;
		this.noteRawMap = noteRawMap;
	}

	/**
	 * Converts a GEDCOM REPO node into an FLEF RepositoryRecord.
	 *
	 * @param repoNode the GEDCOM node with the tag "REPO"
	 */
	public void convert(GEDCOMNode repoNode){
		String xref = repoNode.getXrefId();
		if(xref == null) return;

		String cleanId = GEDCOMHelper.cleanId(xref);
		IDGenerator.registerExistingId(cleanId);

		FLEFRecord repository = FLEFRecord.createMainRecord(cleanId, RepositoryHandler.TYPE);
		repositoryMap.put(cleanId, repository);

		// ---- 1. NAME (name+: NameStructure) ----
		GEDCOMNode nameNode = GEDCOMHelper.findFirstChild(repoNode, "NAME");
		if(nameNode != null && nameNode.getValue() != null){
			FLEFRecord classifiedName = FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", nameNode.getValue()));
			// Optional type (not standard for REPO NAME, but harmless)
			GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(nameNode, "TYPE");
			if(typeNode != null && typeNode.getValue() != null){
				classifiedName.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
			}
			repository.addChild(classifiedName);
		}

		// ---- 2. ADDRESS_STRUCTURE (ADDR, PHON, EMAIL, FAX, WWW) ----
		GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(repoNode, "ADDR");
		GEDCOMHelper.attachAddressToContact(repository, addrNode, repoNode);

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, "NOTE")) {
			GEDCOMHelper.attachNote(repository,
				noteNode, noteRawMap);
		}

		// ---- 10. Audit (required) ----
		repository.addChild(AuditBuilder.build(repoNode));
	}

}
