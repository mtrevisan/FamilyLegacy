package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;


public class ChildrenPanel extends JPanel{

	private static final long serialVersionUID = -1250057284416778781L;

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private GroupLayout.SequentialGroup sequentialGroup;
	private GroupLayout.ParallelGroup parallelGroup;

	private final GedcomNode family;
	private final Flef store;
	private final IndividualListenerInterface individualListener;


	public ChildrenPanel(final GedcomNode family, final Flef store, final IndividualListenerInterface individualListener){
		this.family = family;
		this.store = store;
		this.individualListener = individualListener;

		loadData();
	}

	public void loadData(){
		//FIXME really needed?
//		removeAll();

		final GroupLayout layout = new GroupLayout(this);
		sequentialGroup = layout.createSequentialGroup();
		parallelGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
		layout.setHorizontalGroup(sequentialGroup);
		layout.setVerticalGroup(parallelGroup);
		setLayout(layout);

		final List<GedcomNode> children = TRANSFORMER.traverseAsList(family, "CHILD[]");
		if(!children.isEmpty()){
			sequentialGroup.addGap(0, 0, Short.MAX_VALUE);

			final Iterator<GedcomNode> itr = children.iterator();
			while(itr.hasNext()){
				final String individualXRef = itr.next().getXRef();
				final GedcomNode individual = store.getIndividual(individualXRef);
				final IndividualPanel individualBox = new IndividualPanel(individual, store, BoxPanelType.SECONDARY, individualListener);

				sequentialGroup.addComponent(individualBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
				if(itr.hasNext())
					sequentialGroup.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
				parallelGroup.addComponent(individualBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			}

			revalidate();
			repaint();
		}
	}


	//TODO
	public Point[] getChildrenPaintingEntryPoints(){
		//halfway between spouse1 and spouse2 boxes
//		final int x = (spouse1Panel.getX() + spouse1Panel.getWidth() + spouse2Panel.getX()) / 2;
//		//the bottom point of the marriage panel (that is: bottom point of spouse1 box minus the height of the horizontal connection line
//		//plus half the size of the marriage panel box)
//		final int y = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT + MARRIAGE_PANEL_DIMENSION.height / 2;
//		return new Point(x, y);
		return null;
	}

	public Point getChildrenPaintingExitPoint(){
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
		final GedcomNode family = storeFlef.getFamilies().get(0);
//		GedcomNode family = null;
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
		});
	}

}
