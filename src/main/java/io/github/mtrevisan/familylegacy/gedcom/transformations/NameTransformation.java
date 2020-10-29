package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.addNode;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.mergeNote;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class NameTransformation implements Transformation{

	@Override
	public void to(final GedcomNode root){
		final GedcomNode name = extractSubStructure(root, "NAME");
		moveTag("NAME_PREFIX", name, "NPFX");
		moveTag("NAME", name, "GIVN");
		moveTag("NAME_SUFFIX", name, "NSFX");
		final GedcomNode surnameSuffix = extractSubStructure(name, "NSFX");
		final GedcomNode surname = extractSubStructure(name, "SURN");
		deleteTag(name, "NSFX");
		deleteTag(name, "SURN");
		if(!surnameSuffix.isEmpty() || !surname.isEmpty()){
			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			if(!surnameSuffix.isEmpty())
				sj.add(surnameSuffix.getValue());
			if(!surname.isEmpty())
				sj.add(surname.getValue());
			final GedcomNode anotherSurname = GedcomNode.create("SURNAME")
				.withValue(sj.toString());
			addNode(anotherSurname, name);
		}
		deleteTag(name, "SPFX");
		final GedcomNode nameNickname = extractSubStructure(name, "NICK");
		if(!nameNickname.isEmpty()){
			final GedcomNode anotherNameNickname = GedcomNode.create("NAME")
				.withValue(nameNickname.getValue());
			addNode(anotherNameNickname, root);
		}
		final List<GedcomNode> namePhonetics = name.getChildrenWithTag("FONE");
		for(final GedcomNode namePhonetic : namePhonetics){
			if(!namePhonetic.isEmpty()){
				to(namePhonetic);
				moveTag("NAME", name, "FONE");
				addNode(namePhonetic, root);
			}
		}
		deleteTag(name, "FONE");

		final List<GedcomNode> nameRomanizeds = name.getChildrenWithTag("ROMN");
		for(final GedcomNode nameRomanized : nameRomanizeds){
			if(!nameRomanized.isEmpty()){
				to(nameRomanized);
				moveTag("NAME", name, "ROMN");
				addNode(nameRomanized, root);
			}
		}
		deleteTag(name, "ROMN");
		mergeNote(name, "NOTE");

/*
		+1 <<NOTE_STRUCTURE>>    {0:M}					+1 NOTE @<XREF:NOTE>@    {0:M}
		+1 <<SOURCE_CITATION>>    {0:M}					+1 SOURCE @<XREF:SOURCE>@    {0:M}


		+1 FAMILY_NICKNAME <FAMILY_NICKNAME>    {0:M}
*/
	}

	@Override
	public void from(final GedcomNode root){
		final GedcomNode name = extractSubStructure(root, "NAME");
		deleteTag(name, "LOCALE");
		deleteTag(name, "DATE");
		deleteTag(name, "CREDIBILITY");
	}

}
