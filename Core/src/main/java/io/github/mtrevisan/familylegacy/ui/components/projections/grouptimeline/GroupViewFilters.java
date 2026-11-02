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

import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionModel;

import java.util.function.Supplier;


/**
 * The filters of a group view (the timeline of a group or the career of an individual), the same for both: the year, then the roles of
 * the legend.
 * <p>
 * The model of the view is built from the whole file, and building it again at every click of a filter would be a waste: when only a filter
 * has changed the model built last is used again (see {@link #apply(Supplier)}); when the data may have changed, it is built again.
 */
final class GroupViewFilters{

	private final GroupYearControl yearControl = new GroupYearControl();
	private final GroupRoleLegend legend = new GroupRoleLegend();

	/** The whole model built last, before the filters. */
	private TemporalProjectionModel whole;
	/** Whether the next {@link #apply(Supplier)} can use the model built last: only a filter has changed. */
	private boolean reuse;


	GroupYearControl yearControl(){
		return yearControl;
	}

	GroupRoleLegend legend(){
		return legend;
	}


	/**
	 * Tells how to show the view again when a filter changes.
	 *
	 * @param reload	Shows the view again (it calls {@link #apply(Supplier)}).
	 */
	void onChange(final Runnable reload){
		final Runnable onlyAFilterChanged = () -> {
			reuse = true;
			reload.run();
		};
		yearControl.withListener(onlyAFilterChanged);
		legend.withListener(onlyAFilterChanged);
	}

	/**
	 * @param builder	Builds the whole model of the view, without filters.
	 * @return	The model to show: the whole one with the bars of the year, without the bars of the roles that are hidden. The legend lists
	 * 	the roles of the bars of the year.
	 */
	TemporalProjectionModel apply(final Supplier<TemporalProjectionModel> builder){
		final TemporalProjectionModel built = (reuse && whole != null? whole: builder.get());
		reuse = false;
		whole = built;

		final TemporalProjectionModel inYear = GroupYearFilter.filter(built, yearControl.getYear());
		legend.update(inYear);
		return legend.filter(inYear);
	}

	/** Forgets the roles that were hidden: a new view has other roles. The year stays, it is a choice about the time. */
	void reset(){
		legend.reset();
	}

}
