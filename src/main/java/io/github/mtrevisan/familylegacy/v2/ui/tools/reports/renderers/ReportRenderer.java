package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportDocument;

import java.io.IOException;
import java.nio.file.Path;


public interface ReportRenderer{
	void render(ReportDocument doc, Path target) throws IOException;
}
