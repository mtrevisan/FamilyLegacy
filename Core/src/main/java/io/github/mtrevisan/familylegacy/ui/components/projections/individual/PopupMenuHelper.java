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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.Consumer;


public class PopupMenuHelper{

	private PopupMenuHelper(){}


	/**
	 * Checks if pasting an individual from the clipboard is permitted, verifying gender compatibility
	 * if placed inside a {@code PartnersPanel}.
	 */
	public static boolean isPasteAllowed(final FLEFModel model, final IndividualPanel panel){
		final ToolContext context = new ToolContext(model, null, panel, null);
		if(context.canPaste()){
			final PartnersPanel partnersPanel = PartnersPanel.findContainingPartnersPanel(panel.getParent());
			if(partnersPanel == null)
				return true;

			final Side side = partnersPanel.getSideOf(panel);
			if(side == null)
				return true;

			final IndividualData otherData = (side == Side.LEFT
				? partnersPanel.getMotherData()
				: partnersPanel.getFatherData());
			if(otherData == null || otherData.isEmpty())
				return true;

			final FLEFRecord clipped = context.clippedRecord();
			if(clipped == null)
				return false;

			final String clippedSex = IndividualReader.extractRawSex(clipped);
			final String otherSex = otherData.getSex()
				.name()
				.toLowerCase();
			final String requiredSex = IndividualReader.getOppositeSex(otherSex);
			return requiredSex.equals(clippedSex);
		}
		return false;
	}

	/**
	 * Checks if pasting a group from the clipboard is permitted.
	 */
	public static boolean isPasteAllowed(final FLEFModel model, final GroupPanel panel){
		final ToolContext context = new ToolContext(model, null, panel, null);
		if(context.canPaste()){
			final GroupData otherData = panel.getData();
			if(otherData == null || otherData.isEmpty())
				return true;

			return (context.clippedRecord() != null);
		}
		return false;
	}


	static void addMenuItem(final JPopupMenu popup, final JMenuItem item, final IndividualPanel panel,
			final Consumer<FLEFRecord> action){
		item.addActionListener(e -> {
			final IndividualData data = panel.getData();
			action.accept(data != null? data.getIndividual(): null);
		});
		popup.add(item);
	}

	static void addMenuItem(final JPopupMenu popup, final JMenuItem item, final GroupPanel panel,
			final Consumer<FLEFRecord> action){
		item.addActionListener(e -> {
			final GroupData data = panel.getData();
			action.accept(data != null? data.getGroup(): null);
		});
		popup.add(item);
	}

}
