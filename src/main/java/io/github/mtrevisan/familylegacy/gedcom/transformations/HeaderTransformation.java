package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.extractSubStructure;


public class HeaderTransformation implements Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = extractSubStructure(header, "SOUR");
		final GedcomNode date = extractSubStructure(header, "DATE");
		final GedcomNode time = extractSubStructure(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(!date.isEmpty())
			sj.add(date.getValue());
		if(!time.isEmpty())
			sj.add(time.getValue());
		final String language = extractSubStructure(source, "LANG")
			.getValue();
		final Locale locale = (language != null? new Locale(language): Locale.forLanguageTag("en-US"));
		final GedcomNode destinationHeader = GedcomNode.create("HEADER")
			.addChild(GedcomNode.create("SOURCE")
				.withValue(source.getValue())
				.addChildValue("NAME", extractSubStructure(source, "NAME")
					.getValue())
				.addChildValue("VERSION", extractSubStructure(source, "VERS")
					.getValue())
				.addChildValue("CORPORATE", extractSubStructure(source, "CORP")
					.getValue())
			)
			.addChild(GedcomNode.create("PROTOCOL")
				.withValue("FLEF")
				.addChildValue("NAME", "Family LEgacy Format")
				.addChildValue("VERSION", "0.0.2")
			)
			.addChildValue("DATE", (sj.length() > 0? sj.toString(): null))
			.addChildValue("DEFAULT_CALENDAR", "GREGORIAN")
			.addChildValue("DEFAULT_LOCALE", locale.toLanguageTag())
			.addChildValue("COPYRIGHT", extractSubStructure(source, "COPR")
				.getValue())
			.addChildReference("SUBMITTER", extractSubStructure(source, "SUBM")
				.getID())
			.addChildValue("NOTE", extractSubStructure(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = extractSubStructure(header, "SOURCE");
		final String date = extractSubStructure(header, "DATE")
			.getValue();
		final String language = extractSubStructure(source, "DEFAULT_LOCALE")
			.getValue();
		final Locale locale = Locale.forLanguageTag(language != null? language: "en-US");
		final GedcomNode destinationHeader = GedcomNode.create("HEAD")
			.addChild(GedcomNode.create("SOUR")
				.withValueConcatenated(source.getValue())
				.addChildValue("VERS", extractSubStructure(source, "VERSION")
					.getValue())
				.addChildValue("NAME", extractSubStructure(source, "NAME")
					.getValue())
				.addChildValue("CORP", extractSubStructure(source, "CORPORATE")
					.getValue())
			)
			.addChildValue("DATE", date)
			.addChildReference("SUBM", extractSubStructure(source, "SUBMITTER")
				.getID())
			.addChildValue("COPR", extractSubStructure(source, "COPYRIGHT")
				.getValue())
			.addChild(GedcomNode.create("GEDC")
				.addChildValue("VERS", "5.5.1")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChildValue("CHAR", "UTF-8")
			.addChildValue("LANG", locale.getDisplayLanguage(Locale.ENGLISH))
			.addChildValue("NOTE", extractSubStructure(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

}
