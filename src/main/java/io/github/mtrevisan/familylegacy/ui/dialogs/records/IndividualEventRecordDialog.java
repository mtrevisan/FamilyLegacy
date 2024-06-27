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
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.utilities.ScaledImage;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.StringJoiner;


//TODO
public class IndividualEventRecordDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualEventRecordDialog.class);

	@Serial
	private static final long serialVersionUID = 2075397360104239479L;

	private static final String NAMES_SEPARATOR = ", ";
	private static final String NO_DATA = "?";

	private static final int PARTNER_IMAGE_MINIMUM_WIDTH = 30;
	private static final int PARTNER_IMAGE_MINIMUM_HEIGHT = 38;

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 11);

	private static final DefaultComboBoxModel<String> TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY, "unknown", "marriage", "not married", "civil marriage", "religious marriage", "common law marriage", "partnership", "registered partnership", "living together", "living apart together"});
	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY, "confidential", "locked", "private"});

	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);

	private final JLabel individualLabel = new JLabel("Indivisdual:");
	private final ScaledImage individualImage = new ScaledImage(null);
	private final JLabel individualName = new JLabel(StringUtils.EMPTY);
	private final JButton individualNoteButton = new JButton(StringUtils.EMPTY);
	private final JButton eventButton = new JButton("Events");
	private final JButton groupButton = new JButton("Groups");
	private final JButton culturalNormButton = new JButton("Cultural norms");
	private final JButton noteButton = new JButton("Notes");
	private final JButton sourceButton = new JButton("Sources");
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);

	private GedcomNode individual;
	private final Flef store;


	public IndividualEventRecordDialog(final GedcomNode individual, final Flef store, final Frame parent){
		super(parent, true);

		this.individual = individual;
		this.store = store;

		initComponents();

		loadData();
	}


	private void initComponents(){
		individualLabel.setFont(FONT_PRIMARY);
		individualLabel.setLabelFor(individualName);
		individualNoteButton.setIcon(ICON_NOTE);
		individualNoteButton.setToolTipText("Add note to parent 1");
		individualNoteButton.addActionListener(evt -> {
			final Frame parent = (Frame)getParent();
			final NoteDialog noteCitationDialog = NoteDialog.createNote(store, parent);
			//TODO onCloseGracefully
			if(!noteCitationDialog.loadData(individual, null))
				noteCitationDialog.showNewRecord();

			noteCitationDialog.setSize(450, 260);
			noteCitationDialog.setLocationRelativeTo(parent);
			noteCitationDialog.setVisible(true);
		});

		eventButton.addActionListener(e -> {
			//TODO
		});

		groupButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP_CITATION, individual)));

		culturalNormButton.addActionListener(e -> {
			//TODO
		});

		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, individual)));

		sourceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, individual)));

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

		final JPanel panelMembers = new JPanel(new MigLayout("debug", "[fill][][]"));
		panelMembers.add(individualLabel, "span 3,wrap");
		panelMembers.add(individualImage);
		panelMembers.add(individualName, "grow");
		panelMembers.add(individualNoteButton, "top");

		final JPanel panelEvents = new JPanel(new MigLayout());
		panelEvents.add(eventButton, "sizegroup button,grow,wrap");

		final JPanel panelGroups = new JPanel(new MigLayout());
		panelGroups.add(groupButton, "sizegroup button,grow,wrap");

		final JPanel panelCulturalNorms = new JPanel(new MigLayout());
		panelCulturalNorms.add(culturalNormButton, "sizegroup button,grow,wrap");

		final JPanel panelNotes = new JPanel(new MigLayout());
		panelNotes.add(noteButton, "sizegroup button,grow,wrap");

		final JPanel panelSources = new JPanel(new MigLayout());
		panelSources.add(sourceButton, "sizegroup button,grow,wrap");

		final JPanel panelGeneral = new JPanel(new MigLayout());
		panelGeneral.add(restrictionLabel, "align label,split 2");
		panelGeneral.add(restrictionComboBox, "grow");

		tabbedPane.add("Members", panelMembers);
		tabbedPane.add("Events", panelEvents);
		tabbedPane.add("Groups", panelGroups);
		tabbedPane.add("Cultural rules", panelCulturalNorms);
		tabbedPane.add("Notes", panelNotes);
		tabbedPane.add("Sources", panelSources);
		tabbedPane.add("General", panelGeneral);

		setLayout(new MigLayout());
		add(tabbedPane, "grow,wrap");
	}

	public final void loadData(final GedcomNode individual){
		this.individual = individual;

		loadData();

		repaint();
	}

	private void loadData(){
		individualNoteButton.setEnabled(!individual.isEmpty());
		loadPartnerData(individual, individualImage, individualName, individualNoteButton);

		//TODO

		restrictionComboBox.setSelectedItem(store.traverse(individual, "RESTRICTION").getValue());
	}

	private void loadPartnerData(final GedcomNode partner, final ScaledImage partnerImage, final JLabel partnerName, final JButton partnerNotes){
		if(!partner.isEmpty()){
			GedcomNode preferredImage = store.traverse(partner, "PREFERRED_IMAGE");
			final String partnerPreferredImageXRef = preferredImage.getValue();
			//top-left and bottom-right
			final String partnerPreferredImageCropCoordinates = store.traverse(preferredImage, "CROP")
				.getValue();
			try{
				preferredImage = store.getSource(partnerPreferredImageXRef);
				final String partnerPreferredImagePath = store.traverse(preferredImage, "FILE")
					.getValue();
				partnerImage.setImage(ResourceHelper.readImage(new File(store.getBasePath(), partnerPreferredImagePath)));
				partnerImage.setMinimumSize(new Dimension(PARTNER_IMAGE_MINIMUM_WIDTH, PARTNER_IMAGE_MINIMUM_HEIGHT));
				partnerImage.setEnabled(true);

				if(StringUtils.isNotBlank(partnerPreferredImageCropCoordinates)){
					final String[] coords = StringUtils.split(partnerPreferredImageCropCoordinates, ' ');
					final int startX = Integer.parseInt(coords[0]);
					final int startY = Integer.parseInt(coords[1]);
					final int endX = Integer.parseInt(coords[2]);
					final int endY = Integer.parseInt(coords[3]);
//					partnerImage.setWindow(startX, startY, endX, endY);
					partnerImage.setWindow(190, 120, 500, 500);
				}
			}
			catch(final IOException e){
				LOGGER.error("Cannot load preferred image of individual {}", partner.getID(), e);

				partnerImage.setEnabled(false);
			}
			partnerName.setEnabled(true);
			partnerName.setText(getIndividualText(partner));
			partnerNotes.setEnabled(true);
		}
		else{
			partnerImage.setEnabled(false);
			partnerImage.setImage(null);
			partnerName.setText(null);
			partnerName.setEnabled(false);
			partnerNotes.setEnabled(false);
		}
	}

	private String getIndividualText(final GedcomNode partner){
		final StringJoiner text = new StringJoiner(StringUtils.SPACE);
		text.add(partner.getID() + ":");
		text.add(IndividualPanel.extractFirstCompleteName(partner, NAMES_SEPARATOR, store));
		final String birthYear = IndividualPanel.extractBirthYear(partner, store);
		final String deathYear = IndividualPanel.extractDeathYear(partner, store);
		text.add("(" + (StringUtils.isNotBlank(birthYear)? birthYear: NO_DATA) + "–"
			+ (StringUtils.isNotBlank(deathYear)? deathYear: NO_DATA) + ")");
		return text.toString();
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
//		final GedcomNode individual = storeFlef.getIndividuals().get(0);
		final GedcomNode individual = storeFlef.getIndividual("I1");

		EventQueue.invokeLater(() -> {
			final IndividualEventRecordDialog dialog = new IndividualEventRecordDialog(individual, storeFlef, new JFrame());
			dialog.setTitle("Individual record");

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(400, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
