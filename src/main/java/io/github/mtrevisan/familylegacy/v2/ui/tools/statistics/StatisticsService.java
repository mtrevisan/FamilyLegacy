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
package io.github.mtrevisan.familylegacy.v2.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Computes descriptive statistics about a {@link FLEFModel}.
 * <p>
 * The service counts records by type, individuals by sex, and reports
 * the ratio of individuals that carry a name. The list of record types
 * is the one declared by the FLEF protocol; unknown types are still
 * counted under their own tag so nothing is silently dropped.
 */
public final class StatisticsService{

	private static final String TAG_SEX = "sex";
	private static final String VALUE_MALE = "male";
	private static final String VALUE_FEMALE = "female";

	/**
	 * The record types declared by the FLEF protocol, in a stable
	 * order. Used to build the "records by type" table.
	 */
	private static final String[] KNOWN_TYPES = {
		"individual", "group", "event", "individual_attribute",
		"event_participation", "group_attribute", "relationship",
		"place", "place_relationship", "repository", "cultural_norm",
		"source", "document", "historic_event", "context_impact",
		"identity_hypothesis", "research_question", "research_activity",
		"research_task", "conclusion"
	};


	/** Result of the statistics computation. */
	public record Report(
		Map<String, Integer> countByType,
		int maleCount, int femaleCount, int unknownSexCount,
		int withNameCount, int withoutNameCount){

		public int totalRecords(){
			int total = 0;
			for(final int c : countByType.values())
				total += c;
			return total;
		}

		public String toHtml(){
			final StringBuilder body = new StringBuilder();
			body.append("<h1>Statistics</h1>");
			body.append("<p>Total records: <b>").append(totalRecords()).append("</b></p>");

			body.append("<h2>Records by type</h2>");
			body.append("<table>");
			body.append("<tr><th>Type</th><th>Count</th></tr>");
			for(final Map.Entry<String, Integer> e : countByType.entrySet()){
				if(e.getValue() == 0)
					continue;
				body.append("<tr><td>").append(ReportDialog.escape(e.getKey()))
					.append("</td><td>").append(e.getValue()).append("</td></tr>");
			}
			body.append("</table>");

			body.append("<h2>Individuals by sex</h2>");
			body.append("<table>");
			body.append("<tr><th>Sex</th><th>Count</th></tr>");
			body.append("<tr><td>Male</td><td>").append(maleCount).append("</td></tr>");
			body.append("<tr><td>Female</td><td>").append(femaleCount).append("</td></tr>");
			body.append("<tr><td>Unknown</td><td>").append(unknownSexCount).append("</td></tr>");
			body.append("</table>");

			body.append("<h2>Names</h2>");
			body.append("<table>");
			body.append("<tr><td>Individuals with a name</td><td>").append(withNameCount).append("</td></tr>");
			body.append("<tr><td>Individuals without a name</td><td>").append(withoutNameCount).append("</td></tr>");
			body.append("</table>");

			return ReportDialog.document(body.toString());
		}
	}


	private StatisticsService(){
	}


	public static Report compute(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final String type : KNOWN_TYPES)
			counts.put(type, 0);

		int male = 0, female = 0, unknownSex = 0;
		int withName = 0, withoutName = 0;

		// Count every known type through its dedicated getter, so that a
		// type declared in the protocol but not present in the file still
		// appears in the table with a count of zero.
		for(final String type : KNOWN_TYPES)
			counts.put(type, model.getRecordsByType(type).size());

		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);
		for(final FLEFRecord individual : individuals){
			final String sex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);
			if(VALUE_MALE.equalsIgnoreCase(sex))
				male ++;
			else if(VALUE_FEMALE.equalsIgnoreCase(sex))
				female ++;
			else
				unknownSex ++;

			final boolean hasName = individual.getChildren()
				.stream()
				.anyMatch(c -> "name".equalsIgnoreCase(c.getTag()));
			if(hasName)
				withName++;
			else
				withoutName++;
		}

		return new Report(new TreeMap<>(counts), male, female, unknownSex,
			withName, withoutName);
	}

}
