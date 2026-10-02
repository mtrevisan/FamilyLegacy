package io.github.mtrevisan.familylegacy.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportDialog;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportType;


public final class ResearchProgressOperation implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.reports.research.progress");
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel() && context.hasResearchData());
	}

	@Override
	public void run(final ToolContext context){
		ReportDialog.showDialog(context, ReportType.RESEARCH_PROGRESS);
	}

}
