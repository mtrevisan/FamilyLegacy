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

import io.github.mtrevisan.familylegacy.groups.JurisdictionTimelineBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;


/**
 * The timeline of the jurisdictions of a place: a row for each place it is related to, with a bar for each relationship
 * ({@code administrative_part_of}, {@code ecclesiastical_part_of}, ...), colored by type (see {@link JurisdictionTimelineBuilder}).
 * <p>
 * It is the panel of the general temporal projection, fed with the model of one place, with the same filters of the group views: the year
 * above it, the legend of the types of relationship under it, that also hides them (see {@link GroupViewFilters}).
 */
public final class JurisdictionTimelinePanel extends JPanel{

	private final FLEFModel model;
	private final TemporalProjectionPanel projectionPanel;
	private final GroupViewFilters filters = new GroupViewFilters();

	private String placeId;
	/** The model last shown: the whole one with the bars of the year, without those of the hidden types. */
	private TemporalProjectionModel shown = TemporalProjectionModel.empty();


	public JurisdictionTimelinePanel(final FLEFModel model){
		this.model = model;
		projectionPanel = new TemporalProjectionPanel(model)
			.withModelSource(() -> {
				shown = filters.apply(() -> JurisdictionTimelineBuilder.build(this.model, placeId));
				return shown;
			});
		filters.onChange(projectionPanel::loadProjection);

		setLayout(new BorderLayout());
		add(filters.topBar(), BorderLayout.NORTH);
		add(projectionPanel, BorderLayout.CENTER);
		add(filters.legend(), BorderLayout.SOUTH);
	}


	/**
	 * Shows the timeline of the jurisdictions of the given place.
	 *
	 * @param placeId	The identifier of the place ({@code null} shows nothing).
	 */
	public void showPlace(final String placeId){
		this.placeId = placeId;
		//another place has other jurisdictions
		filters.reset();

		projectionPanel.loadProjection();
	}

	/** @return	The legend of the types of relationship, that filters them. */
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

	/** @return	The identifier of the place shown, {@code null} if none. */
	public String getPlaceId(){
		return placeId;
	}

	/** Shows again the place, after the data have changed. */
	public void refresh(){
		projectionPanel.refreshProjection();
	}

}
