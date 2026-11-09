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

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.HeaderReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.util.SoftwareInfo;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;


/**
 * Converts the GEDCOM HEAD into the FLEF Header.
 * <p>
 * Handles:
 * <ul>
 *   <li>SOUR (NAME, VERS, CORP) → source</li>
 *   <li>DATE (TIME) → date</li>
 *   <li>COPR → copyright</li>
 *   <li>SUBM → submitter (from the SUBM record)</li>
 *   <li>NOTE (or the shared note it points to) → scope</li>
 * </ul>
 * DEST, FILE, GEDC, CHAR, LANG, PLAC, and SCHMA describe the GEDCOM file itself and have no counterpart.
 */
public class HeaderConverter{

	private static final String PROTOCOL_NAME = "Family LEgacy Format";
	private static final String PROTOCOL_VERSION = "0.2.0";


	private final FLEFModel model;
	private final Map<String, GEDCOMNode> noteRawMap;


	public HeaderConverter(FLEFModel model, Map<String, GEDCOMNode> noteRawMap){
		this.model = model;
		this.noteRawMap = noteRawMap;
	}

	/**
	 * @param submitterMap	The submitter structures, by GEDCOM SUBM id.
	 */
	public void convert(GEDCOMNode headNode, Map<String, FLEFRecord> submitterMap){
		FLEFRecord header = FLEFRecord.createChildWithTag(FLEFGrammarParser.FIELD_HEADER);
		GEDCOMImportContext.setRecord(GEDCOMMapper.TAG_HEADER);
		//the default language of the file has no counterpart
		final GEDCOMNode languageNode = GEDCOMHelper.findFirstChild(headNode, GEDCOMMapper.TAG_LANGUAGE);
		if(languageNode != null && StringUtils.isNotBlank(languageNode.getValue()))
			GEDCOMImportContext.discard(ImportReasons.OTHER, languageNode, StringUtils.EMPTY);

		// ---- Protocol (hardcoded) ----
		FLEFRecord protocol = FLEFRecord.createChildWithTag(HeaderReader.TAG_PROTOCOL)
			.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_NAME, PROTOCOL_NAME))
			.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_VERSION, PROTOCOL_VERSION));
		header.addChild(protocol);

		// ---- Source: the software that writes this file is this one, whatever wrote the GEDCOM ----
		header.addChild(FLEFRecord.createChildWithTag(HeaderReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_NAME, SoftwareInfo.NAME))
			.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_VERSION, SoftwareInfo.version()))
			.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_SOURCE_ORGANIZATION, SoftwareInfo.ORGANIZATION)));

		// ---- Date (transmission date) ----
		String iso = GEDCOMDateConverter.toDateTime(GEDCOMHelper.findFirstChild(headNode, GEDCOMMapper.TAG_DATE));
		if(iso == null)
			iso = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		header.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_DATE, iso));

		// ---- Copyright ----
		GEDCOMNode coprNode = GEDCOMHelper.findFirstChild(headNode, "COPR");
		GEDCOMRecordBuilder.transferValue(header, HeaderReader.TAG_COPYRIGHT, coprNode);

		// ---- Submitter ----
		final String submitterId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(headNode, "SUBM"));
		final FLEFRecord submitter = (submitterId != null? submitterMap.get(submitterId): null);
		if(submitter != null)
			header.addChild(submitter);

		// ---- Scope (from NOTE) ----
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(headNode, GEDCOMMapper.TAG_NOTE);
		final String scope = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMRecordBuilder.resolveNote(noteNode, noteRawMap)));
		if(scope != null)
			header.addChild(FLEFRecord.createChildWithTagAndValue(HeaderReader.TAG_SCOPE, scope));

		model.setHeader(header);
	}

}
