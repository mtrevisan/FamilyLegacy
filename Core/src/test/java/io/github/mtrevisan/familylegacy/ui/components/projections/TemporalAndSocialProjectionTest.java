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
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkType;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.components.projections.services.ModelUtils;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialNetworkPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionService;
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


/** The temporal projection and the social network are views of the switcher, and they follow the individual of the other views. */
class TemporalAndSocialProjectionTest{

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
	void theTemporalProjectionIsOneOfTheProjections() throws IOException, InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = convert();
		final String root = ModelUtils.findLowestIndividualId(model);

		final List<Object> results = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ProjectionSwitcherPanel switcher = new ProjectionSwitcherPanel(TreeType.BIOLOGICAL, model);
			switcher.loadRoot(root);

			switcher.setProjection(ProjectionType.TEMPORAL);
			results.add(switcher.getCurrentProjectionType());
			results.add(switcher.getCurrentPanel() instanceof TemporalProjectionPanel);
			results.add(switcher.getCurrentRootId());

			//the individual is not lost going through a view that has no root
			switcher.setProjection(ProjectionType.EGO_NETWORK);
			results.add(switcher.getCurrentProjectionType());
			results.add(switcher.getCurrentRootId());
		});

		Assertions.assertEquals(ProjectionType.TEMPORAL, results.get(0));
		Assertions.assertEquals(true, results.get(1), "the temporal projection is the panel shown");
		Assertions.assertEquals(root, results.get(2), "the individual of the previous view is kept");
		Assertions.assertEquals(ProjectionType.EGO_NETWORK, results.get(3));
		Assertions.assertEquals(root, results.get(4));
	}

	@Test
	void theSocialNetworkIsOneOfTheProjections() throws IOException, InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = convert();
		final String root = ModelUtils.findLowestIndividualId(model);

		final List<Object> results = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ProjectionSwitcherPanel switcher = new ProjectionSwitcherPanel(TreeType.BIOLOGICAL, model);
			switcher.loadRoot(root);

			switcher.setProjection(ProjectionType.SOCIAL_NETWORK);
			results.add(switcher.getCurrentProjectionType());
			results.add(switcher.getCurrentPanel() instanceof SocialNetworkPanel);
			results.add(switcher.getCurrentRootId());
			results.add(switcher.getSelectedEntityId());

			switcher.setProjection(ProjectionType.BIOLOGICAL_TREE);
			results.add(switcher.getCurrentRootId());

			//back to it: it is created once
			switcher.setProjection(ProjectionType.SOCIAL_NETWORK);
			results.add(switcher.getCurrentProjectionType());
		});

		Assertions.assertEquals(ProjectionType.SOCIAL_NETWORK, results.get(0));
		Assertions.assertEquals(true, results.get(1), "the social network is the panel shown");
		Assertions.assertEquals(root, results.get(2), "the network is focused on the individual of the previous view");
		Assertions.assertEquals(root, results.get(3));
		Assertions.assertEquals(root, results.get(4));
		Assertions.assertEquals(ProjectionType.SOCIAL_NETWORK, results.get(5));
	}

	@Test
	void everyDatedIndividualHasARowInTheTemporalProjection() throws IOException{
		//regression: the participations were looked up by the wrong tag, so no event reached a row and the view was always empty
		final TemporalProjectionModel projection = new TemporalProjectionService(convert()).build();

		Assertions.assertEquals(2, projection.rows().size());
		Assertions.assertTrue(projection.hasDomain());
		for(final var row : projection.rows())
			Assertions.assertTrue(row.hasEntries(), row.entity().displayLabel());
	}

	@Test
	void everyProjectionCanBeBookmarked(){
		for(final ProjectionType type : ProjectionType.values()){
			Assertions.assertFalse(type.displayName().isBlank(), type.name());
			Assertions.assertDoesNotThrow(() -> BookmarkType.valueOf(type.name()), type.name());
		}
	}

}
