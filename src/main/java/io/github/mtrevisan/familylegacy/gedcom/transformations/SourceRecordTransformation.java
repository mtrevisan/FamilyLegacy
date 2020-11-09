package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class SourceRecordTransformation implements Transformation{

	private static final Transformation CHANGE_DATE_TRANSFORMATION = new ChangeDateTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();
	private static final Transformation SOURCE_REPOSITORY_CITATION = new SourceRepositoryCitationTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("SOURCE");
		moveMultipleTag("PLACE", node, "DATA", "EVEN", "PLAC");
		moveMultipleTag("EVENT", node, "DATA", "EVEN");
		moveMultipleTag("AGENCY", node, "DATA", "AGNC");
		deleteMultipleTag(node, "DATA");
		final GedcomNode author = extractSubStructure(node, "AUTH");
		if(!author.isEmpty()){
			author.withTag("AUTHOR");
			author.withValue(author.getValueConcatenated());
			deleteMultipleTag(author, "CONC");
			deleteMultipleTag(author, "CONT");
		}
		final GedcomNode title = extractSubStructure(node, "TITL");
		if(!title.isEmpty()){
			title.withTag("TITLE");
			title.withValue(title.getValueConcatenated());
			deleteMultipleTag(title, "CONC");
			deleteMultipleTag(title, "CONT");
		}
		moveTag("_ABBR", node, "ABBR");
		final GedcomNode publication = extractSubStructure(node, "PUBL");
		if(!publication.isEmpty()){
			publication.withTag("PUBLICATION");
			publication.withValue(publication.getValueConcatenated());
			deleteMultipleTag(publication, "CONC");
			deleteMultipleTag(publication, "CONT");
		}
		final GedcomNode text = extractSubStructure(node, "TEXT");
		if(!text.isEmpty()){
			text.withValue(text.getValueConcatenated());
			deleteMultipleTag(text, "CONC");
			deleteMultipleTag(text, "CONT");
		}
		final List<GedcomNode> sourceRepositoryCitations = node.getChildrenWithTag("REPO");
		for(final GedcomNode sourceRepositoryCitation : sourceRepositoryCitations)
			SOURCE_REPOSITORY_CITATION.to(sourceRepositoryCitation, root);
		moveMultipleTag("_REFN", node, "REFN");
		moveTag("_RIN", node, "RIN");
		final GedcomNode changeDate = extractSubStructure(node, "CHAN");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.to(changeDate, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("SOUR");
		final GedcomNode title = extractSubStructure(node, "TITLE");
		if(!title.isEmpty()){
			title.withTag("TITL");
			title.withValueConcatenated(title.getValue());
		}
		final GedcomNode author = extractSubStructure(node, "AUTHOR");
		if(!author.isEmpty()){
			author.withTag("AUTH");
			author.withValueConcatenated(author.getValue());
		}
		moveTag("_PUBLICATION_DATE", node, "PUBLICATION_DATE");
		final GedcomNode publication = extractSubStructure(node, "PUBLICATION");
		if(!publication.isEmpty()){
			publication.withTag("PUBL");
			publication.withValueConcatenated(publication.getValue());
		}
		moveTag("_LOCATION", node, "LOCATION");
		final GedcomNode text = extractSubStructure(node, "TEXT");
		if(!text.isEmpty())
			text.withValueConcatenated(text.getValue());
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("DOCUMENT");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> sourceRepositoryCitations = node.getChildrenWithTag("REPOSITORY");
		for(final GedcomNode sourceRepositoryCitation : sourceRepositoryCitations)
			SOURCE_REPOSITORY_CITATION.from(sourceRepositoryCitation, root);
		moveMultipleTag("_RESTRICTION", node, "RESTRICTION");
		final GedcomNode changeDate = extractSubStructure(node, "CHANGE");
		if(!changeDate.isEmpty())
			CHANGE_DATE_TRANSFORMATION.from(changeDate, root);
	}

}
