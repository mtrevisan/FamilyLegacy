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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.FamilyTableCellRenderer;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;


//TODO
public class FamilyDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 2075397360104239479L;

	private static final DefaultComboBoxModel<String> TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY, "unknown",
		"marriage", "not married", "civil marriage", "religious marriage", "common law marriage", "partnership", "registered partnership",
		"living together", "living apart together"});
	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"confidential", "locked", "private"});

	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(TYPE_MODEL);
	private final JButton eventsButton = new JButton("Events");
	private final JButton groupsButton = new JButton("Groups");
	private final JButton culturalRulesButton = new JButton("Cultural Rules");
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);

	private GedcomNode family;
	private final Flef store;


	public FamilyDialog(final GedcomNode family, final Flef store, final Frame parent){
		super(parent, true);

		this.family = family;
		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Family record");

/*
		+1 GROUP @<XREF:GROUP>@    {0:M}	/* A GROUP_RECORD() object giving the group in which this family belongs. * /
			+2 ROLE <ROLE_IN_GROUP>    {0:1}	/* Indicates what role this family played in the group that is being cited in this context. * /
			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. * /
			+2 SOURCE @<XREF:SOURCE>@    {0:M}	/* An xref ID of a source record. * /
				+3 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (e.g. 'Film: 1234567, Frame: 344, Line: 28'). * /
				+3 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND/PARENT1, MOTHER/PARENT2, WIFE, SPOUSE/PARENT, etc. * /
				+3 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. * /
				+3 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. * /
			+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. * /
		+1 CULTURAL_RULE @<XREF:RULE>@    {0:M}	/* An xref ID of a cultural rule record. * /
		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. * /
		+1 SOURCE @<XREF:SOURCE>@    {0:M}	/* An xref ID of a source record. * /
			+2 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). * /
			+2 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND/PARENT1, MOTHER/PARENT2, WIFE, SPOUSE/PARENT, etc. * /
			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. * /
			+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. * /
		+1 {EVENT} <<FAMILY_EVENT_STRUCTURE>>    {0:M}	/* A list of FAMILY_EVENT_STRUCTURE() objects giving events associated with this family. * /
*/

		final FamilyTableCellRenderer rightAlignedRenderer = new FamilyTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);

		typeLabel.setLabelFor(typeComboBox);
		typeComboBox.setEditable(true);
		typeComboBox.addActionListener(e -> {
			if("comboBoxEdited".equals(e.getActionCommand())){
				final String newValue = (String)TYPE_MODEL.getSelectedItem();
				TYPE_MODEL.addElement(newValue);

				typeComboBox.setSelectedItem(newValue);
			}
		});
		typeComboBox.setSelectedIndex(0);

		eventsButton.addActionListener(e -> {
			//TODO
		});

		groupsButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP_CITATION, family)));

		culturalRulesButton.addActionListener(e -> {
			//TODO
		});

		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, family)));

		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, family)));

		restrictionLabel.setLabelFor(restrictionComboBox);
		restrictionComboBox.setEditable(true);
		restrictionComboBox.addActionListener(e -> {
			if("comboBoxEdited".equals(e.getActionCommand())){
				final String newValue = (String)RESTRICTION_MODEL.getSelectedItem();
				RESTRICTION_MODEL.addElement(newValue);

				restrictionComboBox.setSelectedItem(newValue);
			}
		});
		restrictionComboBox.setSelectedIndex(0);

		setLayout(new MigLayout());
		add(typeLabel, "align label,split 2");
		add(typeComboBox, "grow,wrap");
		add(eventsButton, "sizegroup button,grow,wrap");
		add(groupsButton, "sizegroup button,grow,wrap");
		add(culturalRulesButton, "sizegroup button,grow,wrap");
		add(notesButton, "sizegroup button,grow,wrap");
		add(sourcesButton, "sizegroup button,grow,wrap");
		add(restrictionLabel, "align label,split 2");
		add(restrictionComboBox, "grow");
	}

	public void loadData(final GedcomNode family){
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		typeComboBox.setSelectedItem(store.traverse(family, "TYPE").getValue());
		restrictionComboBox.setSelectedItem(store.traverse(family, "RESTRICTION").getValue());
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
		final GedcomNode family = storeFlef.getFamilies().get(0);

		EventQueue.invokeLater(() -> {
			final FamilyDialog dialog = new FamilyDialog(family, storeFlef, new JFrame());

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(200, 250);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
