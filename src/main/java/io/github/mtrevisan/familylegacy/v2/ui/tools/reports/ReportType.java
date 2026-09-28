package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.util.Objects;


/**
 * Enumerates all supported report types along with their human-readable title.
 */
public enum ReportType{

	ANCESTOR("Ancestor Report"),
	DESCENDANT("Descendant Report"),
	GROUP("Group Sheet"),
	INDIVIDUAL("Individual Summary"),
	RELATIONSHIP("Relationship Report"),
	BIBLIOGRAPHY("Bibliography"),
	RESEARCH_PROGRESS("Research Progress");

	private final String title;

	ReportType(final String title) {
		this.title = Objects.requireNonNull(title);
	}

	public String getTitle() {
		return title;
	}

}
