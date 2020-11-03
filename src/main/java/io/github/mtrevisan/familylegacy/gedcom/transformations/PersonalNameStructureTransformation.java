package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class PersonalNameStructureTransformation implements Transformation{

	private static final Transformation NOTE_STRUCTURE_TRANSFORMATION = new NoteStructureTransformation();
	private static final Transformation SOURCE_CITATION_TRANSFORMATION = new SourceCitationTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		node.withTag("NAME");
		final String nameValue = node.getValue();
		node.removeValue();
		final String nameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(0, nameValue.indexOf('/') - 1): nameValue);
		final String surnameComponent = (nameValue != null && StringUtils.contains(nameValue, '/')?
			nameValue.substring(nameValue.indexOf('/') + 1, nameValue.length() - 1): null);
		moveTag("NAME_PREFIX", node, "NPFX");
		final GedcomNode nameGiven = moveTag("NAME", node, "GIVN");
		if(nameGiven.isEmpty() && nameComponent != null)
			node.addChild(nameGiven.withTag("NAME")
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
			NOTE_STRUCTURE_TRANSFORMATION.to(note, root);
		final List<GedcomNode> sources = node.getChildrenWithTag("SOUR");
		for(final GedcomNode source : sources)
			SOURCE_CITATION_TRANSFORMATION.to(source, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final String nameType = extractSubStructure(node, "TYPE")
			.getValue();
		moveTag("_LOCALE", node, "LOCALE");
		if("NAME_PHONETIC_VARIATION".equals(nameType))
			node.withTag("FONE");
		else if("NAME_ROMANIZED_VARIATION".equals(nameType))
			node.withTag("ROMN");
		final GedcomNode namePrefix = moveTag("NPFX", node, "NAME_PREFIX");
		final GedcomNode nameGiven = moveTag("GIVN", node, "NAME");
		moveTag("NSFX", node, "NAME_SUFFIX");
		moveTag("NICK", node, "NICKNAME");
		final GedcomNode nameSurname = moveTag("SURN", node, "SURNAME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(!namePrefix.isEmpty() && namePrefix.getValue() != null)
			sj.add(namePrefix.getValue());
		if(!nameGiven.isEmpty() && nameGiven.getValue() != null)
			sj.add(nameGiven.getValue());
		if(!nameSurname.isEmpty() && nameSurname.getValue() != null)
			sj.add("/" + StringUtils.replace(nameSurname.getValue(), ",", StringUtils.SPACE) + "/");
		if(sj.length() > 0)
			node.withValue(sj.toString());
		final List<GedcomNode> notes = node.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			NOTE_STRUCTURE_TRANSFORMATION.from(note, root);
		final List<GedcomNode> sources = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode source : sources)
			SOURCE_CITATION_TRANSFORMATION.from(source, root);
	}

}
