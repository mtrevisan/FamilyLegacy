/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class LinkPersonPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -1361635723036701664L;


	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;


	static LinkPersonPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new LinkPersonPanel(store);
	}


	private LinkPersonPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.store = store;
	}


	void initComponents(){
//		partner1Panel = PersonPanel.create(boxType, store);
//		partner1Panel.initComponents();
//		EventBusService.subscribe(partner1Panel);
//		partner2Panel = PersonPanel.create(boxType, store);
//		partner2Panel.initComponents();
//		EventBusService.subscribe(partner2Panel);
//
//		unionPanel.setBackground(Color.WHITE);
//
//		partner1ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));
//		partner2ArrowsSpacer.setPreferredSize(new Dimension(UNION_ARROWS_WIDTH, 0));
//
//		final JPanel arrowPanel1 = new JPanel(new MigLayout("insets 0",
//			"[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
//		arrowPanel1.add(partner1ArrowsSpacer, "");
//		arrowPanel1.add(partner1PreviousParentsLabel, "right");
//		arrowPanel1.add(partner1NextParentsLabel, "left");
//		arrowPanel1.add(partner1PreviousUnionLabel, "right");
//		arrowPanel1.add(partner1NextUnionLabel, "right");
//		arrowPanel1.setOpaque(false);
//
//		arrowPersonPanel1 = new JPanel(new MigLayout("insets 0",
//			"[grow,fill]",
//			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
//		arrowPersonPanel1.add(arrowPanel1, "wrap");
//		arrowPersonPanel1.add(partner1Panel, "right");
//		arrowPersonPanel1.setOpaque(false);
//
//		final JPanel arrowPanel2 = new JPanel(new MigLayout("insets 0",
//			"[]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]0[grow]" + NAVIGATION_PARENTS_ARROW_SEPARATION + "[grow]0[]"));
//		arrowPanel2.add(partner2PreviousUnionLabel, "left");
//		arrowPanel2.add(partner2NextUnionLabel, "left");
//		arrowPanel2.add(partner2PreviousParentsLabel, "right");
//		arrowPanel2.add(partner2NextParentsLabel, "left");
//		arrowPanel2.add(partner2ArrowsSpacer, "hidemode 2");
//		arrowPanel2.setOpaque(false);
//
//		arrowPersonPanel2 = new JPanel(new MigLayout("insets 0",
//			"[grow,fill]",
//			"[" + PREVIOUS_NEXT_SIZE.getHeight() + "]" + NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
//		arrowPersonPanel2.add(arrowPanel2, "wrap");
//		arrowPersonPanel2.add(partner2Panel, "left");
//		arrowPersonPanel2.setOpaque(false);
//
//		setLayout(new MigLayout("insets 0",
//			"[right,grow]" + HALF_PARTNER_SEPARATION + "[center,grow]" + HALF_PARTNER_SEPARATION + "[left,grow]",
//			"[bottom]"));
//		add(arrowPersonPanel1, "right,grow");
//		add(unionPanel, "gapbottom " + GROUP_EXITING_HEIGHT);
//		add(arrowPersonPanel2, "left,grow");
	}

	public final void setPersonListener(final PersonListenerInterface personListener){
//		partner1Panel.setPersonListener(personListener);
//		partner2Panel.setPersonListener(personListener);
	}


	public void loadData(final Map<String, Object> group){
		loadData(group, Collections.emptyMap(), Collections.emptyMap());
	}

	void loadData(final Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
		prepareData(group, partner1, partner2);

		loadData();
	}

	private void prepareData(Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
//		if(group.isEmpty()){
//			final List<Map<String, Object>> unions = extractUnions(partner1);
//			if(!unions.isEmpty())
//				//FIXME choose the last shown family, if any
//				group = unions.getFirst();
//		}
//
//		if(!group.isEmpty()){
//			final Integer homeUnionID = extractRecordID(group);
//			final List<Integer> personIDsInUnion = getPersonIDsInGroup(homeUnionID);
//			Integer partner1ID = extractRecordID(partner1);
//			if(partner1ID != null && !personIDsInUnion.contains(partner1ID)){
//				LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner1ID, homeUnionID);
//
//				partner1 = Collections.emptyMap();
//			}
//			Integer partner2ID = extractRecordID(partner2);
//			if(partner2ID != null && !personIDsInUnion.contains(partner2ID)){
//				LOGGER.warn("Person {} does not belongs to the union {} (this cannot be)", partner2ID, homeUnionID);
//
//				partner2 = Collections.emptyMap();
//			}
//
//			if(partner1.isEmpty() || partner2.isEmpty()){
//				final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
//
//				//extract the first two persons from the union:
//				if(!partner1.isEmpty())
//					personIDsInUnion.remove(extractRecordID(partner1));
//				if(!partner2.isEmpty())
//					personIDsInUnion.remove(extractRecordID(partner2));
//				if(partner1.isEmpty() && !personIDsInUnion.isEmpty()){
//					//FIXME choose the last shown person, if any
//					partner1ID = personIDsInUnion.getFirst();
//					if(persons.containsKey(partner1ID))
//						partner1 = persons.get(partner1ID);
//					personIDsInUnion.remove(partner1ID);
//				}
//				if(partner2.isEmpty() && !personIDsInUnion.isEmpty()){
//					//FIXME choose the last shown person, if any
//					partner2ID = personIDsInUnion.getFirst();
//					if(persons.containsKey(partner2ID))
//						partner2 = persons.get(partner2ID);
//					personIDsInUnion.remove(partner2ID);
//				}
//			}
//		}
//
//		this.union = group;
//		this.partner1 = partner1;
//		this.partner2 = partner2;
	}

	private void loadData(){
//		partner1Panel.loadData(partner1);
//		partner2Panel.loadData(partner2);
//
//		if(boxType == BoxPanelType.PRIMARY){
//			final Integer groupID = extractRecordID(union);
//			updatePreviousNextUnionIcons(groupID, partner2, partner1PreviousUnionLabel, partner1NextUnionLabel);
//			updatePreviousNextUnionIcons(groupID, partner1, partner2PreviousUnionLabel, partner2NextUnionLabel);
//
//			updatePreviousNextParentsIcons(partner1, partner1PreviousParentsLabel, partner1NextParentsLabel);
//			updatePreviousNextParentsIcons(partner2, partner2PreviousParentsLabel, partner2NextParentsLabel);
//		}
//
//		unionPanel.setBorder(!union.isEmpty()? BorderFactory.createLineBorder(BORDER_COLOR):
//			BorderFactory.createDashedBorder(BORDER_COLOR));
//
//		refresh(ActionCommand.ACTION_COMMAND_GROUP);
//
//		partner1Panel.repaint();
//		partner2Panel.repaint();
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_GROUP)
			return;

//		final boolean hasData = !union.isEmpty();
//		//		final boolean hasGroups = !getRecords(TABLE_NAME_GROUP).isEmpty();
//		//		final boolean hasChildren = (getChildren().length > 0);
//		editGroupItem.setEnabled(hasData);
//		addGroupItem.setEnabled(!hasData);
//		//		linkGroupItem.setEnabled(!hasData && hasGroups);
//		removeGroupItem.setEnabled(hasData);
	}


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

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName11 = new HashMap<>();
		personName11.put("id", 1);
		personName11.put("person_id", 1);
		personName11.put("personal_name", "tòni");
		personName11.put("family_name", "bruxatin");
		personName11.put("locale", "vec-IT");
		personName11.put("type", "birth name");
		personNames.put((Integer)personName11.get("id"), personName11);
		final Map<String, Object> personName12 = new HashMap<>();
		personName12.put("id", 2);
		personName12.put("person_id", 1);
		personName12.put("personal_name", "antonio");
		personName12.put("family_name", "bruciatino");
		personName12.put("locale", "it-IT");
		personName12.put("type", "death name");
		personNames.put((Integer)personName12.get("id"), personName12);
		final Map<String, Object> personName21 = new HashMap<>();
		personName21.put("id", 3);
		personName21.put("person_id", 2);
		personName21.put("personal_name", "bèpi");
		personName21.put("family_name", "marangon");
		personName21.put("locale", "vec-IT");
		personName21.put("type", "birth name");
		personNames.put((Integer)personName21.get("id"), personName21);

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
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("id", 3);
		group3.put("type", "family");
		groups.put((Integer)group3.get("id"), group3);
		final Map<String, Object> group4 = new HashMap<>();
		group4.put("id", 4);
		group4.put("type", "family");
		groups.put((Integer)group4.get("id"), group4);

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
		groupJunction4.put("group_id", 4);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 2);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 3);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 2);
		groupJunction5.put("role", "adoptee");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
		//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

//		final GroupListenerInterface unionListener = new GroupListenerInterface(){
//			@Override
//			public void onGroupEdit(final GroupPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getUnion();
//				System.out.println("onEditGroup " + extractRecordID(group));
//			}
//
//			@Override
//			public void onGroupAdd(final GroupPanel groupPanel){
//				System.out.println("onAddGroup");
//			}
//
//			@Override
//			public void onGroupLink(final GroupPanel groupPanel){
//				final PersonPanel partner1 = groupPanel.getPartner1();
//				final PersonPanel partner2 = groupPanel.getPartner2();
//				final Map<String, Object> group = groupPanel.union;
//				System.out.println("onLinkPersonToSiblingGroup (partner 1: " + extractRecordID(partner1.getPerson())
//					+ ", partner 2: " + extractRecordID(partner2.getPerson()) + "group: " + extractRecordID(group));
//			}
//
//			@Override
//			public void onGroupRemove(final GroupPanel groupPanel){
//				final Map<String, Object> group = groupPanel.getUnion();
//				System.out.println("onRemoveGroup " + extractRecordID(group));
//			}
//
//			@Override
//			public void onPersonChangeParents(final GroupPanel groupPanel, final PersonPanel personPanel, final Map<String, Object> newParents){
//				System.out.println("onGroupChangeParents person: " + extractRecordID(personPanel.getPerson())
//					+ ", new parents: " + extractRecordID(newParents));
//			}
//
//			@Override
//			public void onPersonChangeUnion(final GroupPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
//				final Map<String, Object> newUnion){
//				final Map<String, Object> oldUnion = groupPanel.getUnion();
//				System.out.println("onPersonChangeUnion old partner: " + extractRecordID(oldPartner.getPerson())
//					+ ", old union: " + oldUnion.get("id") + ", new partner: " + extractRecordID(newPartner)
//					+ ", new union: " + extractRecordID(newUnion));
//			}
//		};

		EventQueue.invokeLater(() -> {
			final LinkPersonPanel panel = create(store);
			panel.initComponents();
			panel.loadData(group1);
//			panel.setGroupListener(unionListener);
			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
