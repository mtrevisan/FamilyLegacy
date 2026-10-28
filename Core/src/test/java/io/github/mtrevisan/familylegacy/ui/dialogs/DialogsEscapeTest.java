package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
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
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
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


	private static FLEFModel convert() throws IOException{
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


	@Test
	void everyRecordDialogClosesWithEscAndOnlyItself() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final int records = model.getRecords().size();
		final List<String> audited = new ArrayList<>();
		final List<String> failed = new ArrayList<>();

		for(final String type : RECORD_TYPES){
			//the dialogs opened one from the other: a record dialog over another one
			final JDialog parent = new JDialog((java.awt.Window)null, "parent");
			parent.pack();
			final JDialog child;
			try{
				child = HandlerRegistry.getHandler(type).createNewDialog(parent, model);
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
			final JDialog parent = new JDialog((java.awt.Window)null, "parent");
			parent.pack();
			final JDialog child;
			try{
				child = HandlerRegistry.getHandler(type).createNewDialog(parent, model);
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
		final JDialog first = HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model);
		final JDialog second = HandlerRegistry.getHandler(IndividualHandler.TYPE).createNewDialog(first, model);
		final JDialog third = HandlerRegistry.getHandler(PlaceHandler.TYPE).createNewDialog(second, model);

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
		final JDialog parent = new JDialog((java.awt.Window)null, "parent");
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
