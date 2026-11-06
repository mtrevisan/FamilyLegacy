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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Replaces a record with another one in a model: the references to it, found through the Xref paths of the grammar, and the
 * {@code flef:} links in the notes.
 */
public final class RecordIdReplacer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RecordIdReplacer.class);


	private static final String TAG_NOTE_TEXT = NoteReader.TAG_TEXT;
	private static final String TAG_NOTE_TRANSLATION_TEXT = FLEFRecordHelper.composePath(NoteReader.TAG_TRANSLATION, NoteReader.TAG_TEXT);


	private RecordIdReplacer(){}


	/**
	 * Redirects, in the model, to {@code newId} every reference to {@code oldId}, strictly across pre-indexed Xref paths derived from the grammar,
	 * together with the {@code flef:} links in notes, then removes the record {@code oldId}.
	 * Highly optimized to avoid global record lookups per path iteration.
	 *
	 * @param model the model to change
	 * @param oldId the ID to replace
	 * @param newId the target ID, of an existing record
	 * @throws IllegalArgumentException if {@code newId} is not a record of this model
	 */
	public static void replace(final FLEFModel model, final String oldId, final String newId){
		if(!XrefPathIndex.isInitialized())
			throw new IllegalStateException("Xref paths index is not initialized");

		if(StringUtils.isEmpty(oldId) || oldId.equals(newId))
			return;

		final FLEFRecord newRecord = model.getRecordById(newId);
		if(newRecord == null)
			throw new IllegalArgumentException("Record does not exist: " + newId);
		final String newRecordType = newRecord.getTag();
		final String newTargetRecordType = CaseUtils.toCamelCase(newRecordType, true, '_')
			+ XrefPathIndex.RECORD_UNION_NAME;
		final Set<String> xrefPaths = XrefPathIndex.getPaths(newTargetRecordType);
		if(xrefPaths.isEmpty())
			return;

		// Iterate once through all records in the model to update structural Xref fields
		for(final FLEFRecord record : model.getRecords()){
			final String recordType = record.getTag();
			for(final String path : xrefPaths){
				final int searchRecordTypeIndex = path.indexOf('.');
				final String searchRecordType = path.substring(0, searchRecordTypeIndex);
				if(!Strings.CI.equals(searchRecordType, recordType))
					continue;

				final String searchPath = path.substring(searchRecordTypeIndex + 1);
				final List<FLEFRecord> targetNodes = FLEFRecordHelper.findChildren(record, searchPath);
				for(final FLEFRecord node : targetNodes)
					if(oldId.equals(node.getValue())){
						node.setValue(newId);

						LOGGER.info("Updated Xref ID from {} to {} in record {}, path {}", oldId, newId, record.getId(),
							searchPath);
					}
			}
		}

		// Construct the markdown link pattern for FLEF: URI references (e.g., flef:I12); the id ends at the first character that cannot
		// belong to an id, so that flef:I1 does not match flef:I12
		final Pattern flefLinkOld = Pattern.compile(Pattern.quote(FLEFModel.FLEF_LINK_PREFIX + oldId) + "(?![A-Za-z0-9_-])");
		final String flefLinkNew = Matcher.quoteReplacement(FLEFModel.FLEF_LINK_PREFIX + newId);
		// Iterate through all records to update flef: links inside NoteStructure text fields and translations
		for(final FLEFRecord record : model.getRecords())
			updateNoteLinksRecursive(record, flefLinkOld, flefLinkNew);

		// The old record is now unreferenced: remove it from the records and from every index
		model.removeRecord(oldId);
	}

	/**
	 * Recursively searches for note nodes and updates markdown flef: URI references in their text and translations.
	 *
	 * @param record      the current FLEF record being inspected
	 * @param flefLinkOld the pattern of the old {@code flef:} link
	 * @param flefLinkNew the replacement {@code flef:} link
	 */
	private static void updateNoteLinksRecursive(final FLEFRecord record, final Pattern flefLinkOld, final String flefLinkNew){
		if(record == null)
			return;

		// If the record is a note structure, check and replace within its text field and the text of its translations
		if(NoteReader.isOfType(record)){
			final List<FLEFRecord> textNodes = new ArrayList<>(FLEFRecordHelper.findChildren(record, TAG_NOTE_TEXT));
			textNodes.addAll(FLEFRecordHelper.findChildren(record, TAG_NOTE_TRANSLATION_TEXT));
			for(final FLEFRecord textNode : textNodes){
				final String text = textNode.getValue();
				if(text == null)
					continue;

				final String updatedText = flefLinkOld.matcher(text).replaceAll(flefLinkNew);
				if(!updatedText.equals(text)){
					textNode.setValue(updatedText);

					LOGGER.info("Updated markdown note link to {} in record {}", flefLinkNew, record.getId());
				}
			}
		}

		// Recursively inspect child nodes (including translations or nested structures)
		final List<FLEFRecord> children = record.getChildren();
		for(int i = 0, childrenSize = children.size(); i < childrenSize; i ++){
			final FLEFRecord child = children.get(i);

			updateNoteLinksRecursive(child, flefLinkOld, flefLinkNew);
		}
	}

}
