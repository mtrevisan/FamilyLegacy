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
package io.github.mtrevisan.familylegacy.search;

import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;


/**
 * Collects the text of the notes and of the citations that belong to a record, so that the search can look into them
 * ({@link SearchMode#NOTES_AND_CITATIONS}).
 * <p>
 * The notes of a record are the {@code note} structures written in it, at any depth (e.g. in a name, a date, a citation), and, for
 * a record that is itself a note, its own text. For each note the title, the text and the text of its translations are taken.
 * The citations are the {@code source} structures written in it: for each one the locator and the text and remarks of its extracts.
 * The notes and citations of the <em>other</em> records the record points to (the events of an individual, for example) are not
 * included: they are found as the records that own them.
 */
public final class AttachedTextExtractor{

	private AttachedTextExtractor(){}


	/**
	 * @param record	The record to read.
	 * @return	The text of all its notes, separated by a line break; empty if it has none.
	 */
	public static String extract(final FLEFRecord record){
		if(record == null)
			return StringUtils.EMPTY;

		final StringBuilder sb = new StringBuilder();
		if(NoteReader.isOfType(record))
			appendNote(record, sb);
		collect(record, sb);
		return sb.toString();
	}

	private static void collect(final FLEFRecord node, final StringBuilder sb){
		for(final FLEFRecord child : node.getChildren()){
			final String tag = child.getTag();
			if(NoteReader.TYPE.equalsIgnoreCase(tag))
				appendNote(child, sb);
			else if(SourceCitationReader.TAG_SOURCE.equalsIgnoreCase(tag) || SourceReader.TAG_DERIVED_FROM.equalsIgnoreCase(tag))
				appendCitation(child, sb);
			collect(child, sb);
		}
	}

	/** The locator, and the text and the remarks of every extract (the source cited is another record: not read here). */
	private static void appendCitation(final FLEFRecord citation, final StringBuilder sb){
		for(final FLEFRecord child : citation.getChildren()){
			final String tag = child.getTag();
			if(SourceCitationReader.TAG_LOCATOR.equalsIgnoreCase(tag))
				append(sb, child.getValue());
			else if(SourceCitationReader.TAG_EXTRACT.equalsIgnoreCase(tag))
				for(final FLEFRecord part : child.getChildren())
					if(SourceCitationReader.TAG_TEXT.equalsIgnoreCase(part.getTag())
							|| SourceCitationReader.TAG_REMARK.equalsIgnoreCase(part.getTag()))
						append(sb, part.getValue());
		}
	}

	private static void appendNote(final FLEFRecord note, final StringBuilder sb){
		for(final FLEFRecord child : note.getChildren()){
			final String tag = child.getTag();
			if(NoteReader.TAG_TITLE.equalsIgnoreCase(tag) || NoteReader.TAG_TEXT.equalsIgnoreCase(tag))
				append(sb, child.getValue());
			else if(NoteReader.TAG_TRANSLATION.equalsIgnoreCase(tag))
				for(final FLEFRecord translationPart : child.getChildren())
					if(NoteReader.TAG_TEXT.equalsIgnoreCase(translationPart.getTag()))
						append(sb, translationPart.getValue());
		}
	}

	private static void append(final StringBuilder sb, final String text){
		if(StringUtils.isBlank(text))
			return;

		if(!sb.isEmpty())
			sb.append('\n');
		sb.append(text);
	}

}
