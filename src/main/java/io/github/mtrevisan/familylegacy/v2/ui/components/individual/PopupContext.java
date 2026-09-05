package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.AncestorNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.BiologicalTreePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.Side;


/**
 * Context data for building a context menu on an IndividualPanel.
 * It holds references to the parent panel and the side (if the panel is inside a PartnersPanel),
 * plus a reference to the owning BiologicalTreePanel for executing actions.
 */
public final class PopupContext{

	private final BiologicalTreePanel treePanel;   // the owning panel
	private final boolean rootPanel;

	private final AncestorNode node;               // may be null for child panels
	private final PartnersPanel partnerPanel;
	private final Side side;


	public PopupContext(final BiologicalTreePanel treePanel){
		this(treePanel, null, null, false, null);
	}

	public PopupContext(final BiologicalTreePanel treePanel, final AncestorNode node, final PartnersPanel partnerPanel,
			final boolean rootPanel, final Side side){
		this.treePanel = treePanel;

		this.node = node;
		this.partnerPanel = partnerPanel;
		this.rootPanel = rootPanel;
		this.side = side;
	}


	public BiologicalTreePanel getTreePanel(){
		return treePanel;
	}

	public AncestorNode getNode(){
		return node;
	}

	public PartnersPanel getPartnerPanel(){
		return partnerPanel;
	}

	public boolean isRootPanel(){
		return rootPanel;
	}

	public Side getSide(){
		return side;
	}

}
