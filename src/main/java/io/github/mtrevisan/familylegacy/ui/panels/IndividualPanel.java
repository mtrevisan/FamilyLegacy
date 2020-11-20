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
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public class IndividualPanel extends JPanel{

	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";

	private static final Color BACKGROUND_COLOR_NO_INDIVIDUAL = Color.WHITE;
	private static final Color BACKGROUND_COLOR_FADE_TO = Color.WHITE;
	private static final Color BACKGROUND_COLOR_MALE = new Color(180, 197, 213);
	private static final Color BACKGROUND_COLOR_FEMALE = new Color(255, 212, 177);
	private static final Color BACKGROUND_COLOR_UNKNOWN = new Color(221, 221, 221);

	private static final Map<Sex, Color> BACKGROUND_COLOR_FROM_SEX = new EnumMap<>(Sex.class);

	static{
		BACKGROUND_COLOR_FROM_SEX.put(Sex.MALE, BACKGROUND_COLOR_MALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.FEMALE, BACKGROUND_COLOR_FEMALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.UNKNOWN, BACKGROUND_COLOR_UNKNOWN);
	}
	static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);
	private static final Color BORDER_COLOR_SHADOW_SELECTED = new Color(131, 131, 131, 130);

	private static final Color BIRTH_DEATH_AGE_COLOR = new Color(110, 110, 110);
	private static final Color IMAGE_LABEL_BORDER_COLOR = new Color(255, 255, 255);

	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);

	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final ImageIcon ADD_PHOTO_MAN = ResourceHelper.getImage("/images/addPhoto.man.jpg");
	private static final ImageIcon ADD_PHOTO_WOMAN = ResourceHelper.getImage("/images/addPhoto.woman.jpg");
	private static final ImageIcon ADD_PHOTO_BOY = ResourceHelper.getImage("/images/addPhoto.boy.jpg");
	private static final ImageIcon ADD_PHOTO_GIRL = ResourceHelper.getImage("/images/addPhoto.girl.jpg");
	private static final ImageIcon ADD_PHOTO_UNKNOWN = ResourceHelper.getImage("/images/addPhoto.unknown.jpg");

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 14);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 11);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;


	private final JLabel familyNameLabel = new JLabel();
	private final JLabel personalNameLabel = new JLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel preferredImageLabel = new JLabel();
	private final JLabel newIndividualLabel = new JLabel();
	private final JLabel linkIndividualLabel = new JLabel();

	private final BoxPanelType boxType;
	private final GedcomNode individual;
	private final Flef store;


	public IndividualPanel(final GedcomNode individual, final Flef store, final BoxPanelType boxType,
			final IndividualListenerInterface listener){
		this.boxType = boxType;
		this.individual = individual;
		this.store = store;

		initComponents(listener);

		loadData();
	}

	private void initComponents(final IndividualListenerInterface listener){
		setOpaque(false);
		final Dimension size = (boxType == BoxPanelType.PRIMARY? new Dimension(220, 90):
			new Dimension(170, 55));
		setSize(size);
		setPreferredSize(size);
		setMaximumSize(boxType == BoxPanelType.PRIMARY? new Dimension(373, size.height):
			new Dimension(280, size.height));
		if(listener != null)
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(individual != null && SwingUtilities.isRightMouseButton(evt))
						listener.onIndividualEdit(IndividualPanel.this, individual);
					else if(individual != null && boxType == BoxPanelType.SECONDARY && SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualFocus(IndividualPanel.this, individual);
				}
			});

		final Font font = (boxType == BoxPanelType.PRIMARY? FONT_PRIMARY: FONT_SECONDARY);
		familyNameLabel.setVerticalAlignment(SwingConstants.TOP);
		familyNameLabel.setFont(font);

		personalNameLabel.setVerticalAlignment(SwingConstants.TOP);
//		individualNameLabel.setMinimumSize(new Dimension(0, 16));
		personalNameLabel.setFont(font);

		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		if(listener != null){
			newIndividualLabel.setText("New individual");
			newIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			newIndividualLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualNew(IndividualPanel.this);
				}
			});
		}

		if(listener != null){
			linkIndividualLabel.setText("Link individual");
			linkIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
			preferredImageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			preferredImageLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualAddPreferredImage(IndividualPanel.this, individual);
				}
			});
		}

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
			.addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(familyNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							)
							.addGroup(layout.createSequentialGroup()
								.addComponent(personalNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(newIndividualLabel)
							)
							.addGroup(layout.createSequentialGroup()
								.addComponent(infoLabel)
								.addComponent(linkIndividualLabel)
							)
						)
						.addGap(0, 0, Short.MAX_VALUE)
					)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(preferredImageLabel, GroupLayout.Alignment.TRAILING)
				)
				.addContainerGap()
			)
		);
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
			.addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(familyNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(personalNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(newIndividualLabel)
						)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(infoLabel)
							.addComponent(linkIndividualLabel)
						)
					)
					.addGroup(layout.createSequentialGroup()
						.addComponent(preferredImageLabel)
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
						.addGap(0, 0, Short.MAX_VALUE)
					)
				)
				.addContainerGap()
			)
		);

//		final Dimension namePreferredSize = personalNameLabel.getPreferredSize();
//		final int individualMaxWidth = (int)Math.ceil(namePreferredSize.getWidth());
//		final int individualMaxHeight = (int)Math.ceil(namePreferredSize.getHeight());
//		personalNameLabel.setMaximumSize(new Dimension(individualMaxWidth, individualMaxHeight));
	}

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
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

	private Color getBackgroundColor(){
		Color backgroundColor = BACKGROUND_COLOR_NO_INDIVIDUAL;
		if(individual != null){
			final Sex sex = extractSex();
			backgroundColor = BACKGROUND_COLOR_FROM_SEX.getOrDefault(sex, BACKGROUND_COLOR_UNKNOWN);
		}
		return backgroundColor;
	}

	private Sex extractSex(){
		return Sex.fromCode(store.traverse(individual, "SEX")
			.getValue());
	}

	public void loadData(){
		final String[] personalName = composeIndividualName();
		familyNameLabel.setText(personalName[0]);
		personalNameLabel.setText(personalName[1]);

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final int years = extractBirthDeathAge(sj);
		infoLabel.setText(sj.toString());
		infoLabel.setFont(deriveInfoFont(personalNameLabel.getFont()));

		final ImageIcon icon = ResourceHelper.getImage(getAddPhotoImage(years), preferredImageLabel.getPreferredSize());
		preferredImageLabel.setIcon(icon);

		familyNameLabel.setVisible(individual != null);
		personalNameLabel.setVisible(individual != null);
		infoLabel.setVisible(individual != null);
		newIndividualLabel.setVisible(individual == null);
		linkIndividualLabel.setVisible(individual == null && store.hasIndividuals());
		preferredImageLabel.setVisible(individual != null);
	}

	private String[] composeIndividualName(){
		final String[] personalName = extractCompleteName();
		personalName[0] = "<html>" + personalName[0] + "</html>";
		personalName[1] = "<html>" + personalName[1] + "</html>";
		return personalName;
	}

	private String[] extractCompleteName(){
		final StringJoiner family = new StringJoiner(StringUtils.SPACE);
		final StringJoiner personal = new StringJoiner(StringUtils.SPACE);
		if(individual != null){
			GedcomNode name = store.createEmptyNode();
			final List<GedcomNode> names = individual.getChildrenWithTag("NAME");
			if(!names.isEmpty())
				name = names.get(0);
			final String title = store.traverse(name, "TITLE")
				.getValue();
			final GedcomNode personalName = store.traverse(name, "PERSONAL_NAME");
			final String nameSuffix = store.traverse(personalName, "NAME_SUFFIX")
				.getValue();
			final String familyName = store.traverse(name, "FAMILY_NAME")
				.getValueOrDefault(NO_DATA);

			if(title != null)
				personal.add(title);
			if(!personalName.isEmpty())
				personal.add(personalName.getValueOrDefault(NO_DATA));
			if(familyName != null)
				family.add(familyName);
			if(nameSuffix != null)
				family.add(nameSuffix);

			return new String[]{
				(family.length() > 0? family.toString(): NO_DATA),
				(personal.length() > 0? personal.toString(): NO_DATA)
			};
		}
		return new String[]{NO_DATA, NO_DATA};
	}

	private int extractBirthDeathAge(final StringJoiner sj){
		int years = -1;
		if(individual != null){
			final String birthDate = extractEarliestBirthDate();
			final String deathDate = extractLatestDeathDate();
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

	private String extractEarliestBirthDate(){
		int birthYear = 0;
		String birthDate = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{BIRTH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int by = date.getYear();
				if(birthDate == null){
					birthYear = by;
					birthDate = dateValue;
				}
				else if(by < birthYear){
					birthYear = by;
					birthDate = dateValue;
				}
			}
		}
		return birthDate;
	}

	private String extractLatestDeathDate(){
		int deathYear = 0;
		String deathDate = null;
		for(final GedcomNode node : store.traverseAsList(individual, "EVENT{DEATH}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int by = date.getYear();
				if(deathDate == null){
					deathYear = by;
					deathDate = dateValue;
				}
				else if(by > deathYear){
					deathYear = by;
					deathDate = dateValue;
				}
			}
		}
		return deathDate;
	}

	private Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	private ImageIcon getAddPhotoImage(final int years){
		ImageIcon icon = ADD_PHOTO_UNKNOWN;
		if(individual != null){
			final Sex sex = extractSex();
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

	public Point getIndividualPaintingExitRightPoint(){
		final int x = getX() + getWidth();
		final int y = getY() + getHeight();
		return new Point(x, y);
	}

	public Point getIndividualPaintingExitLeftPoint(){
		final int x = getX();
		final int y = getY() + getHeight();
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
		final GedcomNode individual = storeFlef.getIndividuals().get(1500);
//		GedcomNode individual = null;
		final BoxPanelType boxType = BoxPanelType.PRIMARY;

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
		});
	}

}