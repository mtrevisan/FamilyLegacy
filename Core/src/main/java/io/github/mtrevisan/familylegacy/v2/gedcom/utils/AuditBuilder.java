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
package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class AuditBuilder{

	private AuditBuilder(){}

	public static FLEFRecord build(){
		return build(null);
	}

	public static FLEFRecord build(GEDCOMNode node){
		FLEFRecord audit = FLEFRecord.createChildWithTag("audit");
		GEDCOMNode chanNode = (node != null? GEDCOMHelper.findFirstChild(node, "CHAN"): null);

		// Extraction of CHAN -> DATE and TIME
		String isoDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(chanNode, "DATE");
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue()))
			isoDateTime = GEDCOMHelper.getDateTime(dateNode);

		// Extraction of CHAN -> NOTE
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(chanNode, "NOTE");
		String chanNoteText = GEDCOMHelper.extractFullText(noteNode);
//		if(chanNoteText == null){
//			chanNoteText = "From GEDCOM conversion";
//		}

		FLEFRecord creation = FLEFRecord.createChildWithTag("creation");
		creation.addChild(FLEFRecord.createChildWithTagAndValue("date", isoDateTime));
		if(StringUtils.isNotEmpty(chanNoteText)){
			creation.addChild(FLEFRecord.createChildWithTagAndValue("comment", chanNoteText));
		}

		audit.addChild(creation);

		return audit;
	}

}
