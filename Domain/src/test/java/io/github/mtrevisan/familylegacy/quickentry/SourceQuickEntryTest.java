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
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


class SourceQuickEntryTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
	}


	private static FLEFRecord repository(final String id, final String name){
		return FLEFRecord.createMainRecord(id, RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, name)));
	}


	@Test
	void aSourceHasOneTitleThatIsOneNameStructureWithTheText(){
		final SourceQuickEntry.Created created = SourceQuickEntry.createSource(model, "  Parish of San   Martino, baptisms 1840-1860 ", null);

		final FLEFRecord source = model.getRecordById(created.sourceId());
		Assertions.assertEquals(List.of(created.sourceId()), created.recordIds());
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE).size());
		Assertions.assertEquals("Parish of San Martino, baptisms 1840-1860", FLEFRecordHelper.getChildValue(source,
			FLEFRecordHelper.composePath(SourceReader.TAG_TITLE, SourceReader.TAG_TITLE_VALUE)));
		Assertions.assertNull(FLEFRecordHelper.findChild(source, SourceReader.TAG_REPOSITORY));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(source, SourceReader.TAG_AUDIT));
	}

	@Test
	void aNewRepositoryIsCreatedAndCitedBySource(){
		final SourceQuickEntry.Created created = SourceQuickEntry.createSource(model, "Register", RecordRef.named("State Archive of Padova"));

		Assertions.assertEquals(2, created.recordIds().size());
		final FLEFRecord source = model.getRecordById(created.sourceId());
		final String repositoryId = FLEFRecordHelper.getChildValue(source, FLEFRecordHelper.composePath(SourceReader.TAG_REPOSITORY,
			RepositoryCitationReader.TAG_REPOSITORY));
		final FLEFRecord repository = model.getRecordById(repositoryId);
		Assertions.assertEquals("State Archive of Padova", FLEFRecordHelper.getChildValue(repository,
			FLEFRecordHelper.composePath(RepositoryReader.TAG_NAME, NameReader.TAG_VALUE)));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(repository, RepositoryReader.TAG_AUDIT));
	}

	@Test
	void anExistingRepositoryIsCitedAndNoneIsCreated(){
		model.addRecord(repository("R9", "State Archive"));

		SourceQuickEntry.createSource(model, "Register", RecordRef.existing("R9"));

		Assertions.assertEquals(1, model.getRecordsByType(RepositoryReader.TYPE).size());
		Assertions.assertEquals("R9", FLEFRecordHelper.getChildValue(model.getRecordsByType(SourceReader.TYPE).getFirst(),
			FLEFRecordHelper.composePath(SourceReader.TAG_REPOSITORY, RepositoryCitationReader.TAG_REPOSITORY)));
	}

	@Test
	void aBlankTitleOrAMissingRepositoryChangesNothing(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> SourceQuickEntry.createSource(model, "   ", null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> SourceQuickEntry.createSource(model, null, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> SourceQuickEntry.createSource(model, "Register", RecordRef.existing("R404")));
		Assertions.assertEquals(0, model.getRecordCount());

		//a source is not a repository
		final SourceQuickEntry.Created created = SourceQuickEntry.createSource(model, "Register", null);
		final int count = model.getRecordCount();
		Assertions.assertThrows(IllegalArgumentException.class, () -> SourceQuickEntry.createSource(model, "Other", RecordRef.existing(created.sourceId())));
		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void theCitationSupportsByDefaultAndHasTheLocatorIfThereIsOne(){
		final FLEFRecord citation = SourceQuickEntry.citationOf("S1", " book 2,  page 143 ");

		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals("book 2, page 143", FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR));
		Assertions.assertEquals(SourceCitationReader.STANCE_SUPPORTS, FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE));

		final FLEFRecord withoutLocator = SourceQuickEntry.citationOf("S1", "  ");
		Assertions.assertNull(FLEFRecordHelper.findChild(withoutLocator, SourceCitationReader.TAG_LOCATOR));
		Assertions.assertEquals(SourceCitationReader.STANCE_SUPPORTS, FLEFRecordHelper.getChildValue(withoutLocator, SourceCitationReader.TAG_STANCE));
	}

	@Test
	void aRepositoryIsFoundByItsNameLikeAPlace(){
		model.addRecord(repository("R1", "State Archive of Padova"));
		model.addRecord(repository("R2", "state archive"));
		model.addRecord(repository("R3", "Diocesan Archive"));

		Assertions.assertEquals(List.of("R2", "R1"), RepositoryLookup.find(model, "  STATE  archive ", 10).stream().map(FLEFRecord::getId).toList());
		Assertions.assertTrue(RepositoryLookup.find(model, "nothing", 10).isEmpty());
		Assertions.assertTrue(RepositoryLookup.find(model, " ", 10).isEmpty());
	}

	@Test
	void theRecordRefMakesNothingOfABlankNameOrId(){
		Assertions.assertTrue(RecordRef.named("  ").isNone());
		Assertions.assertTrue(RecordRef.existing(" ").isNone());
		Assertions.assertEquals("A B", RecordRef.named(" A   B ").newName());
	}

}
