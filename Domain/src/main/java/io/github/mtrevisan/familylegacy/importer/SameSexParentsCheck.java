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
package io.github.mtrevisan.familylegacy.importer;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.individual.IndividualLabel;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.CoParentSex;



/**
 * Tells in the {@link ImportReport} the children whose biological parents are of the same sex: the file is kept as it is, but it is
 * most likely wrong (or the sex of a parent, or the kind of the link).
 */
public final class SameSexParentsCheck{

	private SameSexParentsCheck(){}


	/** Adds to the report an issue for each pair of same-sex parents of a child (only the certain ones, or also the possible ones). */
	public static void report(final FLEFModel model, final ImportReport report){
		if(model == null || report == null)
			return;

		for(final CoParentSex.Clash clash : CoParentSex.findAll(model))
			report.discard((clash.certain()? ImportReasons.SAME_SEX_PARENTS: ImportReasons.SAME_SEX_POSSIBLE_PARENTS),
				IndividualLabel.of(model, clash.individualId()) + " + "
					+ IndividualLabel.of(model, clash.coParentId()),
				I18N.tf("import.same_sex_parents.where", IndividualLabel.of(model, clash.childId())));
	}

}
