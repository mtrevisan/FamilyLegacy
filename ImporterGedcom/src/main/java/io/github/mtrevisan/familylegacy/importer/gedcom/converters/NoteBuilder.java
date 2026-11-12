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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;


/** Builds the notes of the converted records. */
final class NoteBuilder{

	/** The title of the note of an age. */
	static final String AGE_NOTE_TITLE = GedcomTexts.t("title.age");


	private NoteBuilder(){}

	static FLEFRecord createNoteStruct(String title, String text, GEDCOMNode node){
		if(StringUtils.isBlank(text))
			return null;

		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE);
		if(StringUtils.isNotBlank(title))
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title));
		return note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text.trim()))
			.addChild(AuditBuilder.build(node));
	}

	/**
	 * Creates a note whose text has links to the records of the file ({@code flef:<Id>}), so it is a markdown one.
	 *
	 * @return	The note, or {@code null} if the text is blank.
	 */
	static FLEFRecord createLinkNote(final String title, final String text, final GEDCOMNode node){
		final FLEFRecord note = createNoteStruct(title, text, node);
		if(note != null)
			//the media type goes before the audit, the last field of a note
			note.getChildren()
				.add(note.getChildren().size() - 1, FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, GEDCOMMapper.MIME_MARKDOWN));
		return note;
	}

}
