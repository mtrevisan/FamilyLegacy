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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LabelAutoToolTip;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;


public class PersonPanel extends JPanel implements PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;


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

	private final LabelAutoToolTip personalNameLabel = new LabelAutoToolTip();
	private final LabelAutoToolTip familyNameLabel = new LabelAutoToolTip();
	private final LabelAutoToolTip infoLabel = new LabelAutoToolTip();
	private final JLabel imageLabel = new JLabel();
	private final JMenuItem editPersonItem = new JMenuItem("Edit Person…", 'E');
	private final JMenuItem linkPersonItem = new JMenuItem("Link Person…", 'L');
	private final JMenuItem unlinkPersonItem = new JMenuItem("Unlink Person", 'U');
	private final JMenuItem addPersonItem = new JMenuItem("Add Person…", 'A');
	private final JMenuItem removePersonItem = new JMenuItem("Remove Person", 'R');

	private final SelectedNodeType type;
	private Map<String, Object> person;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private final BoxPanelType boxType;

	private Map<String, Object> childReference;


	public static PersonPanel create(final SelectedNodeType type, final Map<String, Object> person,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		return new PersonPanel(type, person, store, boxType);
	}


	private PersonPanel(final SelectedNodeType type, final Map<String, Object> person,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		this.type = type;
		this.person = person;
		this.store = store;
		this.boxType = boxType;
	}


	private void initComponents(){
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		imageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		setPreferredSize(imageLabel, 48., IMAGE_ASPECT_RATIO, shrinkFactor);

		setLayout(new MigLayout("insets 7", "[grow]0[]", "[]0[]10[]"));
		final double shrink = PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3;
		add(personalNameLabel, "cell 0 0,top,width ::100%-" + shrink + ",hidemode 3");
		add(imageLabel, "cell 1 0 1 3,aligny top");
		add(familyNameLabel, "cell 0 1,top,width ::100%-" + shrink + ",hidemode 3");
		add(infoLabel, "cell 0 2");
	}

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio, final double shrinkFactor){
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	final void setPersonListener(final PersonListenerInterface listener){
		if(listener != null){
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(!person.isEmpty() && evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						listener.onPersonEdit(PersonPanel.this, person);
				}
			});


			if(boxType == BoxPanelType.SECONDARY){
				personalNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onPersonFocus(PersonPanel.this, type, person);
					}
				});
				familyNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onPersonFocus(PersonPanel.this, type, person);
					}
				});
			}

			attachPopUpMenu(listener);

			//TODO
//			refresh(Flef.ACTION_COMMAND_INDIVIDUAL_COUNT);


			imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			imageLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt))
						listener.onPersonAddImage(PersonPanel.this, person);
				}
			});
		}
	}

	private void attachPopUpMenu(final PersonListenerInterface listener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editPersonItem.addActionListener(e -> listener.onPersonEdit(this, person));
		popupMenu.add(editPersonItem);

		linkPersonItem.addActionListener(e -> listener.onPersonLink(this, type));
		popupMenu.add(linkPersonItem);

		unlinkPersonItem.addActionListener(e -> listener.onPersonUnlink(this, person));
		popupMenu.add(unlinkPersonItem);

		addPersonItem.addActionListener(e -> listener.onPersonAdd(this));
		popupMenu.add(addPersonItem);

		removePersonItem.addActionListener(e -> listener.onPersonRemove(this, person));
		popupMenu.add(removePersonItem);

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

			graphics2D.dispose();
		}
	}

	@Override
	public final void propertyChange(final PropertyChangeEvent evt){
		//show tooltip with full text if it's too long to be displayed
		if(PROPERTY_NAME_TEXT_CHANGE.equals(evt.getPropertyName())){
			personalNameLabel.manageToolTip();
			familyNameLabel.manageToolTip();
			infoLabel.manageToolTip();
		}
	}

	private Color getBackgroundColor(){
		return (person.isEmpty()? BACKGROUND_COLOR_NO_PERSON: BACKGROUND_COLOR_PERSON);
	}

	public final void loadData(final Map<String, Object> person){
		this.person = person;

		loadData();

		//TODO
//		repaint();
	}

	private void loadData(){
		final Dimension size = (boxType == BoxPanelType.PRIMARY? new Dimension(260, 90):
			new Dimension(170, SECONDARY_MAX_HEIGHT));
		setPreferredSize(size);
		setMaximumSize(boxType == BoxPanelType.PRIMARY? new Dimension(420, size.height):
			new Dimension(240, size.height));

		Font font = (boxType == BoxPanelType.PRIMARY? FONT_PRIMARY: FONT_SECONDARY);
		final Font infoFont = deriveInfoFont(font);
		if(boxType == BoxPanelType.SECONDARY){
			//add underline to mark this person as eligible for primary position
			@SuppressWarnings("unchecked")
			final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
			font = font.deriveFont(attributes);
		}
		final Cursor cursor = Cursor.getPredefinedCursor(boxType == BoxPanelType.PRIMARY? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR);
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
		final StringJoiner toolTipSJ = new StringJoiner(StringUtils.SPACE);
		extractBirthDeathAge(sj, toolTipSJ);
		infoLabel.setText(sj.toString());
		infoLabel.setToolTipText(toolTipSJ.toString());

		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		setPreferredSize(imageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO, shrinkFactor);
		final ImageIcon icon = ResourceHelper.getImage(ADD_PHOTO, imageLabel.getPreferredSize());
		imageLabel.setIcon(icon);

		personalNameLabel.setVisible(!person.isEmpty());
		familyNameLabel.setVisible(!person.isEmpty());
		infoLabel.setVisible(!person.isEmpty());
		imageLabel.setVisible(!person.isEmpty());

		//TODO
//		refresh(Flef.ACTION_COMMAND_INDIVIDUAL_COUNT);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		//TODO
//		if(actionCommand != Flef.ACTION_COMMAND_INDIVIDUAL_COUNT)
//			return;
//
//		linkPersonItem.setEnabled(person.isEmpty() && store.hasIndividuals());
//		editPersonItem.setEnabled(!person.isEmpty());
//		final boolean isChildOfFamily = !store.traverseAsList(person, "FAMILY_CHILD[]").isEmpty();
//		unlinkPersonItem.setEnabled(!person.isEmpty() && isChildOfFamily);
//		addPersonItem.setEnabled(person.isEmpty() && store.hasIndividuals());
//		removePersonItem.setEnabled(!person.isEmpty());
	}

	private String extractIdentifier(final int selectedRecordID){
		final StringJoiner identifier = new StringJoiner(" / ");
		getRecords("person_name").values().stream()
			.filter(record -> extractRecordPersonID(record) == selectedRecordID)
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

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (int)record.get("id"): null);
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

	private void extractBirthDeathAge(final StringJoiner sj, final StringJoiner toolTipSJ){
		if(!person.isEmpty()){
			final int personID = extractRecordID(person);
			final List<Map<String, Object>> earliestBirth = extractEarliestBirth(personID);
			final List<Map<String, Object>> latestDeath = extractLatestDeath(personID);
//			final String birthDate = extractEarliestBirthDate(earliestBirth, store);
//			final String birthPlace = extractEarliestBirthPlace(earliestBirth, store);
//			final String deathDate = extractLatestDeathDate(latestDeath, store);
//			final String deathPlace = extractLatestDeathPlace(latestDeath, store);
			String age = null;
//			if(birthDate != null && deathDate != null){
//				//TODO
//				final boolean isAgeApproximated = (AbstractCalendarParser.isApproximation(birthDate)
//					|| AbstractCalendarParser.isRange(birthDate)
//					|| AbstractCalendarParser.isApproximation(deathDate)
//					|| AbstractCalendarParser.isRange(deathDate));
//				final boolean isAgeLessThan = (AbstractCalendarParser.isExact(birthDate) && AbstractCalendarParser.isBefore(deathDate)
//					|| AbstractCalendarParser.isAfter(birthDate) && AbstractCalendarParser.isExact(deathDate)
//					|| AbstractCalendarParser.isRange(birthDate)
//					|| AbstractCalendarParser.isRange(deathDate));
//				age = StringUtils.EMPTY;
//				if(isAgeLessThan)
//					age = "<";
//				else if(isAgeApproximated)
//					age = "~";
//				final LocalDate birth = DateParser.parse(birthDate);
//				final LocalDate death = DateParser.parse(deathDate);
//				age += Period.between(birth, death).getYears();
//			}
//
//			sj.add(birthDate != null? DateParser.extractYear(birthDate): NO_DATA);
//			sj.add("–");
//			sj.add(deathDate != null? DateParser.extractYear(deathDate): NO_DATA);
//			if(age != null)
//				sj.add("(" + age + ")");
//
//			if(birthPlace != null || deathPlace != null){
//				toolTipSJ.add("<html>");
//				toolTipSJ.add(birthDate != null? DateParser.formatDate(birthDate): NO_DATA);
//				if(birthPlace != null)
//					toolTipSJ.add("<br>" + birthPlace);
//				toolTipSJ.add("<br>-<br>");
//				toolTipSJ.add(deathDate != null? DateParser.formatDate(deathDate): NO_DATA);
//				if(deathPlace != null)
//					toolTipSJ.add("<br>" + deathPlace);
//				toolTipSJ.add("</html>");
//			}
//			else{
//				toolTipSJ.add(birthDate != null? DateParser.formatDate(birthDate): NO_DATA);
//				toolTipSJ.add("–");
//				toolTipSJ.add(deathDate != null? DateParser.formatDate(deathDate): NO_DATA);
//			}
		}
	}

	private List<Map<String, Object>> extractEarliestBirth(final int personID){
		final TreeMap<Integer, Map<String, Object>> historicDates = getRecords("historic_date");
		final TreeMap<Integer, Map<String, Object>> calendars = getRecords("calendar");
		final List<Map<String, Object>> birthEvents = extractReferences("event", personID, "birth");
		for(int i = 0, length = birthEvents.size(); i < length; i ++){
			final Map<String, Object> birthEvent = birthEvents.get(i);

			final int dateID = extractRecordDateID(birthEvent);
			final Map<String, Object> dateRecord = historicDates.get(dateID);
			final String date = extractRecordDate(dateRecord);
			final Integer calendarID = extractRecordCalendarID(dateRecord);
			final String calendarType = extractRecordType(calendars.get(calendarID));

			//TODO
		}
		//TODO
//		int birthYear = Integer.MAX_VALUE;
//		Map<String, Object> birth = store.createEmptyNode();
//		final List<Map<String, Object>> birthEvents = extractTaggedEvents(person, "BIRTH", store);
//		for(final Map<String, Object> node : birthEvents){
//			final String dateValue = store.traverse(node, "DATE").getValue();
//			final LocalDate date = DateParser.parse(dateValue);
//			if(date != null){
//				final int y = date.getYear();
//				if(y < birthYear){
//					birthYear = y;
//					birth = node;
//				}
//			}
//		}
//		return birth;
		return birthEvents;
	}

	private List<Map<String, Object>> extractLatestDeath(final int personID){
		final TreeMap<Integer, Map<String, Object>> historicDates = getRecords("historic_date");
		final TreeMap<Integer, Map<String, Object>> calendars = getRecords("calendar");
		final List<Map<String, Object>> deathEvents = extractReferences("event", personID, "death");
		for(int i = 0, length = deathEvents.size(); i < length; i ++){
			final Map<String, Object> deathEvent = deathEvents.get(i);

			final int dateID = extractRecordDateID(deathEvent);
			final Map<String, Object> dateRecord = historicDates.get(dateID);
			final String date = extractRecordDate(dateRecord);
			final Integer calendarID = extractRecordCalendarID(dateRecord);
			final String calendarType = extractRecordType(calendars.get(calendarID));

			//TODO
		}
		//TODO
//		int deathYear = Integer.MIN_VALUE;
//		Map<String, Object> death = store.createEmptyNode();
//		final List<Map<String, Object>> deathEvents = extractTaggedEvents(person, "DEATH", store);
//		for(final Map<String, Object> node : deathEvents){
//			final String dateValue = store.traverse(node, "DATE").getValue();
//			final LocalDate date = DateParser.parse(dateValue);
//			if(date != null){
//				final int y = date.getYear();
//				if(y > deathYear){
//					deathYear = y;
//					death = node;
//				}
//			}
//		}
//		return death;
		return deathEvents;
	}

	private List<Map<String, Object>> extractReferences(final String fromTable, final int personID, final String eventType){
		final List<Map<String, Object>> matchedRecords = new ArrayList<>();
		final TreeMap<Integer, Map<String, Object>> records = getRecords(fromTable);
		for(final Map<String, Object> record : records.values())
			if("person".equals(extractRecordReferenceTable(record))
					&& Objects.equals(personID, extractRecordReferenceID(record))
					&& eventType.equals(extractRecordType(record)))
				matchedRecords.add(record);
		return matchedRecords;
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
		return (String)record.get("date");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordCalendarID(final Map<String, Object> record){
		return (Integer)record.get("calendar_id");
	}

	public static String extractBirthYear(final Map<String, Object> person, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
		//		String year = null;
		//		if(!person.isEmpty()){
		//			final Map<String, Object> earliestBirth = extractEarliestBirth(person, store);
		//			final String birthDate = extractEarliestBirthDate(earliestBirth, store);
		//			if(birthDate != null)
		//				year = DateParser.extractYear(birthDate);
		//		}
		//		return year;
		return "*";
	}

//	public String extractBirthPlace(final int personID){
//		final List<Map<String, Object>> earliestBirth = extractEarliestBirth(personID);
//		return (!earliestBirth.isEmpty()? extractEarliestBirthPlace(earliestBirth, store): null);
//	}

	public static String extractDeathYear(final Map<String, Object> person, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		String year = null;
//		if(!person.isEmpty()){
//			final Map<String, Object> latestDeath = extractLatestDeath(person, store);
//			final String deathDate = extractLatestDeathDate(latestDeath, store);
//			if(deathDate != null)
//				year = DateParser.extractYear(deathDate);
//		}
//		return year;
		return "*";
	}

//	public String extractDeathPlace(final int personID, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
//		final List<Map<String, Object>> latestDeath = extractLatestDeath(personID);
//		return (!latestDeath.isEmpty()? extractLatestDeathPlace(latestDeath, store): null);
//	}

	private static String extractEarliestBirthDate(final Map<String, Object> earliestBirth,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		String birthDate = null;
//		if(!earliestBirth.isEmpty()){
//			final String dateValue = store.traverse(earliestBirth, "DATE").getValue();
//			final LocalDate date = DateParser.parse(dateValue);
//			if(date != null)
//				birthDate = dateValue;
//		}
//		return birthDate;
		return "*";
	}

	private static String extractEarliestBirthPlace(final Map<String, Object> earliestBirth,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		String birthPlace = null;
//		if(!earliestBirth.isEmpty()){
//			final Map<String, Object> place = store.getPlace(store.traverse(earliestBirth, "PLACE").getXRef());
//			if(place != null)
//				birthPlace = extractPlace(place, store);
//		}
//		return birthPlace;
		return "*";
	}

	private static String extractLatestDeathDate(final Map<String, Object> latestDeath,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		String deathDate = null;
//		if(!latestDeath.isEmpty()){
//			final String dateValue = store.traverse(latestDeath, "DATE").getValue();
//			final LocalDate date = DateParser.parse(dateValue);
//			if(date != null)
//				deathDate = dateValue;
//		}
//		return deathDate;
		return "*";
	}

	private static String extractLatestDeathPlace(final Map<String, Object> latestDeath,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		String deathPlace = null;
//		if(!latestDeath.isEmpty()){
//			final Map<String, Object> place = store.getPlace(store.traverse(latestDeath, "PLACE").getXRef());
//			if(place != null)
//				deathPlace = extractPlace(place, store);
//		}
//		return deathPlace;
		return "*";
	}

	private static String extractPlace(final Map<String, Object> place, final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		final Map<String, Object> addressEarliest = extractEarliestAddress(place, store);
//		return addressEarliest.getValue();
		return "*";
	}

	private static Map<String, Object> extractEarliestAddress(final Map<String, Object> place,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		final List<Map<String, Object>> addresses = store.traverseAsList(place, "ADDRESS[]");
//		return (!addresses.isEmpty()? addresses.get(0): store.traverse(place, "NAME"));
		return Collections.emptyMap();
	}

	private static List<Map<String, Object>> extractTaggedEvents(final Map<String, Object> node, final String eventType,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		//TODO
//		final List<Map<String, Object>> events = store.traverseAsList(node, "EVENT[]");
//		final List<Map<String, Object>> birthEvents = new ArrayList<>(events.size());
//		for(Map<String, Object> event : events){
//			event = store.getEvent(event.getXRef());
//			if(eventType.equals(store.traverse(event, "TYPE").getValue()))
//				birthEvents.add(event);
//		}
//		return birthEvents;
		return Collections.emptyList();
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}


	public final Map<String, Object> getChildReference(){
		return childReference;
	}

	/** Set the direct child of the family to be linked. */
	final void setChildReference(final Map<String, Object> childReference){
		this.childReference = childReference;
	}

	final Point getPersonPaintingEnterPoint(){
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
		personName1.put("personal_name", "toni");
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
		event1.put("reference_table", "person");
		event1.put("reference_id", 1);
		events.put((Integer)event1.get("id"), event1);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("id", 2);
		event2.put("type", "death");
		event2.put("person_id", 1);
		event2.put("date_id", 2);
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

		final PersonListenerInterface listener = new PersonListenerInterface(){
			@Override
			public void onPersonEdit(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onEditPerson " + person.get("id"));
			}

			@Override
			public void onPersonFocus(final PersonPanel boxPanel, final SelectedNodeType type, final Map<String, Object> person){
				System.out.println("onFocusPerson " + person.get("id") + ", type is " + type);
			}

			@Override
			public void onPersonLink(final PersonPanel boxPanel, final SelectedNodeType type){
				System.out.println("onLinkPerson " + type);
			}

			@Override
			public void onPersonUnlink(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onUnlinkPerson " + person.get("id"));
			}

			@Override
			public void onPersonAdd(final PersonPanel boxPanel){
				System.out.println("onAddPerson");
			}

			@Override
			public void onPersonRemove(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onRemovePerson " + person.get("id"));
			}

			@Override
			public void onPersonAddImage(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onAddPreferredImage " + person.get("id"));
			}
		};

		EventQueue.invokeLater(() -> {
			final PersonPanel panel = create(SelectedNodeType.PARTNER1, person1, store, boxType);
			panel.initComponents();
			panel.loadData();
			panel.setPersonListener(listener);

			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(panel, BorderLayout.NORTH);
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
