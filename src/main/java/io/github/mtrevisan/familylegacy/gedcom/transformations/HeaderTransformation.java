package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.StringJoiner;


public class HeaderTransformation implements Transformation<Gedcom, Flef>{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = transformerTo.extractSubStructure(header, "SOUR");
		final GedcomNode date = transformerTo.extractSubStructure(header, "DATE");
		final GedcomNode time = transformerTo.extractSubStructure(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(!date.isEmpty())
			sj.add(date.getValue());
		if(!time.isEmpty())
			sj.add(time.getValue());
		final String language = transformerTo.extractSubStructure(source, "LANG")
			.getValue();
		final Locale locale = (language != null? new Locale(language): Locale.forLanguageTag("en-US"));
		final GedcomNode destinationHeader = transformerTo.create("HEADER")
			.addChild(transformerTo.create("PROTOCOL")
				.withValue("FLEF")
				.addChildValue("NAME", "Family LEgacy Format")
				.addChildValue("VERSION", "0.0.2")
			)
			.addChild(transformerTo.create("SOURCE")
				.withValue(source.getValue())
				.addChildValue("NAME", transformerTo.extractSubStructure(source, "NAME")
					.getValue())
				.addChildValue("VERSION", transformerTo.extractSubStructure(source, "VERS")
					.getValue())
				.addChildValue("CORPORATE", transformerTo.extractSubStructure(source, "CORP")
					.getValue())
			)
			.addChildValue("DATE", (sj.length() > 0? sj.toString(): null))
			.addChildValue("DEFAULT_CALENDAR", "GREGORIAN")
			.addChildValue("DEFAULT_LOCALE", locale.toLanguageTag())
			.addChildValue("COPYRIGHT", transformerTo.extractSubStructure(source, "COPR")
				.getValue())
			.addChildReference("SUBMITTER", transformerTo.extractSubStructure(source, "SUBM")
				.getID())
			.addChildValue("NOTE", transformerTo.extractSubStructure(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = transformerFrom.extractSubStructure(header, "SOURCE");
		final String date = transformerFrom.extractSubStructure(header, "DATE")
			.getValue();
		final String language = transformerFrom.extractSubStructure(source, "DEFAULT_LOCALE")
			.getValue();
		final Locale locale = Locale.forLanguageTag(language != null? language: "en-US");
		final GedcomNode destinationHeader = transformerFrom.create("HEAD")
			.addChild(transformerFrom.create("SOUR")
				.withValue(source.getValue())
				.addChildValue("VERS", transformerFrom.extractSubStructure(source, "VERSION")
					.getValue())
				.addChildValue("NAME", transformerFrom.extractSubStructure(source, "NAME")
					.getValue())
				.addChildValue("CORP", transformerFrom.extractSubStructure(source, "CORPORATE")
					.getValue())
			)
			.addChildValue("DATE", date)
			.addChildReference("SUBM", transformerFrom.extractSubStructure(source, "SUBMITTER")
				.getID())
			.addChildValue("COPR", transformerFrom.extractSubStructure(source, "COPYRIGHT")
				.getValue())
			.addChild(transformerFrom.create("GEDC")
				.addChildValue("VERS", "5.5.1")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChildValue("CHAR", "UTF-8")
			.addChildValue("LANG", locale.getDisplayLanguage(Locale.ENGLISH))
			.addChildValue("NOTE", transformerFrom.extractSubStructure(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

}
