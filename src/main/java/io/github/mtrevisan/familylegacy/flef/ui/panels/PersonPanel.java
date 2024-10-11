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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.helpers.parsers.AbstractCalendarParser;
import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.LabelAutoToolTip;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.time.LocalDate;
import java.time.Period;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPhotoCrop;


public class PersonPanel extends JPanel implements PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PersonPanel.class);

	private static final String NO_DATA = "?";
	private static final String[] NO_NAME = {NO_DATA, NO_DATA};

	private static final int SECONDARY_MAX_HEIGHT = 65;

	private static final Color BACKGROUND_COLOR_NO_PERSON = Color.WHITE;
	private static final Color BACKGROUND_COLOR_FADE_TO = Color.WHITE;
	private static final Color BACKGROUND_COLOR_PERSON = new Color(221, 221, 221);

	private static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);
	private static final Color BORDER_COLOR_SHADOW_SELECTED = Color.BLACK;

	private static final Color BIRTH_DEATH_AGE_COLOR = new Color(110, 110, 110);
	private static final Color IMAGE_LABEL_BORDER_COLOR = new Color(255, 255, 255);

	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final ImageIcon ADD_PHOTO = ResourceHelper.getOriginalImage("/images/add_photo.jpg");

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 14);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 11);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	private static final String PROPERTY_NAME_TEXT_CHANGE = "text";

	private static final String EVENT_TYPE_CATEGORY_BIRTH = "birth";
	private static final String EVENT_TYPE_CATEGORY_DEATH = "death";

	private final LabelAutoToolTip personalNameLabel = new LabelAutoToolTip();
	private final LabelAutoToolTip familyNameLabel = new LabelAutoToolTip();
	private final LabelAutoToolTip infoLabel = new LabelAutoToolTip();
	private final JLabel imageLabel = new JLabel();
	private final JMenuItem editPersonItem = new JMenuItem("Edit Person…", 'E');
	private final JMenuItem addPersonItem = new JMenuItem("Add Person…", 'A');
	private final JMenuItem linkPersonItem = new JMenuItem("Link Person…", 'L');
	private final JMenuItem removePersonItem = new JMenuItem("Remove Person", 'R');
	private final JMenuItem unlinkFromParentGroupItem = new JMenuItem("Unlink from parent Group", 'U');
	private final JMenuItem addToNewSiblingGroupItem = new JMenuItem("Add to new sibling Group…", 'S');
	private final JMenuItem unlinkFromSiblingGroupItem = new JMenuItem("Unlink from sibling Group", 'G');

	private final BoxPanelType boxType;

	private Map<String, Object> person = new HashMap<>(0);


	static PersonPanel create(final BoxPanelType boxType){
		return new PersonPanel(boxType);
	}


	private PersonPanel(final BoxPanelType boxType){
		this.boxType = boxType;
		person.clear();


		initComponents();
	}


	private void initComponents(){
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		imageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		final double shrinkFactor = (isPrimaryBox()? 1.: 2.);
		setPreferredSize(imageLabel, 48., IMAGE_ASPECT_RATIO, shrinkFactor);

		setLayout(new MigLayout("insets 7", "[grow]0[]", "[]0[]10[]"));
		final int shrink = (int)Math.round(PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3);
		add(personalNameLabel, "cell 0 0,top,width ::100%-" + shrink + ",hidemode 3");
		add(imageLabel, "cell 1 0 1 3,top");
		add(familyNameLabel, "cell 0 1,top,width ::100%-" + shrink + ",hidemode 3");
		add(infoLabel, "cell 0 2");

		setOpaque(false);
	}

	private static void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio,
			final double shrinkFactor){
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	public final Map<String, Object> getPerson(){
		return person;
	}

	final void setPersonListener(final PersonListenerInterface listener){
		if(listener != null){
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(!person.isEmpty() && evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						listener.onPersonEdit(PersonPanel.this);
				}
			});


			if(!isPrimaryBox()){
				personalNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onPersonFocus(PersonPanel.this);
					}
				});
				familyNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onPersonFocus(PersonPanel.this);
					}
				});
			}

			attachPopUpMenu(listener);


			imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			imageLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt)){
						final ImageIcon icon = extractPreferredImage();
						if(icon == null)
							listener.onPersonAddPreferredImage(PersonPanel.this);
						else
							listener.onPersonEditPreferredImage(PersonPanel.this);
					}
					else if(SwingUtilities.isRightMouseButton(evt)){
						final Integer personID = extractRecordID(person);
						final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_PERSON, personID);
						final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
						if(photoID != null){
							final int response = JOptionPane.showConfirmDialog(PersonPanel.this,
								"Remove preferred photo?", "Warning", JOptionPane.YES_NO_OPTION);
							if(response == JOptionPane.YES_OPTION){
								//remove preferred image
								Repository.deleteRelationship(EntityManager.NODE_PERSON, personID,
									EntityManager.NODE_MEDIA, photoID,
									EntityManager.RELATIONSHIP_DEPICTED_BY);

								insertRecordPhotoCrop(person, null);
								Repository.upsert(person, EntityManager.NODE_PERSON_NAME);

								imageLabel.setIcon(ResourceHelper.getImage(ADD_PHOTO, imageLabel.getPreferredSize()));
							}
						}
					}
				}
			});
		}
	}

	private void attachPopUpMenu(final PersonListenerInterface listener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editPersonItem.addActionListener(e -> listener.onPersonEdit(this));
		popupMenu.add(editPersonItem);

		addPersonItem.addActionListener(e -> listener.onPersonAdd(this));
		popupMenu.add(addPersonItem);

		linkPersonItem.addActionListener(e -> listener.onPersonLink(this));
		popupMenu.add(linkPersonItem);

		removePersonItem.addActionListener(e -> listener.onPersonRemove(this));
		popupMenu.add(removePersonItem);

		popupMenu.addSeparator();

		unlinkFromParentGroupItem.addActionListener(e -> listener.onPersonUnlinkFromParentGroup(this));
		popupMenu.add(unlinkFromParentGroupItem);

		popupMenu.addSeparator();

		addToNewSiblingGroupItem.addActionListener(e -> listener.onPersonAddToSiblingGroup(this));
		popupMenu.add(addToNewSiblingGroupItem);

		unlinkFromSiblingGroupItem.addActionListener(e -> listener.onPersonUnlinkFromSiblingGroup(this));
		popupMenu.add(unlinkFromSiblingGroupItem);

		addMouseListener(new PopupMouseAdapter(popupMenu, this));
	}

	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			final int panelHeight = getHeight();
			final int panelWidth = getWidth();

			final Color startColor = getBackgroundColor();
			if(!person.isEmpty()){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(person.isEmpty()){
				final Stroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					10.f, new float[]{5.f}, 0.f);
				graphics2D.setStroke(dashedStroke);
			}
			graphics2D.drawRoundRect(0, 0,
				panelWidth - 1, panelHeight - 1,
				ARCS.width, ARCS.height);


			//for test purposes
//			final Point enterPoint = getPaintingEnterPoint();
//			graphics2D.setColor(Color.RED);
//			graphics2D.drawLine(enterPoint.x - 10, enterPoint.y - 10, enterPoint.x + 10, enterPoint.y + 10);
//			graphics2D.drawLine(enterPoint.x + 10, enterPoint.y - 10, enterPoint.x - 10, enterPoint.y + 10);
//			graphics2D.setColor(Color.BLACK);


			graphics2D.dispose();
		}
	}

	@Override
	public final void propertyChange(final PropertyChangeEvent evt){
		//show tooltip with full text if it's too long to be displayed
		if(PROPERTY_NAME_TEXT_CHANGE.equals(evt.getPropertyName())){
			personalNameLabel.manageToolTip();
			familyNameLabel.manageToolTip();
		}
	}

	private Color getBackgroundColor(){
		return (person.isEmpty()? BACKGROUND_COLOR_NO_PERSON: BACKGROUND_COLOR_PERSON);
	}


	public void loadData(final Integer personID){
		final Map<String, Object> person = (personID != null
			? Repository.findByID(EntityManager.NODE_PERSON, personID)
			: Collections.emptyMap());

		prepareData(person);

		loadData();
	}

	private void prepareData(final Map<String, Object> person){
		this.person = person;
	}

	private void loadData(){
		final Dimension size = (isPrimaryBox()
			? new Dimension(260, 90)
			: new Dimension(170, SECONDARY_MAX_HEIGHT));
		setPreferredSize(size);

		Font font = (isPrimaryBox()? FONT_PRIMARY: FONT_SECONDARY);
		final Font infoFont = deriveInfoFont(font);
		if(!isPrimaryBox()){
			//add underline to mark this person as eligible for primary position
			@SuppressWarnings("unchecked")
			final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
			font = font.deriveFont(attributes);
		}
		final Cursor cursor = Cursor.getPredefinedCursor(isPrimaryBox()? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR);
		personalNameLabel.setFont(font);
		personalNameLabel.setCursor(cursor);
		familyNameLabel.setFont(font);
		familyNameLabel.setCursor(cursor);
		infoLabel.setFont(infoFont);

		//extract name
		final String identifiers = extractIdentifier(extractRecordID(person));
		final String[] names = StringUtils.splitByWholeSeparator(identifiers, " / ", 2);
		final String[] name = (names.length > 0? StringUtils.splitByWholeSeparator(names[0], ", "): NO_NAME);
		personalNameLabel.addPropertyChangeListener(PROPERTY_NAME_TEXT_CHANGE, this);
		personalNameLabel.setText(name[0]);
		personalNameLabel.setToolTipText(name[0]);
		familyNameLabel.addPropertyChangeListener(PROPERTY_NAME_TEXT_CHANGE, this);
		familyNameLabel.setText(name[1]);
		familyNameLabel.setToolTipText(name[1]);


		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final StringJoiner toolTipSJ = new StringJoiner(StringUtils.EMPTY);
		extractBirthDeathPlaceAge(sj, toolTipSJ);
		infoLabel.setText(sj.toString());
		infoLabel.setToolTipText(toolTipSJ.toString());

		final double shrinkFactor = (isPrimaryBox()? 1.: 2.);
		setPreferredSize(imageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO, shrinkFactor);
		final ImageIcon icon = extractPreferredImage();
		imageLabel.setIcon(icon != null? icon: ResourceHelper.getImage(ADD_PHOTO, imageLabel.getPreferredSize()));

		final boolean hasData = !person.isEmpty();
		personalNameLabel.setVisible(hasData);
		familyNameLabel.setVisible(hasData);
		infoLabel.setVisible(hasData);
		imageLabel.setVisible(hasData);

		refresh(ActionCommand.ACTION_COMMAND_PERSON);
	}

	private ImageIcon extractPreferredImage(){
		ImageIcon icon = null;
		final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_PERSON, extractRecordID(person));
		final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
		if(photoID != null){
			//recover image URI
			final Map<String, Object> media = Repository.findByID(EntityManager.NODE_MEDIA, photoID);
			if(media == null)
				LOGGER.error("Cannot find media ID {}", photoID);
			else{
				final String identifier = FileHelper.getTargetPath(FileHelper.documentsDirectory(), extractRecordIdentifier(media));
				icon = ResourceHelper.getImage(identifier, imageLabel.getPreferredSize());
			}
		}
		return icon;
	}

	private boolean isPrimaryBox(){
		return (boxType == BoxPanelType.PRIMARY);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_PERSON)
			return;

		final boolean hasData = !person.isEmpty();
		final boolean hasPersons = (Repository.count(EntityManager.NODE_PERSON) > 0);
		final boolean hasParentGroup = hasParentGroup(person);
		final boolean hasSiblingGroup = hasSiblingGroup(person);
		editPersonItem.setEnabled(hasData);
		addPersonItem.setEnabled(!hasData);
		linkPersonItem.setEnabled(!hasData && hasPersons);
		removePersonItem.setEnabled(hasData);
		unlinkFromParentGroupItem.setEnabled(hasData && hasParentGroup);
		addToNewSiblingGroupItem.setEnabled(hasData);
		unlinkFromSiblingGroupItem.setEnabled(hasData && hasSiblingGroup);
	}

	private String extractIdentifier(final Integer personID){
		final StringJoiner identifier = new StringJoiner(" / ");
		final List<Map<String, Object>> personNames = Repository.findReferencingNodes(EntityManager.NODE_PERSON_NAME,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_FOR);
		for(int i = 0, length = personNames.size(); i < length; i ++){
			final Map<String, Object> personName = personNames.get(i);

			identifier.add(extractSinglePersonName(personName));
		}
		return identifier.toString();
	}

	private static String extractSinglePersonName(final Map<String, Object> personNameRecord){
		final String personalName = extractRecordPersonalName(personNameRecord);
		final String familyName = extractRecordFamilyName(personNameRecord);
		final StringJoiner name = new StringJoiner(", ");
		name.add(personalName != null? personalName: NO_DATA);
		name.add(familyName != null? familyName: NO_DATA);
		return name.toString();
	}

	private boolean hasParentGroup(final Map<String, Object> child){
		boolean hasParentGroup = false;
		if(!child.isEmpty()){
			final Integer childID = extractRecordID(child);
			//prefer biological family
			final List<Integer> parentsIDs = getParentsIDs(childID, EntityManager.GROUP_ROLE_CHILD);
			if(parentsIDs.size() > 1)
				LOGGER.warn("Person {} belongs to more than one parents (this cannot be), select the first and hope for the best", childID);

			final Integer parentsID = (!parentsIDs.isEmpty()? parentsIDs.getFirst(): null);
			if(parentsID != null)
				hasParentGroup = (Repository.findByID(EntityManager.NODE_GROUP, parentsID) != null);
			else{
				//prefer first adopting family
				final List<Integer> unionIDs = getParentsIDs(childID, EntityManager.GROUP_ROLE_ADOPTEE);
				if(!unionIDs.isEmpty())
					hasParentGroup = (Repository.findByID(EntityManager.NODE_GROUP, unionIDs.getFirst()) != null);
			}
		}
		return hasParentGroup;
	}

	private List<Integer> getParentsIDs(final Integer personID, final String personRole){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				EntityManager.NODE_PERSON, personID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, personRole).stream()
			.map(EntityManager::extractRecordID)
			.toList();
	}

	private boolean hasSiblingGroup(final Map<String, Object> partner){
		final Integer partnerID = extractRecordID(partner);
		return Repository.hasReference(EntityManager.NODE_PERSON, partnerID,
			EntityManager.NODE_GROUP,
			EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER);
	}

	private void extractBirthDeathPlaceAge(final StringJoiner sj, final StringJoiner toolTipSJ){
		if(!person.isEmpty()){
			final int personID = extractRecordID(person);
			final Map<String, Object> birthRecord = extractEarliestBirthDateAndPlace(personID);
			final Map<String, Object> deathRecord = extractLatestDeathDateAndPlace(personID);

			String age = null;
			final String birthDateValue = (String)birthRecord.get("dateValue");
			final String deathDateValue = (String)deathRecord.get("dateValue");
			final LocalDate birthDate = (LocalDate)birthRecord.get("date");
			final LocalDate deathDate = (LocalDate)deathRecord.get("date");
			if(birthDateValue != null && deathDateValue != null){
				final boolean isAgeApproximated = (AbstractCalendarParser.isApproximation(birthDateValue)
					|| AbstractCalendarParser.isRange(birthDateValue)
					|| AbstractCalendarParser.isApproximation(deathDateValue)
					|| AbstractCalendarParser.isRange(deathDateValue));
				final boolean isAgeLessThan = (AbstractCalendarParser.isExact(birthDateValue) && AbstractCalendarParser.isBefore(deathDateValue)
					|| AbstractCalendarParser.isAfter(birthDateValue) && AbstractCalendarParser.isExact(deathDateValue)
					|| AbstractCalendarParser.isRange(birthDateValue)
					|| AbstractCalendarParser.isRange(deathDateValue));
				age = StringUtils.EMPTY;
				if(isAgeLessThan)
					age = "<";
				else if(isAgeApproximated)
					age = "~";
				age += Period.between(birthDate, deathDate)
					.getYears();
			}

			sj.add(birthDate != null? Integer.toString(birthDate.getYear()): NO_DATA);
			sj.add("–");
			sj.add(deathDate != null? Integer.toString(deathDate.getYear()): NO_DATA);
			if(age != null)
				sj.add("(" + age + (isPrimaryBox()? " y/o": StringUtils.EMPTY) + ")");

			final String birthPlace = (String)birthRecord.get("placeName");
			final String deathPlace = (String)deathRecord.get("placeName");
			if(birthPlace != null || deathPlace != null){
				toolTipSJ.add("<html>");
				toolTipSJ.add(birthDate != null? DateParser.formatDate(birthDate): NO_DATA);
				if(birthPlace != null)
					toolTipSJ.add("<br>" + birthPlace);
				toolTipSJ.add("<br>-<br>");
				toolTipSJ.add(deathDate != null? DateParser.formatDate(deathDate): NO_DATA);
				if(deathPlace != null)
					toolTipSJ.add("<br>" + deathPlace);
				toolTipSJ.add("</html>");
			}
			else{
				toolTipSJ.add(birthDate != null? DateParser.formatDate(birthDate): NO_DATA);
				toolTipSJ.add("–");
				toolTipSJ.add(deathDate != null? DateParser.formatDate(deathDate): NO_DATA);
			}
		}
	}

	private Map<String, Object> extractEarliestBirthDateAndPlace(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final Map<String, Object> eventRecord = entry.getValue();
			final Integer eventID = extractRecordID(eventRecord);

			final List<Map.Entry<String, Map<String, Object>>> nodes = Repository.findReferencedNodes(
				EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_ON);
			String dateValue = null;
			if(!nodes.isEmpty()){
				final Map<String, Object> dateEntry = nodes.getFirst().getValue();
				dateValue = extractRecordDate(dateEntry);
			}

			final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_IN);
			final Map<String, Object> place = (placeNode != null? placeNode.getValue(): null);
			final String placeName = extractRecordName(place);

			final Map<String, Object> result = new HashMap<>(2);
			result.put("dateValue", dateValue);
			result.put("date", entry.getKey());
			result.put("placeName", placeName);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_BIRTH, comparator, extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private Map<String, Object> extractLatestDeathDateAndPlace(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final Map<String, Object> eventRecord = entry.getValue();
			final Integer eventID = extractRecordID(eventRecord);

			final List<Map.Entry<String, Map<String, Object>>> nodes = Repository.findReferencedNodes(
				EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_ON);
			String dateValue = null;
			if(!nodes.isEmpty()){
				final Map<String, Object> dateEntry = nodes.getFirst().getValue();
				dateValue = extractRecordDate(dateEntry);
			}

			final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_IN);
			final Map<String, Object> place = (placeNode != null? placeNode.getValue(): null);
			final String placeName = extractRecordName(place);

			final Map<String, Object> result = new HashMap<>(2);
			result.put("dateValue", dateValue);
			result.put("date", entry.getKey());
			result.put("placeName", placeName);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_DEATH, comparator.reversed(), extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private <T> T extractData(final Integer referenceID, final String eventTypeCategory, final Comparator<LocalDate> comparator,
			final Function<Map.Entry<LocalDate, Map<String, Object>>, T> extractor){
		final Set<String> eventTypes = getEventTypes(eventTypeCategory);
		return Repository.findReferencingNodes(EntityManager.NODE_EVENT,
				EntityManager.NODE_PERSON, referenceID,
				EntityManager.RELATIONSHIP_FOR).stream()
			.filter(entry -> {
				final Integer eventID = extractRecordID(entry);
				Map.Entry<String, Map<String, Object>> eventTypeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
					EntityManager.RELATIONSHIP_OF_TYPE);
				final Map<String, Object> eventType = (eventTypeNode != null? eventTypeNode.getValue(): null);
				return eventTypes.contains(extractRecordType(eventType));
			})
			.map(entry -> {
				final Integer eventID = extractRecordID(entry);
				final Map.Entry<String, Map<String, Object>> dateNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
					EntityManager.RELATIONSHIP_HAPPENED_ON);
				final Map<String, Object> date = (dateNode != null? dateNode.getValue(): null);
				final String dateValue = extractRecordDate(date);
				final LocalDate parsedDate = DateParser.parse(dateValue);
				return (parsedDate != null? new AbstractMap.SimpleEntry<>(parsedDate, entry): null);
			})
			.filter(Objects::nonNull)
			.min(Map.Entry.comparingByKey(comparator))
			.map(extractor)
			.orElse(null);
	}

	private Set<String> getEventTypes(final String category){
		return Repository.findAll(EntityManager.NODE_EVENT_TYPE)
			.stream()
			.filter(entry -> Objects.equals(category, extractRecordCategory(entry)))
			.map(EntityManager::extractRecordType)
			.collect(Collectors.toSet());
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	final Point getPaintingEnterPoint(){
		return new Point(getX() + getWidth() / 2, getY());
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> person1 = new HashMap<>();
		person1.put("photo_crop", "0 0 5 10");
		int person1ID = Repository.upsert(person1, EntityManager.NODE_PERSON);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		int media1ID = Repository.upsert(media1, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_MEDIA, media1ID,
			EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("personal_name", "tòni");
		personName1.put("family_name", "bruxatin");
		personName1.put("locale", "vec-IT");
		personName1.put("type", "birth name");
		int personName1ID = Repository.upsert(personName1, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("personal_name", "antonio");
		personName2.put("family_name", "bruciatino");
		personName2.put("locale", "it-IT");
		personName2.put("type", "death name");
		int personName2ID = Repository.upsert(personName2, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "1 JAN 2000");
		int date1ID = Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);
		final Map<String, Object> date2 = new HashMap<>();
		date2.put("date", "31 JAN 2010");
		int date2ID = Repository.upsert(date2, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "qua");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("identifier", "place 2");
		place2.put("name", "là");
		int place2ID = Repository.upsert(place2, EntityManager.NODE_PLACE);

		final Map<String, Object> event1 = new HashMap<>();
		int event1ID = Repository.upsert(event1, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> event2 = new HashMap<>();
		int event2ID = Repository.upsert(event2, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_PLACE, place2ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_HISTORIC_DATE, date2ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "birth");
		eventType1.put("category", EVENT_TYPE_CATEGORY_BIRTH);
		int eventType1ID = Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("type", "death");
		eventType2.put("category", EVENT_TYPE_CATEGORY_DEATH);
		int eventType2ID = Repository.upsert(eventType2, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_EVENT_TYPE, eventType2ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		int localizedText1ID = Repository.upsert(localizedText1, EntityManager.NODE_LOCALIZED_TEXT);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		int localizedText2ID = Repository.upsert(localizedText2, EntityManager.NODE_LOCALIZED_TEXT);

		final Map<String, Object> localizedTextRelationship1 = new HashMap<>();
		localizedTextRelationship1.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, localizedText1ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextRelationship2 = new HashMap<>();
		localizedTextRelationship2.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, localizedText2ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonFocus(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onFocusPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonEdit(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onEditPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonAdd(final PersonPanel personPanel){
				System.out.println("onAddPerson");
			}

			@Override
			public void onPersonLink(final PersonPanel personPanel){
				System.out.println("onLinkPerson");
			}

			@Override
			public void onPersonRemove(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onRemovePerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onUnlinkPersonFromParentGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddToSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onAddToSiblingGroupPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onUnlinkPersonFromSiblingGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.person;
				System.out.println("onAddPreferredImage " + extractRecordID(person));
			}

			@Override
			public void onPersonEditPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPreferredImage " + extractRecordID(person));
			}
		};


		EventQueue.invokeLater(() -> {
			final PersonPanel panel = create(boxType);
			panel.loadData(1);
			panel.setPersonListener(personListener);

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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
