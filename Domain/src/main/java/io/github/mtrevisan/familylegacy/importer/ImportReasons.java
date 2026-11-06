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
package io.github.mtrevisan.familylegacy.importer;


/** The I18N keys of the reasons why an import discards information (see {@link ImportReport}). */
public final class ImportReasons{

	/** Identifiers that FLEF cannot store yet. */
	public static final String IDENTIFIER = "import.discard.identifier";
	/** Contact data attached to an address of an event or place. */
	public static final String CONTACT_DATA = "import.discard.contact_data";
	/** Place data beyond the first place of an event, place translations and external ids. */
	public static final String PLACE_DETAIL = "import.discard.place_detail";
	/** References or details attached to a note that cannot be stored. */
	public static final String NOTE_DETAIL = "import.discard.note_detail";
	/** Media data that cannot be stored. */
	public static final String MEDIA_DETAIL = "import.discard.media_detail";
	/** Name data with no FLEF field. */
	public static final String NAME_DETAIL = "import.discard.name_detail";
	/** Date information that was lost. */
	public static final String DATE_DETAIL = "import.discard.date_detail";
	/** Source or citation data with no FLEF field. */
	public static final String SOURCE_DETAIL = "import.discard.source_detail";
	/** Data that the importer does not know. */
	public static final String UNMAPPED_TAG = "import.discard.unmapped_tag";
	/** Other information that could not be imported. */
	public static final String OTHER = "import.discard.other";


	private ImportReasons(){}

}
