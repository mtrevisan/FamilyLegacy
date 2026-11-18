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
import io.github.mtrevisan.familylegacy.importer.gedcom.UnconsumedTags;
import io.github.mtrevisan.familylegacy.io.model.readers.ContactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HeaderReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Converts GEDCOM SUBM (submitter) records into FLEF submitter structures.
 * <p>
 * Submitters are used in the header, not as top‑level records.
 * The resulting structure is stored in the submitterMap for later inclusion
 * in the Header by HeaderConverter.
 * <p>
 * Handles:
 * <ul>
 *   <li>Name (NAME) → name of each contact</li>
 *   <li>Address structure (ADDR, PHON, EMAIL, FAX, WWW) → contact</li>
 *   <li>Language (LANG), registered number (RFN), the other identifiers, the notes (NOTE), the unmapped tags → remark</li>
 * </ul>
 * The multimedia (OBJE) of a submitter have no place in the header, and are reported.
 * A submitter without contacts keeps its name in the remark.
 */
public class SubmitterConverter{

	private final Map<String, FLEFRecord> submitterMap;
	private final Map<String, GEDCOMNode> noteRawMap;

	/**
	 * Constructor.
	 *
	 * @param submitterMap map of submitter IDs to FLEF submitter structures
	 */
	public SubmitterConverter(Map<String, FLEFRecord> submitterMap){
		this(submitterMap, Map.of());
	}

	/**
	 * Constructor.
	 *
	 * @param submitterMap map of submitter IDs to FLEF submitter structures
	 * @param noteRawMap	the NOTE records, by id
	 */
	public SubmitterConverter(Map<String, FLEFRecord> submitterMap, Map<String, GEDCOMNode> noteRawMap){
		this.submitterMap = submitterMap;
		this.noteRawMap = noteRawMap;
	}

	/**
	 * Converts a GEDCOM SUBM node into an FLEF submitter structure.
	 *
	 * @param subNode the GEDCOM node with the tag "SUBM"
	 */
	public void convert(GEDCOMNode subNode){
		String xref = subNode.getXrefId();
		if(xref == null)
			return;

		GEDCOMImportContext.setRecord(xref);
		//the multimedia of a submitter have no place in the header
		for(final GEDCOMNode child : subNode.getChildren())
			if(GEDCOMMapper.TAG_MULTIMEDIA.equals(child.getTag()) && GEDCOMImportContext.isSignificant(child))
				GEDCOMImportContext.discard(ImportReasons.MEDIA_DETAIL, child, StringUtils.EMPTY);

		FLEFRecord submitter = FLEFRecord.createChildWithTag(HeaderReader.TAG_SUBMITTER);

		final String name = GEDCOMHelper.findFirstValue(subNode, GEDCOMMapper.TAG_NAME);
		GEDCOMRecordBuilder.attachContacts(submitter, subNode, name);

		final List<String> remarks = new ArrayList<>();
		if(name != null && !submitter.hasChildren())
			remarks.add(name);
		for(GEDCOMNode langNode : GEDCOMHelper.findChildren(subNode, GEDCOMMapper.TAG_LANGUAGE))
			if(langNode.getValue() != null)
				remarks.add("Language: " + langNode.getValue().trim());
		final String rfn = GEDCOMHelper.findFirstValue(subNode, "RFN");
		if(rfn != null)
			remarks.add("RFN: " + rfn);
		for(final GEDCOMNode child : subNode.getChildren()){
			final String tag = child.getTag();
			if(GEDCOMMapper.TAG_NOTE.equals(tag) && GEDCOMImportContext.isSignificant(child)){
				//the notes of the submitter go to the remark, in separate lines
				final String text = StringUtils.trimToNull(GEDCOMHelper.extractFullText(GEDCOMRecordBuilder.resolveNote(child, noteRawMap)));
				if(text != null)
					remarks.add(text);
				else
					GEDCOMImportContext.discard(ImportReasons.OTHER, child, StringUtils.EMPTY);
			}
			else if(UnconsumedTags.isUnmapped(child, GEDCOMMapper.SUBMITTER_CONSUMED_TAGS)
					|| (GEDCOMMapper.IDENTIFIER_TAGS.contains(tag) && !GEDCOMMapper.TAG_RECORD_FILE_NUMBER.equals(tag)
					&& GEDCOMImportContext.isSignificant(child)))
				remarks.add(describe(child));
		}
		if(!remarks.isEmpty())
			submitter.addChild(FLEFRecord.createChildWithTagAndValue(ContactReader.TAG_REMARK, String.join(StringUtils.LF, remarks)));

		if(submitter.hasChildren())
			submitterMap.put(xref, submitter);
	}

	/** Describes an identifier or an unmapped line as {@code <TAG>: <value>} followed by its subtree. */
	private static String describe(final GEDCOMNode node){
		final StringBuilder text = new StringBuilder(node.getTag()).append(':');
		final String value = StringUtils.trimToNull(GEDCOMHelper.extractFullText(node));
		if(value != null)
			text.append(StringUtils.SPACE).append(value);
		final String details = KeptNotes.flatten(node, null);
		if(!details.isEmpty())
			text.append(StringUtils.LF).append(details);
		return text.toString();
	}

}
