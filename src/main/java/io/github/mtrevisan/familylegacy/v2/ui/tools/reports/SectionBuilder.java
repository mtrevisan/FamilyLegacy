package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.util.List;


/**
 * A self-contained builder of one or more {@link ReportSection}s.
 *
 * <p>Implementations receive the shared {@link ReportContext} at construction
 * time and return the sections they contribute. Every builder is responsible
 * for honouring the relevant {@link ReportConfig} flags; the orchestrator
 * simply concatenates the results.</p>
 */
interface SectionBuilder{

	List<ReportSection> build();

}
