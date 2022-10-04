/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.utilities.ScaledImage;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;
import java.util.StringJoiner;


//TODO
public class FamilyRecordDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(FamilyRecordDialog.class);

	@Serial
	private static final long serialVersionUID = 2075397360104239479L;

	private static final String NAMES_SEPARATOR = ", ";

	private static final int ID_PREFERRED_WIDTH = 43;

	private static final int CHILDREN_TABLE_INDEX_ID = 0;
	private static final int CHILDREN_TABLE_INDEX_NAME = 1;

	private static final DefaultComboBoxModel<String> TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY, "unknown",
		"marriage", "not married", "civil marriage", "religious marriage", "common law marriage", "partnership", "registered partnership",
		"living together", "living apart together"});
	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"confidential", "locked", "private"});

	private final JLabel partner1Label = new JLabel("Partner 1:");
	private final ScaledImage partner1Image = new ScaledImage(null);
	private final JLabel partner1Name = new JLabel(StringUtils.EMPTY);
	private final JButton partner1Notes = new JButton(StringUtils.EMPTY);
	private final JLabel partner2Label = new JLabel("Partner 2:");
	private final ScaledImage partner2Image = new ScaledImage(null);
	private final JLabel partner2Name = new JLabel(StringUtils.EMPTY);
	private final JButton partner2Notes = new JButton(StringUtils.EMPTY);
	private final JLabel childrenLabel = new JLabel("Children:");
	private final JTable childrenTable = new JTable(new ChildrenTableModel());
	private final JButton eventsButton = new JButton("Events");
	private final JButton groupsButton = new JButton("Groups");
	private final JButton culturalRulesButton = new JButton("Cultural Rules");
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);

	private GedcomNode family;
	private final Flef store;


	public FamilyRecordDialog(final GedcomNode family, final Flef store, final Frame parent){
		super(parent, true);

		this.family = family;
		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Family record");

		childrenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		childrenTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableHelper.setColumnWidth(childrenTable, CHILDREN_TABLE_INDEX_ID, 0, ID_PREFERRED_WIDTH);

/*
		+1 PARTNER1 @<XREF:INDIVIDUAL>@    {0:1}
			+2 NOTE @<XREF:NOTE>@    {0:M}
		+1 PARTNER2 @<XREF:INDIVIDUAL>@    {0:1}
			+2 NOTE @<XREF:NOTE>@    {0:M}
		+1 CHILD @<XREF:INDIVIDUAL>@    {0:M}
			+2 NOTE @<XREF:NOTE>@    {0:M}

		+1 EVENT @<XREF:EVENT>@    {0:M}

		+1 <<GROUP_CITATION>>    {0:M}

		+1 CULTURAL_RULE @<XREF:RULE>@    {0:M}

		+1 NOTE @<XREF:NOTE>@    {0:M}

		+1 <<SOURCE_CITATION>>    {0:M}

		+1 PREFERRED_IMAGE <DOCUMENT_FILE_REFERENCE>    {0:1}
			+2 CROP <CROP_COORDINATES>    {0:1}
		+1 RESTRICTION <confidential>    {0:1}
*/

		partner1Label.setLabelFor(partner1Name);
		partner2Label.setLabelFor(partner2Name);

		final List<GedcomNode> children = store.traverseAsList(family, "CHILD[]");
		final DefaultTableModel childrenTableModel = (DefaultTableModel)childrenTable.getModel();
		final int size = children.size();
		childrenTableModel.setRowCount(size);
		for(int row = 0; row < size; row ++){
			final String childXRef = children.get(row)
				.getXRef();
			final GedcomNode child = store.getIndividual(childXRef);

			childrenTableModel.setValueAt(child.getID(), row, CHILDREN_TABLE_INDEX_ID);
			final List<String[]> completeNames = IndividualPanel.extractCompleteName(child, store);
			if(!completeNames.isEmpty()){
				final String[] firstCompleteName = completeNames.get(0);
				childrenTableModel.setValueAt(firstCompleteName[0] + NAMES_SEPARATOR + firstCompleteName[1], row,
					CHILDREN_TABLE_INDEX_NAME);
			}
		}

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


		final JTabbedPane tabbedPane = new JTabbedPane();

		final JPanel panelMembers = new JPanel();
		panelMembers.setLayout(new MigLayout("debug", "[][][]"));
		panelMembers.add(partner1Label, "span 3,wrap");
		panelMembers.add(partner1Image, "grow");
		panelMembers.add(partner1Name, "grow");
		panelMembers.add(partner1Notes, "wrap");
		panelMembers.add(partner2Label, "span 3,wrap");
		panelMembers.add(partner2Image, "grow");
		panelMembers.add(partner2Name, "grow");
		panelMembers.add(partner2Notes, "wrap");
		panelMembers.add(childrenLabel, "span 3,wrap");
		panelMembers.add(childrenTable, "span 3,grow,wrap");

		final JPanel panelEvents = new JPanel();
		panelEvents.setLayout(new MigLayout());
		panelEvents.add(eventsButton, "sizegroup button,grow,wrap");

		final JPanel panelGroups = new JPanel();
		panelGroups.setLayout(new MigLayout());
		panelGroups.add(groupsButton, "sizegroup button,grow,wrap");

		final JPanel panelCulturalRules = new JPanel();
		panelCulturalRules.setLayout(new MigLayout());
		panelCulturalRules.add(culturalRulesButton, "sizegroup button,grow,wrap");

		final JPanel panelNotes = new JPanel();
		panelNotes.setLayout(new MigLayout());
		panelNotes.add(notesButton, "sizegroup button,grow,wrap");

		final JPanel panelSources = new JPanel();
		panelSources.setLayout(new MigLayout());
		panelSources.add(sourcesButton, "sizegroup button,grow,wrap");

		final JPanel panelGeneral = new JPanel();
		panelGeneral.setLayout(new MigLayout());
		panelGeneral.add(restrictionLabel, "align label,split 2");
		panelGeneral.add(restrictionComboBox, "grow");

		tabbedPane.add("Members", panelMembers);
		tabbedPane.add("Events", panelEvents);
		tabbedPane.add("Groups", panelGroups);
		tabbedPane.add("Cultural rules", panelCulturalRules);
		tabbedPane.add("Notes", panelNotes);
		tabbedPane.add("Sources", panelSources);
		tabbedPane.add("General", panelGeneral);

		setLayout(new MigLayout());
		add(tabbedPane, "grow,wrap");
	}

	public final void loadData(final GedcomNode family){
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		final GedcomNode partner1 = store.getPartner1(family);
		if(!partner1.isEmpty()){
			GedcomNode preferredImage = store.traverse(partner1, "PREFERRED_IMAGE");
			final String partner1PreferredImageXRef = preferredImage.getValue();
			//top-left and bottom-right
			final String partner1PreferredImageCropCoordinates = store.traverse(preferredImage, "CROP")
				.getValue();
			try{
				preferredImage = store.getSource(partner1PreferredImageXRef);
				final String partner1PreferredImagePath = store.traverse(preferredImage, "FILE")
					.getValue();
				final String basePath = "C:\\Users\\mauro\\Documents\\My Genealogy Projects\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)\\";
				partner1Image.setImage(ResourceHelper.readImage(basePath + partner1PreferredImagePath));
				partner1Image.setEnabled(true);

				int endX, startY;
				int startX, endY;
				if(StringUtils.isNotBlank(partner1PreferredImageCropCoordinates)){
					final String[] coords = StringUtils.split(partner1PreferredImageCropCoordinates, ' ');
					startX = Integer.parseInt(coords[0]);
					startY = Integer.parseInt(coords[1]);
					endX = Integer.parseInt(coords[2]);
					endY = Integer.parseInt(coords[3]);
				}
				else{
					endX = 0;
					startY = 0;
					startX = partner1Image.getWidth();
					endY = partner1Image.getHeight();
				}
				partner1Image.setCropStartPoint(startX, startY);
				partner1Image.setCropEndPoint(endX, endY);
			}
			catch(final IOException e){
				LOGGER.error("Cannot load preferred image of individual {}", partner1.getID(), e);

				partner1Image.setEnabled(false);
			}
			partner1Name.setEnabled(true);
			partner1Name.setText(getPartnerReference(partner1));
			partner1Notes.setEnabled(true);
		}
		else{
			partner1Image.setEnabled(false);
			partner1Image.setImage(null);
			partner1Name.setText(null);
			partner1Name.setEnabled(false);
			partner1Name.setText(null);
			partner1Notes.setEnabled(false);
		}

//		final GedcomNode partner2 = store.getPartner2(family);
//		partner2Name.setEnabled(!partner2.isEmpty());
//		if(!partner2.isEmpty())
//			partner2Name.setText(getPartnerReference(partner2));
//		partner2Image.setImage(ResourceHelper.readImage(path));

		restrictionComboBox.setSelectedItem(store.traverse(family, "RESTRICTION").getValue());
	}

	private String getPartnerReference(final GedcomNode partner){
		final StringJoiner reference = new StringJoiner(StringUtils.SPACE);
		reference.add(partner.getID() + ":");

		final List<GedcomNode> familyNames = partner.getChildrenWithTag("NAME.FAMILY_NAME");
		reference.add(familyNames.isEmpty()? "--,": familyNames.get(0).getValue() + ",");

		final List<GedcomNode> individualNames = partner.getChildrenWithTag("NAME.INDIVIDUAL_NAME");
		reference.add(individualNames.isEmpty()? "--": individualNames.get(0).getValue());

		final String birthYear = IndividualPanel.extractBirthYear(partner, store);
		final String deathYear = IndividualPanel.extractDeathYear(partner, store);
		reference.add("(" + (StringUtils.isNotBlank(birthYear)? birthYear: "--") + " – "
			+ (StringUtils.isNotBlank(deathYear)? deathYear: "--") + ")");

		return reference.toString();
	}


	private static class ChildrenTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 1218097182058055067L;


		ChildrenTableModel(){
			super(0, 2);
		}

		@Override
		public final Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public final boolean isCellEditable(final int row, final int column){
			return false;
		}


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
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
//		final GedcomNode family = storeFlef.getFamilies().get(0);
		final GedcomNode family = storeFlef.getFamily("F2");

		EventQueue.invokeLater(() -> {
			final FamilyRecordDialog dialog = new FamilyRecordDialog(family, storeFlef, new JFrame());

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(350, 400);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
