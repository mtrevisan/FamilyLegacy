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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The table of Manage Media tells how many people are tagged in each media, and the dialog can open the viewer. */
class MediaManagementTaggedTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";
	private static final String FACE = "\t\tcrop {\n\t\t\tx 20\n\t\t\ty 10\n\t\t\twidth 40\n\t\t\theight 30\n\t\t}\n";


	private static <T extends Component> List<T> find(final Container container, final Class<T> type){
		final List<T> found = new ArrayList<>();
		for(final Component component : container.getComponents()){
			if(type.isInstance(component))
				found.add(type.cast(component));
			if(component instanceof Container child)
				found.addAll(find(child, type));
		}
		return found;
	}


	@Test
	void theTableTellsHowManyPeopleAreTaggedInEachMedia(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = new FLEFParser().parse("media M1 {\n\turi photo.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi house.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tname.part.type given\n\tname.part.value Anna\n\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n"
			+ "\tmedia {\n\t\tmedia M2\n\t\tcaption the house\n\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tname.part.type given\n\tname.part.value Bruno\n\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n" + AUDIT + "}\n");

		final MediaManagementDialog dialog = new MediaManagementDialog(new ToolContext(model, null, new JPanel(), null));
		try{
			final JTable table = find(dialog.getContentPane(), JTable.class).getFirst();
			Assertions.assertEquals(5, table.getColumnCount());
			Assertions.assertEquals("People tagged", table.getColumnName(4));
			Assertions.assertEquals(Integer.class, table.getColumnClass(4));

			final int[] tagged = new int[2];
			for(int row = 0; row < table.getRowCount(); row ++){
				final String uri = String.valueOf(table.getValueAt(row, 0));
				tagged[uri.contains("photo")? 0: 1] = (Integer)table.getValueAt(row, 4);
			}
			Assertions.assertEquals(2, tagged[0], "Anna and Bruno are tagged in the photograph");
			Assertions.assertEquals(0, tagged[1], "the house is cited with no crop");

			Assertions.assertTrue(find(dialog.getContentPane(), JButton.class).stream().anyMatch(button -> "View…".equals(button.getText())),
				"the viewer can be opened from the dialog");
		}
		finally{
			dialog.dispose();
		}
	}

	@Test
	void theTableCanShowOnlyTheMediaOfAnIndividualOrOfAGroup(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = new FLEFParser().parse("media M1 {\n\turi photo.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi house.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M3 {\n\turi reunion.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tname.part.type given\n\tname.part.value Anna\n\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n"
			+ "\tpreferred_image {\n\t\tmedia M2\n\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tname.part.type given\n\tname.part.value Bruno\n\tmedia {\n\t\tmedia M1\n" + FACE + "\t}\n" + AUDIT + "}\n"
			+ "individual I3 {\n\tname.part.type given\n\tname.part.value Carla\n" + AUDIT + "}\n"
			+ "group G1 {\n\tname.value Rossi\n\tmedia {\n\t\tmedia M3\n\t}\n" + AUDIT + "}\n");

		final MediaManagementDialog dialog = new MediaManagementDialog(new ToolContext(model, null, new JPanel(), null));
		try{
			final JTable table = find(dialog.getContentPane(), JTable.class).getFirst();
			Assertions.assertEquals(3, table.getRowCount());

			//(what the individual cites, and its preferred image)
			dialog.showMediaOf("I1");
			Assertions.assertEquals(List.of("house.jpg", "photo.jpg"), uris(table));
			dialog.showMediaOf("I2");
			Assertions.assertEquals(List.of("photo.jpg"), uris(table));
			//(a group has media too)
			dialog.showMediaOf("G1");
			Assertions.assertEquals(List.of("reunion.jpg"), uris(table));
			//(somebody with no media: none; nobody, or a record that is not there: all)
			dialog.showMediaOf("I3");
			Assertions.assertEquals(0, table.getRowCount());
			dialog.showMediaOf("I9");
			Assertions.assertEquals(3, table.getRowCount());
			dialog.showMediaOf("I2");
			dialog.showMediaOf(null);
			Assertions.assertEquals(3, table.getRowCount());
		}
		finally{
			dialog.dispose();
		}
	}

	private static List<String> uris(final JTable table){
		final List<String> uris = new ArrayList<>();
		for(int row = 0; row < table.getRowCount(); row ++)
			uris.add(String.valueOf(table.getValueAt(row, 0)));
		uris.sort(null);
		return uris;
	}
}
