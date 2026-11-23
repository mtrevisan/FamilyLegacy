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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.OpenTasks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/** The tasks panel of the main window, driven: what it lists, the filter on the selected record, the change of status. */
class TasksPanelTest{

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);


	private static FLEFRecord task(final String id, final String due, final String questionId){
		final FLEFRecord task = FLEFRecord.createMainRecord(id, ResearchTaskReader.TYPE);
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Task " + id));
		if(questionId != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, questionId));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_OPEN));
		if(due != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, due));
		return task;
	}

	private static FLEFRecord question(final String id, final String targetId){
		final FLEFRecord question = FLEFRecord.createMainRecord(id, ResearchQuestionReader.TYPE);
		question.addChild(FLEFRecord.createChildWithTagAndValue(ResearchQuestionReader.TAG_TITLE, "Who were the parents of " + targetId + "?"));
		question.addChild(FLEFRecord.createChildWithTag(ResearchQuestionReader.TAG_TARGET)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", targetId)));
		return question;
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(question("Q1", "I1"));
		model.addRecord(question("Q2", "I2"));
		model.addRecord(task("T1", "2026-05-20", "Q1"));
		model.addRecord(task("T2", "2026-07-01", "Q2"));
		model.addRecord(task("T3", null, null));
		return model;
	}

	private static List<String> ids(final List<OpenTasks.Task> tasks){
		return tasks.stream()
			.map(task -> task.record().getId())
			.toList();
	}


	@Test
	void listsTheOpenTasksAndCountsTheOverdueOnes() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final FLEFModel model = model();
		final List<String> listed = new ArrayList<>();
		final AtomicReference<String> summary = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final TasksPanel panel = new TasksPanel(() -> model, () -> null, () -> TODAY);
			listed.addAll(ids(panel.shownTasks()));
			summary.set(panel.summaryText());
		});

		Assertions.assertEquals(List.of("T1", "T2", "T3"), listed);
		Assertions.assertEquals("3 tasks, 1 overdue", summary.get());
	}

	@Test
	void theFilterKeepsTheTasksOfTheSelectedRecord() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final FLEFModel model = model();
		final AtomicReference<String> selection = new AtomicReference<>("I2");
		final List<List<String>> listed = new ArrayList<>();
		final AtomicReference<String> summary = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final TasksPanel panel = new TasksPanel(() -> model, selection::get, () -> TODAY);
			panel.setOnlySelection(true);
			listed.add(ids(panel.shownTasks()));

			selection.set("I1");
			panel.selectionChanged();
			listed.add(ids(panel.shownTasks()));

			selection.set(null);
			panel.selectionChanged();
			listed.add(ids(panel.shownTasks()));
			summary.set(panel.summaryText());

			panel.setOnlySelection(false);
			listed.add(ids(panel.shownTasks()));
		});

		Assertions.assertEquals(List.of("T2"), listed.get(0));
		Assertions.assertEquals(List.of("T1"), listed.get(1));
		Assertions.assertEquals(List.of(), listed.get(2));
		Assertions.assertEquals("nothing to do, no record is selected in the view", summary.get());
		Assertions.assertEquals(List.of("T1", "T2", "T3"), listed.get(3));
	}

	@Test
	void aCompletedTaskLeavesTheList() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final FLEFModel model = model();
		final AtomicInteger changes = new AtomicInteger();
		final List<String> listed = new ArrayList<>();
		final List<String> menu = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TasksPanel panel = new TasksPanel(() -> model, () -> null, () -> TODAY);
			panel.setAfterChange(changes::incrementAndGet);
			final OpenTasks.Task first = panel.shownTasks().getFirst();

			final JPopupMenu popup = panel.createPopup(first);
			for(final Component item : popup.getComponents())
				if(item instanceof JMenuItem menuItem)
					menu.add(menuItem.getText() + (menuItem.isEnabled()? "": " (disabled)"));

			panel.changeStatus(first, ResearchTaskReader.ENUM_STATUS_COMPLETED);
			listed.addAll(ids(panel.shownTasks()));
		});

		Assertions.assertEquals(List.of("Edit Task…", "Mark as open (disabled)", "Mark as in progress", "Mark as completed",
			"Mark as abandoned", "Edit Question “Who were the parents of I1?”…"), menu);
		Assertions.assertEquals(1, changes.get());
		Assertions.assertEquals(List.of("T2", "T3"), listed);
		Assertions.assertEquals(ResearchTaskReader.ENUM_STATUS_COMPLETED,
			FLEFRecordHelper.getChildValue(model.getRecordById("T1"), ResearchTaskReader.TAG_STATUS));
	}

	@Test
	void aNewTaskIsOfTheQuestionAboutTheSelectedRecord() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final FLEFModel model = model();
		final FLEFRecord second = question("Q3", "I1");
		model.addRecord(second);
		final AtomicReference<String> selection = new AtomicReference<>();
		final List<String> opened = new ArrayList<>();
		final List<String> menu = new ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			final TasksPanel panel = new TasksPanel(() -> model, selection::get, () -> TODAY);
			panel.setNewTaskOpener((m, question) -> opened.add(question != null? question.getId(): "none"));

			//one question about the record: it is filled in
			selection.set("I2");
			panel.createTask();
			//no record selected, or no question about it: a task of no question
			selection.set(null);
			panel.createTask();
			selection.set("I9");
			panel.createTask();

			//two questions: the user picks one, or none
			final JPopupMenu chooser = panel.createQuestionChooser(model, List.of(model.getRecordById("Q1"), second));
			for(final Component item : chooser.getComponents())
				if(item instanceof JMenuItem menuItem){
					menu.add(menuItem.getText());
					menuItem.doClick();
				}
		});

		Assertions.assertEquals(List.of("Q2", "none", "none", "Q1", "Q3", "none"), opened);
		Assertions.assertEquals(List.of("Task of “Who were the parents of I1?”", "Task of “Who were the parents of I1?”",
			"Without a Question"), menu);
	}

	@Test
	void theDockMenuOffersTheOtherSides() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final FLEFModel model = model();
		final List<String> docked = new ArrayList<>();
		final List<String> menu = new ArrayList<>();
		final AtomicReference<TasksPanel.Dock> dock = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final TasksPanel panel = new TasksPanel(() -> model, () -> null, () -> TODAY);
			panel.setOnDock(side -> docked.add(side.name()));
			panel.setDock(TasksPanel.Dock.of("right"));
			dock.set(panel.getDock());
			for(final Component item : panel.createDockMenu().getComponents())
				if(item instanceof JMenuItem menuItem){
					menu.add(menuItem.getText() + (menuItem.isEnabled()? "": " (disabled)"));
					if(menuItem.isEnabled() && docked.isEmpty())
						menuItem.doClick();
				}
		});

		Assertions.assertEquals(TasksPanel.Dock.RIGHT, dock.get());
		Assertions.assertEquals(List.of("BOTTOM"), docked);
		Assertions.assertEquals(List.of("Dock at the Bottom", "Dock at the Top", "Dock at the Left", "Dock at the Right (disabled)"), menu);
		Assertions.assertEquals(TasksPanel.Dock.BOTTOM, TasksPanel.Dock.of("nonsense"));
	}

	/** Paints the panel to a PNG in the folder named by the environment variable {@code TASKS_PANEL_IMAGES}, to look at it. */
	@Test
	void paintsThePanel() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");
		final String folder = System.getenv("TASKS_PANEL_IMAGES");
		Assumptions.assumeTrue(folder != null, "Set TASKS_PANEL_IMAGES to a folder to get the image");
		final FLEFModel model = model();
		final AtomicReference<JFrame> frame = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> {
			final JFrame window = new JFrame();
			window.add(new TasksPanel(() -> model, () -> null, () -> TODAY));
			window.setSize(1000, 200);
			window.setVisible(true);
			frame.set(window);
		});
		for(int i = 0; i < 3; i ++)
			SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {
			final JFrame window = frame.get();
			final BufferedImage image = new BufferedImage(window.getContentPane().getWidth(), window.getContentPane().getHeight(),
				BufferedImage.TYPE_INT_RGB);
			final Graphics2D g = image.createGraphics();
			window.getContentPane().paint(g);
			g.dispose();
			try{
				ImageIO.write(image, "png", new File(folder, "tasks-panel.png"));
			}
			catch(final Exception e){
				throw new IllegalStateException(e);
			}
			window.dispose();
		});
	}

}
