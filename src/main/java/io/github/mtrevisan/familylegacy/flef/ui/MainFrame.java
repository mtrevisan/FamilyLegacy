/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.SelectionListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyPanel;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


public final class MainFrame extends JFrame implements FamilyListenerInterface, IndividualListenerInterface, SelectionListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);


//	private Flef store;
//
//	private TreePanel treePanel;
//	private LinkFamilyDialog linkFamilyDialog;
//	private LinkIndividualDialog linkIndividualDialog;


	public MainFrame(){
//		try{
//			final Store storeGedcom = new Gedcom();
//			final Store load = storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");
//			store = (Flef)load.transform();
//			final GedcomNode family = store.getFamilies().get(0);
////			final GedcomNode family = store.getFamilies().get(4);
////			final GedcomNode family = store.getFamilies().get(9);
////			final GedcomNode family = store.getFamilies().get(64);
////			final GedcomNode family = store.getFamily("F797");
////			final GedcomNode family = store.createEmptyNode();
//
//			//FIXME
//			linkFamilyDialog = new LinkFamilyDialog(store, this);
//			linkFamilyDialog.setSize(945, 500);
//			linkFamilyDialog.setLocationRelativeTo(null);
//
//			//FIXME
//			linkIndividualDialog = new LinkIndividualDialog(store, this);
//			linkIndividualDialog.setSize(850, 500);
//			linkIndividualDialog.setLocationRelativeTo(null);
//
//			getContentPane().setLayout(new BorderLayout());
//			treePanel = new TreePanel(family, 4, store);
//			getContentPane().add(treePanel, BorderLayout.NORTH);
//			pack();
//
//			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//			addWindowListener(new MyWindowAdapter());
//			setSize(1150, 472);
//			setLocationRelativeTo(null);
//			setVisible(true);
//		}
//		catch(final GedcomParseException | GedcomGrammarParseException e){
//			e.printStackTrace();
//		}
	}

	public void setFamilyListener(final FamilyListenerInterface familyListener){
//		treePanel.setFamilyListener(familyListener);
	}

	public void setIndividualListener(final IndividualListenerInterface individualListener){
//		treePanel.setIndividualListener(individualListener);
	}

	public void setSelectionListener(final SelectionListenerInterface selectionListener){
//		linkFamilyDialog.setSelectionListener(selectionListener);
//		linkIndividualDialog.setSelectionListener(selectionListener);
	}

	@EventHandler
	public void error(final BusExceptionEvent exceptionEvent){
//		final Throwable cause = exceptionEvent.getCause();
//		JOptionPane.showMessageDialog(this, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	public void refresh(final EditEvent editCommand){
//		switch(editCommand.getType()){
//			case GROUP -> {
//				final GroupDialog dialog = new GroupDialog(store, this);
//				final GedcomNode container = editCommand.getContainer();
//				dialog.setTitle(container.getID() != null? "Group for " + container.getID(): "Group");
//				if(!dialog.loadData(container, editCommand.getOnCloseGracefully()))
//					dialog.showNewRecord();
//
//				dialog.setSize(905, 396);
//				dialog.setLocationRelativeTo(this);
//				dialog.setVisible(true);
//			}
//			case NOTE -> {
//				final NoteDialog dialog = NoteDialog.createNote(store, this);
//				final GedcomNode note = editCommand.getContainer();
//				dialog.setTitle("Note for " + note.getID());
//				if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
//					dialog.showNewRecord();
//
//				dialog.setSize(500, 330);
//				dialog.setLocationRelativeTo(this);
//				dialog.setVisible(true);
//			}
//			case SOURCE -> {
//				final SourceDialog dialog = new SourceDialog(store, this);
//				final GedcomNode source = editCommand.getContainer();
//				dialog.setTitle(source.getID() != null
//					? "Source " + source.getID()
//					: "New source for " + source.getID());
//				if(!dialog.loadData(source, editCommand.getOnCloseGracefully()))
//					dialog.showNewRecord();
//
//				dialog.setSize(946, 396);
//				dialog.setLocationRelativeTo(this);
//				dialog.setVisible(true);
//			}
//			case EVENT -> {
//				//TODO
////				final EventDialog dialog = new EventDialog(store, this);
////				dialog.loadData(editCommand.getContainer());
////
////				dialog.setSize(450, 500);
////				dialog.setLocationRelativeTo(this);
////				dialog.setVisible(true);
//			}
//			case EVENT_CITATION -> {
//				//TODO
////				final EventCitationDialog dialog = new EventCitationDialog(store, this);
////				if(!dialog.loadData(editCommand.getContainer()))
////					dialog.addAction();
////
////				dialog.setSize(450, 500);
////				dialog.setLocationRelativeTo(this);
////				dialog.setVisible(true);
//			}
//		}
	}

	@Override
	public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
//		LOGGER.debug("onEditFamily " + family.getID());
//
//		//TODO
//		final FamilyRecordDialog familyDialog = new FamilyRecordDialog(family, store, this);
//		familyDialog.setSize(340, 300);
//		familyDialog.setLocationRelativeTo(this);
//		familyDialog.setVisible(true);
	}

	@Override
	public void onFamilyLink(final FamilyPanel boxPanel){
//		LOGGER.debug("onLinkFamily");
//
//		linkFamilyDialog.setPanelReference(boxPanel);
//		linkFamilyDialog.setVisible(true);
	}

	@Override
	public void onFamilyUnlink(final FamilyPanel boxPanel, final GedcomNode family){
//		LOGGER.debug("onUnlinkFamily " + family.getID());
//
//		//TODO
	}

	@Override
	public void onFamilyRemove(final FamilyPanel boxPanel, final GedcomNode family){
//		LOGGER.debug("onRemoveFamily " + family.getID());
//
//		//TODO
	}

	@Override
	public void onFamilyPreviousPartner(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
			final GedcomNode currentFamily){
//		LOGGER.debug("onPrevparentFamily this: {}, other: {}, family: {}", thisParent.getID(), otherCurrentParent.getID(),
//			currentFamily.getID());
//
//		GedcomNode nextFamily = null;
//		final String currentFamilyID = currentFamily.getID();
//		final List<GedcomNode> familyXRefs = store.traverseAsList(thisParent, "FAMILY_PARTNER[]");
//		for(int familyIndex = 1; familyIndex < familyXRefs.size(); familyIndex ++)
//			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
//				nextFamily = store.getFamily(familyXRefs.get(familyIndex - 1).getXRef());
//				break;
//			}
//
//		//update primary family
//		treePanel.loadData(store.createEmptyNode(), store.createEmptyNode(), nextFamily);
	}

	@Override
	public void onFamilyNextPartner(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
			final GedcomNode currentFamily){
//		LOGGER.debug("onNextParentFamily this: {}, other: {}, family: {}", thisParent.getID(), otherCurrentParent.getID(),
//			currentFamily.getID());
//
//		GedcomNode nextFamily = null;
//		final String currentFamilyID = currentFamily.getID();
//		final List<GedcomNode> familyXRefs = store.traverseAsList(thisParent, "FAMILY_PARTNER[]");
//		for(int familyIndex = 0; familyIndex < familyXRefs.size() - 1; familyIndex ++){
//			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
//				nextFamily = store.getFamily(familyXRefs.get(familyIndex + 1).getXRef());
//				break;
//			}
//		}
//
//		//update primary family
//		treePanel.loadData(store.createEmptyNode(), store.createEmptyNode(), nextFamily);
	}


	@Override
	public void onIndividualFocus(final IndividualPanel boxPanel, final SelectedNodeType type, final GedcomNode individual){
//		LOGGER.debug("onFocusIndividual {}, type is {}", individual.getID(), type);
//
//		//prefer left position if male or unknown, right if female
//		final GedcomNode partner1 = (type == SelectedNodeType.PARTNER1? individual: store.createEmptyNode());
//		final GedcomNode partner2 = (type == SelectedNodeType.PARTNER2? individual: store.createEmptyNode());
//		//FIXME choose the current shown family, if any
//		final GedcomNode family = treePanel.getPreferredFamily(individual);
//
//		//update primary family
//		treePanel.loadData(partner1, partner2, family);
	}

	@Override
	public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
//		//TODO
//		LOGGER.debug("onEditIndividual " + individual.getID());
	}

	@Override
	public void onIndividualLink(final IndividualPanel boxPanel, final SelectedNodeType type){
//		LOGGER.debug("onLinkIndividual");
//
//		linkIndividualDialog.setPanelReference(boxPanel);
//		linkIndividualDialog.setSelectionType(type);
//		linkIndividualDialog.setVisible(true);
	}

	@Override
	public void onIndividualUnlink(final IndividualPanel boxPanel, final GedcomNode individual){
//		//TODO
//		LOGGER.debug("onUnlinkIndividual " + individual.getID());
	}

	@Override
	public void onIndividualAdd(final IndividualPanel boxPanel){
//		//TODO
//		LOGGER.debug("onAddIndividual");
	}

	@Override
	public void onIndividualRemove(final IndividualPanel boxPanel, final GedcomNode individual){
//		//TODO
//		LOGGER.debug("onRemoveIndividual " + individual.getID());
	}

	@Override
	public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
//		//TODO
//		LOGGER.debug("onAddPreferredImage " + individual.getID());
	}


	@Override
	public void onItemSelected(final GedcomNode node, final SelectedNodeType type, final JPanel panelReference){
//		if(type == SelectedNodeType.FAMILY){
//			final FamilyPanel familyPanel = (FamilyPanel)panelReference;
//			final GedcomNode child = familyPanel.getChildReference();
//			linkFamilyToChild(child, node);
//
//			familyPanel.loadData(node);
//
//			EventBusService.publish(Flef.ACTION_COMMAND_FAMILY_COUNT);
//		}
//		else{
//			final IndividualPanel individualPanel = (IndividualPanel)panelReference;
//			final GedcomNode child = individualPanel.getChildReference();
//			if(type == SelectedNodeType.PARTNER1)
//				linkChildToFamily(child, node, store.getPartner1s(child), "PARTNER1");
//			else if(type == SelectedNodeType.PARTNER2)
//				linkChildToFamily(child, node, store.getPartner2s(child), "PARTNER2");
//
//			individualPanel.loadData(node);
//
//			EventBusService.publish(Flef.ACTION_COMMAND_INDIVIDUAL_COUNT);
//		}
	}

	private void linkFamilyToChild(final GedcomNode child, final GedcomNode family){
//		LOGGER.debug("Add family {} to child {}", family.getID(), child.getID());
//
//		store.linkFamilyToChild(child, family);
	}

	private void linkChildToFamily(final GedcomNode child, final GedcomNode partner, final List<GedcomNode> partners,
			final String partnerTag){
//		if(partners.isEmpty())
//			linkIndividualToNewFamily(child, partner, partnerTag);
//		else if(partners.size() == 1)
//			linkIndividualToExistingFamily(partners.get(0), partner, partnerTag);
//		else
//			LOGGER.warn("Individual {} belongs to more than one family (this cannot be)", child.getID());
	}

	private void linkIndividualToNewFamily(final GedcomNode child, final GedcomNode partner, final String partnerTag){
//		//create new family and add parent
//		final GedcomNode family = store.create("FAMILY")
//			.addChildReference(partnerTag, partner.getID());
//		store.addFamily(family);
//		store.linkFamilyToChild(child, family);
	}

	private void linkIndividualToExistingFamily(final GedcomNode family, final GedcomNode partner, final String partnerTag){
//		//link to existing family as parent
//		family.addChild(store.create(partnerTag)
//			.withXRef(partner.getID()));
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		//create and display the form
		EventQueue.invokeLater(() -> {
			final MainFrame frame = new MainFrame();
			frame.setFamilyListener(frame);
			frame.setIndividualListener(frame);
			frame.setSelectionListener(frame);
			frame.setVisible(true);
			EventBusService.subscribe(frame);
		});
	}

	private static class MyWindowAdapter extends WindowAdapter{
		@Override
		public final void windowClosing(final WindowEvent e){
			JavaHelper.exit(0);
		}
	}

}
