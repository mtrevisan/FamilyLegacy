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
	public void to(final GedcomNode root){
		final GedcomNode name = extractSubStructure(root, "NAME");
		final String nameValue = name.getValue();
		name.removeValue();
		final String nameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(0, nameValue.indexOf('/') - 1): null);
		final String surnameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(nameValue.indexOf('/') + 1, nameValue.length() - 1): null);
		moveTag("NAME_PREFIX", name, "NPFX");
		final GedcomNode nameName = moveTag("NAME", name, "GIVN");
		if(nameName.isEmpty())
			name.addChild(nameName.withTag("NAME")
				.withValue(nameComponent));
		moveTag("NICKNAME", name, "NICK");
		final GedcomNode nameSurname = moveTag("SURNAME", name, "SURN");
		if(nameSurname.isEmpty())
			name.addChild(GedcomNode.create("SURNAME")
				.withValue(surnameComponent));
		final GedcomNode surnamePrefix = extractSubStructure(name, "SPFX");
		if(!surnamePrefix.isEmpty()){
			final String nameSurnamePrefix = surnamePrefix.getValue();
			if(nameSurname.isEmpty())
				name.addChild(GedcomNode.create("SURNAME")
					.withValue(nameSurnamePrefix));
			else
				nameSurname.withValue(nameSurnamePrefix + StringUtils.SPACE + nameSurname.getValue());
			deleteTag(name, "SPFX");
		}
		moveTag("NAME_SUFFIX", name, "NSFX");
		final List<GedcomNode> notes = name.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			if(note.getID() == null){
				note.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
				root.addChild(GedcomNode.create("NOTE")
					.withID(note.getID())
					.withValue(note.getValueConcatenated()), 1);
				note.removeValue();
				deleteTag(note, "CONC");
				deleteTag(note, "CONT");
			}
		final List<GedcomNode> sources = name.getChildrenWithTag("SOUR");
		for(final GedcomNode source : sources){
			final GedcomNode s = extractSourceCitation(source, root);
			if(s.getID() == null){
				s.withID(Flef.getNextSourceID(root.getChildrenWithTag("SOURCE").size()));
				root.addChild(s, 1);
				//FIXME make this line works
				name.removeChild(s);
				name.addChild(GedcomNode.create("SOURCE")
					.withID(s.getID()));
			}
		}
		//TODO

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
	public void from(final GedcomNode root){
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
