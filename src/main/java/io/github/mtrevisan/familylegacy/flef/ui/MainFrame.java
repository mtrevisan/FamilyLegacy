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

import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.SelectedNodeType;
import io.github.mtrevisan.familylegacy.flef.ui.panels.TreePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public final class MainFrame extends JFrame implements GroupListenerInterface, PersonListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

	private static final String TABLE_NAME_GROUP = "group";


	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private final TreePanel treePanel;
//	private LinkFamilyDialog linkFamilyDialog;
//	private LinkIndividualDialog linkIndividualDialog;


	private MainFrame(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Map<String, Object> homeGroup){
		this.store = store;

		treePanel = TreePanel.create(4, store);
		treePanel.loadDataFromUnion(homeGroup);
		treePanel.setUnionListener(this);
		treePanel.setPersonListener(this);

		final JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(treePanel, BorderLayout.NORTH);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				System.exit(0);
			}
		});
		frame.setSize(1200, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		EventBusService.subscribe(this);
	}


	private static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}


	@EventHandler
	public void error(final BusExceptionEvent exceptionEvent){
		final Throwable cause = exceptionEvent.getCause();
		JOptionPane.showMessageDialog(this, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	public void refresh(final EditEvent editCommand){
		LOGGER.debug("refresh {}", editCommand);

		final Map<String, Object> container = editCommand.getContainer();
		final String tableName = editCommand.getIdentifier();
		final int recordID = extractRecordID(container);
		switch(editCommand.getType()){
			case GROUP -> {
				final GroupDialog groupDialog = GroupDialog.create(store, this);
				groupDialog.initComponents();
				groupDialog.loadData();
				final Integer groupID = extractRecordID(container);
				if(groupID != null)
					groupDialog.selectData(groupID);

				groupDialog.setSize(541, 481);
				groupDialog.setLocationRelativeTo(null);
				groupDialog.setVisible(true);
			}
//TODO
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
		}
	}


	@Override
	public void onGroupEdit(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onEditGroup {}", group.get("id"));

		EventBusService.publish(EditEvent.create(EditEvent.EditType.GROUP, TABLE_NAME_GROUP, group));

		//TODO
//		final FamilyRecordDialog familyDialog = new FamilyRecordDialog(family, store, this);
//		familyDialog.setSize(340, 300);
//		familyDialog.setLocationRelativeTo(this);
//		familyDialog.setVisible(true);
	}

	@Override
	public void onGroupLink(final GroupPanel groupPanel){
		final Map<String, Object> partner1 = groupPanel.getPartner1();
		final Map<String, Object> partner2 = groupPanel.getPartner2();
		final Map<String, Object>[] children = groupPanel.getChildren();
		LOGGER.debug("onLinkGroup (partner 1: {}, partner 2: {}, child: {})", partner1.get("id"), partner2.get("id"),
			Arrays.toString(Arrays.stream(children).map(MainFrame::extractRecordID).toArray(Integer[]::new)));

		//TODO
//		linkFamilyDialog.setPanelReference(boxPanel);
//		linkFamilyDialog.setVisible(true);
	}

	@Override
	public void onGroupUnlink(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onUnlinkGroup {}", group.get("id"));

		//TODO
	}

	@Override
	public void onGroupAdd(final GroupPanel groupPanel){
		final Map<String, Object> partner1 = groupPanel.getPartner1();
		final Map<String, Object> partner2 = groupPanel.getPartner2();
		final Map<String, Object>[] children = groupPanel.getChildren();
		LOGGER.debug("onAddGroup (partner 1: {}, partner 2: {}, child: {})", partner1.get("id"), partner2.get("id"),
			Arrays.toString(Arrays.stream(children).map(MainFrame::extractRecordID).toArray(Integer[]::new)));

		//TODO
//		linkFamilyDialog.setPanelReference(groupPanel);
//		linkFamilyDialog.setVisible(true);
	}

	@Override
	public void onGroupRemove(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onRemoveGroup {}", group.get("id"));

		//TODO
	}

	@Override
	public void onGroupChangeParents(final GroupPanel groupPanel, final Map<String, Object> person, final Map<String, Object> newUnion){
		final Map<String, Object> currentUnion = groupPanel.getGroup();
		LOGGER.debug("onGroupChangeParents person: {}, current: {}, new: {}", person.get("id"), currentUnion.get("id"), newUnion.get("id"));

		//TODO
//		setPersonListener etc...
	}

	@Override
	public void onGroupChangeUnion(final GroupPanel groupPanel, final Map<String, Object> currentPartner,
			final Map<String, Object> otherPartner){
		final Map<String, Object> currentGroup = groupPanel.getGroup();
		LOGGER.debug("onPrevPartnerGroup this: {}, other: {}, current group: {}", currentPartner.get("id"), otherPartner.get("id"),
			currentGroup.get("id"));

		//TODO
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
//		setPersonListener
	}


	@Override
	public void onPersonEdit(final PersonPanel boxPanel){
		final Map<String, Object> person = boxPanel.getPerson();
		LOGGER.debug("onEditPerson {}", person.get("id"));

		//TODO
	}

	@Override
	public void onPersonFocus(final PersonPanel boxPanel, final SelectedNodeType type){
		final Map<String, Object> person = boxPanel.getPerson();
		LOGGER.debug("onFocusPerson {}, type is {}", person.get("id"), type);

		treePanel.loadDataFromPerson(person);

		//TODO
		//prefer left position if male or unknown, right if female
//		final GedcomNode partner1 = (type == SelectedNodeType.PARTNER1? individual: store.createEmptyNode());
//		final GedcomNode partner2 = (type == SelectedNodeType.PARTNER2? individual: store.createEmptyNode());
		//FIXME choose the current shown family, if any
//		final GedcomNode family = treePanel.getPreferredFamily(individual);
//
		//update primary family
//		treePanel.loadData(partner1, partner2, family);
	}

	@Override
	public void onPersonLink(final PersonPanel boxPanel, final SelectedNodeType type){
		final Map<String, Object> partner = boxPanel.getPartner();
		final Map<String, Object> marriage = boxPanel.getUnion();
		final Map<String, Object>[] children = boxPanel.getChildren();
		LOGGER.debug("onLinkPerson (partner {}, marriage {}, child {}), type is " + type, partner.get("id"), marriage.get("id"),
			Arrays.toString(Arrays.stream(children).map(child -> child.get("id")).toArray(Object[]::new)));

		//TODO
//		linkIndividualDialog.setPanelReference(boxPanel);
//		linkIndividualDialog.setSelectionType(type);
//		linkIndividualDialog.setVisible(true);
	}

	@Override
	public void onPersonUnlink(final PersonPanel boxPanel){
		final Map<String, Object> person = boxPanel.getPerson();
		LOGGER.debug("onUnlinkPerson {}", person.get("id"));

		//TODO
	}

	@Override
	public void onPersonAdd(final PersonPanel boxPanel, final SelectedNodeType type){
		final Map<String, Object> partner = boxPanel.getPartner();
		final Map<String, Object> marriage = boxPanel.getUnion();
		final Map<String, Object>[] children = boxPanel.getChildren();
		System.out.println("onAddPerson (partner " + partner.get("id") + ", marriage " + marriage.get("id") + ", child "
			+ Arrays.toString(Arrays.stream(children).map(child -> child.get("id")).toArray(Object[]::new)) + "), type is " + type);

		//TODO
	}

	@Override
	public void onPersonRemove(final PersonPanel boxPanel){
		final Map<String, Object> person = boxPanel.getPerson();
		LOGGER.debug("onRemovePerson {}", person.get("id"));

		//TODO
	}

	@Override
	public void onPersonAddImage(final PersonPanel boxPanel){
		final Map<String, Object> person = boxPanel.getPerson();
		LOGGER.debug("onAddPreferredImage {}", person.get("id"));

		//TODO
	}


	private void linkFamilyToChild(final Map<String, Object> child, final Map<String, Object> family){
		//TODO
//		LOGGER.debug("Add family {} to child {}", family.getID(), child.getID());
//
//		store.linkFamilyToChild(child, family);
	}

	private void linkChildToFamily(final Map<String, Object> child, final Map<String, Object> partner,
			final List<Map<String, Object>> partners, final String partnerTag){
		//TODO
//		if(partners.isEmpty())
//			linkIndividualToNewFamily(child, partner, partnerTag);
//		else if(partners.size() == 1)
//			linkIndividualToExistingFamily(partners.get(0), partner, partnerTag);
//		else
//			LOGGER.warn("Individual {} belongs to more than one family (this cannot be)", child.getID());
	}

	private void linkIndividualToNewFamily(final Map<String, Object> child, final Map<String, Object> partner, final String partnerTag){
		//TODO
//		//create new family and add parent
//		final Map<String, Object> family = store.create("FAMILY")
//			.addChildReference(partnerTag, partner.getID());
//		store.addFamily(family);
//		store.linkFamilyToChild(child, family);
	}

	private void linkIndividualToExistingFamily(final Map<String, Object> family, final Map<String, Object> partner, final String partnerTag){
		//TODO
//		//link to existing family as parent
//		family.addChild(store.create(partnerTag)
//			.withXRef(partner.getID()));
	}


	//TODO
//	@Override
//	public void onItemSelected(final GedcomNode node, final SelectedNodeType type, final JPanel panelReference){
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
//	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		persons.put((Integer)person3.get("id"), person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		persons.put((Integer)person4.get("id"), person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		persons.put((Integer)person5.get("id"), person5);

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put("group", groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put("group_junction", groupJunctions);
		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("id", 1);
		groupJunction11.put("group_id", 1);
		groupJunction11.put("reference_table", "person");
		groupJunction11.put("reference_id", 1);
		groupJunction11.put("role", "partner");
		groupJunctions.put((Integer)groupJunction11.get("id"), groupJunction11);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunction2.put("role", "partner");
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("id", 3);
		groupJunction13.put("group_id", 2);
		groupJunction13.put("reference_table", "person");
		groupJunction13.put("reference_id", 1);
		groupJunction13.put("role", "partner");
		groupJunctions.put((Integer)groupJunction13.get("id"), groupJunction13);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 4);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "person");
		groupJunction3.put("reference_id", 3);
		groupJunction3.put("role", "partner");
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("id", 5);
		groupJunction4.put("group_id", 1);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 4);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 1);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 5);
		groupJunction5.put("role", "child");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("id", 7);
		groupJunction6.put("group_id", 2);
		groupJunction6.put("reference_table", "person");
		groupJunction6.put("reference_id", 4);
		groupJunction6.put("role", "partner");
		groupJunctions.put((Integer)groupJunction6.get("id"), groupJunction6);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type", "adoption");
		event1.put("reference_table", "person");
		event1.put("reference_id", 5);
		events.put((Integer)event1.get("id"), event1);


		//create and display the form
		EventQueue.invokeLater(() -> new MainFrame(store, group1));
	}

}
