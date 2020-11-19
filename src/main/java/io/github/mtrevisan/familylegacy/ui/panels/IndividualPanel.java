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
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.AbstractCalendarParser;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
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

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private final JLabel individualNameLabel = new JLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel imgLabel = new JLabel();
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
		setBackground(null);
		setOpaque(false);
		final Dimension size = (boxType == BoxPanelType.PRIMARY? new Dimension(220, 90):
			new Dimension(170, 55));
		setSize(size);
		setPreferredSize(size);
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

		individualNameLabel.setVerticalAlignment(SwingConstants.TOP);
//		individualNameLabel.setMinimumSize(new Dimension(0, 16));
		final Font font = (boxType == BoxPanelType.PRIMARY? FONT_PRIMARY: FONT_SECONDARY);
		individualNameLabel.setFont(font);

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

		imgLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		setPreferredSize(imgLabel, 48.);
		if(listener != null){
			imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			imgLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualAddPreferredImage(IndividualPanel.this, individual);
				}
			});
		}

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(infoLabel)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(linkIndividualLabel)
							)
							.addGroup(layout.createSequentialGroup()
								.addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(newIndividualLabel)
							)
						)
						.addGap(0, 0, Short.MAX_VALUE)
					)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(imgLabel, GroupLayout.Alignment.TRAILING)
				)
				.addContainerGap()
			)
		);
		layout.setVerticalGroup(layout.createParallelGroup()
			.addGroup(GroupLayout.Alignment.CENTER, layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(newIndividualLabel)
						)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(infoLabel)
							.addComponent(linkIndividualLabel)
						)
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
					)
					.addGroup(layout.createSequentialGroup()
						.addComponent(imgLabel)
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
						.addGap(0, 0, Short.MAX_VALUE)
					)
				)
				.addContainerGap()
			)
		);

		final Dimension namePreferredSize = individualNameLabel.getPreferredSize();
		final int individualMaxWidth = (int)Math.ceil(namePreferredSize.getWidth());
		final int individualMaxHeight = (int)Math.ceil(namePreferredSize.getHeight());
		individualNameLabel.setMaximumSize(new Dimension(individualMaxWidth, individualMaxHeight));
	}

	private void setPreferredSize(final JComponent component, final double baseWidth){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * IMAGE_ASPECT_RATIO / shrinkFactor);
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
		return Sex.fromCode(TRANSFORMER.traverse(individual, "SEX")
			.getValue());
	}

	public void loadData(){
		individualNameLabel.setText(composeIndividualName());

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final int years = extractBirthDeathAge(sj);
		infoLabel.setText(sj.toString());
		infoLabel.setFont(deriveInfoFont(individualNameLabel.getFont()));

		final ImageIcon icon = ResourceHelper.getImage(getAddPhotoImage(years), imgLabel.getPreferredSize());
		imgLabel.setIcon(icon);

		individualNameLabel.setVisible(individual != null);
		infoLabel.setVisible(individual != null);
		newIndividualLabel.setVisible(individual == null);
		linkIndividualLabel.setVisible(individual == null && store.hasIndividuals());
		imgLabel.setVisible(individual != null);
	}

	private String composeIndividualName(){
		final String personalName = extractCompleteName();
		return (boxType == BoxPanelType.PRIMARY?
			"<html><font style=\"text-decoration:underline\">" + personalName + "</font></html>":
			"<html>" + personalName + "</html>");
	}

	private String extractCompleteName(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(individual != null){
			GedcomNode name = TRANSFORMER.createEmpty();
			final List<GedcomNode> names = individual.getChildrenWithTag("NAME");
			if(!names.isEmpty())
				name = names.get(0);
			final String title = TRANSFORMER.traverse(name, "TITLE")
				.getValue();
			final GedcomNode personalName = TRANSFORMER.traverse(name, "PERSONAL_NAME");
			final String nameSuffix = TRANSFORMER.traverse(personalName, "NAME_SUFFIX")
				.getValue();
			final String familyName = TRANSFORMER.traverse(name, "FAMILY_NAME")
				.getValueOrDefault(NO_DATA);

			if(title != null)
				sj.add(title);
			if(familyName != null)
				sj.add(familyName + ",");
			if(!personalName.isEmpty())
				sj.add(personalName.getValueOrDefault(NO_DATA));
			if(nameSuffix != null)
				sj.add(nameSuffix);
		}
		else
			sj.add(NO_DATA);
		return sj.toString();
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
		for(final GedcomNode node : TRANSFORMER.traverseAsList(individual, "EVENT{BIRTH}[]")){
			final String date = TRANSFORMER.traverse(node, "DATE").getValue();
			if(date != null){
				final int by = DateParser.parse(date).getYear();
				if(birthDate == null){
					birthYear = by;
					birthDate = date;
				}
				else if(by < birthYear){
					birthYear = by;
					birthDate = date;
				}
			}
		}
		return birthDate;
	}

	private String extractLatestDeathDate(){
		int deathYear = 0;
		String deathDate = null;
		for(final GedcomNode node : TRANSFORMER.traverseAsList(individual, "EVENT{DEATH}[]")){
			final String date = TRANSFORMER.traverse(node, "DATE").getValue();
			if(date != null){
				final int by = DateParser.parse(date).getYear();
				if(deathDate == null){
					deathYear = by;
					deathDate = date;
				}
				else if(by > deathYear){
					deathYear = by;
					deathDate = date;
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
