package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.util.List;


public record ReportDocument(String title, String subtitle, List<ReportSection> sections){

	public ReportDocument{
		sections = List.copyOf(sections);
	}

}
