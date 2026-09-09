/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class HeaderConverter{

	private final FLEFModel model;


	public HeaderConverter(FLEFModel model){
		this.model = model;
	}

	public void convert(GEDCOMNode headNode){
		FLEFRecord header = FLEFRecord.createChildWithTag("header");

		// ---- Protocol (hardcoded) ----
		FLEFRecord protocol = FLEFRecord.createChildWithTag("protocol")
			.addChild(FLEFRecord.createChildWithTagAndValue("name", "Family LEgacy Format"))
			.addChild(FLEFRecord.createChildWithTagAndValue("version", "0.1.2"));
		header.addChild(protocol);

		// ---- Source (from GEDCOM SOUR) ----
		GEDCOMNode sourNode = GEDCOMHelper.findFirstChild(headNode, "SOUR");
		if(sourNode != null){
			GEDCOMNode nameNode = GEDCOMHelper.findFirstChild(sourNode, "NAME");
			GEDCOMNode versNode = GEDCOMHelper.findFirstChild(sourNode, "VERS");
			GEDCOMNode corpNode = GEDCOMHelper.findFirstChild(sourNode, "CORP");

			FLEFRecord source = FLEFRecord.createChildWithTag("source");
			GEDCOMHelper.transferValue(source, "system_id", sourNode);
			GEDCOMHelper.transferValue(source, "name", nameNode);
			GEDCOMHelper.transferValue(source, "version", versNode);
			GEDCOMHelper.transferValue(source, "organization", corpNode);
			header.addChild(source);
		}

		// ---- Date (transmission date) ----
		String iso = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(headNode, "DATE");
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue()))
			iso = GEDCOMHelper.getDateTime(dateNode);
		header.addChild(FLEFRecord.createChildWithTagAndValue("date", iso));

		// ---- Copyright ----
		GEDCOMNode coprNode = GEDCOMHelper.findFirstChild(headNode, "COPR");
		GEDCOMHelper.transferValue(header, "copyright", coprNode);

		// ---- Scope (from NOTE) ----
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(headNode, "NOTE");
		if(noteNode != null && noteNode.getValue() != null){
			header.addChild(FLEFRecord.createChildWithTagAndValue("scope", GEDCOMHelper.extractFullText(noteNode)));
		}

		// ---- Submitter (will be added later) ----
		model.setHeader(header);
	}

}
