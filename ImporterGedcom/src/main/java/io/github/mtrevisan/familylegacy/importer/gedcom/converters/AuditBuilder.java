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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class AuditBuilder{

	private AuditBuilder(){}

	/**
	 * @param record	A GEDCOM record, may be {@code null}.
	 * @return	The date and time of its creation ({@code CREA}) or, lacking it, of its last change ({@code CHAN}), {@code null} if it has none.
	 */
	public static String dateTimeOf(final GEDCOMNode record){
		for(final String tag : new String[]{"CREA", "CHAN"}){
			final String dateTime = GEDCOMDateConverter.toDateTime(GEDCOMHelper.findFirstChild(GEDCOMHelper.findFirstChild(record, tag),
				GEDCOMMapper.TAG_DATE));
			if(dateTime != null)
				return dateTime;
		}
		return null;
	}

	public static FLEFRecord build(){
		return build(null);
	}

	public static FLEFRecord build(GEDCOMNode node){
		FLEFRecord audit = FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT);
		GEDCOMNode chanNode = (node != null? GEDCOMHelper.findFirstChild(node, "CHAN"): null);

		// Extraction of CHAN -> DATE and TIME
		//what has no date is as old as its record, or as the file (the date of the import is the last resort)
		String isoDateTime = ObjectUtils.firstNonNull(GEDCOMImportContext.inheritedDateTime(),
			DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS)));
		final String chanDateTime = GEDCOMDateConverter.toDateTime(GEDCOMHelper.findFirstChild(chanNode, GEDCOMMapper.TAG_DATE));
		if(chanDateTime != null)
			isoDateTime = chanDateTime;

		// Extraction of CHAN -> NOTE
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(chanNode, GEDCOMMapper.TAG_NOTE);
		String chanNoteText = GEDCOMHelper.extractFullText(noteNode);
//		if(chanNoteText == null){
//			chanNoteText = "From GEDCOM conversion";
//		}

		// GEDCOM 7 records the creation (CREA): the last change (CHAN) is then an update
		final GEDCOMNode creaNode = (node != null? GEDCOMHelper.findFirstChild(node, "CREA"): null);
		final String creationDateTime = GEDCOMDateConverter.toDateTime(GEDCOMHelper.findFirstChild(creaNode, GEDCOMMapper.TAG_DATE));

		final String creationDate = (creationDateTime != null? creationDateTime: isoDateTime);
		final FLEFRecord creation = FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
			.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, creationDate));
		//the last change (CHAN) is an update, unless it is at the very moment of the creation (a creation is not an update): when there is no CREA, its date is the creation fallback, and there is no update
		final FLEFRecord update = (chanDateTime != null && !chanDateTime.equals(creationDate)
			? FLEFRecord.createChildWithTag(AuditReader.TAG_UPDATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, chanDateTime))
			: null);
		if(StringUtils.isNotEmpty(chanNoteText))
			(update != null? update: creation)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, chanNoteText));

		audit.addChild(creation)
			.addChild(update);

		return audit;
	}

}
