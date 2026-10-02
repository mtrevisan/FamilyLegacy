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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for HISTORIC EVENT records.
 * <p>
 * Structure:
 * <pre>
 * // Historic events provide contextual information. They SHOULD NOT be interpreted as evidence that a specific individual,
 * // group, or place participated in the event unless additional records establish such a connection.
 * // A significant historical event that may provide context for genealogical research. Historic events can explain migrations, mortality patterns,
 * // social changes, legal reforms, economic conditions, territorial changes, epidemics, wars, natural disasters, or scientific discoveries that
 * // affected individuals, families, groups, or places. Unlike EVENT records, a HISTORIC_EVENT is not normally tied to a specific individual or group. It
 * // exists primarily to provide historical, legal, social, environmental, or political context.
 * record HistoricEventRecord {
 *   type?: enum {
 *     war,                    // Armed conflict between states, governments, factions, or organized groups.
 *     epidemic,               // Widespread outbreak of an infectious disease affecting a population within a region.
 *     famine,                 // Severe shortage of food resulting in widespread hunger, malnutrition, or mortality.
 *     migration,              // Large-scale movement or resettlement of populations across regions or political boundaries.
 *     legal_reform,           // Introduction, modification, or abolition of laws, legal systems, or judicial frameworks.
 *     political_change,       // Significant alteration of political authority, governance, sovereignty, or regime structure.
 *     territorial_change,     // Modification of borders, jurisdictions, territorial ownership, or administrative organization.
 *     natural_disaster,       // Destructive natural event such as an earthquake, flood, volcanic eruption, wildfire, drought, or storm.
 *     economic_crisis,        // Major disruption of economic activity, including depressions, financial crashes, inflation crises, or trade collapses.
 *     scientific_discovery,   // Scientific breakthrough, technological innovation, or major advancement affecting society or historical development.
 *     religious_reform,       // Major religious, ecclesiastical, or doctrinal change affecting institutions or populations.
 *     social_movement,        // Organized social or cultural movement that influences societal norms, rights, or collective behavior.
 *     pandemic                // Epidemic affecting multiple countries or continents.
 *   } | Text
 *   title?: Text            // the title of the event
 *   date?: DateStructure    // the date on which this historic event happened
 *   place?: PlaceCitation   // the place in which this historic event happened
 *   note*: NoteStructure
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class HistoricEventReader{

	public static final String TAG_TYPE = "type";
	public static final String TAG_TITLE = "title";
	public static final String TAG_DATE = "date";
	public static final String TAG_PLACE = "place";
	public static final String TAG_NOTE = "note";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_AUDIT = "audit";

	public static final String[] TYPES = new String[]{
		"war", "epidemic", "famine", "migration", "legal_reform", "political_change", "territorial_change",
		"natural_disaster", "economic_crisis", "scientific_discovery", "religious_reform", "social_movement",
		"pandemic"
	};


	private HistoricEventReader(){}


	public static String extractTitle(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TITLE);
	}

	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

}
