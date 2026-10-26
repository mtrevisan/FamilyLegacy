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
package io.github.mtrevisan.familylegacy.gedcom.converters;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

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
 *   <li>Language (LANG), registered number (RFN) → remark</li>
 * </ul>
 * A submitter without contacts keeps its name in the remark.
 */
public class SubmitterConverter{

	private final Map<String, FLEFRecord> submitterMap;

	/**
	 * Constructor.
	 *
	 * @param submitterMap map of submitter IDs to FLEF submitter structures
	 */
	public SubmitterConverter(Map<String, FLEFRecord> submitterMap){
		this.submitterMap = submitterMap;
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

		FLEFRecord submitter = FLEFRecord.createChildWithTag("submitter");

		final String name = GEDCOMHelper.findFirstValue(subNode, "NAME");
		GEDCOMHelper.attachContacts(submitter, subNode, name);

		final List<String> remarks = new ArrayList<>();
		if(name != null && !submitter.hasChildren())
			remarks.add(name);
		for(GEDCOMNode langNode : GEDCOMHelper.findChildren(subNode, "LANG"))
			if(langNode.getValue() != null)
				remarks.add("Language: " + langNode.getValue().trim());
		final String rfn = GEDCOMHelper.findFirstValue(subNode, "RFN");
		if(rfn != null)
			remarks.add("RFN: " + rfn);
		if(!remarks.isEmpty())
			submitter.addChild(FLEFRecord.createChildWithTagAndValue("remark", String.join("\n", remarks)));

		if(submitter.hasChildren())
			submitterMap.put(xref, submitter);
	}

}
