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


public class FamilyDialog extends JDialog{

//	private static final long serialVersionUID = -3246390161022821225L;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JScrollPane familiesScrollPane = new JScrollPane();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

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
	type: ------
	groups ?
	cultural rules ?
	notes ?
	sources ?
	events ?
	restriction: ---
*/
//		+1 TYPE <FAMILY_TYPE>    {1:1}	/* One of 'unknown', 'marriage', 'not married', 'civil marriage', 'religious marriage', 'common law marriage', 'partnership', 'registered partnership', 'living together', 'living apart together'. */
//		+1 SPOUSE1 @<XREF:INDIVIDUAL>@    {0:1}	/* An xref ID of the first spouse. In a heterosexual pair union, this is traditionally the husband. */
//		+1 SPOUSE2 @<XREF:INDIVIDUAL>@    {0:1}	/* An xref ID of the second spouse. In a heterosexual pair union, this is traditionally the wife. */
//		+1 CHILD @<XREF:INDIVIDUAL>@    {0:M}	/* A vector of xref IDs of children in this family. */
//		+1 GROUP @<XREF:GROUP>@    {0:M}	/* A GROUP_RECORD() object giving the group in which this family belongs. */
//			+2 ROLE <ROLE_IN_GROUP>    {0:1}	/* Indicates what role this family played in the group that is being cited in this context. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//			+2 SOURCE @<XREF:SOURCE>@    {0:M}	/* An xref ID of a source record. */
//				+3 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//				+3 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND, MOTHER, WIFE, SPOUSE, etc. */
//				+3 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//				+3 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
//				+3 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
//		+1 CULTURAL_RULE @<XREF:RULE>@    {0:M}	/* An xref ID of a cultural rule record. */
//		+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//		+1 SOURCE @<XREF:SOURCE>@    {0:M}	/* An xref ID of a source record. */
//			+2 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//			+2 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND, MOTHER, WIFE, SPOUSE, etc. */
//			+2 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//			+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */
//		+1 {EVENT} <<FAMILY_EVENT_STRUCTURE>>    {0:M}	/* A list of FAMILY_EVENT_STRUCTURE() objects giving events associated with this family. */
//		+1 RESTRICTION <RESTRICTION_NOTICE>    {0:1}	/* Specifies how the superstructure should be treated. Known values and their meaning are: "confidential" (should not be distributed or exported), "locked" (should not be edited), "private" (has had information omitted to maintain confidentiality) */

		final FamilyTableCellRenderer rightAlignedRenderer = new FamilyTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);

		filterLabel.setLabelFor(filterField);
		filterField.setEnabled(false);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow");
		add(familiesScrollPane, "newline,width 100%,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final GedcomNode family){
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> families = store.getFamilies();
		okButton.setEnabled(!families.isEmpty());

		final int size = families.size();
		if(size > 0){
			filterField.setEnabled(true);
		}
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
			dialog.setSize(700, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);

//			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//			scheduler.schedule(dialog::loadData, 3, TimeUnit.SECONDS);
		});
	}

}
