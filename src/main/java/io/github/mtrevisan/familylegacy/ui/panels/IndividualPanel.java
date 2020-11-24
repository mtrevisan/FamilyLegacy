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
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.AbstractCalendarParser;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public class IndividualPanel extends JPanel implements PropertyChangeListener{

	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";

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
	private static final Color BORDER_COLOR_SHADOW_SELECTED = new Color(131, 131, 131, 130);

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


	private final JLabel familyNameLabel = new JLabel();
	private final JLabel personalNameLabel = new JLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel preferredImageLabel = new JLabel();
	private final JLabel newIndividualLabel = new JLabel();
	private final JLabel linkIndividualLabel = new JLabel();

	private GedcomNode individual;
	private final Flef store;
	private BoxPanelType boxType;
	private final IndividualListenerInterface listener;


	public IndividualPanel(final GedcomNode individual, final Flef store, final BoxPanelType boxType,
			final IndividualListenerInterface listener){
		this.store = store;
		this.listener = listener;

		this.individual = individual;
		this.boxType = boxType;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setOpaque(false);
		if(listener != null)
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isRightMouseButton(evt))
						listener.onIndividualEdit(IndividualPanel.this, individual);
				}
			});

		familyNameLabel.setVerticalAlignment(SwingConstants.TOP);
		personalNameLabel.setVerticalAlignment(SwingConstants.TOP);
		if(listener != null && boxType == BoxPanelType.SECONDARY){
			familyNameLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualFocus(IndividualPanel.this, individual);
				}
			});
			personalNameLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualFocus(IndividualPanel.this, individual);
				}
			});
		}
		addPropertyChangeListener(PROPERTY_NAME_TEXT_CHANGE, this);
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		if(listener != null){
			newIndividualLabel.setText("New individual");
			newIndividualLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			newIndividualLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualNew(IndividualPanel.this);
				}
			});

			linkIndividualLabel.setText("Link individual");
			linkIndividualLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			linkIndividualLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualLink(IndividualPanel.this);
				}
			});
		}

		preferredImageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		setPreferredSize(preferredImageLabel, 48., IMAGE_ASPECT_RATIO);
		if(listener != null){
			preferredImageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			preferredImageLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualAddPreferredImage(IndividualPanel.this, individual);
				}
			});
		}

		final double shrinkFactor = getShrinkFactor();
		setLayout(new MigLayout("insets 7", "[grow]0[]", "[]0[]0[]"));
		add(familyNameLabel, "cell 0 0,width ::100%-" + (PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3) + ",hidemode 3");
		add(newIndividualLabel, "cell 0 0,hidemode 3");
		add(preferredImageLabel, "cell 1 0 1 3,aligny top");
		add(personalNameLabel, "cell 0 1,width ::100%-" + (PREFERRED_IMAGE_WIDTH / shrinkFactor + 7 * 3) + ",hidemode 3");
		add(linkIndividualLabel, "cell 0 1,hidemode 3");
		add(infoLabel, "cell 0 2");
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
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final int panelHeight = getHeight();
			final int panelWidth = getWidth();

			final Color startColor = getBackgroundColor();
			if(individual != null){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(individual == null){
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
	public void propertyChange(final PropertyChangeEvent evt){
		//show tooltips with full names if they are too long to be displayed
		if(PROPERTY_NAME_TEXT_CHANGE.equals(evt.getPropertyName())){
			int requiredWidth = familyNameLabel.getUI().getPreferredSize(familyNameLabel).width;
			familyNameLabel.setToolTipText(requiredWidth > familyNameLabel.getWidth()? familyNameLabel.getText(): null);

			requiredWidth = personalNameLabel.getUI().getPreferredSize(personalNameLabel).width;
			personalNameLabel.setToolTipText(requiredWidth > personalNameLabel.getWidth()? personalNameLabel.getText(): null);
		}
	}

	private Color getBackgroundColor(){
		Color backgroundColor = BACKGROUND_COLOR_NO_INDIVIDUAL;
		if(individual != null){
			final Sex sex = extractSex(individual, store);
			backgroundColor = BACKGROUND_COLOR_FROM_SEX.getOrDefault(sex, BACKGROUND_COLOR_UNKNOWN);
		}
		return backgroundColor;
	}

	public static Sex extractSex(final GedcomNode individual, final Flef store){
		return Sex.fromCode(store.traverse(individual, "SEX")
			.getValue());
	}

	public void loadData(final GedcomNode individual, final BoxPanelType boxType){
		this.individual = individual;
		this.boxType = boxType;

		loadData();
	}

	private void loadData(){
		final Dimension size = (boxType == BoxPanelType.PRIMARY? new Dimension(260, 90):
			new Dimension(170, 65));
		setPreferredSize(size);
		setMaximumSize(boxType == BoxPanelType.PRIMARY? new Dimension(373, size.height):
			new Dimension(280, size.height));

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
		familyNameLabel.setText(personalName[0]);
		personalNameLabel.setText(personalName[1]);
		firePropertyChange(PROPERTY_NAME_TEXT_CHANGE, null, null);


		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final int years = extractBirthDeathAge(sj);
		infoLabel.setText(sj.toString());

		setPreferredSize(preferredImageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO);
		final ImageIcon icon = ResourceHelper.getImage(getAddPhotoImage(years), preferredImageLabel.getPreferredSize());
		preferredImageLabel.setIcon(icon);

		familyNameLabel.setVisible(individual != null);
		personalNameLabel.setVisible(individual != null);
		infoLabel.setVisible(individual != null);
		newIndividualLabel.setVisible(individual == null);
		linkIndividualLabel.setVisible(individual == null && store.hasIndividuals());
		preferredImageLabel.setVisible(individual != null);
	}

	public static List<String[]> extractCompleteName(final GedcomNode individual, final Flef store){
		final List<String[]> completeNames = new ArrayList<>();
		if(individual != null){
			final List<GedcomNode> names = store.traverseAsList(individual, "NAME[]");
			for(final GedcomNode name : names){
				final String title = store.traverse(name, "TITLE")
					.getValue();
				final GedcomNode personalName = store.traverse(name, "PERSONAL_NAME");
				final String nameSuffix = store.traverse(personalName, "NAME_SUFFIX")
					.getValue();
				final String familyName = store.traverse(name, "FAMILY_NAME")
					.getValueOrDefault(NO_DATA);

				final StringJoiner personal = new StringJoiner(StringUtils.SPACE);
				final StringJoiner family = new StringJoiner(StringUtils.SPACE);
				if(title != null)
					personal.add(title);
				if(!personalName.isEmpty())
					personal.add(personalName.getValueOrDefault(NO_DATA));
				if(familyName != null)
					family.add(familyName);
				if(nameSuffix != null)
					family.add(nameSuffix);

				completeNames.add(new String[]{
					(family.length() > 0? family.toString(): NO_DATA),
					(personal.length() > 0? personal.toString(): NO_DATA)
				});
			}
		}
		else
			completeNames.add(new String[]{NO_DATA, NO_DATA});
		return completeNames;
	}

	public static String extractBirthYear(final GedcomNode individual, final Flef store){
		String year = null;
		if(individual != null){
			final String birthDate = extractEarliestBirthDate(individual, store);
			if(birthDate != null)
				year = DateParser.extractYear(birthDate);
		}
		return year;
	}

	public static String extractBirthPlace(final GedcomNode individual, final Flef store){
		return (individual != null? extractEarliestBirthPlace(individual, store): null);
	}

	public static String extractDeathYear(final GedcomNode individual, final Flef store){
		String year = null;
		if(individual != null){
			final String deathDate = extractLatestDeathDate(individual, store);
			if(deathDate != null)
				year = DateParser.extractYear(deathDate);
		}
		return year;
	}

	public static String extractDeathPlace(final GedcomNode individual, final Flef store){
		return (individual != null? extractLatestDeathPlace(individual, store): null);
	}

	private int extractBirthDeathAge(final StringJoiner sj){
		int years = -1;
		if(individual != null){
			final String birthDate = extractEarliestBirthDate(individual, store);
			final String deathDate = extractLatestDeathDate(individual, store);
			String age = null;
			if(birthDate != null && deathDate != null){
				final boolean isApproximated = (AbstractCalendarParser.isApproximation(birthDate)
					|| AbstractCalendarParser.isApproximation(deathDate));
				final LocalDate birth = DateParser.parse(birthDate);
				final LocalDate death = DateParser.parse(deathDate);
				years = Period.between(birth, death).getYears();
				age = (isApproximated? "~" + years: Integer.toString(years));
			}

			sj.add(birthDate != null? DateParser.extractYear(birthDate): NO_DATA);
			sj.add("-");
			sj.add(deathDate != null? DateParser.extractYear(deathDate): NO_DATA);
			if(age != null)
				sj.add("(" + age + ")");
		}
		return years;
	}

	private static String extractEarliestBirthDate(final GedcomNode individual, final Flef store){
		int birthYear = 0;
		String birthDate = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{BIRTH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int by = date.getYear();
				if(birthDate == null || by < birthYear){
					birthYear = by;
					birthDate = dateValue;
				}
			}
		}
		return birthDate;
	}

	private static String extractEarliestBirthPlace(final GedcomNode individual, final Flef store){
		int birthYear = 0;
		String birthPlace = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{BIRTH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(birthPlace == null || my < birthYear){
					final GedcomNode place = store.getPlace(store.traverse(node, "PLACE").getXRef());
					if(place != null){
						final String placeValue = extractPlace(place, store);
						if(placeValue != null){
							birthYear = my;
							birthPlace = placeValue;
						}
					}
				}
			}
		}
		return birthPlace;
	}

	private static String extractPlace(final GedcomNode place, final Flef store){
		final GedcomNode addressEarliest = extractEarliestAddress(place, store);

		//extract place as town, county, state, country, otherwise from value
		String placeValue = place.getValue();
		if(addressEarliest != null){
			final GedcomNode town = store.traverse(addressEarliest, "TOWN");
			final GedcomNode city = store.traverse(addressEarliest, "CITY");
			final GedcomNode county = store.traverse(addressEarliest, "COUNTY");
			final GedcomNode state = store.traverse(addressEarliest, "STATE");
			final GedcomNode country = store.traverse(addressEarliest, "COUNTRY");
			final StringJoiner sj = new StringJoiner(", ");
			JavaHelper.addValueIfNotNull(sj, town);
			JavaHelper.addValueIfNotNull(sj, city);
			JavaHelper.addValueIfNotNull(sj, county);
			JavaHelper.addValueIfNotNull(sj, state);
			JavaHelper.addValueIfNotNull(sj, country);
			if(sj.length() > 0)
				placeValue = sj.toString();
		}
		return placeValue;
	}

	private static GedcomNode extractEarliestAddress(final GedcomNode place, final Flef store){
		int addressYear = 0;
		GedcomNode addressEarliest = null;
		final List<GedcomNode> addresses = store.traverseAsList(place, "ADDRESS");
		for(final GedcomNode address : addresses){
			final GedcomNode source = store.getSource(store.traverse(address, "SOURCE").getXRef());
			final String addressDateValue = store.traverse(source, "DATE").getValue();
			final LocalDate addressDate = DateParser.parse(addressDateValue);
			if(addressDate != null){
				final int ay = addressDate.getYear();
				if(addressEarliest == null || ay < addressYear){
					addressYear = ay;
					addressEarliest = address;
				}
			}
		}
		return addressEarliest;
	}

	private static String extractLatestDeathDate(final GedcomNode individual, final Flef store){
		int deathYear = 0;
		String deathDate = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{DEATH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int by = date.getYear();
				if(deathDate == null || by > deathYear){
					deathYear = by;
					deathDate = dateValue;
				}
			}
		}
		return deathDate;
	}

	private static String extractLatestDeathPlace(final GedcomNode individual, final Flef store){
		int deathYear = 0;
		String deathPlace = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{DEATH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(deathPlace == null || my > deathYear){
					final GedcomNode place = store.getPlace(store.traverse(node, "PLACE").getXRef());
					if(place != null){
						final String placeValue = extractPlace(place, store);
						if(placeValue != null){
							deathYear = my;
							deathPlace = placeValue;
						}
					}
				}
			}
		}
		return deathPlace;
	}

	private Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	private ImageIcon getAddPhotoImage(final int years){
		ImageIcon icon = ADD_PHOTO_UNKNOWN;
		if(individual != null){
			final Sex sex = extractSex(individual, store);
			switch(sex){
				case MALE:
					icon = (years >= 0 && years < 11? ADD_PHOTO_BOY: ADD_PHOTO_MAN);
					break;

				case FEMALE:
					icon = (years >= 0 && years < 11? ADD_PHOTO_GIRL: ADD_PHOTO_WOMAN);
			}
		}
		return icon;
	}


	public Point getIndividualPaintingEnterPoint(){
		final int x = getX() + getWidth() / 2;
		final int y = getY();
		return new Point(x, y);
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
//		final GedcomNode individual = storeFlef.getIndividuals().get(1500);
		//long names
		final GedcomNode individual = storeFlef.getIndividual("I2365");
//		final GedcomNode individual = null;
		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final IndividualListenerInterface listener = new IndividualListenerInterface(){
			@Override
			public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onEditIndividual " + individual.getID());
			}

			@Override
			public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID());
			}

			@Override
			public void onIndividualNew(final IndividualPanel boxPanel){
				System.out.println("onNewIndividual");
			}

			@Override
			public void onIndividualLink(final IndividualPanel boxPanel){
				System.out.println("onLinkIndividual");
			}

			@Override
			public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final IndividualPanel panel = new IndividualPanel(individual, storeFlef, boxType, listener);

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
