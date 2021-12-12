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
package io.github.mtrevisan.familylegacy.ui;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.FamilyDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.LinkFamilyDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.LinkIndividualDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.SourceDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.GroupCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.SelectionListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyPanel;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.panels.TreePanel;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


public class MainFrame extends JFrame implements FamilyListenerInterface, IndividualListenerInterface, SelectionListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);


	private Flef store;

	private TreePanel treePanel;
	private LinkFamilyDialog linkFamilyDialog;
	private LinkIndividualDialog linkIndividualDialog;


	public MainFrame(){
		try{
			final Store storeGedcom = new Gedcom();
			final Store load = storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");
			store = (Flef)load.transform();
//			final GedcomNode family = store.getFamilies().get(0);
//			final GedcomNode family = store.getFamilies().get(4);
//			final GedcomNode family = store.getFamilies().get(9);
//			final GedcomNode family = store.getFamilies().get(64);
			final GedcomNode family = store.getFamilies().get(75);
//			final GedcomNode family = null;

			linkFamilyDialog = new LinkFamilyDialog(store, this, this);
			linkFamilyDialog.setSize(945, 500);
			linkFamilyDialog.setLocationRelativeTo(null);

			linkIndividualDialog = new LinkIndividualDialog(store, this, this);
			linkIndividualDialog.setSize(850, 500);
			linkIndividualDialog.setLocationRelativeTo(null);

			getContentPane().setLayout(new BorderLayout());
			treePanel = new TreePanel(family, 4, store, this, this);
			getContentPane().add(treePanel, BorderLayout.NORTH);
			pack();

			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			addWindowListener(new MyWindowAdapter());
			setSize(1120, 490);
			setLocationRelativeTo(null);
			setVisible(true);

			EventBusService.subscribe(this);
		}
		catch(final GedcomParseException | GedcomGrammarParseException e){
			e.printStackTrace();
		}
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	public void refresh(final EditEvent editCommand){
		switch(editCommand.getType()){
			case GROUP:
				final GroupDialog groupDialog = new GroupDialog(store, this);
				groupDialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

				groupDialog.setSize(450, 500);
				groupDialog.setLocationRelativeTo(this);
				groupDialog.setVisible(true);
				break;

			case GROUP_CITATION:
				final GroupCitationDialog groupCitationDialog = new GroupCitationDialog(store, this);
				groupCitationDialog.loadData(editCommand.getContainer());

				groupCitationDialog.setSize(450, 500);
				groupCitationDialog.setLocationRelativeTo(this);
				groupCitationDialog.setVisible(true);
				break;

			case NOTE:
				final NoteDialog noteDialog = new NoteDialog(store, this);
				noteDialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

				noteDialog.setSize(450, 500);
				noteDialog.setLocationRelativeTo(this);
				noteDialog.setVisible(true);
				break;

			case NOTE_CITATION:
				final NoteCitationDialog noteCitationDialog = new NoteCitationDialog(store, this);
				noteCitationDialog.loadData(editCommand.getContainer());

				noteCitationDialog.setSize(450, 500);
				noteCitationDialog.setLocationRelativeTo(this);
				noteCitationDialog.setVisible(true);
				break;

			case SOURCE:
				final SourceDialog sourceDialog = new SourceDialog(store, this);
				sourceDialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

				sourceDialog.setSize(450, 500);
				sourceDialog.setLocationRelativeTo(this);
				sourceDialog.setVisible(true);
				break;

			case SOURCE_CITATION:
				//TODO
//				final SourceCitationDialog sourceCitationDialog = new SourceCitationDialog(store, this);
//				sourceCitationDialog.loadData(editCommand.getContainer());
//
//				sourceCitationDialog.setSize(450, 500);
//				sourceCitationDialog.setLocationRelativeTo(this);
//				sourceCitationDialog.setVisible(true);
				break;

			case EVENT:
				//TODO
//				final EventDialog eventDialog = new EventDialog(store, this);
//				eventDialog.loadData(editCommand.getContainer());
//
//				eventDialog.setSize(450, 500);
//				eventDialog.setLocationRelativeTo(this);
//				eventDialog.setVisible(true);
				break;

			case EVENT_CITATION:
				//TODO
//				final EventCitationDialog eventCitationDialog = new EventCitationDialog(store, this);
//				eventCitationDialog.loadData(editCommand.getContainer());
//
//				eventCitationDialog.setSize(450, 500);
//				eventCitationDialog.setLocationRelativeTo(this);
//				eventCitationDialog.setVisible(true);
		}
	}

	@Override
	public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
		//TODO
		System.out.println("onEditFamily " + family.getID());

		final FamilyDialog familyDialog = new FamilyDialog(family, store, this);
		familyDialog.setSize(200, 250);
		familyDialog.setLocationRelativeTo(this);
		familyDialog.setVisible(true);
	}

	@Override
	public void onFamilyLink(final FamilyPanel boxPanel){
		LOGGER.debug("onLinkFamily");

		linkFamilyDialog.setPanelReference(boxPanel);
		linkFamilyDialog.setVisible(true);
	}

	@Override
	public void onFamilyUnlink(final FamilyPanel boxPanel, final GedcomNode family){
		//TODO
		System.out.println("onUnlinkFamily " + family.getID());
	}

	@Override
	public void onFamilyRemove(final FamilyPanel boxPanel, final GedcomNode family){
		//TODO
		System.out.println("onRemoveFamily " + family.getID());
	}

	@Override
	public void onFamilyPreviousParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
			final GedcomNode currentFamily){
		LOGGER.debug("onPrevparentFamily this: {}, other: {}, family: {}", thisParent.getID(), otherCurrentParent.getID(),
			currentFamily.getID());

		GedcomNode nextFamily = null;
		final String currentFamilyID = currentFamily.getID();
		final List<GedcomNode> familyXRefs = store.traverseAsList(thisParent, "FAMILY_PARENT[]");
		for(int familyIndex = 1; familyIndex < familyXRefs.size(); familyIndex ++)
			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
				nextFamily = store.getFamily(familyXRefs.get(familyIndex - 1).getXRef());
				break;
			}

		//update primary family
		treePanel.loadData(null, null, nextFamily);
	}

	@Override
	public void onFamilyNextParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
			final GedcomNode currentFamily){
		LOGGER.debug("onNextParentFamily this: {}, other: {}, family: {}", thisParent.getID(), otherCurrentParent.getID(),
			currentFamily.getID());

		GedcomNode nextFamily = null;
		final String currentFamilyID = currentFamily.getID();
		final List<GedcomNode> familyXRefs = store.traverseAsList(thisParent, "FAMILY_PARENT[]");
		for(int familyIndex = 0; familyIndex < familyXRefs.size() - 1; familyIndex ++){
			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
				nextFamily = store.getFamily(familyXRefs.get(familyIndex + 1).getXRef());
				break;
			}
		}

		//update primary family
		treePanel.loadData(null, null, nextFamily);
	}


	@Override
	public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
		LOGGER.debug("onFocusIndividual {}", individual.getID());

		//prefer left position if male or unknown, right if female
		GedcomNode parent1 = null;
		GedcomNode parent2 = null;
		if(IndividualPanel.extractSex(individual, store) == Sex.FEMALE)
			//put in the right box
			parent2 = individual;
		else
			//put in the left box
			parent1 = individual;

		final GedcomNode family = treePanel.getPreferredFamily(individual);

		//update primary family
		treePanel.loadData(parent1, parent2, family);
	}

	@Override
	public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onEditIndividual " + individual.getID());
	}

	@Override
	public void onIndividualLink(final IndividualPanel boxPanel, final SelectedNodeType type){
		LOGGER.debug("onLinkIndividual");

		linkIndividualDialog.setPanelReference(boxPanel);
		linkIndividualDialog.setSelectionType(type);
		linkIndividualDialog.setVisible(true);
	}

	@Override
	public void onIndividualUnlink(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onUnlinkIndividual " + individual.getID());
	}

	@Override
	public void onIndividualAdd(final IndividualPanel boxPanel){
		//TODO
		System.out.println("onAddIndividual");
	}

	@Override
	public void onIndividualRemove(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onRemoveIndividual " + individual.getID());
	}

	@Override
	public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onAddPreferredImage " + individual.getID());
	}


	@Override
	public void onNodeSelected(final GedcomNode node, final SelectedNodeType type, final JPanel panelReference){
		if(type == SelectedNodeType.FAMILY){
			final FamilyPanel familyPanel = (FamilyPanel)panelReference;
			final GedcomNode child = familyPanel.getChildReference();
			linkFamilyToChild(child, node);

			familyPanel.loadData(node);
		}
		else{
			final IndividualPanel individualPanel = (IndividualPanel)panelReference;
			final GedcomNode child = individualPanel.getChildReference();
			if(type == SelectedNodeType.INDIVIDUAL1)
				linkParentToFamily(child, node, store.getParent1s(child), "PARENT1");
			else if(type == SelectedNodeType.INDIVIDUAL2)
				linkParentToFamily(child, node, store.getParent2s(child), "PARENT2");

			individualPanel.loadData(node);
		}
	}

	private void linkFamilyToChild(final GedcomNode child, final GedcomNode family){
		LOGGER.debug("Add family {} to child {}", family.getID(), child.getID());

		store.linkFamilyToChild(child, family);
	}

	private void linkParentToFamily(final GedcomNode child, final GedcomNode parent, final List<GedcomNode> parents, final String parentTag){
		if(parents.isEmpty())
			linkParentToNewFamily(child, parent, parentTag);
		else if(parents.size() == 1)
			linkParentToExistingFamily(parents.get(0), parent, parentTag);
		else
			LOGGER.warn("Individual {} belongs to more than one family (this cannot be)", child.getID());
	}

	private void linkParentToNewFamily(final GedcomNode child, final GedcomNode parent, final String parentTag){
		//create new family and add parent
		final GedcomNode family = store.create("FAMILY")
			.addChildReference(parentTag, parent.getID());
		store.addFamily(family);
		store.linkFamilyToChild(child, family);
	}

	private void linkParentToExistingFamily(final GedcomNode family, final GedcomNode parent, final String parentTag){
		//link to existing family as parent
		family.addChild(store.create(parentTag)
			.withXRef(parent.getID()));
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		//create and display the form
		EventQueue.invokeLater(() -> (new MainFrame()).setVisible(true));
	}

	private static class MyWindowAdapter extends WindowAdapter{
		@Override
		public void windowClosing(final WindowEvent e){
			JavaHelper.exit(0);
		}
	}
}
