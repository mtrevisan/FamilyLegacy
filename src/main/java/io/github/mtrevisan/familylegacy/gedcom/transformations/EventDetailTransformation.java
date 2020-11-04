package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class EventDetailTransformation implements Transformation{

	private static final Transformation PLACE_RECORD_TRANSFORMATION = new PlaceRecordTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final GedcomNode type = extractSubStructure(node, "TYPE");
		final GedcomNode date = extractSubStructure(node, "DATE");
		if(!date.isEmpty()){
			node.removeChild(date);
			type.addChild(date);
		}
		PLACE_RECORD_TRANSFORMATION.to(node, root);
		final GedcomNode agency = moveTag("AGENCY", node, "AGNC");
		if(!agency.isEmpty()){
			node.removeChild(agency);
			type.addChild(agency);
		}
		//who cares RELI here?
		deleteTag(node, "RELI");
		final GedcomNode cause = moveTag("CAUSE", node, "CAUS");
		if(!cause.isEmpty()){
			node.removeChild(cause);
			type.addChild(cause);
		}
		final GedcomNode restriction = moveTag("RESTRICTION", node, "RESN");
		if(!restriction.isEmpty()){
			node.removeChild(restriction);
			type.addChild(restriction);
		}
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> sourceCitations = node.getChildrenWithTag("SOUR");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.to(sourceCitation, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final GedcomNode type = extractSubStructure(node, "TYPE");
		final GedcomNode date = extractSubStructure(type, "DATE");
		if(!date.isEmpty()){
			moveTag("_CREDIBILITY", date, "CREDIBILITY");
			type.removeChild(date);
			node.addChild(date);
		}
		PLACE_RECORD_TRANSFORMATION.from(type, root);
		final GedcomNode agency = moveTag("AGNC", type, "AGENCY");
		if(!agency.isEmpty()){
			type.removeChild(agency);
			node.addChild(agency);
		}
		final GedcomNode cause = moveTag("CAUS", type, "CAUSE");
		if(!cause.isEmpty()){
			moveTag("_CREDIBILITY", cause, "CREDIBILITY");
			type.removeChild(cause);
			node.addChild(cause);
		}
		final GedcomNode restriction = moveTag("RESN", type, "RESTRICTION");
		if(!restriction.isEmpty()){
			type.removeChild(restriction);
			node.addChild(restriction);
		}
		final List<GedcomNode> notes = type.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> sourceCitations = type.getChildrenWithTag("SOURCE");
		for(final GedcomNode sourceCitation : sourceCitations)
			SOURCE_CITATION_TRANSFORMATION.from(sourceCitation, root);
		final List<GedcomNode> multimediaLinks = type.getChildrenWithTag("DOCUMENT");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
		moveTag("_CREDIBILITY", type, "CREDIBILITY");
	}

}
