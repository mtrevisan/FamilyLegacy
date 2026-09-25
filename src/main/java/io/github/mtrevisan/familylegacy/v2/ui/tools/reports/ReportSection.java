package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.nio.file.Path;
import java.util.List;


public sealed interface ReportSection{

	record Heading(int level, String text) implements ReportSection{}

	record Paragraph(String text) implements ReportSection{}

	record Spacer() implements ReportSection{}

	record BulletList(List<String> items) implements ReportSection{
		public BulletList{
			items = List.copyOf(items);
		}
	}

	record Table(List<String> headers, List<List<String>> rows) implements ReportSection{
		public Table{
			headers = List.copyOf(headers);
			rows = rows.stream().map(List::copyOf).toList();
		}
	}

	record Image(Path file, String caption) implements ReportSection{}

	record PageBreak() implements ReportSection{}

	record HorizontalRule() implements ReportSection{}

}
