package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


/** The indicator of the Research Assistant in the main window, wired as the application does. */
class AssistantIndicatorInFrameTest{

	/** Anna has two births (a conflict), and her child is born before her (a chronological error). */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1850
		1 BIRT
		2 DATE 1 JAN 1860
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1840
		1 FAMC @F1@
		0 @F1@ FAM
		1 WIFE @I1@
		1 CHIL @I2@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static JToolBar toolBar(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JToolBar toolBar)
				return toolBar;
			if(child instanceof Container nested){
				final JToolBar found = toolBar(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static String read(final JButton button) throws InvocationTargetException, InterruptedException{
		final String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> text[0] = button.getText());
		return text[0];
	}


	@Test
	void theIndicatorIsAtTheEndOfTheToolbarAndCountsTheFindings() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = convert();
		final FamilyLegacyFrame[] frame = new FamilyLegacyFrame[1];
		SwingUtilities.invokeAndWait(() -> frame[0] = new FamilyLegacyFrame(TreeType.FAMILY, model));
		try{
			final JToolBar toolBar = toolBar(frame[0].getContentPane());
			Assertions.assertNotNull(toolBar);

			//the last component of the toolbar, after the glue that pushes it to the right
			final Component last = toolBar.getComponent(toolBar.getComponentCount() - 1);
			Assertions.assertInstanceOf(JButton.class, last);
			final JButton indicator = (JButton)last;
			Assertions.assertTrue(read(indicator).startsWith("Assistant"), read(indicator));
			Assertions.assertTrue(indicator.getToolTipText().contains("Ctrl+Shift+A"), indicator.getToolTipText());

			//the first computation starts by itself when the window is built, away from the event dispatch thread
			final long deadline = System.currentTimeMillis() + 20_000;
			while(read(indicator).endsWith("…") && System.currentTimeMillis() < deadline)
				Thread.sleep(100);

			final String text = read(indicator);
			Assertions.assertFalse(text.endsWith("…"), "the findings were not computed: " + text);
			//an error and a warning at least: the child born before the mother, the two births
			Assertions.assertTrue(text.contains("✖"), text);
			Assertions.assertTrue(text.contains("⚠"), text);
		}
		finally{
			SwingUtilities.invokeAndWait(() -> frame[0].dispose());
		}
	}

}
