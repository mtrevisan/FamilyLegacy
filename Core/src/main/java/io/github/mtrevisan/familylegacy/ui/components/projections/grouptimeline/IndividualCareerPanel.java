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

import io.github.mtrevisan.familylegacy.groups.IndividualCareerBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Consumer;


/**
 * The group career of an individual (view C of the group views): its memberships in time, a row for each group, with a bar for each
 * period colored by role, under the row of its own life (see {@link IndividualCareerBuilder}).
 * <p>
 * It is the panel of the general temporal projection, fed with the model of one individual, as {@link GroupTimelinePanel}, with the
 * choice of the year above it and the legend of the role colors under it, that also hides the roles (see {@link GroupViewFilters}).
 */
public final class IndividualCareerPanel extends JPanel{

	private final FLEFModel model;
	private final TemporalProjectionPanel projectionPanel;
	private final GroupViewFilters filters = new GroupViewFilters();

	private String individualId;
	/** The model last shown: the whole one with the bars of the year, without those of the hidden roles. */
	private TemporalProjectionModel shown = TemporalProjectionModel.empty();


	public IndividualCareerPanel(final FLEFModel model){
		this.model = model;
		projectionPanel = new TemporalProjectionPanel(model)
			.withModelSource(() -> {
				shown = filters.apply(() -> IndividualCareerBuilder.build(this.model, individualId));
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
	public IndividualCareerPanel withSelectionCallback(final Consumer<String> callback){
		projectionPanel.withSelectionCallback(callback);

		return this;
	}

	/**
	 * Shows the career of the given individual.
	 *
	 * @param individualId	The identifier of the individual ({@code null} shows nothing).
	 */
	public void showIndividual(final String individualId){
		this.individualId = individualId;
		//another individual has other roles
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

	/** @return	The identifier of the individual shown, {@code null} if none. */
	public String getIndividualId(){
		return individualId;
	}

	/** Shows again the individual, after the data have changed. */
	public void refresh(){
		projectionPanel.refreshProjection();
	}

}
