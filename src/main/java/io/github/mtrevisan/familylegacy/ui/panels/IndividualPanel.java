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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.AbstractCalendarParser;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.LabelAutoToolTip;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
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
import javax.swing.SwingConstants;
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
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public class IndividualPanel extends JPanel implements PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";

	static final int SECONDARY_MAX_HEIGHT = 65;

	private static final Color BACKGROUND_COLOR_NO_INDIVIDUAL = Color.WHITE;
	private static final Color BACKGROUND_COLOR_FADE_TO = Color.WHITE;
	private static final Color BACKGROUND_COLOR_MALE = new Color(180, 197, 213);
	private static final Color BACKGROUND_COLOR_FEMALE = new Color(255, 212, 177);
	private static final Color BACKGROUND_COLOR_UNKNOWN = new Color(221, 221, 221);

	public static final Map<Sex, Color> BACKGROUND_COLOR_FROM_SEX = new EnumMap<>(Sex.class);
	static{
		BACKGROUND_COLOR_FROM_SEX.put(Sex.MALE, BACKGROUND_COLOR_MALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.FEMALE, BACKGROUND_COLOR_FEMALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.UNKNOWN, BACKGROUND_COLOR_UNKNOWN);
	}
	private static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);
	private static final Color BORDER_COLOR_SHADOW_SELECTED = Color.BLACK;

	private static final Color BIRTH_DEATH_AGE_COLOR = new Color(110, 110, 110);
	private static final Color IMAGE_LABEL_BORDER_COLOR = new Color(255, 255, 255);

	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final ImageIcon ADD_PHOTO_MAN = ResourceHelper.getImage("/images/addPhoto.man.jpg");
	private static final ImageIcon ADD_PHOTO_WOMAN = ResourceHelper.getImage("/images/addPhoto.woman.jpg");
	private static final ImageIcon ADD_PHOTO_BOY = ResourceHelper.getImage("/images/addPhoto.boy.jpg");
	private static final ImageIcon ADD_PHOTO_GIRL = ResourceHelper.getImage("/images/addPhoto.girl.jpg");
	private static final ImageIcon ADD_PHOTO_UNKNOWN = ResourceHelper.getImage("/images/addPhoto.unknown.jpg");

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 14);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 11);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	private static final String PROPERTY_NAME_TEXT_CHANGE = "text";

	private final LabelAutoToolTip familyNameLabel = new LabelAutoToolTip();
	private final LabelAutoToolTip personalNameLabel = new LabelAutoToolTip();
	private final JLabel infoLabel = new JLabel();
	private final JLabel preferredImageLabel = new JLabel();
	private final JMenuItem editIndividualItem = new JMenuItem("Edit Individual…", 'E');
	private final JMenuItem linkIndividualItem = new JMenuItem("Link Individual…", 'L');
	private final JMenuItem unlinkIndividualItem = new JMenuItem("Unlink Individual", 'U');
	private final JMenuItem addIndividualItem = new JMenuItem("Add Individual…", 'A');
	private final JMenuItem removeIndividualItem = new JMenuItem("Remove Individual", 'R');

	private final SelectedNodeType type;
	private GedcomNode individual;
	private final Flef store;
	private final BoxPanelType boxType;

	private GedcomNode childReference;


	public IndividualPanel(final SelectedNodeType type, final GedcomNode individual, final Flef store, final BoxPanelType boxType){
		this.type = type;
		this.store = store;

		this.individual = individual;
		this.boxType = boxType;

		initComponents();

		loadData();
	}

	final void setIndividualListener(final IndividualListenerInterface listener){
		if(listener != null){
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(!individual.isEmpty() && evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualEdit(IndividualPanel.this, individual);
				}
			});


			if(boxType == BoxPanelType.SECONDARY){
				familyNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onIndividualFocus(IndividualPanel.this, type, individual);
					}
				});
				personalNameLabel.addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent evt){
						if(SwingUtilities.isLeftMouseButton(evt))
							listener.onIndividualFocus(IndividualPanel.this, type, individual);
					}
				});
			}

			attachPopUpMenu(listener);

			refresh(Flef.ACTION_COMMAND_INDIVIDUAL_COUNT);


			preferredImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			preferredImageLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualAddPreferredImage(IndividualPanel.this, individual);
				}
			});
		}
	}

	private void initComponents(){
		setOpaque(false);

		familyNameLabel.setVerticalAlignment(SwingConstants.TOP);
		personalNameLabel.setVerticalAlignment(SwingConstants.TOP);
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		preferredImageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		setPreferredSize(preferredImageLabel, 48., IMAGE_ASPECT_RATIO);

		final double shrinkFactor = getShrinkFactor();
		setLayout(new MigLayout("insets 7", "[grow]0[]", "[]0[]10[]"));
		add(familyNameLabel, "cell 0 0,width ::100%-" + (PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3) + ",hidemode 3");
		add(preferredImageLabel, "cell 1 0 1 3,aligny top");
		add(personalNameLabel, "cell 0 1,width ::100%-" + (PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3) + ",hidemode 3");
		add(infoLabel, "cell 0 2");
	}

	private void attachPopUpMenu(final IndividualListenerInterface listener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editIndividualItem.addActionListener(e -> listener.onIndividualEdit(this, individual));
		popupMenu.add(editIndividualItem);

		linkIndividualItem.addActionListener(e -> listener.onIndividualLink(this, type));
		popupMenu.add(linkIndividualItem);

		unlinkIndividualItem.addActionListener(e -> listener.onIndividualUnlink(this, individual));
		popupMenu.add(unlinkIndividualItem);

		addIndividualItem.addActionListener(e -> listener.onIndividualAdd(this));
		popupMenu.add(addIndividualItem);

		removeIndividualItem.addActionListener(e -> listener.onIndividualRemove(this, individual));
		popupMenu.add(removeIndividualItem);

		addMouseListener(new PopupMouseAdapter(popupMenu, this));
	}

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio){
		final double shrinkFactor = getShrinkFactor();
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	private double getShrinkFactor(){
		return (boxType == BoxPanelType.PRIMARY? 1.: 2.);
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
			if(!individual.isEmpty()){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(individual.isEmpty()){
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
		//show tooltips with full names if they are too long to be displayed
		if(PROPERTY_NAME_TEXT_CHANGE.equals(evt.getPropertyName())){
			familyNameLabel.manageToolTip();
			personalNameLabel.manageToolTip();
		}
	}

	private Color getBackgroundColor(){
		Color backgroundColor = BACKGROUND_COLOR_NO_INDIVIDUAL;
		if(!individual.isEmpty()){
			final Sex sex = extractSex(individual, store);
			backgroundColor = BACKGROUND_COLOR_FROM_SEX.getOrDefault(sex, BACKGROUND_COLOR_UNKNOWN);
		}
		return backgroundColor;
	}

	public static Sex extractSex(final GedcomNode individual, final Flef store){
		return Sex.fromCode(store.traverse(individual, "SEX")
			.getValue());
	}

	public final void loadData(final GedcomNode individual){
		this.individual = individual;

		loadData();

		repaint();
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
			//add underline to mark this individual as eligible for primary position
			@SuppressWarnings("unchecked")
			final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
			font = font.deriveFont(attributes);
		}
		familyNameLabel.setFont(font);
		familyNameLabel.setCursor(Cursor.getPredefinedCursor(boxType == BoxPanelType.PRIMARY? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR));
		personalNameLabel.setFont(font);
		personalNameLabel.setCursor(Cursor.getPredefinedCursor(boxType == BoxPanelType.PRIMARY? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR));
		infoLabel.setFont(infoFont);

		final String[] personalName = extractCompleteName(individual, store).get(0);
		familyNameLabel.addPropertyChangeListener(PROPERTY_NAME_TEXT_CHANGE, this);
		familyNameLabel.setText(personalName[0]);
		familyNameLabel.setToolTipText(personalName[0]);
		personalNameLabel.addPropertyChangeListener(PROPERTY_NAME_TEXT_CHANGE, this);
		personalNameLabel.setText(personalName[1]);
		personalNameLabel.setToolTipText(personalName[1]);


		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final StringJoiner toolTipSJ = new StringJoiner(StringUtils.SPACE);
		final int age = extractBirthDeathAge(sj, toolTipSJ);
		infoLabel.setText(sj.toString());
		infoLabel.setToolTipText(toolTipSJ.toString());

		setPreferredSize(preferredImageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO);
		final ImageIcon icon = ResourceHelper.getImage(getAddPhotoImage(age), preferredImageLabel.getPreferredSize());
		preferredImageLabel.setIcon(icon);

		familyNameLabel.setVisible(!individual.isEmpty());
		personalNameLabel.setVisible(!individual.isEmpty());
		infoLabel.setVisible(!individual.isEmpty());
		preferredImageLabel.setVisible(!individual.isEmpty());

		refresh(Flef.ACTION_COMMAND_INDIVIDUAL_COUNT);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != Flef.ACTION_COMMAND_INDIVIDUAL_COUNT)
			return;

		linkIndividualItem.setEnabled(individual.isEmpty() && store.hasIndividuals());
		editIndividualItem.setEnabled(!individual.isEmpty());
		final boolean isChildOfFamily = !store.traverseAsList(individual, "FAMILY_CHILD[]").isEmpty();
		unlinkIndividualItem.setEnabled(!individual.isEmpty() && isChildOfFamily);
		addIndividualItem.setEnabled(individual.isEmpty() && store.hasIndividuals());
		removeIndividualItem.setEnabled(!individual.isEmpty());
	}

	public static List<String[]> extractCompleteName(final GedcomNode individual, final Flef store){
		final List<String[]> completeNames = new ArrayList<>(1);
		if(!individual.isEmpty()){
			final List<GedcomNode> names = store.traverseAsList(individual, "NAME[]");
			for(final GedcomNode name : names){
				final String title = store.traverse(name, "TITLE")
					.getValue();
				final GedcomNode individualName = store.traverse(name, "INDIVIDUAL_NAME");
				final String individualNameSuffix = store.traverse(individualName, "SUFFIX")
					.getValue();
				final String familyName = store.traverse(name, "FAMILY_NAME")
					.getValueOrDefault(NO_DATA);

				final StringJoiner personal = new StringJoiner(StringUtils.SPACE);
				final StringJoiner family = new StringJoiner(StringUtils.SPACE);
				if(title != null)
					personal.add(title);
				if(!individualName.isEmpty())
					personal.add(individualName.getValueOrDefault(NO_DATA));
				if(familyName != null)
					family.add(familyName);
				if(individualNameSuffix != null)
					family.add(individualNameSuffix);

				completeNames.add(new String[]{
					(family.length() > 0? family.toString(): NO_DATA),
					(personal.length() > 0? personal.toString(): NO_DATA)
				});
			}
		}
		if(completeNames.isEmpty())
			completeNames.add(new String[]{NO_DATA, NO_DATA});
		return completeNames;
	}

	public static String extractBirthYear(final GedcomNode individual, final Flef store){
		String year = null;
		if(!individual.isEmpty()){
			final GedcomNode earliestBirth = extractEarliestBirth(individual, store);
			final String birthDate = extractEarliestBirthDate(earliestBirth, store);
			if(birthDate != null)
				year = DateParser.extractYear(birthDate);
		}
		return year;
	}

	public static String extractBirthPlace(final GedcomNode individual, final Flef store){
		final GedcomNode earliestBirth = extractEarliestBirth(individual, store);
		return (!individual.isEmpty()? extractEarliestBirthPlace(earliestBirth, store): null);
	}

	public static String extractDeathYear(final GedcomNode individual, final Flef store){
		String year = null;
		if(!individual.isEmpty()){
			final GedcomNode latestDeath = extractLatestDeath(individual, store);
			final String deathDate = extractLatestDeathDate(latestDeath, store);
			if(deathDate != null)
				year = DateParser.extractYear(deathDate);
		}
		return year;
	}

	public static String extractDeathPlace(final GedcomNode individual, final Flef store){
		final GedcomNode latestDeath = extractLatestDeath(individual, store);
		return (!individual.isEmpty()? extractLatestDeathPlace(latestDeath, store): null);
	}

	private int extractBirthDeathAge(final StringJoiner sj, final StringJoiner toolTipSJ){
		int lifeSpan = -1;
		if(!individual.isEmpty()){
			final GedcomNode earliestBirth = extractEarliestBirth(individual, store);
			final GedcomNode latestDeath = extractLatestDeath(individual, store);
			final String birthDate = extractEarliestBirthDate(earliestBirth, store);
			final String birthPlace = extractEarliestBirthPlace(earliestBirth, store);
			final String deathDate = extractLatestDeathDate(latestDeath, store);
			final String deathPlace = extractLatestDeathPlace(latestDeath, store);
			String age = null;
			if(birthDate != null && deathDate != null){
				final boolean isAgeApproximated = (AbstractCalendarParser.isApproximation(birthDate)
					|| AbstractCalendarParser.isRange(birthDate)
					|| AbstractCalendarParser.isApproximation(deathDate)
					|| AbstractCalendarParser.isRange(deathDate));
				final boolean isAgeLessThan = (AbstractCalendarParser.isExact(birthDate) && AbstractCalendarParser.isBefore(deathDate)
					|| AbstractCalendarParser.isAfter(birthDate) && AbstractCalendarParser.isExact(deathDate)
					|| AbstractCalendarParser.isRange(birthDate)
					|| AbstractCalendarParser.isRange(deathDate));
				age = StringUtils.EMPTY;
				if(isAgeLessThan)
					age = "<";
				else if(isAgeApproximated)
					age = "~";
				final LocalDate birth = DateParser.parse(birthDate);
				final LocalDate death = DateParser.parse(deathDate);
				lifeSpan = Period.between(birth, death).getYears();
				age += lifeSpan;
			}

			sj.add(birthDate != null? DateParser.extractYear(birthDate): NO_DATA);
			sj.add("-");
			sj.add(deathDate != null? DateParser.extractYear(deathDate): NO_DATA);
			if(age != null)
				sj.add("(" + age + ")");

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
				toolTipSJ.add("-");
				toolTipSJ.add(deathDate != null? DateParser.formatDate(deathDate): NO_DATA);
			}
		}
		return lifeSpan;
	}

	private static GedcomNode extractEarliestBirth(final GedcomNode individual, final Flef store){
		int birthYear = Integer.MAX_VALUE;
		GedcomNode birth = store.createEmptyNode();
		final List<GedcomNode> birthEvents = extractTaggedEvents(individual, "BIRTH", store);
		for(final GedcomNode node : birthEvents){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int y = date.getYear();
				if(y < birthYear){
					birthYear = y;
					birth = node;
				}
			}
		}
		return birth;
	}

	private static String extractEarliestBirthDate(final GedcomNode earliestBirth, final Flef store){
		String birthDate = null;
		if(!earliestBirth.isEmpty()){
			final String dateValue = store.traverse(earliestBirth, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null)
				birthDate = dateValue;
		}
		return birthDate;
	}

	private static String extractEarliestBirthPlace(final GedcomNode earliestBirth, final Flef store){
		String birthPlace = null;
		if(!earliestBirth.isEmpty()){
			final GedcomNode place = store.getPlace(store.traverse(earliestBirth, "PLACE").getXRef());
			if(place != null)
				birthPlace = extractPlace(place, store);
		}
		return birthPlace;
	}

	private static GedcomNode extractLatestDeath(final GedcomNode individual, final Flef store){
		int deathYear = Integer.MIN_VALUE;
		GedcomNode death = store.createEmptyNode();
		final List<GedcomNode> deathEvents = extractTaggedEvents(individual, "DEATH", store);
		for(final GedcomNode node : deathEvents){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int y = date.getYear();
				if(y > deathYear){
					deathYear = y;
					death = node;
				}
			}
		}
		return death;
	}

	private static String extractLatestDeathDate(final GedcomNode latestDeath, final Flef store){
		String deathDate = null;
		if(!latestDeath.isEmpty()){
			final String dateValue = store.traverse(latestDeath, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null)
				deathDate = dateValue;
		}
		return deathDate;
	}

	private static String extractLatestDeathPlace(final GedcomNode latestDeath, final Flef store){
		String deathPlace = null;
		if(!latestDeath.isEmpty()){
			final GedcomNode place = store.getPlace(store.traverse(latestDeath, "PLACE").getXRef());
			if(place != null)
				deathPlace = extractPlace(place, store);
		}
		return deathPlace;
	}

	private static String extractPlace(final GedcomNode place, final Flef store){
		final GedcomNode addressEarliest = extractEarliestAddress(place, store);
		return addressEarliest.getValue();
	}

	private static GedcomNode extractEarliestAddress(final GedcomNode place, final Flef store){
		final List<GedcomNode> addresses = store.traverseAsList(place, "ADDRESS[]");
		return (!addresses.isEmpty()? addresses.get(0): store.traverse(place, "NAME"));
	}

	private static List<GedcomNode> extractTaggedEvents(final GedcomNode node, final String eventType, final Flef store){
		final List<GedcomNode> events = store.traverseAsList(node, "EVENT[]");
		final List<GedcomNode> birthEvents = new ArrayList<>(events.size());
		for(GedcomNode event : events){
			event = store.getEvent(event.getXRef());
			if(eventType.equals(store.traverse(event, "TYPE").getValue()))
				birthEvents.add(event);
		}
		return birthEvents;
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	private ImageIcon getAddPhotoImage(final int years){
		ImageIcon icon = ADD_PHOTO_UNKNOWN;
		if(!individual.isEmpty()){
			final Sex sex = extractSex(individual, store);
			switch(sex){
				case MALE -> icon = (years >= 0 && years < 11? ADD_PHOTO_BOY: ADD_PHOTO_MAN);
				case FEMALE -> icon = (years >= 0 && years < 11? ADD_PHOTO_GIRL: ADD_PHOTO_WOMAN);
			}
		}
		return icon;
	}


	public final GedcomNode getChildReference(){
		return childReference;
	}

	/** Set the direct child of the family to be linked. */
	final void setChildReference(final GedcomNode childReference){
		this.childReference = childReference;
	}

	final Point getIndividualPaintingEnterPoint(){
		return new Point(getX() + getWidth() / 2, getY());
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg",
				"src/main/resources/ged/large.ged")
			.transform();
//		final GedcomNode individual = storeFlef.getIndividuals().get(0);
//		final GedcomNode individual = storeFlef.getIndividuals().get(1500);
		//long names
		final GedcomNode individual = storeFlef.getIndividual("I2365");
//		final GedcomNode individual = storeFlef.createEmptyNode();
		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final IndividualListenerInterface listener = new IndividualListenerInterface(){
			@Override
			public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onEditIndividual " + individual.getID());
			}

			@Override
			public void onIndividualFocus(final IndividualPanel boxPanel, final SelectedNodeType type, final GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID() + ", type is " + type);
			}

			@Override
			public void onIndividualLink(final IndividualPanel boxPanel, final SelectedNodeType type){
				System.out.println("onLinkIndividual " + type);
			}

			@Override
			public void onIndividualUnlink(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onUnlinkIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAdd(final IndividualPanel boxPanel){
				System.out.println("onAddIndividual");
			}

			@Override
			public void onIndividualRemove(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onRemoveIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final IndividualPanel panel = new IndividualPanel(SelectedNodeType.PARTNER1, individual, storeFlef, boxType);
			panel.setIndividualListener(listener);
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

//			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
////			final Runnable task = () -> panel.loadData(storeFlef.getIndividuals().get(0), BoxPanelType.SECONDARY);
////			final Runnable task = () -> panel.loadData(storeFlef.getIndividuals().get(0), BoxPanelType.PRIMARY);
////			final Runnable task = () -> panel.loadData(null, BoxPanelType.SECONDARY);
//			final Runnable task = () -> panel.loadData(null, BoxPanelType.PRIMARY);
//			scheduler.schedule(task, 3, TimeUnit.SECONDS);
		});
	}

}
