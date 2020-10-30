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
		final GedcomNode namePhonetic = extractSubStructure(root, "FONE");
		if(!namePhonetic.isEmpty()){
			to(namePhonetic, root);
			//TODO
		}
		final GedcomNode nameRomanized = extractSubStructure(root, "ROMN");
		if(!nameRomanized.isEmpty()){
			to(nameRomanized, root);
			//TODO
		}

/*
		+1 FONE <NAME_PHONETIC_VARIATION>    {0:M}				add another name
			+2 TYPE <PHONETIC_TYPE>    {1:1}
			+2 <<PERSONAL_NAME_PIECES>>    {0:1}
		+1 ROMN <NAME_ROMANIZED_VARIATION>    {0:M}				add another name
			+2 TYPE <ROMANIZED_TYPE>    {1:1}
			+2 <<PERSONAL_NAME_PIECES>>    {0:1}

		-- +1 LOCALE <NAME_LOCALE>    {0:1}
 */
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final List<GedcomNode> names = root.getChildrenWithTag("NAME");
		for(final GedcomNode name : names){
			if("INDIVIDUAL_NICKNAME".equals(extractSubStructure(name, "TYPE"))){
				//TODO go back to previous name and add a NICK tag
//		+1 NICK <NAME_PIECE_NICKNAME>    {0:1}
			}
			moveTag("NPFX", name, "NAME_PREFIX");
			moveTag("GIVN", name, "NAME");
			moveTag("SURN", name, "SURNAME");
			moveTag("NSFX", name, "NAME_SUFFIX");
			//TODO
		}
	}

}
