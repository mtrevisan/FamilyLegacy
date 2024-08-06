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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LabelAutoToolTip;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class PersonPanel extends JPanel implements PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PersonPanel.class);


	private static final String NO_DATA = "?";
	private static final String[] NO_NAME = {NO_DATA, NO_DATA};

	static final int SECONDARY_MAX_HEIGHT = 65;

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

	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_HISTORIC_DATE = "historic_date";
	private static final String TABLE_NAME_CALENDAR = "calendar";
	private static final String TABLE_NAME_PLACE = "place";
	private static final String TABLE_NAME_EVENT = "event";
	private static final String TABLE_NAME_GROUP = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_MEDIA = "media";

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
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private Map<String, Object> person = new HashMap<>(0);


	static PersonPanel create(final BoxPanelType boxType, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new PersonPanel(boxType, store);
	}


	private PersonPanel(final BoxPanelType boxType, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.boxType = boxType;
		this.person.clear();
		this.store = store;
	}


	void initComponents(){
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

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio, final double shrinkFactor){
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
						final Integer photoID = extractRecordPhotoID(person);
						if(photoID != null){
							final int response = JOptionPane.showConfirmDialog(PersonPanel.this,
								"Remove preferred photo?", "Warning", JOptionPane.YES_NO_OPTION);
							if(response == JOptionPane.YES_OPTION){
								//remove preferred image
								person.remove("photo_id");
								person.remove("photo_crop");

								getRecords(TABLE_NAME_MEDIA)
									.remove(photoID);

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


	public void loadData(final Map<String, Object> person){
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
		final Integer photoID = extractRecordPhotoID(person);
		if(photoID != null){
			//recover image URI
			final TreeMap<Integer, Map<String, Object>> medias = getRecords(TABLE_NAME_MEDIA);
			final Map<String, Object> media = medias.get(photoID);
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
		final boolean hasPersons = !getRecords(TABLE_NAME_PERSON).isEmpty();
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

	private String extractIdentifier(final Integer selectedRecordID){
		final StringJoiner identifier = new StringJoiner(" / ");
		getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(selectedRecordID, extractRecordPersonID(record)))
			.forEach(record -> identifier.add(extractName(record)));
		return identifier.toString();
	}

	private static String extractName(final Map<String, Object> record){
		final String personalName = extractRecordPersonalName(record);
		final String familyName = extractRecordFamilyName(record);
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
			final List<Integer> parentsIDs = getParentsIDs(childID, "child");
			if(parentsIDs.size() > 1)
				LOGGER.warn("Person {} belongs to more than one parents (this cannot be), select the first and hope for the best", childID);

			final Integer parentsID = (!parentsIDs.isEmpty()? parentsIDs.getFirst(): null);
			if(parentsID != null){
				final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
				hasParentGroup = groups.containsKey(parentsID);
			}
			else{
				//prefer first adopting family
				final List<Integer> unionIDs = getParentsIDs(childID, "adoptee");
				if(!unionIDs.isEmpty()){
					final TreeMap<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
					hasParentGroup = groups.containsKey(unionIDs.getFirst());
				}
			}
		}
		return hasParentGroup;
	}

	private List<Integer> getParentsIDs(final Integer personID, final String personRole){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(personID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(personRole, extractRecordRole(entry)))
			.map(PersonPanel::extractRecordGroupID)
			.toList();
	}

	private boolean hasSiblingGroup(final Map<String, Object> partner){
		final Integer partnerID = extractRecordID(partner);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(partnerID, extractRecordReferenceID(entry)))
			.anyMatch(entry -> Objects.equals("partner", extractRecordRole(entry)));
	}

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final TreeMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
		final Integer filterReferenceID){
		return getRecords(tableName)
			.entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceTable, extractRecordReferenceTable(entry.getValue())))
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	private static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static String extractRecordPersonalName(final Map<String, Object> record){
		return (String)record.get("personal_name");
	}

	private static String extractRecordFamilyName(final Map<String, Object> record){
		return (String)record.get("family_name");
	}

	private void extractBirthDeathPlaceAge(final StringJoiner sj, final StringJoiner toolTipSJ){
		if(!person.isEmpty()){
			final int personID = extractRecordID(person);
			final Map<String, Object> birthRecord = extractEarliestBirth(personID);
			final Map<String, Object> deathRecord = extractLatestDeath(personID);

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

	private Map<String, Object> extractEarliestBirth(final int personID){
		String earliestBirthValue = null;
		LocalDate earliestBirthDate = null;
		String earliestBirthPlaceName = null;
		final TreeMap<Integer, Map<String, Object>> historicDates = getRecords(TABLE_NAME_HISTORIC_DATE);
		final TreeMap<Integer, Map<String, Object>> calendars = getRecords(TABLE_NAME_CALENDAR);
		final TreeMap<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);
		final List<Map<String, Object>> birthEvents = extractReferences(TABLE_NAME_EVENT, personID, "birth");
		for(int i = 0, length = birthEvents.size(); i < length; i ++){
			final Map<String, Object> event = birthEvents.get(i);

			final Integer dateID = extractRecordDateID(event);
			final Integer placeID = extractRecordPlaceID(event);
			final Map<String, Object> dateRecord = (dateID != null? historicDates.get(dateID): null);
			final Map<String, Object> placeRecord = (placeID != null? places.get(placeID): null);
			final String dateValue = extractRecordDate(dateRecord);
			final Integer calendarID = extractRecordCalendarID(dateRecord);
			final String calendarType = (calendarID != null? extractRecordType(calendars.get(calendarID)): null);
			final LocalDate date = DateParser.parse(dateValue, calendarType);

			if(date != null && (earliestBirthDate == null || date.isBefore(earliestBirthDate))){
				earliestBirthValue = dateValue;
				earliestBirthDate = date;
				earliestBirthPlaceName = (placeRecord != null? (String)placeRecord.get("name"): null);
			}
		}
		final Map<String, Object> result = new HashMap<>(3);
		result.put("dateValue", earliestBirthValue);
		result.put("date", earliestBirthDate);
		result.put("placeName", earliestBirthPlaceName);
		return result;
	}

	private Map<String, Object> extractLatestDeath(final int personID){
		LocalDate latestDeathDate = null;
		String latestDeathValue = null;
		String latestDeathPlaceName = null;
		final TreeMap<Integer, Map<String, Object>> historicDates = getRecords(TABLE_NAME_HISTORIC_DATE);
		final TreeMap<Integer, Map<String, Object>> calendars = getRecords(TABLE_NAME_CALENDAR);
		final TreeMap<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);
		final List<Map<String, Object>> deathEvents = extractReferences(TABLE_NAME_EVENT, personID, "death");
		for(int i = 0, length = deathEvents.size(); i < length; i ++){
			final Map<String, Object> event = deathEvents.get(i);

			final Integer dateID = extractRecordDateID(event);
			final Integer placeID = extractRecordPlaceID(event);
			final Map<String, Object> dateRecord = (dateID != null? historicDates.get(dateID): null);
			final Map<String, Object> placeRecord = (placeID != null? places.get(placeID): null);
			final String dateValue = extractRecordDate(dateRecord);
			final Integer calendarID = extractRecordCalendarID(dateRecord);
			final String calendarType = (calendarID != null? extractRecordType(calendars.get(calendarID)): null);
			final LocalDate date = DateParser.parse(dateValue, calendarType);

			if(date != null && (latestDeathDate == null || date.isAfter(latestDeathDate))){
				latestDeathValue = dateValue;
				latestDeathDate = date;
				latestDeathPlaceName = (placeRecord != null? (String)placeRecord.get("name"): null);
			}
		}
		final Map<String, Object> result = new HashMap<>(3);
		result.put("dateValue", latestDeathValue);
		result.put("date", latestDeathDate);
		result.put("placeName", latestDeathPlaceName);
		return result;
	}

	private List<Map<String, Object>> extractReferences(final String fromTable, final int personID, final String eventType){
		return getRecords(fromTable)
			.values().stream()
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(personID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(eventType, extractRecordType(entry)))
			.toList();
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordDate(final Map<String, Object> record){
		return (record != null? (String)record.get("date"): null);
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordCalendarID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("calendar_id"): null);
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (record != null? (String)record.get("identifier"): null);
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


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		person1.put("photo_id", 3);
		person1.put("photo_crop", "0 0 5 10");
		persons.put((Integer)person1.get("id"), person1);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "tòni");
		personName1.put("family_name", "bruxatin");
		personName1.put("name_locale", "vec-IT");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "antonio");
		personName2.put("family_name", "bruciatino");
		personName2.put("name_locale", "it-IT");
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type", "birth");
		event1.put("date_id", 1);
		event1.put("place_id", 1);
		event1.put("reference_table", "person");
		event1.put("reference_id", 1);
		events.put((Integer)event1.get("id"), event1);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("id", 2);
		event2.put("type", "death");
		event2.put("person_id", 1);
		event2.put("date_id", 2);
		event2.put("place_id", 2);
		event2.put("reference_table", "person");
		event2.put("reference_id", 1);
		events.put((Integer)event2.get("id"), event2);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "1 JAN 2000");
		dates.put((Integer)date1.get("id"), date1);
		final Map<String, Object> date2 = new HashMap<>();
		date2.put("id", 2);
		date2.put("date", "31 JAN 2010");
		dates.put((Integer)date2.get("id"), date2);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "qua");
		places.put((Integer)place1.get("id"), place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "place 2");
		place2.put("name", "là");
		places.put((Integer)place2.get("id"), place2);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put("localized_text_junction", localizedTextJunctions);
		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("id", 1);
		localizedTextJunction1.put("localized_text_id", 1);
		localizedTextJunction1.put("reference_type", "name");
		localizedTextJunction1.put("reference_table", "person_name");
		localizedTextJunction1.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction1.get("id"), localizedTextJunction1);
		final Map<String, Object> localizedTextJunction2 = new HashMap<>();
		localizedTextJunction2.put("id", 2);
		localizedTextJunction2.put("localized_text_id", 2);
		localizedTextJunction2.put("reference_type", "name");
		localizedTextJunction2.put("reference_table", "person_name");
		localizedTextJunction2.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction2.get("id"), localizedTextJunction2);

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
			final PersonPanel panel = create(boxType, store);
			panel.initComponents();
			panel.loadData(person1);
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
					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
