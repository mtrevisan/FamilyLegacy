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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapWorkspace;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.shell.ModelUtils;
import io.github.mtrevisan.familylegacy.ui.shell.ProjectionSwitcherPanel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/** The chronomap is one of the views of the switcher, and it follows the individual selected in the others. */
class ChronomapProjectionTest{

	//no places: nothing to geocode, hence no network
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
		1 BIRT
		2 DATE 1 JAN 1840
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}


	@Test
	void theChronomapIsOneOfTheProjections() throws IOException, InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = convert();
		final String root = ModelUtils.findLowestIndividualId(model);

		final List<Object> results = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ProjectionSwitcherPanel switcher = new ProjectionSwitcherPanel(TreeType.BIOLOGICAL, model);
			final List<String> selected = new ArrayList<>();
			switcher.setSelectionCallback(selected::add);
			switcher.loadRoot(root);

			switcher.setProjection(ProjectionType.CHRONOMAP);
			results.add(switcher.getCurrentProjectionType());
			results.add(switcher.getCurrentPanel() instanceof ChronomapWorkspace);
			results.add(switcher.getSelectedEntityId());
			results.add(selected.contains(root));

			//the individual selected in the chronomap is the one the other views are rooted on
			switcher.setProjection(ProjectionType.BIOLOGICAL_TREE);
			results.add(switcher.getCurrentProjectionType());
			results.add(switcher.getCurrentRootId());

			switcher.setProjection(ProjectionType.CHRONOMAP);
			results.add(switcher.getCurrentProjectionType());
		});

		Assertions.assertEquals(ProjectionType.CHRONOMAP, results.get(0));
		Assertions.assertEquals(true, results.get(1), "the chronomap is the panel shown");
		Assertions.assertEquals(root, results.get(2), "the chronomap starts from the individual of the previous view");
		Assertions.assertEquals(true, results.get(3), "the dossier is told which individual is selected");
		Assertions.assertEquals(ProjectionType.BIOLOGICAL_TREE, results.get(4));
		Assertions.assertEquals(root, results.get(5));
		Assertions.assertEquals(ProjectionType.CHRONOMAP, results.get(6), "back to the chronomap, that is created once");
	}

	@Test
	void everyProjectionHasAName(){
		for(final ProjectionType type : ProjectionType.values())
			Assertions.assertFalse(type.displayName().isBlank(), type.name());
	}

}
