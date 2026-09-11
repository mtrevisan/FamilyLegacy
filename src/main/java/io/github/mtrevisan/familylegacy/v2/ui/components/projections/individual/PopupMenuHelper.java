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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.function.Consumer;


public class PopupMenuHelper{

	private static final String TAG_SEX = "sex";

	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";


	private PopupMenuHelper(){}


	/**
	 * Checks if pasting an individual from the clipboard is permitted, verifying gender compatibility
	 * if placed inside a {@code PartnersPanel}.
	 */
	static boolean isPasteAllowed(final IndividualPanel panel){
		final boolean hasClippedRecord = RelationClipboard.getInstance().hasRecord();
		if(hasClippedRecord){
			final PartnersPanel partnersPanel = PartnersPanel.findContainingPartnersPanel(panel.getParent());
			if(partnersPanel == null)
				return true;

			final Side side = partnersPanel.getSideOf(panel);
			if(side == null)
				return true;

			final IndividualPanel otherPanel = (side == Side.LEFT
				? partnersPanel.getMotherPanel()
				: partnersPanel.getFatherPanel());
			final IndividualData otherData = otherPanel.getData();
			if(otherData == null || otherData.isEmpty())
				return true;

			final FLEFRecord clipped = RelationClipboard.getInstance().getRecord();
			if(clipped == null)
				return false;

			final String clippedSex = FLEFRecordHelper.getChildValue(clipped, TAG_SEX);
			final String otherSex = otherData.getSex().name().toLowerCase();
			final String requiredSex = (otherSex.equals(ENUM_SEX_MALE)? ENUM_SEX_FEMALE: ENUM_SEX_MALE);
			return requiredSex.equals(clippedSex);
		}
		return false;
	}

	/**
	 * Checks if pasting a group from the clipboard is permitted.
	 */
	static boolean isPasteAllowed(final GroupPanel panel){
		final boolean hasClippedRecord = RelationClipboard.getInstance().hasRecord();
		if(hasClippedRecord){
			final GroupData otherData = panel.getData();
			if(otherData == null || otherData.isEmpty())
				return true;

			final FLEFRecord clipped = RelationClipboard.getInstance().getRecord();
			return (clipped != null);
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
