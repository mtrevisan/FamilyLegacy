package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.deleteTag;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;
import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.moveTag;


public class HeaderTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final GedcomNode header = origin.getHeader();

		header.withTag("HEADER");
		final GedcomNode source = extractSubStructure(header, "SOUR");
		source.withTag("SOURCE");
		moveTag("VERSION", source, "VERS");
		moveTag("CORPORATE", source, "CORP")
			.removeChildren();
		final GedcomNode data = extractSubStructure(source, "DATA");
		if(!data.isEmpty()){
			final GedcomNode date = extractSubStructure(data, "DATE");
			if(!date.isEmpty())
				source.addChildBefore(date, data);

			source.removeChild(data);
		}
		deleteTag(header, "DEST");
		deleteTag(header, "DATE");
		moveTag("SUBMITTER", header, "SUBM");
		deleteTag(header, "SUBN");
		deleteTag(header, "FILE");
		moveTag("COPYRIGHT", header, "COPR");
		final GedcomNode version = extractSubStructure(header, "GEDC", "VERS");
		final GedcomNode charset = extractSubStructure(header, "CHAR");
		if(!version.isEmpty()){
			version.withTag("PROTOCOL_VERSION");
			header.addChildBefore(version, charset);
			deleteTag(header, "GEDC");
		}
		charset.removeChildren();
		charset.withTag("CHARSET");
		deleteTag(header, "LANG");
		deleteTag(header, "PLAC");
		final GedcomNode note = extractSubStructure(header, "NOTE");
		if(!note.isEmpty())
			note.withValue(note.extractValueConcatenated());

		destination.getHeader()
			.cloneFrom(header);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){
		final GedcomNode header = origin.getHeader();

		header.withTag("HEAD");
		final GedcomNode source = extractSubStructure(header, "SOURCE");
		source.withTag("SOUR");
		moveTag("VERS", source, "VERSION");
		moveTag("CORP", source, "CORPORATE");
		final GedcomNode date = extractSubStructure(header, "DATE");
		if(!date.isEmpty()){
			source.addChild(GedcomNode.create("DATA")
				.addChild(date));
		}
		final GedcomNode copyright = extractSubStructure(header, "COPYRIGHT");
		header.removeChild(copyright);
		moveTag("SUBM", source, "SUBMITTER");
		deleteTag(header, "PROTOCOL_VERSION");
		final GedcomNode gedcom = GedcomNode.create("GEDC")
			.addChild(GedcomNode.create("VERS")
				.withValue("5.5.1"))
			.addChild(GedcomNode.create("FORM")
				.withValue("LINEAGE_LINKED"));
		final GedcomNode charset = extractSubStructure(header, "CHARSET");
		charset.withTag("CHAR");
		header.addChildBefore(gedcom, charset);
		header.addChildBefore(copyright, charset);
		final GedcomNode note = extractSubStructure(header, "NOTE");
		note.withValue(note.getValue());

		destination.getHeader()
			.cloneFrom(header);
	}

}
