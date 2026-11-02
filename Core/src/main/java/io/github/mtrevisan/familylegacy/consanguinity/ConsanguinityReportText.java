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
package io.github.mtrevisan.familylegacy.consanguinity;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;


/** The consanguinity report as plain text, to read in a dialog or to copy. */
public final class ConsanguinityReportText{

	private ConsanguinityReportText(){}


	/** @return	A coefficient as a percentage with two decimals (the locale does not change it: it is for comparing and copying). */
	public static String percent(final double coefficient){
		return String.format(Locale.ROOT, "%.2f%%", coefficient * 100.);
	}

	/** @return	A coefficient with four decimals. */
	public static String decimal(final double coefficient){
		return String.format(Locale.ROOT, "%.4f", coefficient);
	}

	/**
	 * @param report the report
	 * @param names  the name to show of an individual
	 * @return	The text of the report.
	 */
	public static String format(final ConsanguinityReport report, final Function<String, String> names){
		final StringBuilder sb = new StringBuilder();
		sb.append("Consanguinity report")
			.append(StringUtils.LF)
			.append("Computed on the recorded biological ancestry (adoptive, foster, guarded and step links are left out): an unknown ancestor is")
			.append(" taken as unrelated, so the values are lower bounds, and a wrong link changes them. A list of possibilities to look into,")
			.append(" not a conclusion.")
			.append(StringUtils.LF)
			.append(StringUtils.LF);

		sb.append("Couples whose partners are related (kinship of ").append(decimal(report.minimumKinship())).append(" or more): ")
			.append(report.unions().size())
			.append(StringUtils.LF);
		int index = 0;
		for(final ConsanguinityReport.ConsanguineousUnion union : report.unions()){
			index ++;
			sb.append(StringUtils.LF)
				.append(String.format(Locale.ROOT, "[%d] %s + %s", index, union.firstName(), union.secondName()))
				.append(StringUtils.LF);
			if(!union.term().isEmpty())
				sb.append("    Related as: ").append(union.term()).append(StringUtils.LF);
			sb.append("    Coefficient of relationship R = ").append(decimal(union.relationship()))
				.append(", a child of theirs would have F = ").append(percent(union.kinship()))
				.append(StringUtils.LF);
			if(!union.commonAncestors().isEmpty())
				sb.append("    Closest common ancestors: ").append(ancestors(union.commonAncestors(), names)).append(StringUtils.LF);
			sb.append("    Recorded as: ")
				.append(union.relationshipIds().isEmpty()? "parents of the same child, no union recorded": "union " + String.join(", ", union.relationshipIds()))
				.append(StringUtils.LF);
			sb.append("    Biological children together: ").append(union.childIds().size())
				.append(union.childIds().isEmpty()? "": " (" + String.join(", ", union.childIds()) + ")")
				.append(StringUtils.LF);
		}

		sb.append(StringUtils.LF)
			.append("Inbred individuals (coefficient of inbreeding of ").append(percent(report.minimumInbreeding())).append(" or more): ")
			.append(report.inbredIndividuals().size())
			.append(StringUtils.LF);
		index = 0;
		for(final ConsanguinityReport.InbredIndividual individual : report.inbredIndividuals()){
			index ++;
			sb.append(StringUtils.LF)
				.append(String.format(Locale.ROOT, "[%d] %s (%s): F = %s", index, individual.name(), individual.id(),
					percent(individual.inbreeding())))
				.append(StringUtils.LF);
			if(!individual.term().isEmpty())
				sb.append("    The parents are related as: ").append(individual.term()).append(StringUtils.LF);
			if(!individual.commonAncestors().isEmpty())
				sb.append("    Closest common ancestors of the parents: ").append(ancestors(individual.commonAncestors(), names))
					.append(StringUtils.LF);
		}
		return sb.toString();
	}

	/** @return	The names of the ancestors with the generations that separate them from the two individuals; at most 4. */
	public static String ancestors(final List<ConsanguinityCalculator.CommonAncestor> ancestors, final Function<String, String> names){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < Math.min(4, ancestors.size()); i ++){
			final ConsanguinityCalculator.CommonAncestor ancestor = ancestors.get(i);
			if(i > 0)
				sb.append("; ");
			sb.append(names.apply(ancestor.id()))
				.append(" (").append(ancestor.stepsFromFirst()).append(" and ").append(ancestor.stepsFromSecond())
				.append(" generations)");
		}
		if(ancestors.size() > 4)
			sb.append("; and ").append(ancestors.size() - 4).append(" more");
		return sb.toString();
	}

}
