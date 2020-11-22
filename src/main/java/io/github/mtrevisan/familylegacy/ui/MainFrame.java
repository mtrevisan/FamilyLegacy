package io.github.mtrevisan.familylegacy.ui;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNodeBuilder;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyPanel;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.panels.TreePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class MainFrame extends JFrame implements FamilyListenerInterface, IndividualListenerInterface{

	private Flef store;

	private TreePanel panel;
	private final Deque<GedcomNode> selectedNode = new ArrayDeque<>();


	public MainFrame(){
		try{
			final Store storeGedcom = new Gedcom();
			store = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
				.transform();
//			final GedcomNode family = store.getFamilies().get(0);
//			final GedcomNode family = store.getFamilies().get(4);
//			final GedcomNode family = store.getFamilies().get(9);
//			final GedcomNode family = store.getFamilies().get(64);
			final GedcomNode family = store.getFamilies().get(75);
//			GedcomNode family = null;


			getContentPane().setLayout(new BorderLayout());
			panel = new TreePanel(null, null, family, 3, store, this, this);
			getContentPane().add(panel, BorderLayout.NORTH);
			pack();

			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			setSize(new Dimension(1000, 470));
			setLocationRelativeTo(null);
			setVisible(true);
		}
		catch(final GedcomParseException | GedcomGrammarParseException e){
			e.printStackTrace();
		}
	}

	@Override
	public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
		//TODO
		System.out.println("onEditFamily " + family.getID());
	}

	@Override
	public void onFamilyLink(final FamilyPanel boxPanel){
		//TODO
		System.out.println("onLinkFamily");
	}

	@Override
	public void onFamilyAddChild(final FamilyPanel familyPanel, final GedcomNode family){
		//TODO
		System.out.println("onAddChildFamily " + family.getID());
	}

	@Override
	public void onFamilyPreviousSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse){
		//TODO
		System.out.println("onPrevSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID());
	}

	@Override
	public void onFamilyNextSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse){
		//TODO
		System.out.println("onNextSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID());
	}


	@Override
	public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onEditIndividual " + individual.getID());
	}

	/**
	 * Bring individual to primary position.
	 *
	 * @param boxPanel	The box panel that originates the call.
	 * @param individual	The individual that has to obtain focus.
	 */
	@Override
	public void onIndividualFocus(final IndividualPanel boxPanel, GedcomNode individual){
//individual = store.getIndividual("I202");
		//prefer left position if male or unknown, right if female
		GedcomNode spouse1 = null;
		GedcomNode spouse2 = null;
		final Sex sex = extractSex(individual);
		if(sex == Sex.FEMALE)
			//put in the right box
			spouse2 = individual;
		else
			//put in the left box
			spouse1 = individual;

		GedcomNode family = null;
		//see if this individual belongs to a family
		final List<GedcomNode> familyXRefs = store.traverseAsList(individual, "FAMILY_SPOUSE[]");
		List<GedcomNode> families = new ArrayList<>(familyXRefs.size());
		for(final GedcomNode familyXRef : familyXRefs)
			families.add(store.getFamily(familyXRef.getXRef()));
		if(familyXRefs.size() > 1){
			//if it belongs to more than one family, select those with the oldest event
			LocalDate oldestDate = null;
			final List<GedcomNode> oldestFamilies = new ArrayList<>(0);
			for(final GedcomNode f : families){
				final LocalDate oldestEventDate = extractOldestEventDate(f);
				if(oldestEventDate != null){
					final int cmp;
					if(oldestDate == null || (cmp = oldestEventDate.compareTo(oldestDate)) < 0){
						oldestDate = oldestEventDate;
						oldestFamilies.clear();
						oldestFamilies.add(f);
					}
					else if(cmp == 0)
						oldestFamilies.add(f);
				}
			}
			if(oldestFamilies.size() == 1)
				family = oldestFamilies.get(0);
			else{
				//choose the one with the lowest ID
				if(!oldestFamilies.isEmpty())
					families = oldestFamilies;
				final Map<Integer, GedcomNode> all = new TreeMap<>();
				for(final GedcomNode fam : families)
					all.put(Integer.parseInt(fam.getID().substring(1)), fam);
				family = all.values().iterator().next();
			}
		}
		else if(familyXRefs.size() == 1)
			//the individual belongs to exact one family, choose it and load as the primary family
			family = families.get(0);

		//TODO update primary family
		panel.loadData(spouse1, spouse2, family, 3);

		System.out.println("onFocusIndividual " + individual.getID());
	}

	private Sex extractSex(final GedcomNode individual){
		return Sex.fromCode(store.traverse(individual, "SEX")
			.getValue());
	}

	private LocalDate extractOldestEventDate(final GedcomNode node){
		final List<GedcomNode> events = store.traverseAsList(node, "EVENT[]");
		final TreeMap<LocalDate, GedcomNode> dateEvent = new TreeMap<>();
		for(final GedcomNode event : events){
			final GedcomNode eventDate = store.traverse(event, "DATE");
			if(!eventDate.isEmpty())
				dateEvent.put(DateParser.parse(eventDate.getValue()), event);
		}
		return (!dateEvent.isEmpty()? dateEvent.keySet().iterator().next(): null);
	}

	@Override
	public void onIndividualNew(final IndividualPanel boxPanel){
		//TODO
		System.out.println("onNewIndividual");
	}

	@Override
	public void onIndividualLink(final IndividualPanel boxPanel){
		//TODO
		System.out.println("onLinkIndividual");
	}

	@Override
	public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onAddPreferredImage " + individual.getID());
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

}
