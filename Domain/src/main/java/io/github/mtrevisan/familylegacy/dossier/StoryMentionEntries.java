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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/** The rows of the section of the dossier that tells the stories, written in other records, that link to the record. */
final class StoryMentionEntries{

	/** How long the text of a story is in a row. */
	private static final int SNIPPET_LENGTH = 80;


	private StoryMentionEntries(){}


	/**
	 * @param model	The model.
	 * @param recordId	The record the dossier is about.
	 * @param proofStatus	The proof status of a record (for the row).
	 * @return	A row for each story that links to the record, that opens the editor of the record the story is written in, on its stories
	 * 	(or on its notes, if the editor has no tab of the stories); the stories of the records that the privacy policy hides are not told.
	 */
	static List<DossierEntry> of(final FLEFModel model, final String recordId, final Function<FLEFRecord, ProofStatus> proofStatus){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final StoryMentions.Mention mention : StoryMentions.of(model, recordId)){
			final FLEFRecord owner = mention.owner();
			if(PrivacyService.isHidden(model, owner))
				continue;

			final String title = FLEFRecordHelper.getChildValue(mention.story(), NoteReader.TAG_TITLE);
			final String text = StringUtils.abbreviate(StringUtils.normalizeSpace(StringUtils.defaultString(NoteReader.extractText(mention.story()))),
				SNIPPET_LENGTH);
			final String label = I18N.tf("text.dossier.mention", describe(model, owner));
			final String value = (StringUtils.isNotEmpty(title)? title: text);
			final String subtitle = (StringUtils.isNotEmpty(title)? text: StringUtils.EMPTY);
			//(the editors that have a tab of the stories: of an individual, a group, an event, a place and a source; the tab is not there below the
			//standard level, and then the editor shows the notes tab)
			final boolean apart = Set.of(IndividualReader.TYPE, GroupReader.TYPE, EventReader.TYPE, PlaceReader.TYPE, SourceReader.TYPE).stream()
				.anyMatch(type -> type.equalsIgnoreCase(owner.getTag()));

			entries.add(new DossierEntry(label, value, subtitle, StringUtils.EMPTY, owner, DossierEntry.Kind.NORMAL, proofStatus.apply(owner))
				.withFocus((apart? EditorTab.STORIES: EditorTab.NOTES), mention.story()));
		}
		return entries;
	}

	private static String describe(final FLEFModel model, final FLEFRecord record){
		try{
			return RecordDisplay.describe(record, model);
		}
		catch(final RuntimeException ignored){
			return StringUtils.defaultString(record.getId());
		}
	}

}
