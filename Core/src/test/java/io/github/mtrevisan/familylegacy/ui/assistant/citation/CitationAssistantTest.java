package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.CitationHost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


class CitationAssistantTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @R1@ REPO
		1 NAME Archivio di Stato di Venezia
		0 @R2@ REPO
		1 NAME Family History Library
		0 @M1@ OBJE
		1 FILE /scans/parish/film1234567_frame245.jpg
		2 FORM jpg
		0 @S1@ SOUR
		1 TITL Parish registers
		1 OBJE @M1@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static FLEFRecord source(final FLEFModel model){
		return model.getRecordsByType("SOURCE").getFirst();
	}

	private static void onEdt(final Runnable action){
		try{
			SwingUtilities.invokeAndWait(action);
		}
		catch(final InvocationTargetException e){
			if(e.getCause() instanceof RuntimeException re)
				throw re;
			if(e.getCause() instanceof Error er)
				throw er;
			throw new IllegalStateException(e.getCause());
		}
		catch(final InterruptedException e){
			Thread.currentThread().interrupt();
		}
	}

	private static <T extends Component> List<T> find(final Container container, final Class<T> type){
		final List<T> found = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(type.isInstance(child))
				found.add(type.cast(child));
			if(child instanceof Container nested)
				found.addAll(find(nested, type));
		}
		return found;
	}


	@Test
	void theAcronymIsMadeOfTheInitialsOfTheWords(){
		Assertions.assertEquals("asv", CitationHints.acronym("Archivio di Stato di Venezia"));
		Assertions.assertEquals("fhl", CitationHints.acronym("Family History Library"));
	}

	@Test
	void aRepositoryIsFoundByNameOrAcronym() throws IOException{
		final FLEFModel model = convert();

		Assertions.assertEquals(1, CitationHints.repositoryMatches(model, List.of("FHL")).size());
		Assertions.assertEquals(1, CitationHints.repositoryMatches(model, List.of("archivio di stato di venezia")).size());
		//no repository is guessed from a partial or unrelated text
		Assertions.assertTrue(CitationHints.repositoryMatches(model, List.of("Archivio", "Venezia", "x")).isEmpty());
		Assertions.assertTrue(CitationHints.repositoryMatches(model, List.of()).isEmpty());
		Assertions.assertTrue(CitationHints.repositoryMatches(null, List.of("FHL")).isEmpty());
	}

	@Test
	void theFileNameOfAMediaIsAClue() throws IOException{
		final FLEFModel model = convert();

		final List<CitationHints.Hint> hints = CitationHints.mediaHints(model, source(model));

		Assertions.assertEquals(1, hints.size(), hints.toString());
		Assertions.assertEquals("Film: 1234567, Frame: 245", hints.getFirst().locator());
		Assertions.assertTrue(CitationHints.mediaHints(model, null).isEmpty());
		Assertions.assertTrue(CitationHints.mediaHints(null, source(model)).isEmpty());
	}


	@Test
	void theLocatorIsWrittenOnlyWhenTheUserAppliesIt(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		onEdt(() -> {
			final AtomicReference<String> applied = new AtomicReference<>();
			final CitationAssistantDialog dialog = new CitationAssistantDialog(null, null, null, "", applied::set, null);

			final JTextArea reference = find(dialog.getContentPane(), JTextArea.class).stream()
				.filter(JTextArea::isEditable).findFirst().orElseThrow();
			reference.setText("vol. 3, p. 45, n. 12");
			//typing is not applying
			Assertions.assertNull(applied.get());

			find(dialog.getContentPane(), JButton.class).stream()
				.filter(button -> button.getText().equals("Apply to Citation")).findFirst().orElseThrow().doClick();

			Assertions.assertEquals("Volume: 3, Page: 45, Entry: 12", applied.get());
			Assertions.assertFalse(dialog.isDisplayable());
		});
	}

	@Test
	void escapeClosesTheAssistantWithoutChangingAnything(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		onEdt(() -> {
			final AtomicReference<String> applied = new AtomicReference<>();
			final CitationAssistantDialog dialog = new CitationAssistantDialog(null, null, null, "p. 4", applied::set, null);

			final Object binding = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.get(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
			Assertions.assertNotNull(binding);
			dialog.getRootPane().getActionMap().get(binding)
				.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "escape"));

			Assertions.assertFalse(dialog.isDisplayable());
			Assertions.assertNull(applied.get());
		});
	}

	@Test
	void theApplyButtonIsDisabledWhenThereIsNothingToApply(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		onEdt(() -> {
			final CitationAssistantDialog dialog = new CitationAssistantDialog(null, null, null, "some words", locator -> {}, null);

			final AbstractButton apply = find(dialog.getContentPane(), JButton.class).stream()
				.filter(button -> button.getText().equals("Apply to Citation")).findFirst().orElseThrow();
			Assertions.assertFalse(apply.isEnabled());
			dialog.dispose();
		});
	}


	/** An editor of a citation, with a field inside. */
	private static final class Editor extends JPanel implements CitationHost{

		private final JPanel field = new JPanel();


		Editor(){
			add(field);
		}


		@Override
		public String locator(){
			return "";
		}

		@Override
		public void applyLocator(final String locator){}

		@Override
		public FLEFRecord citedSource(){
			return null;
		}

	}


	@Test
	void theActionIsOfferedOnlyInTheEditorOfACitation(){
		final AssistantAction action = new CitationAssistantContributor().actions().getFirst();
		final Editor editor = new Editor();

		Assertions.assertEquals("sources.citation", action.id());
		Assertions.assertTrue(action.isApplicable(new AssistantContext(null, editor.field, null)));
		Assertions.assertFalse(action.isApplicable(new AssistantContext(null, new JPanel(), null)));
		Assertions.assertFalse(action.isApplicable(new AssistantContext(null, null, null)));
	}

}
