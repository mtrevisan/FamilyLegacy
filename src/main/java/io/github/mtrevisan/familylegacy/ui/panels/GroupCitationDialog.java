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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.ui.utilities.FamilyTableCellRenderer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class GroupCitationDialog extends JDialog{

//	private static final long serialVersionUID = -3246390161022821225L;

	private static final DefaultComboBoxModel CREDIBILITY_MODEL = new DefaultComboBoxModel(new String[]{
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton groupLinkButton = new JButton("Link");
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JLabel credibilityLabel = new JLabel("Restriction:");
	private final JComboBox credibilityComboBox = new JComboBox(CREDIBILITY_MODEL);

	private GedcomNode container;
	private final Flef store;


	public GroupCitationDialog(final GedcomNode container, final Flef store, final Frame parent){
		super(parent, true);

		this.container = container;
		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Groups");

//		+1 GROUP @<XREF:GROUP>@    {0:M}	/* A GROUP_RECORD() object giving the group in which this family belongs. */
//			+2 ROLE <ROLE_IN_GROUP>    {0:1}	/* Indicates what role this family played in the group that is being cited in this context. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//			+2 SOURCE @<XREF:SOURCE>@    {0:M}	/* An xref ID of a source record. */
//				+3 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//				+3 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND, MOTHER, WIFE, SPOUSE, etc. */
//				+3 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//				+3 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
//			+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */

		final FamilyTableCellRenderer rightAlignedRenderer = new FamilyTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);

		groupLinkButton.addActionListener(e -> {
			//TODO go to group_record
		});

		roleLabel.setLabelFor(roleField);

		notesButton.addActionListener(e -> {
			//TODO
		});

		sourcesButton.addActionListener(e -> {
			//TODO
		});

		credibilityLabel.setLabelFor(credibilityComboBox);

		setLayout(new MigLayout());
		add(groupLinkButton, "sizegroup button,grow,wrap");
		add(roleLabel, "align label,split 2");
		add(roleField, "grow,wrap");
		add(notesButton, "sizegroup button,grow,wrap");
		add(sourcesButton, "sizegroup button,grow,wrap");
		add(credibilityLabel, "align label,split 2");
		add(credibilityComboBox, "grow");
	}

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> groups = store.getGroups();

		//TODO
//		roleField.setText(store.traverse(container, "TYPE").getValue());
//		restrictionField.setText(store.traverse(container, "RESTRICTION").getValue());
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();
		final GedcomNode container = storeFlef.getFamilies().get(0);

		EventQueue.invokeLater(() -> {
			final GroupCitationDialog dialog = new GroupCitationDialog(container, storeFlef, new JFrame());

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 200);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
