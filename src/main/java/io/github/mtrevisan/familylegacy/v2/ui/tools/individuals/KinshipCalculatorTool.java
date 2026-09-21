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
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship.KinshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/**
 * Opens the kinship calculator with the currently selected individual
 * pre-filled as the first side of the comparison.
 * <p>
 * The kinship dialog lives in the {@code individualtree} package and
 * needs a {@code TreeService}. The service is created on demand, with
 * the same relationship filter the tree uses, so the calculator sees the
 * same graph the tree does.
 */
public final class KinshipCalculatorTool implements ToolOperation{

	@Override
	public String getName(){
		return "Kinship Calculator…";
	}

	@Override
	public void run(final ToolContext context){
		final String selected = context.selectedIndividualId();
		final TreeService treeService = new TreeService(
			type -> "biological_child".equalsIgnoreCase(type),
			context.model());
		final io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord initialA =
			(selected != null? context.model().getRecordById(selected): null);
		final KinshipDialog dialog = new KinshipDialog(context.owner(),
			context.model(), treeService, initialA, null);
		dialog.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAtLeastIndividuals(2));
	}

}
