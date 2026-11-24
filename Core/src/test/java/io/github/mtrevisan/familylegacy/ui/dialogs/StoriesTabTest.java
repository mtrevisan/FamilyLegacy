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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.dossier.EditorTab;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The editor of an individual has the stories in a tab of their own, apart from the notes, and saving one tab leaves the other one alone. */
class StoriesTabTest{

	@BeforeAll
	static void standardLevel(){
		//(the level is a preference of the user: the tests say the one they need)
		BaseRecordDialog.useLevelForStories(FeatureVisibilityService.inMemory(UxLevel.STANDARD));
	}

	@AfterAll
	static void userLevel(){
		BaseRecordDialog.useLevelForStories(null);
	}


	private static void collectLists(final Container container, final List<EntityListPanel> lists){
		for(final Component component : container.getComponents())
			if(component instanceof EntityListPanel list)
				lists.add(list);
			else if(component instanceof Container child)
				collectLists(child, lists);
	}

	private static FLEFRecord note(final String text, final String kind){
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text));
		if(kind != null)
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, kind));
		return note;
	}


	@Test
	void theStoriesAndTheNotesAreInTwoTabsAndSavingKeepsBoth() throws Exception{
		check(IndividualReader.TYPE, IndividualHandler.getInstance());
	}

	@Test
	void theEditorOfAGroupHasTheSameTabs() throws Exception{
		check(GroupReader.TYPE, GroupHandler.getInstance());
	}

	@Test
	void theEditorsOfAnEventAPlaceAndASourceHaveTheSameTabs() throws Exception{
		check(EventReader.TYPE, EventHandler.getInstance());
		check(PlaceReader.TYPE, PlaceHandler.getInstance());
		check(SourceReader.TYPE, SourceHandler.getInstance());
	}

	@Test
	void theStoriesAreApartOnlyFromTheStandardLevel(){
		Assertions.assertFalse(BaseRecordDialog.storiesApart(FeatureVisibilityService.inMemory(UxLevel.BASIC)));
		Assertions.assertTrue(BaseRecordDialog.storiesApart(FeatureVisibilityService.inMemory(UxLevel.STANDARD)));
		Assertions.assertTrue(BaseRecordDialog.storiesApart(FeatureVisibilityService.inMemory(UxLevel.ADVANCED)));

		//(below it the one list of the notes has them all)
		Assertions.assertEquals(PanelKey.NOTE, PanelKey.storyPanel(false));
		Assertions.assertEquals(PanelKey.NOTE, PanelKey.notePanel(false));
		Assertions.assertEquals(PanelKey.STORY, PanelKey.storyPanel(true));
		Assertions.assertEquals(PanelKey.NOTE_EXCEPT_STORIES, PanelKey.notePanel(true));
	}

	@Test
	void aNoteWhoseKindChangedMovesToTheOtherListAndIsSavedOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(note("A remark.", null))
			.addChild(note("A tale.", NoteReader.ENUM_KIND_STORY));
		model.addRecord(individual);

		final List<String> storyTexts = new ArrayList<>();
		final List<String> noteTexts = new ArrayList<>();
		final List<String> saved = new ArrayList<>();
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final BaseRecordDialog dialog = (BaseRecordDialog)IndividualHandler.getInstance()
					.createEditDialog(null, model, individual);
				final EntityListPanel stories = (EntityListPanel)dialog.components.getPanel(PanelKey.STORY);
				final EntityListPanel notes = (EntityListPanel)dialog.components.getPanel(PanelKey.NOTE_EXCEPT_STORIES);

				//the story becomes a research note: the list of the stories lets it go, the other one takes it
				final FLEFRecord tale = stories.getItems().getFirst();
				FLEFRecordHelper.updateChildValue(tale, NoteReader.TAG_KIND, NoteReader.ENUM_KIND_RESEARCH);
				dialog.adoptNote(tale);
				stories.getList().clearSelection();
				stories.setItems(stories.getItems().stream().filter(NoteReader::isStory).toList());

				stories.getItems().forEach(item -> storyTexts.add(NoteReader.extractText(item)));
				notes.getItems().forEach(item -> noteTexts.add(NoteReader.extractText(item)));

				dialog.components.save(individual);
				//(the editor puts the record back as it was if it is closed without saving: the record is read before)
				saved.addAll(FLEFRecordHelper.findChildren(individual, NoteReader.TAG_NOTE).stream()
					.map(n -> NoteReader.extractKind(n) + ":" + NoteReader.extractText(n))
					.toList());
				dialog.dispose();
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(List.of(), storyTexts);
		Assertions.assertEquals(List.of("A remark.", "A tale."), noteTexts);
		Assertions.assertEquals(List.of("general:A remark.", "research:A tale."), saved.stream().sorted().toList(),
			"each note is saved by one list only, with its new kind");
	}

	@Test
	void aStoryRowOfAnEditorWithoutTheStoriesTabOpensTheNotesTab() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord story = note("A tale.", NoteReader.ENUM_KIND_STORY);
		//(the editors of a repository have no tab of the stories at any level)
		final FLEFRecord event = FLEFRecord.createMainRecord("RE1", RepositoryReader.TYPE)
			.addChild(story);
		model.addRecord(event);

		final String[] shown = new String[1];
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final BaseRecordDialog dialog = (BaseRecordDialog)RepositoryHandler.getInstance()
					.createEditDialog(null, model, event);
				dialog.showEntry(EditorTab.STORIES, story);

				final JPanel tab = (JPanel)dialog.tabbedPane.getSelectedComponent();
				shown[0] = (String)tab.getClientProperty(BaseRecordDialog.PANEL_PROPERTY_NAME);
				dialog.dispose();
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertEquals(EditorTab.NOTES, shown[0]);
	}

	private static void check(final String type, final RecordTypeHandler<?> handler) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord individual = FLEFRecord.createMainRecord("R1", type)
			.addChild(note("A remark.", null))
			.addChild(note("A tale.", NoteReader.ENUM_KIND_STORY))
			.addChild(note("Another remark.", NoteReader.ENUM_KIND_RESEARCH));
		model.addRecord(individual);

		final List<String> storyTexts = new ArrayList<>();
		final List<String> noteTexts = new ArrayList<>();
		final List<String> tabs = new ArrayList<>();
		final List<FLEFRecord> saved = new ArrayList<>();
		final List<Throwable> failures = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			try{
				final BaseRecordDialog dialog = (BaseRecordDialog)handler
					.createEditDialog(null, model, individual);
				for(int i = 0; i < dialog.tabbedPane.getTabCount(); i ++){
					final JPanel tab = (JPanel)dialog.tabbedPane.getComponentAt(i);
					final String name = (String)tab.getClientProperty(BaseRecordDialog.PANEL_PROPERTY_NAME);
					tabs.add(name);
					final List<EntityListPanel> lists = new ArrayList<>();
					collectLists(tab, lists);
					for(final EntityListPanel list : lists)
						for(final FLEFRecord item : list.getItems()){
							final String text = NoteReader.extractText(item);
							if(EditorTab.STORIES.equals(name))
								storyTexts.add(text);
							else if(EditorTab.NOTES.equals(name))
								noteTexts.add(text);
						}
				}

				//saving the editor writes both tabs on the same tag: neither may erase the other
				dialog.components.save(individual);
				saved.addAll(FLEFRecordHelper.findChildren(individual, NoteReader.TAG_NOTE));
				dialog.dispose();
			}
			catch(final Throwable t){
				failures.add(t);
			}
		});

		Assertions.assertTrue(failures.isEmpty(), failures.toString());
		Assertions.assertTrue(tabs.indexOf(EditorTab.STORIES) >= 0 && tabs.indexOf(EditorTab.STORIES) < tabs.indexOf(EditorTab.NOTES), tabs.toString());
		Assertions.assertEquals(List.of("A tale."), storyTexts);
		Assertions.assertEquals(List.of("A remark.", "Another remark."), noteTexts);

		Assertions.assertEquals(3, saved.size());
		Assertions.assertEquals(1, saved.stream().filter(NoteReader::isStory).count());
		Assertions.assertEquals(1, saved.stream().filter(NoteReader::isResearch).count());
	}

}
