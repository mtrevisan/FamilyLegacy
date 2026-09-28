package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18n;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportType;


public final class BibliographyOperation implements ToolOperation{

	@Override
	public String getName(){
		return I18n.t("menu.reports.bibliography");
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel() && context.hasSourcesOrDocuments());
	}

	@Override
	public void run(final ToolContext context){
		ReportDialog.showDialog(context, ReportType.BIBLIOGRAPHY);
	}

}
