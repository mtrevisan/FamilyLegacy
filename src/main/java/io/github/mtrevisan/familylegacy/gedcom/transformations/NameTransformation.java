package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSourceCitation;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class NameTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final String nameValue = node.getValue();
		node.removeValue();
		final String nameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(0, nameValue.indexOf('/') - 1): null);
		final String surnameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(nameValue.indexOf('/') + 1, nameValue.length() - 1): null);
		moveTag("NAME_PREFIX", node, "NPFX");
		final GedcomNode nameName = moveTag("NAME", node, "GIVN");
		if(nameName.isEmpty() && nameComponent != null)
			node.addChild(nameName.withTag("NAME")
				.withValue(nameComponent));
		moveTag("NICKNAME", node, "NICK");
		final GedcomNode nameSurname = moveTag("SURNAME", node, "SURN");
		if(nameSurname.isEmpty() && surnameComponent != null)
			node.addChild(nameSurname.withTag("SURNAME")
				.withValue(surnameComponent));
		final GedcomNode surnamePrefix = extractSubStructure(node, "SPFX");
		if(!surnamePrefix.isEmpty()){
			final String nameSurnamePrefix = surnamePrefix.getValue();
			if(nameSurname.getTag() == null)
				node.addChild(nameSurname.withTag("SURNAME")
					.withValue(nameSurnamePrefix));
			else
				nameSurname.withValue(nameSurnamePrefix + StringUtils.SPACE + nameSurname.getValue());
			deleteTag(node, "SPFX");
		}
		moveTag("NAME_SUFFIX", node, "NSFX");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			TransformationHelper.transferNote(note, root);
		final List<GedcomNode> sources = node.getChildrenWithTag("SOUR");
		for(final GedcomNode source : sources){
			final GedcomNode s = extractSourceCitation(source, root);
			if(source.getID() == null){
				s.withID(Flef.getNextSourceID(root.getChildrenWithTag("SOURCE").size()));
				source.withID(s.getID());
			}
			root.addChild(s);
			source.removeChildren();
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		deleteTag(node, "LOCALE");
		moveTag("NPFX", node, "NAME_PREFIX");
		final GedcomNode nameName = moveTag("GIVN", node, "NAME");
		final GedcomNode surnameName = moveTag("SURN", node, "SURNAME");
		final StringJoiner ns = new StringJoiner(StringUtils.SPACE);
		if(nameName.getValue() != null)
			ns.add(nameName.getValue());
		if(surnameName.getValue() != null)
			ns.add(surnameName.getValue());
		if(ns.length() > 0)
			node.withValue(ns.toString());
		moveTag("NICK", node, "NICKNAME");
		moveTag("NSFX", node, "NAME_SUFFIX");
		deleteTag(node, "FAMILY_NICKNAME");
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			if(!note.isEmpty()){
				final GedcomNode child = root.getChildWithIDAndTag(note.getID(), "NOTE");
				if(!child.isEmpty()){
					note.removeID();
					note.withValueConcatenated(child.getValue());
				}
			}
		final List<GedcomNode> sources = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode source : sources)
			if(!source.isEmpty()){
				final GedcomNode child = root.getChildWithIDAndTag(source.getID(), "SOURCE");
				if(!child.isEmpty()){
					source.withTag("SOUR");
					moveTag("EVEN", source, "EVENT");
					final GedcomNode sourceDate = extractSubStructure(source, "DATE");
					final GedcomNode sourceText = extractSubStructure(source, "TEXT");
					final GedcomNode data = GedcomNode.create("DATA");
					if(!sourceDate.isEmpty())
						data.addChild(sourceDate);
					if(!sourceText.isEmpty())
						data.addChild(GedcomNode.createEmpty()
							.withValueConcatenated(sourceText.getValue()));
					child.addChild(data);
					final List<GedcomNode> sourceDocuments = source.getChildrenWithTag("OBJE");
					for(final GedcomNode sourceDocument : sourceDocuments)
						sourceDocument.withTag("OBJE");
					final List<GedcomNode> sourceNotes = source.getChildrenWithTag("NOTE");
					for(final GedcomNode sourceNote : sourceNotes)
						if(!sourceNote.isEmpty()){
							final GedcomNode sourceNoteChild = root.getChildWithIDAndTag(sourceNote.getID(), "NOTE");
							if(!sourceNoteChild.isEmpty()){
								sourceNote.removeID();
								sourceNote.withValueConcatenated(sourceNoteChild.getValue());
							}
						}
					moveTag("QUAY", source, "CREDIBILITY");
				}
			}
	}

}
