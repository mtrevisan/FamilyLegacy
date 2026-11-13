package io.github.mtrevisan.familylegacy.ui.tools.consanguinity;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.dialogs.ProgressDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** The tool of the consanguinity report shows the report once it is computed, and nothing else is left over it. */
class ConsanguinityReportToolTest{

	//Xavier and Yvonne are first cousins (grandchildren of Gino and Gina), married, with a child (Zeno)
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Gino /Rossi/
		1 SEX M
		0 @I2@ INDI
		1 NAME Gina /Bianchi/
		1 SEX F
		0 @I3@ INDI
		1 NAME Aldo /Rossi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Berta /Rossi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Wanda /Verdi/
		1 SEX F
		0 @I6@ INDI
		1 NAME Walter /Neri/
		1 SEX M
		0 @I7@ INDI
		1 NAME Xavier /Rossi/
		1 SEX M
		0 @I8@ INDI
		1 NAME Yvonne /Neri/
		1 SEX F
		0 @I9@ INDI
		1 NAME Zeno /Rossi/
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I3@
		1 CHIL @I4@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I5@
		1 CHIL @I7@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I4@
		1 CHIL @I8@
		0 @F4@ FAM
		1 HUSB @I7@
		1 WIFE @I8@
		1 CHIL @I9@
		1 MARR
		2 DATE 1 JAN 1900
		""";

	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}


	@Test
	void theReportIsShownWhenItIsComputed() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		final FLEFModel model = convert();
		final JPanel panel = new JPanel();
		final JFrame frame = new JFrame("main");
		frame.add(panel);
		frame.setSize(600, 400);
		frame.setVisible(true);
		final ToolContext context = new ToolContext(model, "I1", panel, null);

		final List<String> texts = new ArrayList<>();
		final List<Boolean> progressAlongTheReport = new ArrayList<>();
		final List<Throwable> errors = new ArrayList<>();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> errors.add(e));
		final Timer watcher = new Timer(20, e -> {
			boolean report = false;
			boolean progress = false;
			for(final Window window : Window.getWindows())
				if(window.isShowing()){
					if(window instanceof ConsanguinityReportDialog dialog){
						report = true;
						if(texts.isEmpty())
							texts.add(dialog.reportText());
					}
					else if(window instanceof ProgressDialog)
						progress = true;
				}
			if(report)
				progressAlongTheReport.add(progress);
		});
		watcher.start();
		SwingUtilities.invokeLater(() -> new ConsanguinityReportTool().run(context));
		try{
			for(int i = 0; i < 100 && texts.isEmpty(); i ++)
				Thread.sleep(50);
			Thread.sleep(300);
		}
		finally{
			watcher.stop();
			SwingUtilities.invokeAndWait(() -> {
				for(final Window window : Window.getWindows())
					if(window != frame)
						window.dispose();
			});
			frame.dispose();
		}

		Assertions.assertTrue(errors.isEmpty(), errors.toString());
		Assertions.assertEquals(1, texts.size(), "the report was not shown");
		Assertions.assertTrue(texts.get(0).contains("Xavier"), texts.get(0));
		//the window that said that the report was being computed is not there any more when the report is
		Assertions.assertFalse(progressAlongTheReport.contains(true), progressAlongTheReport.toString());
	}

}
