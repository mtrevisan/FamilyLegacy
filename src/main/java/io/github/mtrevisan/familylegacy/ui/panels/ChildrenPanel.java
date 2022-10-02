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
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Iterator;
import java.util.List;


public class ChildrenPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -1250057284416778781L;

	private static final double FAMILY_HEIGHT = 12.;
	private static final double FAMILY_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension FAMILY_SIZE = new Dimension((int)(FAMILY_HEIGHT / FAMILY_ASPECT_RATIO), (int)FAMILY_HEIGHT);

	private static final ImageIcon FAMILY = ResourceHelper.getImage("/images/family.png", FAMILY_SIZE);

	private static final int CHILD_SEPARATION = 15;
	static final int FAMILY_ARROW_HEIGHT = FAMILY.getIconHeight() + FamilyPanel.NAVIGATION_ARROW_SEPARATION;


	private List<GedcomNode> children;
	private boolean[] adoptions;
	private final Flef store;
	private final IndividualListenerInterface individualListener;


	public ChildrenPanel(final GedcomNode family, final Flef store, final IndividualListenerInterface individualListener){
		this.store = store;
		this.individualListener = individualListener;

		setOpaque(false);

		loadData(family);
	}

	public void loadData(final GedcomNode family){
		children = store.traverseAsList(family, "CHILD[]");

		adoptions = new boolean[children.size()];
		for(int i = 0; i < adoptions.length; i ++)
			adoptions[i] = !children.get(i).getChildrenWithTag("ADOPTED").isEmpty();

		removeAll();

		loadData();
	}

	private void loadData(){
		if(!children.isEmpty()){
			setLayout(new MigLayout("insets 0", "[]0[]"));

			final Iterator<GedcomNode> itr = children.iterator();
			while(itr.hasNext()){
				final String individualXRef = itr.next().getXRef();
				final GedcomNode individual = store.getIndividual(individualXRef);
				final boolean isPartner = !store.traverseAsList(individual, "FAMILY_PARTNER[]").isEmpty();
				final IndividualPanel individualBox = new IndividualPanel(SelectedNodeType.CHILD, individual, store, BoxPanelType.SECONDARY,
					individualListener);

				final JPanel box = new JPanel();
				box.setOpaque(false);
				box.setLayout(new MigLayout("flowy,insets 0", "[]",
					"[]" + FamilyPanel.NAVIGATION_ARROW_SEPARATION + "[]"));
				final JLabel familyLabel = new JLabel();
				familyLabel.setMinimumSize(new Dimension(FAMILY.getIconWidth(), FAMILY.getIconHeight()));
				if(isPartner)
					familyLabel.setIcon(FAMILY);
				box.add(familyLabel, "alignx right");
				box.add(individualBox);

				add(box, (itr.hasNext()? "gapright " + CHILD_SEPARATION: StringUtils.EMPTY));
			}
		}
		else
			setLayout(new MigLayout("insets 0", "[]",
				"[" + IndividualPanel.SECONDARY_MAX_HEIGHT + "]"));
	}


	public Point[] getChildrenPaintingEnterPoints(){
		final Component[] components = getComponents();
		final Point[] enterPoints = new Point[components.length];
		for(int i = 0; i < components.length; i ++)
			enterPoints[i] = new Point(components[i].getX() + components[i].getWidth() / 2, components[i].getY() + FAMILY_ARROW_HEIGHT);
		return enterPoints;
	}

	public boolean[] getAdoptions(){
		return adoptions;
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
//		final GedcomNode family = storeFlef.getFamilies().get(0);
		final GedcomNode family = storeFlef.getFamilies().get(4);
//		GedcomNode family = null;

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
			final ChildrenPanel panel = new ChildrenPanel(family, storeFlef, listener);

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
//			final Runnable task = () -> panel.loadData(storeFlef.getFamilies().get(1));
//			scheduler.schedule(task, 3, TimeUnit.SECONDS);
		});
	}

}
