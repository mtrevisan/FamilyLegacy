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
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.FamilyPanel;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.panels.LinkFamilyDialog;
import io.github.mtrevisan.familylegacy.ui.panels.LinkIndividualDialog;
import io.github.mtrevisan.familylegacy.ui.panels.SelectionListenerInterface;
import io.github.mtrevisan.familylegacy.ui.panels.TreePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


public class MainFrame extends JFrame implements FamilyListenerInterface, IndividualListenerInterface, SelectionListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);


	private Flef store;

	private TreePanel panel;
	private LinkFamilyDialog linkFamilyDialog;
	private LinkIndividualDialog linkIndividualDialog;


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
//			final GedcomNode family = null;

			linkFamilyDialog = new LinkFamilyDialog(store, this, this);
			linkFamilyDialog.setSize(945, 500);
			linkFamilyDialog.setLocationRelativeTo(null);

			linkIndividualDialog = new LinkIndividualDialog(store, this, this);
			linkIndividualDialog.setSize(850, 500);
			linkIndividualDialog.setLocationRelativeTo(null);

			getContentPane().setLayout(new BorderLayout());
			panel = new TreePanel(null, null, family, 4, store, this, this);
			getContentPane().add(panel, BorderLayout.NORTH);
			pack();

			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			setSize(1120, 470);
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
		LOGGER.debug("onLinkFamily");

		linkFamilyDialog.setVisible(true);
	}

	@Override
	public void onFamilyAddChild(final FamilyPanel familyPanel, final GedcomNode family){
		//TODO
		System.out.println("onAddChildFamily " + family.getID());
	}

	@Override
	public void onFamilyPreviousSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherCurrentSpouse,
			final GedcomNode currentFamily){
		LOGGER.debug("onPrevSpouseFamily this: {}, other: {}, family: {}", thisSpouse.getID(), otherCurrentSpouse.getID(),
			currentFamily.getID());

		GedcomNode nextFamily = null;
		final String currentFamilyID = currentFamily.getID();
		final List<GedcomNode> familyXRefs = store.traverseAsList(thisSpouse, "FAMILY_SPOUSE[]");
		for(int familyIndex = 1; familyIndex < familyXRefs.size(); familyIndex ++)
			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
				nextFamily = store.getFamily(familyXRefs.get(familyIndex - 1).getXRef());
				break;
			}

		//update primary family
		panel.loadData(null, null, nextFamily);
	}

	@Override
	public void onFamilyNextSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherCurrentSpouse,
			final GedcomNode currentFamily){
		LOGGER.debug("onNextSpouseFamily this: {}, other: {}, family: {}", thisSpouse.getID(), otherCurrentSpouse.getID(),
			currentFamily.getID());

		GedcomNode nextFamily = null;
		final String currentFamilyID = currentFamily.getID();
		final List<GedcomNode> familyXRefs = store.traverseAsList(thisSpouse, "FAMILY_SPOUSE[]");
		for(int familyIndex = 0; familyIndex < familyXRefs.size() - 1; familyIndex ++){
			if(familyXRefs.get(familyIndex).getXRef().equals(currentFamilyID)){
				nextFamily = store.getFamily(familyXRefs.get(familyIndex + 1).getXRef());
				break;
			}
		}

		//update primary family
		panel.loadData(null, null, nextFamily);
	}


	@Override
	public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onEditIndividual " + individual.getID());
	}

	@Override
	public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
		LOGGER.debug("onFocusIndividual {}", individual.getID());

		//prefer left position if male or unknown, right if female
		GedcomNode spouse1 = null;
		GedcomNode spouse2 = null;
		if(IndividualPanel.extractSex(individual, store) == Sex.FEMALE)
			//put in the right box
			spouse2 = individual;
		else
			//put in the left box
			spouse1 = individual;

		final GedcomNode family = panel.getPreferredFamily(individual);

		//update primary family
		panel.loadData(spouse1, spouse2, family);
	}


	@Override
	public void onIndividualNew(final IndividualPanel boxPanel){
		//TODO
		System.out.println("onNewIndividual");
	}

	@Override
	public void onIndividualLink(final IndividualPanel boxPanel){
		LOGGER.debug("onLinkIndividual");

		linkIndividualDialog.setVisible(true);
	}

	@Override
	public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
		//TODO
		System.out.println("onAddPreferredImage " + individual.getID());
	}


	@Override
	public void onNodeSelected(final GedcomNode node){
		//TODO
		final String id = node.getID();
		System.out.println((id.charAt(0) == 'F'? "onFamilySelected ": "onIndividualSelected ") + id);
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
