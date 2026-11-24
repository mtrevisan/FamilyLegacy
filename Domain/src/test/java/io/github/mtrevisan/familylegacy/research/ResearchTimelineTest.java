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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;


/** The timeline of the research: what is read from the audit of the records, and how it is filtered. */
class ResearchTimelineTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		repository R1 {
			name.value Archive of Treviso
			audit.creation.date 2026-01-01T00:00:00Z
		}
		source S1 {
			title.value Parish register
			repository {
				repository R1
			}
			audit.creation.date 2026-01-10T09:00:00Z
		}
		extract X1 {
			source S1
			text Mario born in 1850
			type verbatim
			audit.creation.date 2026-01-11T09:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Mario
				}
			}
			audit {
				creation.date 2026-01-01T00:00:00Z
				update {
					date 2026-01-12T10:00:00Z
					comment + source: S1
				}
			}
		}
		individual I2 {
			name {
				part {
					type given
					value Luigi
				}
			}
			audit.creation.date 2026-01-01T00:00:00Z
		}
		research_question Q1 {
			title Parents of Mario
			question Who?
			status open
			target.individual I1
			audit.creation.date 2026-01-02T00:00:00Z
		}
		research_question Q2 {
			title Parents of Luigi
			question Who?
			status open
			target.individual I2
			audit.creation.date 2026-01-02T00:00:00Z
		}
		research_task T1 {
			description Ask the priest
			question Q1
			status completed
			audit {
				creation.date 2026-01-05T00:00:00Z
				update {
					date 2026-02-01T08:00:00Z
					comment ~ status: open → completed
				}
			}
		}
		research_task T2 {
			description Write to the diocese
			question Q2
			status open
			audit.creation.date 2026-01-06T00:00:00Z
		}
		research_activity A1 {
			question Q1
			activity_type search
			status completed
			action Looked in the register
			result positive
			source.source S1
			audit.creation.date 2026-01-10T10:00:00Z
		}
		research_activity A2 {
			question Q2
			activity_type analysis
			status planned
			action Compare the dates
			audit.creation.date 2026-01-20T10:00:00Z
		}
		conclusion CC1 {
			issue parentage
			proof_status supported
			resolves.individual I1
			resolves.individual I2
			preferred.individual I1
			audit {
				creation.date 2026-03-01T00:00:00Z
				update {
					date 2026-03-05T00:00:00Z
					comment ~ preferred.individual: I2 → I1
				}
			}
		}
		""";


	private static List<ResearchTimeline.Entry> build(){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		return ResearchTimeline.build(model);
	}

	private static ResearchTimeline.Entry entry(final List<ResearchTimeline.Entry> entries, final ResearchTimeline.Kind kind,
			final String recordId){
		return entries.stream()
			.filter(entry -> entry.kind() == kind && entry.recordId().equals(recordId))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no " + kind + " of " + recordId + " in " + entries));
	}

	private static List<String> ids(final List<ResearchTimeline.Entry> entries, final ResearchTimeline.Kind kind){
		return entries.stream()
			.filter(entry -> entry.kind() == kind)
			.map(ResearchTimeline.Entry::recordId)
			.toList();
	}


	@Test
	void theWorkIsReadFromTheAuditOfTheRecords(){
		final List<ResearchTimeline.Entry> entries = build();

		Assertions.assertEquals(List.of("T1", "T2"), ids(entries, ResearchTimeline.Kind.TASK_CREATED));
		Assertions.assertEquals(List.of("T1"), ids(entries, ResearchTimeline.Kind.TASK_COMPLETED), "only the task that is completed");
		Assertions.assertEquals("2026-02-01T08:00:00Z", entry(entries, ResearchTimeline.Kind.TASK_COMPLETED, "T1").when(),
			"the day of the update that completed it, not the one of the creation");
		Assertions.assertEquals(List.of("S1"), ids(entries, ResearchTimeline.Kind.SOURCE_ADDED));
		Assertions.assertEquals(List.of("X1"), ids(entries, ResearchTimeline.Kind.EXTRACT_CREATED));
		Assertions.assertEquals(List.of("CC1"), ids(entries, ResearchTimeline.Kind.CONCLUSION_CREATED));
		Assertions.assertEquals(List.of("CC1"), ids(entries, ResearchTimeline.Kind.CONCLUSION_UPDATED));
	}

	@Test
	void aSearchInAnArchiveIsAVisitToTheRepository(){
		final List<ResearchTimeline.Entry> entries = build();

		final ResearchTimeline.Entry visit = entry(entries, ResearchTimeline.Kind.REPOSITORY_VISITED, "A1");
		Assertions.assertTrue(visit.repositoryIds().contains("R1"), "the repository of the source it cites");
		Assertions.assertTrue(visit.detail().contains("Archive of Treviso"), visit.detail());
		Assertions.assertEquals(List.of("A2"), ids(entries, ResearchTimeline.Kind.ACTIVITY), "an analysis visits nobody");
	}

	@Test
	void aCitationIsDatedByTheUpdateOfTheRecordThatReceivedIt(){
		final List<ResearchTimeline.Entry> entries = build();

		final ResearchTimeline.Entry citation = entry(entries, ResearchTimeline.Kind.CITATION_ADDED, "I1");
		Assertions.assertEquals("2026-01-12T10:00:00Z", citation.when());
		Assertions.assertTrue(citation.sourceIds().contains("S1"));
		Assertions.assertEquals("Parish register", citation.title());
	}

	@Test
	void aConflictIsResolvedWhenTheConclusionTakesAPosition(){
		final List<ResearchTimeline.Entry> entries = build();

		final ResearchTimeline.Entry resolved = entry(entries, ResearchTimeline.Kind.CONFLICT_RESOLVED, "CC1");
		Assertions.assertEquals("2026-03-05T00:00:00Z", resolved.when(), "the update that touched the preferred assertion");
		Assertions.assertTrue(resolved.involved().containsAll(List.of("I1", "I2")));
	}

	@Test
	void theEntriesAreInChronologicalOrder(){
		final List<ResearchTimeline.Entry> entries = build();

		for(int i = 1; i < entries.size(); i ++)
			Assertions.assertTrue(ResearchTimeline.ORDER.compare(entries.get(i - 1), entries.get(i)) <= 0);
		Assertions.assertTrue(entries.getFirst().when().compareTo(entries.getLast().when()) <= 0);
	}

	@Test
	void theTimelineIsFilteredByPerson(){
		final List<ResearchTimeline.Entry> entries = ResearchTimeline.filter(build(),
			new ResearchTimeline.Filter("I1", null, null, null, null, null));

		Assertions.assertEquals(List.of("T1"), ids(entries, ResearchTimeline.Kind.TASK_CREATED), "the task about the other person is out");
		Assertions.assertEquals(List.of("A1"), ids(entries, ResearchTimeline.Kind.REPOSITORY_VISITED));
		Assertions.assertTrue(ids(entries, ResearchTimeline.Kind.ACTIVITY).isEmpty());
	}

	@Test
	void theTimelineIsFilteredBySourceAndByRepository(){
		final List<ResearchTimeline.Entry> all = build();

		final List<ResearchTimeline.Entry> bySource = ResearchTimeline.filter(all,
			new ResearchTimeline.Filter(null, null, "S1", null, null, null));
		Assertions.assertTrue(bySource.stream().allMatch(entry -> entry.sourceIds().contains("S1")));
		Assertions.assertEquals(List.of("S1", "A1", "X1", "I1"), bySource.stream()
			.map(ResearchTimeline.Entry::recordId)
			.toList());

		final List<ResearchTimeline.Entry> byRepository = ResearchTimeline.filter(all,
			new ResearchTimeline.Filter(null, null, null, "R1", null, null));
		Assertions.assertEquals(bySource.size(), byRepository.size(), "everything that is about S1 is held by R1");
	}

	@Test
	void theTimelineIsFilteredByDateRange(){
		final List<ResearchTimeline.Entry> entries = ResearchTimeline.filter(build(), new ResearchTimeline.Filter(null, null, null, null,
			LocalDate.of(2026, 1, 11), LocalDate.of(2026, 2, 1)));

		Assertions.assertEquals(List.of(ResearchTimeline.Kind.EXTRACT_CREATED, ResearchTimeline.Kind.CITATION_ADDED,
				ResearchTimeline.Kind.ACTIVITY, ResearchTimeline.Kind.TASK_COMPLETED),
			entries.stream()
				.map(ResearchTimeline.Entry::kind)
				.toList(), "both ends of the range are included");
	}

}
