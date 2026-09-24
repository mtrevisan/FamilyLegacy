package io.github.mtrevisan.familylegacy.v2.ui.components.projections.services;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.GroupDossierPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.IndividualDossierPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;

import javax.swing.JComponent;
import javax.swing.JSplitPane;


/**
 * Manages individual and group dossier panels and coordinates their display inside the split pane.
 */
public final class DossierManager{

	private final JSplitPane splitPane;
	private final int defaultDividerLocation;

	private IndividualDossierPanel individualDossier;
	private GroupDossierPanel groupDossier;
	private JComponent currentDossier;
	private boolean sidebarVisible = true;


	public DossierManager(final JSplitPane splitPane, final FLEFModel model, final int defaultDividerLocation){
		this.splitPane = splitPane;
		this.defaultDividerLocation = defaultDividerLocation;

		rebuild(model);
	}

	public void rebuild(final FLEFModel model){
		this.individualDossier = new IndividualDossierPanel(model);
		this.groupDossier = new GroupDossierPanel(model);
		this.currentDossier = individualDossier;

		if(sidebarVisible){
			splitPane.setRightComponent(currentDossier);
			splitPane.setDividerLocation(defaultDividerLocation);
		}
	}

	public void syncDossier(final String rootId, final FLEFModel model){
		if(rootId == null)
			return;

		final FLEFRecord record = model.getRecordById(rootId);
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			showGroupDossier(rootId);
		else
			showIndividualDossier(rootId);
	}

	public void showIndividualDossier(final String individualId){
		individualDossier.setIndividual(individualId);
		swapDossier(individualDossier);
	}

	public void showGroupDossier(final String groupId){
		groupDossier.setGroup(groupId);
		swapDossier(groupDossier);
	}

	private void swapDossier(final JComponent target){
		if(target == currentDossier)
			return;

		currentDossier = target;
		if(sidebarVisible){
			final int divider = splitPane.getDividerLocation();
			splitPane.setRightComponent(target);
			splitPane.setDividerLocation(divider);
		}
	}

	public boolean isSidebarVisible(){
		return sidebarVisible;
	}

	public void setSidebarVisible(final boolean visible){
		this.sidebarVisible = visible;
		if(visible){
			splitPane.setRightComponent(currentDossier);
			splitPane.setDividerLocation(defaultDividerLocation);
		}
		else
			splitPane.setRightComponent(null);
		splitPane.revalidate();
	}

}
