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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.AgentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContactNameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.HeaderHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.TextValueVariantHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Every dialog closes with ESC, and only itself: the dialogs that opened it, one from the other, stay as they are. The binding is
 * the one a real key press runs, so running its action is what pressing ESC in the dialog does.
 */
class DialogsEscapeTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		""";

	private static final List<String> RECORD_TYPES = List.of(ConclusionHandler.TYPE, ContextImpactHandler.TYPE,
		CulturalNormHandler.TYPE, EventHandler.TYPE, EventParticipationHandler.TYPE, GroupAttributeHandler.TYPE,
		GroupHandler.TYPE, HistoricEventHandler.TYPE, IdentityHypothesisHandler.TYPE, IndividualAttributeHandler.TYPE,
		IndividualHandler.TYPE, MediaHandler.TYPE, PlaceHandler.TYPE, PlaceRelationshipHandler.TYPE, RelationshipHandler.TYPE,
		RepositoryHandler.TYPE, ResearchActivityHandler.TYPE, ResearchQuestionHandler.TYPE, ResearchTaskHandler.TYPE,
		SourceHandler.TYPE);


	/** The structures and citations, edited inside a record. */
	private static final List<String> STRUCTURE_TYPES = List.of(AgentHandler.TYPE, ContactHandler.TYPE, ContactNameHandler.TYPE,
		NameHandler.TYPE, NoteHandler.TYPE, PartHandler.TYPE, PersonalNameHandler.TYPE, TextValueVariantHandler.TYPE,
		SourceCitationHandler.TYPE, PlaceCitationHandler.TYPE, RepositoryCitationHandler.TYPE, HeaderHandler.TYPE);


	static List<String> recordTypes(){
		return RECORD_TYPES;
	}

	static List<String> structureTypes(){
		return STRUCTURE_TYPES;
	}

	static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** What pressing ESC does in a dialog: the action its root pane binds to the key, wherever the focus is in the window. */
	static void pressEscape(final JDialog dialog){
		final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
		Assertions.assertNotNull(binding, "ESC is not bound in " + dialog.getClass().getSimpleName());
		final Action action = dialog.getRootPane().getActionMap().get(binding);
		Assertions.assertNotNull(action, "ESC has no action in " + dialog.getClass().getSimpleName());
		action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
	}


	/** The first component of the dialog that can take the keyboard focus, text fields first. */
	static Component findFocusTarget(final Container container){
		Component fallback = null;
		for(final Component component : container.getComponents()){
			if(component instanceof JTextComponent && component.isFocusable() && component.isEnabled() && component.isShowing())
				return component;
			if(fallback == null && component instanceof JComponent && component.isFocusable() && component.isEnabled()
					&& component.isShowing() && component.getClass() != JPanel.class)
				fallback = component;
			if(component instanceof Container child){
				final Component found = findFocusTarget(child);
				if(found instanceof JTextComponent)
					return found;
				if(fallback == null)
					fallback = found;
			}
		}
		return fallback;
	}

	static void flushEventQueue() throws Exception{
		SwingUtilities.invokeAndWait(() -> {});
	}

	/**
	 * A real press of ESC: the dialog is shown, the key event is delivered to a component inside it (as the key press that follows the
	 * focus does), and it goes through the key bindings of the component and of the window, as it does for the user.
	 */
	static void typeEscape(final JDialog dialog) throws Exception{
		final Component target = findFocusTarget(dialog.getContentPane());
		Assertions.assertNotNull(target, "no focusable component in " + dialog.getTitle());
		final Component receiver = target;
		target.requestFocusInWindow();
		flushEventQueue();
		//the focus is the dialog's own (it may take a moment to be granted)
		final long deadline = System.currentTimeMillis() + 5_000L;
		while(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != target && System.currentTimeMillis() < deadline){
			//the window may not be active yet (another application can hold the OS focus): ask again
			dialog.toFront();
			target.requestFocusInWindow();
			Thread.sleep(50L);
			flushEventQueue();
		}
		Assertions.assertSame(target, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
			"the focus is not on the component in " + dialog.getTitle());

		final long now = System.currentTimeMillis();
		receiver.dispatchEvent(new KeyEvent(receiver, KeyEvent.KEY_PRESSED, now, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
		receiver.dispatchEvent(new KeyEvent(receiver, KeyEvent.KEY_RELEASED, now + 10, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
		flushEventQueue();
	}

	@Test
	void aRealKeyPressOfEscClosesEveryRecordDialogOnTheFocusedComponentAndOnlyIt() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final List<String> failed = new ArrayList<>();
		final List<String> notClosed = new ArrayList<>();

		final JDialog parent = new JDialog((Window)null, "parent");
		parent.pack();
		parent.setVisible(true);
		try{
			for(final String type : RECORD_TYPES){
				final JDialog child;
				try{
					child = (JDialog)HandlerRegistry.getHandler(type).createNewDialog(parent, model).window();
				}
				catch(final RuntimeException | AssertionError e){
					failed.add(type + ": " + e);
					continue;
				}
				child.setModal(false);
				child.setVisible(true);
				flushEventQueue();

				typeEscape(child);

				if(child.isDisplayable())
					notClosed.add(type);
				Assertions.assertTrue(parent.isDisplayable(), "ESC in " + type + " closed the dialog below it");
				child.dispose();
			}
		}
		finally{
			parent.dispose();
		}

		Assertions.assertTrue(failed.isEmpty(), "dialogs that could not be built: " + failed);
		Assertions.assertTrue(notClosed.isEmpty(), "dialogs not closed by a real ESC press: " + notClosed);
	}

	@Test
	void aRealKeyPressOfEscInAChainClosesOneDialogAtATime() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();

		final JDialog first = (JDialog)HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model).window();
		final JDialog second = (JDialog)HandlerRegistry.getHandler(IndividualHandler.TYPE).createNewDialog(first, model).window();
		first.setModal(false);
		second.setModal(false);
		first.setVisible(true);
		second.setVisible(true);
		flushEventQueue();

		try{
			typeEscape(second);
			Assertions.assertFalse(second.isDisplayable());
			Assertions.assertTrue(first.isDisplayable());

			typeEscape(first);
			Assertions.assertFalse(first.isDisplayable());
		}
		finally{
			second.dispose();
			first.dispose();
		}
	}

	@Test
	void everyRecordDialogClosesWithEscAndOnlyItself() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final int records = model.getRecords().size();
		final List<String> audited = new ArrayList<>();
		final List<String> failed = new ArrayList<>();

		for(final String type : RECORD_TYPES){
			//the dialogs opened one from the other: a record dialog over another one
			final JDialog parent = new JDialog((Window)null, "parent");
			parent.pack();
			final JDialog child;
			try{
				child = (JDialog)HandlerRegistry.getHandler(type).createNewDialog(parent, model).window();
			}
			catch(final RuntimeException | AssertionError e){
				failed.add(type + ": " + e);
				continue;
			}

			pressEscape(child);

			Assertions.assertFalse(child.isDisplayable(), type + " was not closed by ESC");
			Assertions.assertTrue(parent.isDisplayable(), "ESC in " + type + " closed the dialog below it");
			Assertions.assertEquals(records, model.getRecords().size(), type + " changed the model");
			audited.add(type);
			parent.dispose();
		}

		Assertions.assertTrue(failed.isEmpty(), "dialogs that could not be built: " + failed);
		Assertions.assertEquals(RECORD_TYPES.size(), audited.size());
	}

	@Test
	void theStructureAndCitationDialogsCloseWithEscAndOnlyThemselves() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final List<String> audited = new ArrayList<>();
		final List<String> notBuilt = new ArrayList<>();

		for(final String type : STRUCTURE_TYPES){
			final JDialog parent = new JDialog((Window)null, "parent");
			parent.pack();
			final JDialog child;
			try{
				child = (JDialog)HandlerRegistry.getHandler(type).createNewDialog(parent, model).window();
			}
			catch(final RuntimeException | AssertionError e){
				//a structure is edited inside a record: some of them cannot be built without the record that contains them
				notBuilt.add(type);
				continue;
			}

			pressEscape(child);

			Assertions.assertFalse(child.isDisplayable(), type + " was not closed by ESC");
			Assertions.assertTrue(parent.isDisplayable(), "ESC in " + type + " closed the dialog below it");
			audited.add(type);
			parent.dispose();
		}

		Assertions.assertTrue(notBuilt.isEmpty(), "dialogs that could not be built: " + notBuilt);
		Assertions.assertEquals(STRUCTURE_TYPES.size(), audited.size());
	}

	@Test
	void aChainOfDialogsClosesOneAtATime() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();

		//a record dialog, over it another one, over it a third one
		final JDialog first = (JDialog)HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model).window();
		final JDialog second = (JDialog)HandlerRegistry.getHandler(IndividualHandler.TYPE).createNewDialog(first, model).window();
		final JDialog third = (JDialog)HandlerRegistry.getHandler(PlaceHandler.TYPE).createNewDialog(second, model).window();

		pressEscape(third);
		Assertions.assertFalse(third.isDisplayable());
		Assertions.assertTrue(second.isDisplayable());
		Assertions.assertTrue(first.isDisplayable());

		pressEscape(second);
		Assertions.assertFalse(second.isDisplayable());
		Assertions.assertTrue(first.isDisplayable());

		pressEscape(first);
		Assertions.assertFalse(first.isDisplayable());
	}

	@Test
	void theDialogsOfTheToolsCloseWithEscAndOnlyThemselves(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final JDialog parent = new JDialog((Window)null, "parent");
		parent.pack();
		final JDialog child = new JDialog(parent, "tool");
		child.pack();
		ToolDialogs.installEscapeToClose(child);

		pressEscape(child);

		Assertions.assertFalse(child.isDisplayable());
		Assertions.assertTrue(parent.isDisplayable());
		parent.dispose();
	}

}
