package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.transferValues;


public class PlaceAndAddressStructureTransformation implements Transformation{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("ADDR", "CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final StringJoiner street = new StringJoiner(" - ");
		String value = node.getValue();
		final Iterator<GedcomNode> itr = node.getChildren().iterator();
		while(itr.hasNext()){
			final GedcomNode child = itr.next();
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String component = child.getValue();
				if(component != null && ! component.isEmpty())
					street.add(component);

				itr.remove();
			}
		}
		if(street.length() > 0)
			value = street.toString();

		final GedcomNode place = GedcomNode.create("PLACE");
		if(value != null && !value.isEmpty())
			place.addChild(GedcomNode.create("STREET")
				.withValue(value));
		transferValues(node, "CITY", place, "CITY");
		transferValues(node, "STAE", place, "STATE");
		transferValues(node, "POST", place, "POSTAL_CODE");
		transferValues(node, "CTRY", place, "COUNTRY");

		transferValues(root, "PHON", place, "PHONE");
		transferValues(root, "FAX", place, "FAX");
		transferValues(root, "EMAIL", place, "EMAIL");
		transferValues(root, "WWW", place, "WWW");

		root.removeChild(node);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){}

}
