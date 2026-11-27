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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.List;


/**
 * The quick entry of a source: a title, where it is kept (optional) and, for a citation, where in it. It writes the records that the full
 * editors write: the source with its title (one name structure with the text), the repository citation, the repository if it is new,
 * and the citation of the source that says it {@code supports} what it is cited for (almost always the case).
 * <p>
 * Atomic like the other creations: the records are built apart and added to the model together.
 */
public final class SourceQuickEntry{

	/**
	 * What a creation added to the model.
	 *
	 * @param sourceId	The id of the source.
	 * @param recordIds	The ids of every record that was added (the source included), in the order they were added.
	 */
	public record Created(String sourceId, List<String> recordIds){

		public Created{
			recordIds = List.copyOf(recordIds);
		}

	}


	private static final String DIGITS = "0123456789";


	private SourceQuickEntry(){}


	/**
	 * Creates a source with a title, kept in a repository (a new one, or one of the file) if there is one.
	 *
	 * @param model	The model to add the records to.
	 * @param title	The title (not blank).
	 * @param repository	Where it is kept ({@code null} or {@link RecordRef#NONE} if not known).
	 * @return	What was added.
	 * @throws IllegalArgumentException	If the title is blank, or the repository is said to be in the file and is not (nothing is added).
	 */
	public static Created createSource(final FLEFModel model, final String title, final RecordRef repository){
		final String text = StringUtils.normalizeSpace(title);
		if(StringUtils.isEmpty(text))
			throw new IllegalArgumentException("A source needs a title");

		final RecordRef kept = (repository != null? repository: RecordRef.NONE);
		if(kept.existingId() != null){
			final FLEFRecord record = model.getRecordById(kept.existingId());
			if(record == null || !Strings.CI.equals(RepositoryReader.TYPE, record.getTag()))
				throw new IllegalArgumentException("Not a repository of the file: " + kept.existingId());
		}

		final List<FLEFRecord> records = new ArrayList<>();
		try{
			final FLEFRecord source = FLEFRecord.createMainRecord(SourceReader.TYPE, SourceReader.ID_PREFIX, model);
			records.add(source);
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE_VALUE, text)));

			String repositoryId = kept.existingId();
			if(kept.newName() != null){
				final FLEFRecord repositoryRecord = FLEFRecord.createMainRecord(RepositoryReader.TYPE, RepositoryReader.ID_PREFIX, model)
					.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
						.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, kept.newName())))
					.addChild(AuditStamp.now());
				records.add(repositoryRecord);
				repositoryId = repositoryRecord.getId();
			}
			if(repositoryId != null)
				source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
					.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryCitationReader.TAG_REPOSITORY, repositoryId)));
			source.addChild(AuditStamp.now());

			//the records are in the model together, only now
			final List<String> ids = new ArrayList<>(records.size());
			for(final FLEFRecord record : records){
				model.addRecord(record);
				ids.add(record.getId());
			}
			return new Created(source.getId(), ids);
		}
		catch(final RuntimeException re){
			for(final FLEFRecord record : records)
				if(!model.hasRecord(record.getId()))
					FLEFRecord.releaseReservedId(StringUtils.stripEnd(record.getId(), DIGITS));
			throw re;
		}
	}

	/**
	 * @param sourceId	The id of the source that is cited.
	 * @param locator	Where in the source ({@code null} or blank if not said).
	 * @return	The citation of the source (to be put in the list of the sources of a record): the source, the locator, and the stance
	 * 	{@code supports}.
	 */
	public static FLEFRecord citationOf(final String sourceId, final String locator){
		final FLEFRecord citation = FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId));
		if(StringUtils.isNotBlank(locator))
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, StringUtils.normalizeSpace(locator)));
		return citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS));
	}

}
