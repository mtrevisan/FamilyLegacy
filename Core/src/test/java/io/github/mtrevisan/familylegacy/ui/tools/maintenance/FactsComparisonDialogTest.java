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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.comparison.IndividualFacts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;


/** The dialog of the facts of some individuals: the rows, the filter on the differences, and how it looks. */
class FactsComparisonDialogTest{

	private static IndividualFacts.Comparison comparison(){
		return new IndividualFacts.Comparison(List.of("I1", "I2", "I3"),
			List.of("Giovanni Masutti [I1]", "Giovanni Masutti [I2]", "Giovanni Masutti [I3]"),
			List.of(
				new IndividualFacts.Row("name", "Name", List.of("Giovanni Masutti", "Giovanni Masutti", "Giovanni Masutti"), IndividualFacts.State.SAME),
				new IndividualFacts.Row("birth_date", "Born", List.of("1850-03-01", "1851-04-02", "1850-03-01"), IndividualFacts.State.DIFFERENT),
				new IndividualFacts.Row("birth_place", "Born in", List.of("Borgo, Treviso, Veneto", "", "Borgo, Treviso, Veneto"), IndividualFacts.State.MISSING),
				new IndividualFacts.Row("children", "Children", List.of("Antonio Masutti; Maria Masutti; Luigi Masutti; Teresa Masutti; Pietro Masutti; Carlo Masutti; Anna Masutti",
					"Antonio Masutti", ""), IndividualFacts.State.DIFFERENT)));
	}


	@Test
	void theDifferencesCanBeTheOnlyRowsShown() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		SwingUtilities.invokeAndWait(() -> {
			final FactsComparisonDialog dialog = new FactsComparisonDialog(null, comparison(), 0);

			Assertions.assertEquals(4, dialog.shownRows().size());
			//a row for each fact, a column for each individual, and one for the names of the facts
			Assertions.assertEquals(4, dialog.table().getColumnCount());
			dialog.setOnlyDifferences(true);
			Assertions.assertEquals(List.of("birth_date", "birth_place", "children"), dialog.shownRows().stream().map(IndividualFacts.Row::key).toList());
			dialog.setOnlyDifferences(false);
			Assertions.assertEquals(4, dialog.shownRows().size());
			dialog.dispose();
		});
	}

	@Test
	void aTallCellMakesItsRowTaller() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		SwingUtilities.invokeAndWait(() -> {
			final FactsComparisonDialog dialog = new FactsComparisonDialog(null, comparison(), 0);

			//the children, many names in a narrow column, take more than a line; the name, a line
			Assertions.assertTrue(dialog.table().getRowHeight(3) > dialog.table().getRowHeight(0),
				dialog.table().getRowHeight(3) + " / " + dialog.table().getRowHeight(0));
			dialog.dispose();
		});
	}

	/** The dialog is painted to a picture: in the folder of the environment variable FACTS_SHOTS if there is one, otherwise in a temporary one. */
	@Test
	void picture(@TempDir final Path temporary) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
		final String shots = System.getenv("FACTS_SHOTS");
		final File folder = (shots != null? new File(shots): temporary.toFile());

		final FactsComparisonDialog[] dialog = new FactsComparisonDialog[1];
		SwingUtilities.invokeAndWait(() -> {
			dialog[0] = new FactsComparisonDialog(null, comparison(), 4);
			dialog[0].setModal(false);
			dialog[0].setVisible(true);
		});
		for(int i = 0; i < 4; i ++)
			SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {
			final BufferedImage image = new BufferedImage(dialog[0].getWidth(), dialog[0].getHeight(), BufferedImage.TYPE_INT_RGB);
			dialog[0].getRootPane().paint(image.getGraphics());
			try{
				Assertions.assertTrue(ImageIO.write(image, "png", new File(folder, "facts.png")));
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
			dialog[0].dispose();
		});
		Assertions.assertTrue(new File(folder, "facts.png").length() > 0);
	}

}
