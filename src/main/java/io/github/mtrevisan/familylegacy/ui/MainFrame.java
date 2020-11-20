package io.github.mtrevisan.familylegacy.ui;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyPanel;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.panels.TreePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainFrame extends JFrame implements FamilyListenerInterface, IndividualListenerInterface{

	private Flef store;

	/** Stores the gedcom node of the last selected family for an individual. */
	private final Map<GedcomNode, GedcomNode> individualLastSelectedFamily = new HashMap<>(0);
	/** Stores the gedcom node of the last selected child for a family. */
	private final Map<GedcomNode, GedcomNode> familyLastSelectedChild = new HashMap<>(0);


	public MainFrame(){
		try{
			final Store storeGedcom = new Gedcom();
			store = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
				.transform();
//			final GedcomNode family = store.getFamilies().get(0);
			final GedcomNode family = store.getFamilies().get(4);
//			final GedcomNode family = store.getFamilies().get(9);
//			final GedcomNode family = store.getFamilies().get(64);
//			final GedcomNode family = store.getFamilies().get(75);
//			GedcomNode family = null;


			getContentPane().setLayout(new BorderLayout());
			final TreePanel panel = new TreePanel(family, 3, store, this, this);
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
	public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
		//prefer left position if male or unknown, right if female
		final Sex sex = extractSex(individual);
		if(sex == Sex.FEMALE){
			//TODO put in the right box
		}
		else{
			//TODO put in the left box
		}
		//TODO see if this individual belongs to a family
		GedcomNode family = null;
		final List<GedcomNode> families = store.traverseAsList(individual, "FAMILY_CHILD");
		if(families.size() > 1){
			//if it belongs to more than one family, select the last one...
			family = individualLastSelectedFamily.get(individual);
			if(family == null){
				//TODO ... or the oldest...
			}
			if(family == null){
				//TODO ... or the first
			}
		}
		else if(families.size() == 1){
			//TODO the individual belongs to exact one family, choose it and load as the primary family
			family = families.get(0);
		}
		else{
			//TODO if it belongs to no families, then put it into a fake family (?)
		}

		//store the node of the last selected family
		individualLastSelectedFamily.put(individual, family);

		System.out.println("onFocusIndividual " + individual.getID());
	}

	private Sex extractSex(final GedcomNode individual){
		return Sex.fromCode(store.traverse(individual, "SEX")
			.getValue());
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
