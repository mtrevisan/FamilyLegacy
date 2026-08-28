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
