package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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
		if(nameName.isEmpty())
			node.addChild(nameName.withTag("NAME")
				.withValue(nameComponent));
		moveTag("NICKNAME", node, "NICK");
		final GedcomNode nameSurname = moveTag("SURNAME", node, "SURN");
		if(nameSurname.isEmpty())
			node.addChild(GedcomNode.create("SURNAME")
				.withValue(surnameComponent));
		final GedcomNode surnamePrefix = extractSubStructure(node, "SPFX");
		if(!surnamePrefix.isEmpty()){
			final String nameSurnamePrefix = surnamePrefix.getValue();
			if(nameSurname.isEmpty())
				node.addChild(GedcomNode.create("SURNAME")
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
		if("INDIVIDUAL_NICKNAME".equals(extractSubStructure(node, "TYPE").getValue())){
			//TODO go back to previous name and add a NICK tag
//		+1 NICK <NAME_PIECE_NICKNAME>    {0:1}
		}
		moveTag("NPFX", node, "NAME_PREFIX");
		moveTag("GIVN", node, "NAME");
		moveTag("SURN", node, "SURNAME");
		moveTag("NSFX", node, "NAME_SUFFIX");
		//TODO
		deleteTag(node, "LOCALE");
	}

}
