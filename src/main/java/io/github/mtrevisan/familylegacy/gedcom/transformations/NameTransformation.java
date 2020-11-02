package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferNoteTo;


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
			transferNoteTo(note, root);
		final List<GedcomNode> sources = node.getChildrenWithTag("SOUR");
		//FIXME
//		for(final GedcomNode source : sources)
//			transferSourceCitationTo(source, root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		deleteTag(node, "LOCALE");
		moveTag("NPFX", node, "NAME_PREFIX");
		final GedcomNode nameName = moveTag("GIVN", node, "NAME");
		final List<GedcomNode> surnames = node.getChildrenWithTag("SURNAME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		for(final GedcomNode surname : surnames){
			sj.add(surname.getValue());
			node.removeChild(surname);
		}
		final GedcomNode surnameName = GedcomNode.create("SURN")
			.withValue(sj.toString());
		node.addChild(surnameName);
		final StringJoiner ns = new StringJoiner(StringUtils.SPACE);
		if(nameName.getValue() != null)
			ns.add(nameName.getValue());
		if(surnameName.getValue() != null)
			ns.add("/" + surnameName.getValue() + "/");
		if(ns.length() > 0)
			node.withValue(ns.toString());
		moveTag("NICK", node, "NICKNAME");
		moveTag("NSFX", node, "NAME_SUFFIX");
		deleteTag(node, "FAMILY_NICKNAME");
		final List<GedcomNode> sources = node.getChildrenWithTag("SOURCE");
		for(final GedcomNode source : sources)
			if(!source.isEmpty()){
				source.withTag("SOUR");
				moveTag("EVEN", source, "EVENT");
				final GedcomNode sourceDate = extractSubStructure(source, "DATE");
				final GedcomNode sourceText = extractSubStructure(source, "TEXT");
				final GedcomNode data = GedcomNode.create("DATA");
				if(!sourceDate.isEmpty())
					data.addChild(sourceDate);
				if(!sourceText.isEmpty())
					data.addChild(sourceText
						.withValueConcatenated(sourceText.getValue()));
				source.removeChild(sourceDate);
				source.removeChild(sourceText);
				source.addChild(data);
				final List<GedcomNode> sourceDocuments = source.getChildrenWithTag("DOCUMENT");
				for(final GedcomNode sourceDocument : sourceDocuments)
					sourceDocument.withTag("OBJE");
				moveTag("QUAY", source, "CREDIBILITY");
			}
	}

}
