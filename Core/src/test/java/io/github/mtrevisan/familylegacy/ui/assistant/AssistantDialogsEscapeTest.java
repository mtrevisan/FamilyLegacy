package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.HistoricalDateAssistantDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.DateStructureDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
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
import java.util.List;


/** The dialogs of the assistants close with ESC, and only themselves, over the dialog they were opened from. */
class AssistantDialogsEscapeTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** What pressing ESC does in a dialog: the action its root pane binds to the key, wherever the focus is in the window. */
	private static void pressEscape(final JDialog dialog){
		final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(GUIHelper.ESCAPE_STROKE);
		Assertions.assertNotNull(binding, "ESC is not bound in " + dialog.getClass().getSimpleName());
		final Action action = dialog.getRootPane().getActionMap().get(binding);
		Assertions.assertNotNull(action);
		action.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));
	}


	@Test
	void theHistoricalDateAssistantClosesOverTheEditorOfTheDate() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final JDialog editor = DateStructureDialog.createNew(null, model, "Date");

		final HistoricalDateAssistantDialog assistant = new HistoricalDateAssistantDialog(editor,
			HistoricalDateAssistantDialog.Tool.CONVERSION, "gregorian", "12 January 1400", model, suggestion -> {});
		pressEscape(assistant);

		Assertions.assertFalse(assistant.isDisplayable());
		Assertions.assertTrue(editor.isDisplayable(), "ESC in the assistant closed the editor of the date");
		//and then the editor closes with its own ESC
		pressEscape(editor);
		Assertions.assertFalse(editor.isDisplayable());
	}

	@Test
	void theListOfTheRecordsClosesOverTheEditorOfARecord() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final JDialog editor = HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model);

		final RecordChooserDialog chooser = new RecordChooserDialog(editor, model, model.getRecordsByType("individual"),
			(owner, m, record) -> {});
		pressEscape(chooser);

		Assertions.assertFalse(chooser.isDisplayable());
		Assertions.assertTrue(editor.isDisplayable(), "ESC in the list closed the editor below it");
	}

	@Test
	void thePaletteClosesOverTheEditorItWasOpenedFrom() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		final JDialog editor = HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model);

		final ResearchAssistantPalette palette = new ResearchAssistantPalette(new ResearchAssistantService(),
			new AssistantContext(editor, null, model));
		pressEscape(palette);

		Assertions.assertFalse(palette.isDisplayable());
		Assertions.assertTrue(editor.isDisplayable(), "ESC in the palette closed the editor below it");
	}

	@Test
	void theChainOfTheAssistantsClosesOneAtATime() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = convert();
		//an editor, the list of the records opened from the palette, the editor of one of them
		final JDialog editor = HandlerRegistry.getHandler(EventHandler.TYPE).createNewDialog(null, model);
		final RecordChooserDialog chooser = new RecordChooserDialog(editor, model, model.getRecordsByType("individual"),
			(owner, m, record) -> {});
		final JDialog opened = HandlerRegistry.getHandler("INDIVIDUAL").createEditDialog(chooser, model,
			model.getRecordsByType("individual").getFirst());

		pressEscape(opened);
		Assertions.assertFalse(opened.isDisplayable());
		Assertions.assertTrue(chooser.isDisplayable());
		Assertions.assertTrue(editor.isDisplayable());

		pressEscape(chooser);
		Assertions.assertFalse(chooser.isDisplayable());
		Assertions.assertTrue(editor.isDisplayable());
	}

}
