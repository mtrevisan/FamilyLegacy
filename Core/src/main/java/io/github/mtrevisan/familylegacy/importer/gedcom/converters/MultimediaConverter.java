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
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Converts a GEDCOM MULTIMEDIA_RECORD (OBJE) into an FLEF MediaRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>FILE → uri (the first one; the others are kept as notes, as a record id can identify one media only)</li>
 *   <li>TITL (under OBJE or FILE) → description</li>
 *   <li>FORM, FORM.TYPE (or FORM.MEDI), _DATE, _PUBL, the alternative versions of a file (FILE.TRAN, GEDCOM 7) → notes</li>
 *   <li>NOTE → note; RESN → privacy</li>
 *   <li>SOUR → the media becomes one of the media of the cited source (see {@link #linkSources()})</li>
 *   <li>CHAN → audit</li>
 * </ul>
 * A record without FILE (e.g. a GEDCOM 5.5 BLOB) gets the placeholder uri {@code unknown}.
 */
public class MultimediaConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> multimediaMap;
	private final Map<String, GEDCOMNode> noteRawMap;

	/** The GEDCOM sources cited by the media, resolved once the sources are converted. */
	private final List<String[]> mediaSources = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param model         the FLEF model
	 * @param multimediaMap map of media IDs to FLEF records
	 */
	public MultimediaConverter(FLEFModel model, Map<String, FLEFRecord> multimediaMap, Map<String, GEDCOMNode> noteRawMap){
		this.model = model;
		this.multimediaMap = multimediaMap;
		this.noteRawMap = noteRawMap;
	}

	/**
	 * Converts a top-level GEDCOM OBJE node into an FLEF MediaRecord.
	 *
	 * @param objNode the GEDCOM node with tag "OBJE"
	 */
	public void convert(GEDCOMNode objNode){
		String xref = objNode.getXrefId();
		if(xref == null)
			return;

		IDGenerator.registerExistingId(xref);

		final FLEFRecord media = FLEFRecord.createMainRecord(xref, MediaReader.TYPE);
		final List<GEDCOMNode> fileNodes = GEDCOMHelper.findChildren(objNode, "FILE");
		GEDCOMHelper.fillMediaRecord(media, objNode, (!fileNodes.isEmpty()? fileNodes.getFirst(): null), noteRawMap);
		for(int i = 1; i < fileNodes.size(); i ++){
			final FLEFRecord note = GEDCOMHelper.createNoteStruct("Additional file", fileNodes.get(i).getValue(), objNode);
			// notes go before privacy and audit, the last fields of a MediaRecord
			media.getChildren()
				.add(media.getChildren().size() - 1, note);
		}

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(objNode, "SOUR")){
			final String sourceId = GEDCOMHelper.pointer(sourNode.getValue());
			if(sourceId != null)
				mediaSources.add(new String[]{xref, sourceId});
		}

		multimediaMap.put(xref, media);
		model.addRecord(media);
	}

	/**
	 * Adds each media to the media of the sources its OBJE record cites.
	 * <p>
	 * A MediaRecord cannot cite a source: the source of a media is the source it belongs to.
	 */
	public void linkSources(){
		for(final String[] mediaSource : mediaSources){
			final FLEFRecord source = model.getRecordById(mediaSource[1]);
			if(!SourceReader.isOfType(source))
				continue;

			final boolean present = FLEFRecordHelper.findChildren(source, "media").stream()
				.anyMatch(reference -> mediaSource[0].equals(reference.getValue()));
			if(!present)
				source.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaSource[0]));
		}
	}

}
