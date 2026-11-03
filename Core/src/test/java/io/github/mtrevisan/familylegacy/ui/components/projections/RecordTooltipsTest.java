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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.RecordTooltips;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


/** The audit and the evidence in the tooltips of the boxes. */
class RecordTooltipsTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
			audit {
				creation.date 2021-12-31T17:11:27Z
				update.date 2026-03-01T10:00:00Z
				update.date 2026-05-02T10:00:00Z
			}
		}
		individual I2 {
			name {
				part {
					type given
					value Bruno
				}
			}
		}
		event E1 {
			type birth
			date.value.point.value.full_date 1850-03-04
			source {
				source S1
				stance supports
				evidence {
					information_type primary
					source_type original
					evidence_type direct
				}
			}
		}
		event E2 {
			type death
			date.value.point.value.full_date 1920-05-06
		}
		""";


	private static FLEFModel model(){
		return new FLEFParser().parse(FLEF);
	}


	@Test
	void theAuditTellsTheCreationAndTheLastChange(){
		final String audit = RecordTooltips.audit(model().getRecordById("I1"));

		Assertions.assertTrue(audit.contains("2021-12-31"), audit);
		Assertions.assertTrue(audit.contains("2026-05-02"), "the last of the changes: " + audit);
		Assertions.assertFalse(audit.contains("2026-03-01"), audit);
		Assertions.assertTrue(audit.contains("(2)"), "how many changes: " + audit);
		Assertions.assertFalse(audit.contains("T17"), "the day only: " + audit);
	}

	@Test
	void aRecordWithoutAuditOrEvidenceHasNothingToSay(){
		final FLEFModel model = model();

		Assertions.assertNull(RecordTooltips.audit(model.getRecordById("I2")));
		Assertions.assertNull(RecordTooltips.audit(null));
		Assertions.assertNull(RecordTooltips.evidence(model.getRecordById("E2")));
		Assertions.assertNull(RecordTooltips.evidence(null));
	}

	@Test
	void theEvidenceTellsTheQualifiersOfTheCitations(){
		Assertions.assertEquals("primary, original, direct", RecordTooltips.evidence(model().getRecordById("E1")));
	}

	@Test
	void linesAreAddedToATooltipAndNothingIsAddedForNone(){
		Assertions.assertEquals("<html>A<br>B<br>C &amp; D</html>", RecordTooltips.append("<html>A<br>B</html>", null, "C & D", " "));
		Assertions.assertEquals("<html>x<br>y</html>", RecordTooltips.append("x", "y"));
		Assertions.assertEquals("<html>y</html>", RecordTooltips.append(null, "y"));
		Assertions.assertEquals("<html>A<br><br>B</html>", RecordTooltips.appendParagraph("<html>A</html>", null, "B"));
		Assertions.assertEquals("<html>A</html>", RecordTooltips.appendParagraph("<html>A</html>", (String)null));
		Assertions.assertEquals("<html>A</html>", RecordTooltips.append("<html>A</html>", (String)null));
		Assertions.assertNull(RecordTooltips.append(null));
	}

	@Test
	void theBoxOfAnIndividualTellsTheAuditAndTheEvidenceOfBirthAndDeath(){
		final FLEFModel model = model();
		final FLEFRecord individual = model.getRecordById("I1");
		final Map<String, List<FLEFRecord>> events = Map.of("I1", List.of(model.getRecordById("E1"), model.getRecordById("E2")));

		final IndividualData data = IndividualData.create(individual, events, model);

		Assertions.assertTrue(data.getNameTooltip().contains("2021-12-31"), data.getNameTooltip());
		Assertions.assertTrue(data.getNameTooltip().contains("Anna"), "what the tooltip told is still there: " + data.getNameTooltip());
		Assertions.assertTrue(data.getInfoTooltip().contains("primary, original, direct"), data.getInfoTooltip());
		Assertions.assertTrue(data.getInfoTooltip().contains("<br><br>Birth evidence"), "an empty line sets the evidence apart from the dates: " + data.getInfoTooltip());
		Assertions.assertEquals(1, org.apache.commons.lang3.StringUtils.countMatches(data.getInfoTooltip(), "evidence"),
			"the death has no citation: only the birth has a line of evidence");
	}

}
