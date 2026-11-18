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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.groups.GroupTimelineBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Consumer;


/**
 * The timeline of a group (view A of the group views): a row for the group, its subgroups and its members, with a bar for each
 * membership, colored by role (see {@link GroupTimelineBuilder}).
 * <p>
 * It is the panel of the general temporal projection, fed with the model of one group: zoom, pan, tooltips and the editing of a record
 * with a click are the ones of that panel. Above it the choice of the year ({@link GroupYearControl}), under it the legend of the role colors, that also hides the roles
 * ({@link GroupRoleLegend}); see {@link GroupViewFilters}.
 */
public final class GroupTimelinePanel extends JPanel{

	private final FLEFModel model;
	private final TemporalProjectionPanel projectionPanel;
	private final GroupViewFilters filters = new GroupViewFilters();

	private String groupId;
	/** The model last shown: the whole one with the bars of the year, without those of the hidden roles. */
	private TemporalProjectionModel shown = TemporalProjectionModel.empty();


	public GroupTimelinePanel(final FLEFModel model){
		this.model = model;
		projectionPanel = new TemporalProjectionPanel(model)
			.withModelSource(() -> {
				shown = filters.apply(() -> GroupTimelineBuilder.build(this.model, groupId));
				return shown;
			});
		filters.onChange(projectionPanel::loadProjection);

		setLayout(new BorderLayout());
		add(filters.topBar(), BorderLayout.NORTH);
		add(projectionPanel, BorderLayout.CENTER);
		add(filters.legend(), BorderLayout.SOUTH);
	}


	/**
	 * Tells the panel to be told when an individual or a group is selected.
	 *
	 * @param callback	Called with the identifier of the selected record.
	 * @return	This panel.
	 */
	public GroupTimelinePanel withSelectionCallback(final Consumer<String> callback){
		projectionPanel.withSelectionCallback(callback);

		return this;
	}

	/**
	 * Shows the timeline of the given group.
	 *
	 * @param groupId	The identifier of the group ({@code null} shows nothing).
	 */
	public void showGroup(final String groupId){
		this.groupId = groupId;
		//another group has other roles
		filters.reset();

		projectionPanel.loadProjection();
	}

	/** @return	The legend of the roles, that filters them. */
	GroupRoleLegend legend(){
		return filters.legend();
	}

	/** @return	The choice of the year. */
	GroupYearControl yearControl(){
		return filters.yearControl();
	}

	/** @return	The model last shown. */
	TemporalProjectionModel shownModel(){
		return shown;
	}

	/** @return	The identifier of the group shown, {@code null} if none. */
	public String getGroupId(){
		return groupId;
	}

	/** Shows again the group, after the data have changed. */
	public void refresh(){
		projectionPanel.refreshProjection();
	}

}
