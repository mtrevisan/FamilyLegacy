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
import io.github.mtrevisan.familylegacy.gedcom.GedcomNodeBuilder;
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
import java.util.EnumMap;
import java.util.List;
import java.util.StringJoiner;


public class IndividualBoxPanel extends JPanel{

	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";
	private static final String FAMILY_NAME_SEPARATOR = ", ";

	private static final Color BACKGROUND_COLOR_NO_INDIVIDUAL = Color.WHITE;
	private static final Color BACKGROUND_COLOR_MALE = new Color(180, 197, 213);
	private static final Color BACKGROUND_COLOR_FEMALE = new Color(255, 212, 177);
	private static final Color BACKGROUND_COLOR_UNKNOWN = new Color(221, 221, 221);

	public static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);

	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);

	private static final EnumMap<Sex, Color> BACKGROUND_COLOR_FROM_SEX = new EnumMap<>(Sex.class);
	static{
		BACKGROUND_COLOR_FROM_SEX.put(Sex.MALE, BACKGROUND_COLOR_MALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.FEMALE, BACKGROUND_COLOR_FEMALE);
		BACKGROUND_COLOR_FROM_SEX.put(Sex.UNKNOWN, BACKGROUND_COLOR_UNKNOWN);
	}

	//preload imaged
	private static final ImageIcon ADD_PHOTO_MAN = ResourceHelper.getImage("/images/addPhoto.man.jpg");
	private static final ImageIcon ADD_PHOTO_WOMAN = ResourceHelper.getImage("/images/addPhoto.woman.jpg");
	private static final ImageIcon ADD_PHOTO_BOY = ResourceHelper.getImage("/images/addPhoto.boy.jpg");
	private static final ImageIcon ADD_PHOTO_GIRL = ResourceHelper.getImage("/images/addPhoto.girl.jpg");
	private static final ImageIcon ADD_PHOTO_UNKNOWN = ResourceHelper.getImage("/images/addPhoto.unknown.jpg");


	private final JLabel birthDeathAgeLabel = new JLabel();
	private final JLabel imgLabel = new JLabel();
	private final JLabel individualNameLabel = new JLabel();
	private final JScrollPane infoScrollPane = new JScrollPane();
	private final JTable infoTable = new JTable();
	private final JLabel linkIndividualLabel = new JLabel();
	private final JLabel newIndividualLabel = new JLabel();
	private final JButton photosButton = new JButton();

	private GedcomNode individualNode;
	private final Transformer transformer = new Transformer(Protocol.FLEF);


	public IndividualBoxPanel(final GedcomNode individualNode, final BoxPanelType boxType){
		this.individualNode = individualNode;
//		this.listener = listener;

		initComponents(boxType);

		loadData(boxType);
	}

	private void initComponents(final BoxPanelType boxType){
		setBackground(null);
		setOpaque(false);
//		addMouseListener(new java.awt.event.MouseAdapter(){
//			public void mouseClicked(java.awt.event.MouseEvent evt){
//				formMouseClicked(evt);
//			}
//		});

		individualNameLabel.setText("individual-name");
		individualNameLabel.setVerticalAlignment(SwingConstants.TOP);
		individualNameLabel.setMinimumSize(new Dimension(0, 16));
		individualNameLabel.setName(""); // NOI18N
//		individualNameLabel.addMouseListener(new java.awt.event.MouseAdapter(){
//			public void mouseClicked(java.awt.event.MouseEvent evt){
//				individualNameLabelMouseClicked(evt);
//			}
//		});

		birthDeathAgeLabel.setForeground(new Color(110, 110, 110));
		birthDeathAgeLabel.setText("birth-death-age");

		newIndividualLabel.setText("New Individual");
		newIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
//		newIndividualLabel.addMouseListener(new java.awt.event.MouseAdapter(){
//			public void mouseClicked(java.awt.event.MouseEvent evt){
//				newIndividualLabelMouseClicked(evt);
//			}
//		});

		linkIndividualLabel.setText("Link Individual");
		linkIndividualLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
//		linkIndividualLabel.addMouseListener(new java.awt.event.MouseAdapter(){
//			public void mouseClicked(java.awt.event.MouseEvent evt){
//				linkIndividualLabelMouseClicked(evt);
//			}
//		});

		imgLabel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255)));
		imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
//		imgLabel.addMouseListener(new java.awt.event.MouseAdapter(){
//			public void mouseClicked(java.awt.event.MouseEvent evt){
//				imgLabelMouseClicked(evt);
//			}
//		});

		infoScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		infoTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{{null, null}, {null, null}, {null, null}, {null, null}, {null, null}}, new String[]{"", ""}){
			final Class[] types = new Class[]{String.class, String.class};
			boolean[] canEdit = new boolean[]{false, false};

			public Class getColumnClass(int columnIndex){
				return types[columnIndex];
			}

			public boolean isCellEditable(int rowIndex, int columnIndex){
				return canEdit[columnIndex];
			}
		});
		infoTable.setFocusable(false);
		infoTable.setIntercellSpacing(new Dimension(0, 0));
		infoTable.setRowSelectionAllowed(false);
		infoTable.setShowHorizontalLines(false);
		infoTable.setShowVerticalLines(false);
		infoTable.setTableHeader(null);
//		TableColumnModel columnModel = infoTable.getColumnModel();
//		TableColumn column = columnModel.getColumn(0);
//		column.setMinWidth(55);
//		column.setMaxWidth(55);
//		infoScrollPane.setVisible(individualBoxType == BoxPanelType.PRIMARY && individual != null);
		infoScrollPane.setViewportView(infoTable);

		photosButton.setText("Photos");
//		photosButton.setVisible(boxType == BoxPanelType.PRIMARY && individual != null);
//		photosButton.addActionListener(new java.awt.event.ActionListener(){
//			public void actionPerformed(java.awt.event.ActionEvent evt){
//				photosButtonActionPerformed(evt);
//			}
//		});

		final GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(infoScrollPane, GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(birthDeathAgeLabel).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(linkIndividualLabel)).addGroup(layout.createSequentialGroup().addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(newIndividualLabel))).addGap(0, 0, Short.MAX_VALUE))).addGap(18, 18, 18).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(photosButton, GroupLayout.Alignment.TRAILING).addComponent(imgLabel, GroupLayout.Alignment.TRAILING)).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(individualNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(newIndividualLabel)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(birthDeathAgeLabel).addComponent(linkIndividualLabel)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(infoScrollPane, GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)).addGroup(layout.createSequentialGroup().addComponent(imgLabel).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(photosButton).addGap(0, 0, Short.MAX_VALUE))).addContainerGap()));

		final int individualMaxWidth = (int)infoScrollPane.getPreferredSize().getWidth();
		final int individualMaxHeight = (int)individualNameLabel.getPreferredSize().getHeight();
		individualNameLabel.setMaximumSize(new Dimension(individualMaxWidth, individualMaxHeight));


		setPreferredSize(boxType == BoxPanelType.PRIMARY? new Dimension(400, 136): new Dimension(190, 68));
		final Font individualNameFont = (boxType == BoxPanelType.PRIMARY?
			new Font("Tahoma", Font.BOLD, 14): new Font("Tahoma", Font.PLAIN, 11));
		individualNameLabel.setFont(individualNameFont);
		individualNameLabel.setCursor(new Cursor(boxType == BoxPanelType.PRIMARY? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR));
		imgLabel.setPreferredSize(boxType == BoxPanelType.PRIMARY?
			new Dimension(60, 80): new Dimension(30, 40));
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
			final Sex sex = Sex.fromCode(transformer.extractSubStructure(individualNode, "SEX")
				.getValue());
			backgroundColor = BACKGROUND_COLOR_FROM_SEX.getOrDefault(sex, BACKGROUND_COLOR_UNKNOWN);
		}
		return backgroundColor;
	}

	private void loadData(final BoxPanelType boxType){
		final String personalName = extractPersonalName();
		if(boxType == BoxPanelType.PRIMARY)
			//FIXME if selected:
			individualNameLabel.setText("<html><font style=\"text-decoration:underline;font-weight:bold\">" + personalName + "</font></html>");
		else
			individualNameLabel.setText("<html>" + personalName + "</html>");

		final String birthDeathAge = getBirthDeathAge();
		birthDeathAgeLabel.setText(birthDeathAge);

		final ImageIcon icon = getAddPhotoImage();
		final ImageIcon img = ResourceHelper.getImage(icon, imgLabel.getPreferredSize());
		imgLabel.setIcon(img);

		if(boxType == BoxPanelType.PRIMARY){
			resetInfo();

			if(individualNode != null)
				writeGeneralInfo();
		}

		boolean hasIndividual = (individualNode != null);
		individualNameLabel.setVisible(hasIndividual);
		birthDeathAgeLabel.setVisible(hasIndividual);
		newIndividualLabel.setVisible(!hasIndividual);
//		linkIndividualLabel.setVisible(!hasIndividual && treeHasIndividuals);
		imgLabel.setVisible(individualNode != null);
		infoScrollPane.setVisible(boxType == BoxPanelType.PRIMARY && individualNode != null);
		photosButton.setVisible(boxType == BoxPanelType.PRIMARY && individualNode != null);
	}

	private String extractPersonalName(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(individualNode != null){
			GedcomNode name = transformer.createEmpty();
			final List<GedcomNode> names = individualNode.getChildrenWithTag("NAME");
			if(!names.isEmpty())
				name = names.get(0);
			final String title = transformer.extractSubStructure(name, "TITLE")
				.getValue();
			final GedcomNode personalName = transformer.extractSubStructure(name, "PERSONAL_NAME");
			final String nameSuffix = transformer.extractSubStructure(personalName, "NAME_SUFFIX")
				.getValue();
			final String familyName = transformer.extractSubStructure(name, "FAMILY_NAME")
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
			GedcomNode birth = transformer.createEmpty();
			final List<GedcomNode> births = individualNode.getChildrenWithTag("BIRTH");
			if(!births.isEmpty())
				birth = births.get(0);
			final String birthDate = birth.getValue();

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
//		if(individualNode != null){
//			Sex sex = individual.getSexAsEnum();
//			String approximatedAge = individual.getAge();
//			int age = (approximatedAge != null? Integer.valueOf(approximatedAge.startsWith("~")? approximatedAge.substring(1): approximatedAge): -1);
//			switch(sex){
//				case MALE:
//					icon = (age >= 0 && age < 11? ADD_PHOTO_BOY: ADD_PHOTO_MAN);
//					break;
//
//				case FEMALE:
//					icon = (age >= 0 && age < 11? ADD_PHOTO_GIRL: ADD_PHOTO_WOMAN);
//			}
//		}
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

		infoScrollPane.setVerticalScrollBarPolicy(row == 5? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS:
			ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
	}

	private void resetInfo(){
		for(int row = 0; row < 5; row ++){
			infoTable.setValueAt(null, row, 0);
			infoTable.setValueAt(null, row, 1);
		}
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
		GedcomNode node = storeFlef.getIndividuals().get(1);

		EventQueue.invokeLater(() -> {
			JDialog dialog = new JDialog(new JFrame(), true);
			BoxPanelType boxType = BoxPanelType.PRIMARY;
			IndividualBoxPanel panel = new IndividualBoxPanel(node, boxType);
			dialog.add(panel);
			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(java.awt.event.WindowEvent e){
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
