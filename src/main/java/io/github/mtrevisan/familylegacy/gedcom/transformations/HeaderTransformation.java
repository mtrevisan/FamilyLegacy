/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.StringJoiner;


public class HeaderTransformation extends Transformation<Gedcom, Flef>{

	@Override
	public void to(final Gedcom origin, final Flef destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = transformerTo.traverse(header, "SOUR");
		final GedcomNode date = transformerTo.traverse(header, "DATE");
		final GedcomNode time = transformerTo.traverse(date, "TIME");
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(!date.isEmpty())
			sj.add(date.getValue());
		if(!time.isEmpty())
			sj.add(time.getValue());
		final String language = transformerTo.traverse(source, "LANG")
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
				.addChildValue("NAME", transformerTo.traverse(source, "NAME")
					.getValue())
				.addChildValue("VERSION", transformerTo.traverse(source, "VERS")
					.getValue())
				.addChildValue("CORPORATE", transformerTo.traverse(source, "CORP")
					.getValue())
			)
			.addChildValue("DATE", (sj.length() > 0? sj.toString(): null))
			.addChildValue("DEFAULT_CALENDAR", "GREGORIAN")
			.addChildValue("DEFAULT_LOCALE", locale.toLanguageTag())
			.addChildValue("COPYRIGHT", transformerTo.traverse(source, "COPR")
				.getValue())
			.addChildReference("SUBMITTER", transformerTo.traverse(source, "SUBM")
				.getXRef())
			.addChildValue("NOTE", transformerTo.traverse(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

	@Override
	public void from(final Flef origin, final Gedcom destination){
		final GedcomNode header = origin.getHeader();
		final GedcomNode source = transformerFrom.traverse(header, "SOURCE");
		final String date = transformerFrom.traverse(header, "DATE")
			.getValue();
		final String language = transformerFrom.traverse(source, "DEFAULT_LOCALE")
			.getValue();
		final Locale locale = Locale.forLanguageTag(language != null? language: "en-US");
		final GedcomNode destinationHeader = transformerFrom.create("HEAD")
			.addChild(transformerFrom.create("SOUR")
				.withValue(source.getValue())
				.addChildValue("VERS", transformerFrom.traverse(source, "VERSION")
					.getValue())
				.addChildValue("NAME", transformerFrom.traverse(source, "NAME")
					.getValue())
				.addChildValue("CORP", transformerFrom.traverse(source, "CORPORATE")
					.getValue())
			)
			.addChildValue("DATE", date)
			.addChildReference("SUBM", transformerFrom.traverse(source, "SUBMITTER")
				.getXRef())
			.addChildValue("COPR", transformerFrom.traverse(source, "COPYRIGHT")
				.getValue())
			.addChild(transformerFrom.create("GEDC")
				.addChildValue("VERS", "5.5.1")
				.addChildValue("FORM", "LINEAGE-LINKED")
			)
			.addChildValue("CHAR", "UTF-8")
			.addChildValue("LANG", locale.getDisplayLanguage(Locale.ENGLISH))
			.addChildValue("NOTE", transformerFrom.traverse(source, "NOTE")
				.getValue());

		destination.setHeader(destinationHeader);
	}

}
