package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


public class PhysicalDescriptionDetailTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final StringJoiner sj = new StringJoiner(", ");
		final List<GedcomNode> keys = node.getChildrenWithTag("KEY");
		for(final GedcomNode key : keys)
			if(key.getValue() != null){
				final GedcomNode value = extractSubStructure(key, "VALUE");
				if(!value.isEmpty())
					sj.add(key.getValue() + StringUtils.SPACE + value.getValue());
			}
		deleteTag(node, "KEY");
		if(sj.length() > 0)
			node.withValue(sj.toString());
	}

}
