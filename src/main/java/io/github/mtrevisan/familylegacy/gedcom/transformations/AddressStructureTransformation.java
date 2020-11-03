package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.moveTag;


public class AddressStructureTransformation implements Transformation{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final GedcomNode addr = extractSubStructure(node, "ADDR");
		addr.withTag("DATA");
		final StringJoiner address = new StringJoiner(" - ");
		final String addrConcatenated = addr.getValueConcatenated();
		if(addrConcatenated != null)
			address.add(addrConcatenated);
		final Iterator<GedcomNode> itr = addr.getChildren().iterator();
		while(itr.hasNext()){
			final GedcomNode child = itr.next();
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					address.add(value);

				itr.remove();
			}
		}
		if(address.length() > 0)
			addr.withValue(address.toString());
		moveTag("STATE", addr, "STAE");
		moveTag("POSTAL_CODE", addr, "POST");
		moveTag("COUNTRY", addr, "CTRY");
		final GedcomNode phon = extractSubStructure(node, "PHON");
		if(!phon.isEmpty()){
			node.removeChild(phon);
			addr.addChild(phon.withTag("PHONE"));
		}
		final GedcomNode fax = extractSubStructure(node, "FAX");
		if(!fax.isEmpty()){
			node.removeChild(fax);
			addr.addChild(fax);
		}
		final GedcomNode email = extractSubStructure(node, "EMAIL");
		if(!email.isEmpty()){
			node.removeChild(email);
			addr.addChild(email);
		}
		final GedcomNode www = extractSubStructure(node, "WWW");
		if(!www.isEmpty()){
			node.removeChild(www);
			addr.addChild(www);
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final GedcomNode addr = extractSubStructure(node, "DATA");
		addr.withTag("ADDR");
		addr.withValueConcatenated(addr.getValueConcatenated());
		moveTag("STAE", addr, "STATE");
		moveTag("POST", addr, "POSTAL_CODE");
		moveTag("CTRY", addr, "COUNTRY");
		final GedcomNode phon = extractSubStructure(addr, "PHONE");
		if(!phon.isEmpty()){
			addr.removeChild(phon);
			node.addChild(phon.withTag("PHON"));
		}
		final GedcomNode fax = extractSubStructure(addr, "FAX");
		if(!fax.isEmpty()){
			addr.removeChild(fax);
			node.addChild(fax);
		}
		final GedcomNode email = extractSubStructure(addr, "EMAIL");
		if(!email.isEmpty()){
			addr.removeChild(email);
			node.addChild(email);
		}
		final GedcomNode www = extractSubStructure(addr, "WWW");
		if(!www.isEmpty()){
			addr.removeChild(www);
			node.addChild(www);
		}
	}

}
