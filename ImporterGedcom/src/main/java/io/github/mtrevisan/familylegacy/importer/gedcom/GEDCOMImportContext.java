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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.PendingFile;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * Holds the {@link ImportReport} of the conversion in progress, and the id of the GEDCOM record being converted.
 * <p>
 * The converters use static builders, so the report is kept here: {@link #begin(ImportReport)} must be called at the start of each
 * conversion (it replaces the report and forgets the record) and {@link #end()} at its end, so that nothing leaks between imports.
 */
public final class GEDCOMImportContext{

	private static final int MAX_ITEM_LENGTH = 80;
	private static final String ELLIPSIS = "…";


	private static ImportReport report = new ImportReport();
	private static String record = StringUtils.EMPTY;
	/** The date and time of creation (or, lacking it, of the last change) of the records, by id, and of the file, for the records that have none. */
	private static final Map<String, String> RECORD_DATES = new HashMap<>();
	private static String fileDate;


	private GEDCOMImportContext(){}


	/** Starts the collection of the discards of a conversion. */
	public static synchronized void begin(final ImportReport importReport){
		report = (importReport != null? importReport: new ImportReport());
		record = StringUtils.EMPTY;
		RECORD_DATES.clear();
		fileDate = null;
	}

	/** Registers the date and time of a record, the date of the elements of the record that have none. */
	public static synchronized void registerRecordDate(final String recordId, final String dateTime){
		if(recordId != null && dateTime != null)
			RECORD_DATES.put(recordId, dateTime);
	}

	/** Registers the date and time of the file, the date of the elements that belong to a record with no date. */
	public static synchronized void registerFileDate(final String dateTime){
		fileDate = dateTime;
	}

	/** @return	The date and time that an element without one inherits: the one of its record, or else the one of the file, {@code null} if unknown. */
	public static synchronized String inheritedDateTime(){
		return RECORD_DATES.getOrDefault(record, fileDate);
	}

	/** Ends the conversion: later discards go to a throw-away report. */
	public static synchronized void end(){
		report = new ImportReport();
		record = StringUtils.EMPTY;
		RECORD_DATES.clear();
		fileDate = null;
	}

	/** Sets the id of the record being converted, the start of the {@code where} of the discards. */
	public static synchronized void setRecord(final String recordId){
		record = StringUtils.defaultString(recordId);
	}

	/**
	 * Records the discard of a GEDCOM line.
	 *
	 * @param reasonKey	One of {@link ImportReasons}.
	 * @param node	The discarded line.
	 * @param parentPath	The tag path of the parent of the line inside the record, empty for a child of the record.
	 */
	public static void discard(final String reasonKey, final GEDCOMNode node, final String parentPath){
		discard(reasonKey, describe(node), pathOf(parentPath, node.getTag()));
	}

	/**
	 * Records a discard.
	 *
	 * @param path	The tag path inside the current record.
	 */
	public static synchronized void discard(final String reasonKey, final String item, final String path){
		report.discard(reasonKey, item, (StringUtils.isEmpty(record)? path: record + StringUtils.SPACE + path));
	}

	/** Records a file embedded in the data (e.g. a BLOB), to be written to disk by the application. */
	public static synchronized void addPendingFile(final PendingFile file){
		report.addPendingFile(file);
	}

	/** Records the discard of each identifier (REFN, UID, EXID, ...) among the children of a node. */
	public static void reportIdentifiers(final GEDCOMNode parent, final String parentPath){
		for(final GEDCOMNode child : parent.getChildren())
			if(GEDCOMMapper.IDENTIFIER_TAGS.contains(child.getTag()) && isSignificant(child))
				discard(ImportReasons.IDENTIFIER, child, parentPath);
	}

	/** Whether a line holds something: a value or children. */
	public static boolean isSignificant(final GEDCOMNode node){
		return (StringUtils.isNotBlank(node.getValue()) || !node.getChildren().isEmpty());
	}

	public static String pathOf(final String parentPath, final String tag){
		return (StringUtils.isEmpty(parentPath)? tag: parentPath + "." + tag);
	}

	/** Describes a line: its tag and value, truncated. */
	public static String describe(final GEDCOMNode node){
		final String value = StringUtils.normalizeSpace(node.getValue());
		final String text = (StringUtils.isEmpty(value)? node.getTag(): node.getTag() + StringUtils.SPACE + value);
		return (text.length() > MAX_ITEM_LENGTH? text.substring(0, MAX_ITEM_LENGTH - 1) + ELLIPSIS: text);
	}

}
