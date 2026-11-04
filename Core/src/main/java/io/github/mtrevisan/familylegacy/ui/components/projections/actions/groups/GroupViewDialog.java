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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.groups;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.projections.groupnetwork.GroupNetworkPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline.GroupTimelinePanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline.IndividualCareerPanel;
import io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline.JurisdictionTimelinePanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;


/**
 * Dialog that shows the timeline of a group, the group career of an individual or the network of the groups around a group (the views
 * A, C and B of the group views).
 * <p>
 * It only holds the panel of the view: zoom, pan and the editing of a record with a click are those of the temporal projection.
 */
public final class GroupViewDialog extends JDialog{

	private GroupViewDialog(final ToolContext context, final String title, final JComponent content){
		super(context.owner(), title, ModalityType.APPLICATION_MODAL);

		setLayout(new BorderLayout(6, 6));
		add(content, BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1100, 560));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());
	}


	/**
	 * @param context  the context of the tool
	 * @param groupId  the group to show
	 * @return	The dialog with the timeline of the group.
	 */
	public static GroupViewDialog forGroup(final ToolContext context, final String groupId){
		final GroupTimelinePanel panel = new GroupTimelinePanel(context.model());
		panel.showGroup(groupId);
		return new GroupViewDialog(context, I18N.t("dialog.group.timeline"), panel);
	}

	/**
	 * @param context  the context of the tool
	 * @param groupId  the group at the center of the network
	 * @return	The dialog with the network of the groups around the group.
	 */
	public static GroupViewDialog forNetwork(final ToolContext context, final String groupId){
		final GroupNetworkPanel panel = new GroupNetworkPanel(context.model());
		panel.showGroup(groupId);
		return new GroupViewDialog(context, I18N.t("dialog.group.network"), panel);
	}

	/**
	 * @param context  the context of the tool
	 * @param placeId  the place to show
	 * @return	The dialog with the timeline of the jurisdictions of the place.
	 */
	public static GroupViewDialog forPlace(final ToolContext context, final String placeId){
		final JurisdictionTimelinePanel panel = new JurisdictionTimelinePanel(context.model());
		panel.showPlace(placeId);
		return new GroupViewDialog(context, I18N.t("dialog.place.jurisdiction.timeline"), panel);
	}

	/**
	 * @param context       the context of the tool
	 * @param individualId  the individual to show
	 * @return	The dialog with the group career of the individual.
	 */
	public static GroupViewDialog forIndividual(final ToolContext context, final String individualId){
		final IndividualCareerPanel panel = new IndividualCareerPanel(context.model());
		panel.showIndividual(individualId);
		return new GroupViewDialog(context, I18N.t("dialog.individual.group_career"), panel);
	}


	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}

}
