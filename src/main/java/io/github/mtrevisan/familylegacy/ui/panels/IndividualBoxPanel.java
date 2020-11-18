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
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.enums.Sex;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;


public class IndividualBoxPanel extends JPanel{

	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";

	private static final Color BACKGROUND_COLOR_NO_INDIVIDUAL = Color.WHITE;
	private static final Color BACKGROUND_COLOR_MALE = new Color(180, 197, 213);
	private static final Color BACKGROUND_COLOR_FEMALE = new Color(255, 212, 177);
	private static final Color BACKGROUND_COLOR_UNKNOWN = new Color(221, 221, 221);

	private static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);

	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);

	private static final Map<Sex, Color> BACKGROUND_COLOR_FROM_SEX = new EnumMap<>(Sex.class);
	static{
		BACKGROUND_COLOR_FROM_SEX.put(Sex.MALE, BACKGROUND_COLOR_MALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.FEMALE, BACKGROUND_COLOR_FEMALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.UNKNOWN, BACKGROUND_COLOR_UNKNOWN);
	}
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final ImageIcon ADD_PHOTO_MAN = ResourceHelper.getImage("/images/addPhoto.man.jpg");
	private static final ImageIcon ADD_PHOTO_WOMAN = ResourceHelper.getImage("/images/addPhoto.woman.jpg");
	private static final ImageIcon ADD_PHOTO_BOY = ResourceHelper.getImage("/images/addPhoto.boy.jpg");
	private static final ImageIcon ADD_PHOTO_GIRL = ResourceHelper.getImage("/images/addPhoto.girl.jpg");
	private static final ImageIcon ADD_PHOTO_UNKNOWN = ResourceHelper.getImage("/images/addPhoto.unknown.jpg");

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 14);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 11);

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private final JLabel birthDeathAgeLabel = new JLabel();
	private final JLabel imgLabel = new JLabel();
	private final JLabel individualNameLabel = new JLabel();
	private final JLabel newIndividualLabel = new JLabel();
	private final JLabel linkIndividualLabel = new JLabel();

	private GedcomNode individualNode;
	private boolean treeHasIndividuals;
	private IndividualBoxListenerInterface listener;


	public IndividualBoxPanel(final GedcomNode individualNode, final boolean treeHasIndividuals, final BoxPanelType boxType,
			final IndividualBoxListenerInterface listener){
		this.individualNode = individualNode;
		this.treeHasIndividuals = treeHasIndividuals;
		this.listener = listener;

		initComponents(boxType);

		loadData(boxType);
	}

	private void initComponents(final BoxPanelType boxType){
		setBackground(null);
		setOpaque(false);
		setPreferredSize(boxType == BoxPanelType.PRIMARY? new Dimension(400, 136): new Dimension(190, 68));
		if(listener != null)
			addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					if(individualNode != null && boxType == BoxPanelType.PRIMARY && SwingUtilities.isRightMouseButton(evt))
						listener.onIndividualEdit(IndividualBoxPanel.this, individualNode);
					else if(individualNode != null && boxType == BoxPanelType.SECONDARY && SwingUtilities.isLeftMouseButton(evt))
						listener.onIndividualFocus(IndividualBoxPanel.this, individualNode);
				}
			});

		individualNameLabel.setVerticalAlignment(SwingConstants.TOP);
		individualNameLabel.setMinimumSize(new Dimension(0, 16));
		final Font font = (boxType == BoxPanelType.PRIMARY? FONT_PRIMARY: FONT_SECONDARY);
		individualNameLabel.setFont(font);

		birthDeathAgeLabel.setForeground(new Color(110, 110, 110));

		if(listener != null){
			newIndividualLabel.setText("New individual");
			newIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			newIndividualLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualNew(IndividualBoxPanel.this);
				}
			});
		}

		if(listener != null){
			linkIndividualLabel.setText("Link individual");
			linkIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			linkIndividualLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					listener.onIndividualLink(IndividualBoxPanel.this);
				}
			});
		}

		imgLabel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255)));
		setComponentSize(imgLabel, 48., boxType);
		if(listener != null){
			imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			imgLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent evt){
					listener.onIndividualAddPreferredImage(IndividualBoxPanel.this, individualNode);
				}
			});
		}

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(birthDeathAgeLabel)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(linkIndividualLabel))
							.addGroup(layout.createSequentialGroup()
								.addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(newIndividualLabel)))
						.addGap(0, 0, Short.MAX_VALUE)))
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addComponent(imgLabel, GroupLayout.Alignment.TRAILING))
				.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(newIndividualLabel))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(birthDeathAgeLabel)
							.addComponent(linkIndividualLabel))
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
					.addGroup(layout.createSequentialGroup()
						.addComponent(imgLabel)
						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
						.addGap(0, 0, Short.MAX_VALUE)))
				.addContainerGap()));

		final int individualMaxWidth = (int)individualNameLabel.getPreferredSize().getWidth();
		final int individualMaxHeight = (int)individualNameLabel.getPreferredSize().getHeight();
		individualNameLabel.setMaximumSize(new Dimension(individualMaxWidth, individualMaxHeight));
	}

	private void setComponentSize(final JComponent component, final double imageWidth, final BoxPanelType boxType){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(imageWidth / shrinkFactor);
		final int height = (int)Math.ceil(imageWidth * IMAGE_ASPECT_RATIO / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();

			final int panelHeight = getHeight();
			final int panelWidth = getWidth();

			final Color startColor = getBackgroundColor();
			final Color endColor = Color.WHITE;

//			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if(individualNode != null){
				final GradientPaint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, endColor);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1, panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(individualNode == null){
				final BasicStroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					10.f, new float[]{5.f}, 0.f);
				graphics2D.setStroke(dashedStroke);
			}
			graphics2D.drawRoundRect(0, 0, panelWidth - 1, panelHeight - 1, ARCS.width, ARCS.height);

			graphics2D.dispose();
		}
	}

	private Color getBackgroundColor(){
		Color backgroundColor = BACKGROUND_COLOR_NO_INDIVIDUAL;
		if(individualNode != null){
			final Sex sex = extractSex();
			backgroundColor = BACKGROUND_COLOR_FROM_SEX.getOrDefault(sex, BACKGROUND_COLOR_UNKNOWN);
		}
		return backgroundColor;
	}

	private Sex extractSex(){
		return Sex.fromCode(TRANSFORMER.traverse(individualNode, "SEX")
			.getValue());
	}

	private void loadData(final BoxPanelType boxType){
		final String personalName = extractCompleteName();
		if(boxType == BoxPanelType.PRIMARY)
			//FIXME if selected (?):
			individualNameLabel.setText("<html><font style=\"text-decoration:underline;font-weight:bold\">" + personalName + "</font></html>");
		else
			individualNameLabel.setText("<html>" + personalName + "</html>");

		final String birthDeathAge = getBirthDeathAge();
		birthDeathAgeLabel.setText(birthDeathAge);

		final ImageIcon icon = getAddPhotoImage();
		final ImageIcon img = ResourceHelper.getImage(icon, imgLabel.getPreferredSize());
		imgLabel.setIcon(img);

		if(boxType == BoxPanelType.PRIMARY && individualNode != null)
			writeGeneralInfo();

		individualNameLabel.setVisible(individualNode != null);
		birthDeathAgeLabel.setVisible(individualNode != null);
		newIndividualLabel.setVisible(individualNode == null);
		linkIndividualLabel.setVisible(individualNode == null && treeHasIndividuals);
		imgLabel.setVisible(individualNode != null);
	}

	private String extractCompleteName(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(individualNode != null){
			GedcomNode name = TRANSFORMER.createEmpty();
			final List<GedcomNode> names = individualNode.getChildrenWithTag("NAME");
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

	private String getBirthDeathAge(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(individualNode != null){
			final List<String> birthYears = TRANSFORMER.traverseAsList(individualNode, "EVENT{BIRTH}[]").stream()
				.map(b -> TRANSFORMER.traverse(b, "EVENT{BIRTH}").getValue())
				.filter(Objects::nonNull)
				//TODO extract year
				.collect(Collectors.toList());
			//TODO what if there are multiple birth dates?
			final String birthDate = (!birthYears.isEmpty()? birthYears.get(0): NO_DATA);

			final List<String> deathDates = individualNode.getChildrenWithTag("EVENT").stream()
				.filter(b -> "DEATH".equals(b.getValue()))
				.map(b -> TRANSFORMER.traverse(b, "DATE").getValue())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			//TODO what if there are multiple death dates?
			final String deathDate = (!deathDates.isEmpty()? deathDates.get(0): NO_DATA);

//			String birthDate = Optional.ofNullable(individual.getBirthDate())
//				.map(DateParser::formatYear)
//				.orElse("?");
//			sj.add(birthDate);
//			sj.add("-");
//			String deathDate = Optional.ofNullable(individual.getDeathDate())
//				.map(DateParser::formatYear)
//				.orElse("?");
//			sj.add(deathDate);
//			String age = individual.getAge();
//			if(age != null)
//				sj.add("(" + age + ")");
		}
		return sj.toString();
	}

	private ImageIcon getAddPhotoImage(){
		ImageIcon icon = ADD_PHOTO_UNKNOWN;
		if(individualNode != null){
			final Sex sex = extractSex();
			final String birth = TRANSFORMER.traverse(individualNode, "EVENT{BIRTH}[0]")
				.getValue();
			final String death = TRANSFORMER.traverse(individualNode, "EVENT{DEATH}[0]")
				.getValue();

//			String approximatedAge = individualNode.getAge();
//			int age = (approximatedAge != null? Integer.valueOf(approximatedAge.startsWith("~")? approximatedAge.substring(1): approximatedAge): -1);
//			switch(sex){
//				case MALE:
//					icon = (age >= 0 && age < 11? ADD_PHOTO_BOY: ADD_PHOTO_MAN);
//					break;
//
//				case FEMALE:
//					icon = (age >= 0 && age < 11? ADD_PHOTO_GIRL: ADD_PHOTO_WOMAN);
//			}
		}
		return icon;
	}

	private void writeGeneralInfo(){
		int row = 0;
//		String birthDate = individual.getBirthDate();
//		if(birthDate != null){
//			infoTable.setValueAt("Born:", row, 0);
//			infoTable.setValueAt(DateParser.formatDate(birthDate), row, 1);
//			row ++;
//		}
//		Place birthPlace = individual.getBirthPlace();
//		if(birthPlace != null){
//			infoTable.setValueAt("in:", row, 0);
//			infoTable.setValueAt(birthPlace.getPlaceName(), row, 1);
//			row ++;
//		}
//		String deathDate = individual.getDeathDate();
//		if(deathDate != null){
//			String age = individual.getAge();
//			infoTable.setValueAt("Died:", row, 0);
//			infoTable.setValueAt(DateParser.formatDate(deathDate) + (age != null? " (" + age + ")": StringUtils.EMPTY), row, 1);
//			row ++;
//		}
//		Place deathPlace = individual.getDeathPlace();
//		if(deathPlace != null){
//			infoTable.setValueAt("in:", row, 0);
//			infoTable.setValueAt(deathPlace.getPlaceName(), row, 1);
//			row ++;
//		}
//		List<PersonalName> names = individual.getNames();
//		if(names != null && !names.isEmpty() && names.get(0).getNickname() != null){
//			String nickname = names.get(0).getNickname().getValue();
//			infoTable.setValueAt("Nickname:", row, 0);
//			infoTable.setValueAt(nickname, row, 1);
//			row ++;
//		}
	}

	public Point getIndividualPaintingEnteringPoint(){
		final int x = getX() + getWidth() / 2;
		final int y = getY();
		return new Point(x, y);
	}


	public static void main(String args[]) throws GedcomParseException, GedcomGrammarParseException{
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception e){}

		Store storeGedcom = new Gedcom();
		Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();
		GedcomNode individualNode = storeFlef.getIndividuals().get(0);
//		GedcomNode individualNode = null;

		IndividualBoxListenerInterface listener = new IndividualBoxListenerInterface(){
			@Override
			public void onIndividualEdit(IndividualBoxPanel boxPanel, GedcomNode individual){
				System.out.println("onEdit " + individual.getID());
			}

			@Override
			public void onIndividualFocus(IndividualBoxPanel boxPanel, GedcomNode individual){
				System.out.println("onFocus " + individual.getID());
			}

			@Override
			public void onIndividualNew(IndividualBoxPanel boxPanel){
				System.out.println("onNew");
			}

			@Override
			public void onIndividualLink(IndividualBoxPanel boxPanel){
				System.out.println("onLink");
			}

			@Override
			public void onIndividualAddPreferredImage(IndividualBoxPanel boxPanel, GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			JDialog dialog = new JDialog(new JFrame(), true);
			BoxPanelType boxType = BoxPanelType.PRIMARY;
			IndividualBoxPanel panel = new IndividualBoxPanel(individualNode, true, boxType, listener);
			dialog.add(panel);
			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.setMinimumSize(boxType == BoxPanelType.PRIMARY?
				new Dimension(80*3, 60*3): new Dimension(40*3, 30*3));
			dialog.setVisible(true);
		});
	}

}
