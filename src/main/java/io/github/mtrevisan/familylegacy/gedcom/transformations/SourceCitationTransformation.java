package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class SourceCitationTransformation implements Transformation{

	private static final Transformation MULTIMEDIA_LINK_TRANSFORMATION = new MultimediaLinkTransformation();
	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("SOURCE");
		if(node.getID() != null){
			moveTag("EVENT", node, "EVEN");
			final GedcomNode dataDate = extractSubStructure(node, "DATA", "DATE");
			if(!dataDate.isEmpty())
				node.addChild(dataDate);
			final GedcomNode dataText = extractSubStructure(node, "DATA", "TEXT");
			if(!dataText.isEmpty()){
				dataText.withValue(dataText.getValueConcatenated());
				deleteMultipleTag(dataText, "CONC");
				deleteMultipleTag(dataText, "CONT");
				node.addChild(dataText);
			}
			deleteTag(node, "DATA");
		}
		else{
			node.withValue(node.getValueConcatenated());
			deleteMultipleTag(node, "CONC");
			deleteMultipleTag(node, "CONT");
			final GedcomNode dataText = extractSubStructure(node, "TEXT");
			if(!dataText.isEmpty()){
				dataText.withValue(dataText.getValueConcatenated());
				deleteMultipleTag(dataText, "CONC");
				deleteMultipleTag(dataText, "CONT");
				node.addChild(dataText);
			}
		}
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("OBJE");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.to(multimediaLink, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		moveTag("CREDIBILITY", node, "QUAY");
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withTag("SOUR");
		moveTag("EVEN", node, "EVENT");
		final GedcomNode sourceDate = extractSubStructure(node, "DATE");
		node.removeChild(sourceDate);
		final GedcomNode sourceText = extractSubStructure(node, "TEXT");
		node.removeChild(sourceText);
		final GedcomNode data = GedcomNode.create("DATA");
		if(!sourceDate.isEmpty())
			data.addChild(sourceDate);
		if(!sourceText.isEmpty())
			data.addChild(sourceText
				.withValueConcatenated(sourceText.getValue()));
		if(data.hasChildren())
			node.addChild(data);
		final List<GedcomNode> multimediaLinks = node.getChildrenWithTag("DOCUMENT");
		for(final GedcomNode multimediaLink : multimediaLinks)
			MULTIMEDIA_LINK_TRANSFORMATION.from(multimediaLink, root);
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		moveTag("QUAY", node, "CREDIBILITY");
	}

}
