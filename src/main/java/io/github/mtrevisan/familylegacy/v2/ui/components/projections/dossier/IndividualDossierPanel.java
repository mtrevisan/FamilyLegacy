/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Panel that shows the complete FLEF dossier of a single individual.
 * <p>
 * The panel is organized in collapsible sections, one per
 * {@link DossierSectionType}. Each section shows the entries produced by
 * {@link IndividualDossierService}: label, value, optional subtitle, and
 * an optional evidence badge.
 * <p>
 * <b>Interaction.</b>
 * <ul>
 *   <li>a single click on a section header expands or collapses the section;</li>
 *   <li>a double click on a row that carries a backing record opens the standard edit dialog for that record;</li>
 *   <li>after a successful edit the dossier is rebuilt, so the panel reflects the change immediately.</li>
 * </ul>
 * <p>
 * The panel is designed to be attached as a side panel to any of the
 * projection views (ancestor tree, Sugiyama graph, ego network). The
 * caller is responsible for invoking {@link #setIndividual(String)} when
 * the user selects an individual in the host view.
 */
public class IndividualDossierPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7291047591038475612L;


	private static final Color HEADER_BACKGROUND = new Color(240, 236, 228);
	private static final Color HEADER_BACKGROUND_HOVER = new Color(232, 226, 214);
	private static final Color HEADER_BORDER = new Color(210, 205, 195);
	private static final Color HEADER_TEXT = new Color(40, 35, 25);
	private static final Color SECTION_BACKGROUND = new Color(250, 249, 245);
	private static final Color ROW_BACKGROUND_ALT = new Color(246, 244, 238);
	private static final Color ROW_BACKGROUND_HOVER = new Color(238, 232, 218);
	private static final Color LABEL_COLOR = new Color(60, 55, 45);
	private static final Color VALUE_COLOR = new Color(30, 30, 30);
	private static final Color SUBTITLE_COLOR = new Color(120, 115, 100);
	private static final Color EVIDENCE_COLOR = new Color(90, 120, 90);
	private static final Color LINK_COLOR = new Color(30, 80, 180);
	private static final Color EMPTY_COLOR = new Color(140, 130, 120);

	private static final Color BADGE_BACKGROUND = new Color(224, 218, 204);
	private static final Font BADGE_FONT = new Font("Tahoma", Font.BOLD, 11);
	private static final int PART_INDENT = 8;

	private static final Font HEADER_FONT = new Font("Tahoma", Font.BOLD, 12);
	private static final Font LABEL_FONT = new Font("Tahoma", Font.BOLD, 11);
	private static final Font VALUE_FONT = new Font("Tahoma", Font.PLAIN, 12);
	private static final Font SUBTITLE_FONT = new Font("Tahoma", Font.ITALIC, 10);
	private static final Font EVIDENCE_FONT = new Font("Tahoma", Font.PLAIN, 9);
	private static final Font TITLE_FONT = new Font("Tahoma", Font.BOLD, 14);
	private static final Font EMPTY_FONT = new Font("Tahoma", Font.ITALIC, 12);

	private static final int LABEL_WIDTH = 110;

	private static final String PROPERTY_DOSSIER = "dossier";


	private final FLEFModel model;
	private final IndividualDossierService service;

	/** Content panel: rebuilt on every {@link #setIndividual(String)}. */
	private final JPanel contentPanel = new JPanel(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]", "[]0[]"));

	private final JLabel titleLabel = new JLabel();
	private final JLabel subtitleLabel = new JLabel();

	/** Section collapse state, preserved across reloads. */
	private final Map<DossierSectionType, Boolean> expandedState = new EnumMap<>(DossierSectionType.class);


	public IndividualDossierPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
		this.service = new IndividualDossierService(model);

		buildUI();
		showEmpty();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Populates the panel with the dossier of the given individual. A
	 * {@code null} or unknown id clears the panel.
	 *
	 * @param individualId the individual id; may be {@code null}
	 */
	public void setIndividual(final String individualId){
		final IndividualDossier dossier = service.build(individualId);
		if(dossier.isEmpty()){
			showEmpty();

			return;
		}

		showDossier(dossier);
	}

	/**
	 * Rebuilds the currently displayed dossier, refreshing every section
	 * from the model. Call this after an edit performed elsewhere.
	 */
	public void refresh(){
		final IndividualDossier current = (IndividualDossier)getClientProperty(PROPERTY_DOSSIER);
		if(current == null || current.isEmpty())
			return;

		final String id = current.individual()
			.getId();
		if(id != null)
			setIndividual(id);
	}

	/**
	 * Returns the id of the individual currently shown in the dossier, or
	 * {@code null} when no individual is selected.
	 *
	 * @return the current individual id, or {@code null}
	 */
	public String getCurrentIndividualId(){
		final IndividualDossier dossier = (IndividualDossier)getClientProperty(PROPERTY_DOSSIER);
		return (dossier != null && !dossier.isEmpty()
			? dossier.individual().getId()
			: null);
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private void buildUI(){
		setLayout(new BorderLayout());
		setBackground(SECTION_BACKGROUND);

		final JPanel header = new JPanel(new MigLayout("ins 8,gapy 2,fillx", "[grow]", "[]0[]"));
		header.setBackground(HEADER_BACKGROUND);
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BORDER));
		titleLabel.setFont(TITLE_FONT);
		titleLabel.setForeground(HEADER_TEXT);
		subtitleLabel.setFont(SUBTITLE_FONT);
		subtitleLabel.setForeground(SUBTITLE_COLOR);
		header.add(titleLabel, "growx,wrap");
		header.add(subtitleLabel, "growx");
		add(header, BorderLayout.NORTH);

		final JScrollPane scroll = new JScrollPane(contentPanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar()
			.setUnitIncrement(16);
		scroll.getViewport()
			.setBackground(SECTION_BACKGROUND);
		add(scroll, BorderLayout.CENTER);

		setPreferredSize(new Dimension(360, 700));
	}

	private void showEmpty(){
		titleLabel.setText("Dossier");
		subtitleLabel.setText(StringUtils.EMPTY);
		contentPanel.removeAll();
		final JLabel empty = new JLabel("Select an individual to view its dossier.");
		empty.setFont(EMPTY_FONT);
		empty.setForeground(EMPTY_COLOR);
		contentPanel.add(empty, "gapleft 12,gaptop 12");
		contentPanel.revalidate();
		contentPanel.repaint();
		putClientProperty(PROPERTY_DOSSIER, IndividualDossier.empty());
	}

	private void showDossier(final IndividualDossier dossier){
		putClientProperty(PROPERTY_DOSSIER, dossier);

		titleLabel.setText(dossier.displayName());
		final StringBuilder subtitle = new StringBuilder();
		final List<DossierEntry> identity = dossier.entriesOf(DossierSectionType.IDENTITY);
		for(final DossierEntry e : identity)
			if("Vital".equals(e.label()) && !e.value()
				.isEmpty()){
				subtitle.append(e.value());
				break;
			}
		subtitleLabel.setText(subtitle.toString());

		contentPanel.removeAll();
		for(final DossierSectionType type : DossierSectionType.values()){
			final List<DossierEntry> entries = dossier.entriesOf(type);
			if(entries.isEmpty())
				continue;
			final SectionPanel section = new SectionPanel(type, entries);
			contentPanel.add(section, "growx");
		}

		if(contentPanel.getComponentCount() == 0){
			final JLabel empty = new JLabel("No records linked to this individual.");
			empty.setFont(EMPTY_FONT);
			empty.setForeground(EMPTY_COLOR);
			contentPanel.add(empty, "gapleft 12,gaptop 12");
		}

		contentPanel.revalidate();
		contentPanel.repaint();
	}


	/* ======================================================================
	 *                          Section panel
	 * ====================================================================== */

	/**
	 * A single collapsible section. The header is a clickable bar that
	 * toggles the visibility of the content. The content is a vertical
	 * stack of entry rows.
	 */
	private final class SectionPanel extends JPanel{

		@Serial
		private static final long serialVersionUID = -8910273510289374611L;


		private final DossierSectionType sectionType;
		private final JPanel headerBar;
		private final JPanel contentArea;
		private final ArrowIndicator arrowIndicator;


		SectionPanel(final DossierSectionType type, final List<DossierEntry> entries){
			this.sectionType = type;

			setLayout(new BorderLayout());
			setBackground(SECTION_BACKGROUND);

			// Header bar.
			this.headerBar = new JPanel(new MigLayout("ins 6 10 6 10,fillx", "[]4[grow]", "[]"));
			headerBar.setBackground(HEADER_BACKGROUND);
			headerBar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, HEADER_BORDER));
			headerBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			this.arrowIndicator = new ArrowIndicator();

			final JLabel title = new JLabel(type.getDisplayLabel() + " (" + entries.size() + ")");
			title.setFont(HEADER_FONT);
			title.setForeground(HEADER_TEXT);

			headerBar.add(arrowIndicator);
			headerBar.add(title, "growx");

			headerBar.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					toggle();
				}

				@Override
				public void mouseEntered(final MouseEvent e){
					headerBar.setBackground(HEADER_BACKGROUND_HOVER);
				}

				@Override
				public void mouseExited(final MouseEvent e){
					headerBar.setBackground(HEADER_BACKGROUND);
				}
			});

			// Content area.
			this.contentArea = new JPanel(new MigLayout("ins 0,fillx,wrap 1", "[grow,fill]", "[]"));
			contentArea.setBackground(SECTION_BACKGROUND);

			int rowIndex = 0;
			for(final DossierEntry entry : entries){
				final EntryRow row = new EntryRow(entry, (rowIndex % 2 == 1));
				contentArea.add(row, "growx");
				rowIndex++;
			}

			add(headerBar, BorderLayout.NORTH);
			add(contentArea, BorderLayout.CENTER);

			// Restore the expanded state, defaulting to expanded for the
			// first two sections and collapsed for the rest.
			final Boolean expanded = expandedState.getOrDefault(sectionType, null);
			final boolean initial = (expanded != null
				? expanded
				: (type == DossierSectionType.IDENTITY || type == DossierSectionType.EVENTS));
			setExpanded(initial);
		}

		private void toggle(){
			setExpanded(!contentArea.isVisible());
			expandedState.put(sectionType, contentArea.isVisible());
		}

		private void setExpanded(final boolean expanded){
			contentArea.setVisible(expanded);
			arrowIndicator.setExpanded(expanded);
			revalidate();
			repaint();
		}
	}


	/**
	 * Small triangle indicator shown next to a section title.
	 * <p>
	 * The triangle is drawn with {@link java.awt.geom.Path2D} rather than with
	 * a Unicode geometric-shape glyph, because those characters are not
	 * present in every platform font and would render as replacement boxes on
	 * some systems (including Tahoma on certain Windows configurations).
	 * <p>
	 * The indicator is a fixed-size component: right-pointing when collapsed,
	 * down-pointing when expanded.
	 */
	private static final class ArrowIndicator extends JComponent{

		@Serial
		private static final long serialVersionUID = 4091823047102938471L;

		/** Side of the triangle, in pixels. */
		private static final int SIZE = 8;
		/** Vertical padding used to center the triangle in the header. */
		private static final int PADDING_Y = 4;

		private boolean expanded;


		ArrowIndicator(){
			setOpaque(false);
			setPreferredSize(new Dimension(SIZE, SIZE + 2 * PADDING_Y));
		}

		void setExpanded(final boolean expanded){
			if(this.expanded != expanded){
				this.expanded = expanded;
				repaint();
			}
		}

		@Override
		protected void paintComponent(final Graphics g){
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final int x = Math.max(0, (getWidth() - SIZE) / 2);
			final int y = Math.max(0, (getHeight() - SIZE) / 2);

			final Path2D.Double path = new Path2D.Double();
			if(expanded){
				// Downward triangle.
				path.moveTo(x, y);
				path.lineTo(x + SIZE, y);
				path.lineTo(x + SIZE / 2., y + SIZE);
			}
			else{
				// Rightward triangle.
				path.moveTo(x, y);
				path.lineTo(x, y + SIZE);
				path.lineTo(x + SIZE, y + SIZE / 2.);
			}
			path.closePath();

			g2.setColor(HEADER_TEXT);
			g2.fill(path);
		}

	}

	/* ======================================================================
	 *                          Entry row
	 * ====================================================================== */

	/**
	 * A single row inside a section: label on the left, value on the
	 * right, optional subtitle underneath the value, optional evidence
	 * badge on the far right.
	 * <p>
	 * When the entry carries a backing record, a double click opens the
	 * standard edit dialog for that record.
	 */
	private final class EntryRow extends JPanel{

		@Serial
		private static final long serialVersionUID = -3829104710238475611L;


		private final DossierEntry entry;
		private final Color baseBackground;


		EntryRow(final DossierEntry entry, final boolean alternate){
			this.entry = entry;
			this.baseBackground = (alternate? ROW_BACKGROUND_ALT: SECTION_BACKGROUND);

			// Four columns: proof-status dot, label, value, evidence badge.
			setLayout(new MigLayout("ins 4 10 4 10,gapx 6,fillx",
				"[8!][" + LABEL_WIDTH + "!][grow,fill][]", "[]0[]"));
			setBackground(baseBackground);

			// Proof-status dot: colored square, nullable.
			if(entry.hasProofStatus()){
				final JLabel dot = new JLabel();
				dot.setOpaque(true);
				dot.setBackground(entry.proofStatus().color());
				dot.setBorder(BorderFactory.createLineBorder(
					entry.proofStatus().color().darker()));
				dot.setPreferredSize(new Dimension(8, 8));
				dot.setToolTipText("Proof status: " + entry.proofStatus().displayName());
				add(dot, "aligny center");
			}
			else{
				add(new JLabel(), "aligny center");
			}

			final JLabel labelLabel = new JLabel(entry.label());
			labelLabel.setFont(LABEL_FONT);
			labelLabel.setForeground(LABEL_COLOR);

			final JLabel valueLabel = new JLabel(entry.value());
			valueLabel.setFont(VALUE_FONT);
			valueLabel.setForeground(entry.isEditable()? LINK_COLOR: VALUE_COLOR);

			final JLabel subtitleLabel = new JLabel(entry.subtitle());
			subtitleLabel.setFont(SUBTITLE_FONT);
			subtitleLabel.setForeground(SUBTITLE_COLOR);
			subtitleLabel.setVisible(entry.hasSubtitle());

			final JLabel evidenceLabel = new JLabel(entry.evidenceBadge());
			evidenceLabel.setFont(EVIDENCE_FONT);
			evidenceLabel.setForeground(EVIDENCE_COLOR);
			evidenceLabel.setVisible(entry.hasEvidenceBadge());

			add(labelLabel);
			add(valueLabel, "growx,wrap");
			if(entry.hasSubtitle())
				add(subtitleLabel, "cell 2 1,growx,wrap");
			add(evidenceLabel, "cell 3 0 1 2,aligny center");

			if(entry.isEditable()){
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				addMouseListener(new MouseAdapter(){ /* ... */ });
				attachRecursively(this, this);
			}
		}


		/** Renders a NAME_HEADER row: a small badge with the name type. */
		private void buildNameHeader(){
			setLayout(new MigLayout("ins 6 10 2 10,gapx 4,fillx", "[]", "[]"));
			setBackground(baseBackground);

			final JLabel badge = new JLabel(StringUtils.SPACE + entry.value() + StringUtils.SPACE);
			badge.setFont(BADGE_FONT);
			badge.setForeground(LABEL_COLOR);
			badge.setOpaque(true);
			badge.setBackground(BADGE_BACKGROUND);
			badge.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));

			add(badge);

			if(entry.isEditable()){
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				attachRecursively(this, this);
			}
		}


		/** Renders a NORMAL or NAME_PART row. */
		private void buildStandardRow(){
			final String leftInset = (entry.isNamePart()? "ins 2 " + (10 + PART_INDENT) + " 2 10": "ins 4 10 4 10");
			setLayout(new MigLayout(leftInset + ",gapx 6,fillx",
				"[" + LABEL_WIDTH + "!][grow,fill][]", "[]0[]"));
			setBackground(baseBackground);

			final JLabel labelLabel = new JLabel(entry.label());
			labelLabel.setFont(LABEL_FONT);
			labelLabel.setForeground(LABEL_COLOR);

			final JLabel valueLabel = new JLabel(entry.value());
			valueLabel.setFont(VALUE_FONT);
			valueLabel.setForeground(entry.isEditable()? LINK_COLOR: VALUE_COLOR);

			final JLabel subtitleLabel = new JLabel(entry.subtitle());
			subtitleLabel.setFont(SUBTITLE_FONT);
			subtitleLabel.setForeground(SUBTITLE_COLOR);
			subtitleLabel.setVisible(entry.hasSubtitle());

			final JLabel evidenceLabel = new JLabel(entry.evidenceBadge());
			evidenceLabel.setFont(EVIDENCE_FONT);
			evidenceLabel.setForeground(EVIDENCE_COLOR);
			evidenceLabel.setVisible(entry.hasEvidenceBadge());

			add(labelLabel);
			add(valueLabel, "growx,wrap");
			if(entry.hasSubtitle())
				add(subtitleLabel, "cell 1 1,growx,wrap");
			add(evidenceLabel, "cell 2 0 1 2,aligny center");

			if(entry.isEditable()){
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				addMouseListener(new MouseAdapter(){
					@Override
					public void mouseClicked(final MouseEvent e){
						if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
							openEditDialog();
					}

					@Override
					public void mouseEntered(final MouseEvent e){
						highlight(ROW_BACKGROUND_HOVER);
					}

					@Override
					public void mouseExited(final MouseEvent e){
						highlight(baseBackground);
					}
				});
				attachRecursively(this, this);
			}
		}

		private void highlight(final Color color){
			setBackground(color);

			for(final Component c : getComponents())
				if(c instanceof JLabel)
					c.setBackground(color);

			repaint();
		}

		/**
		 * Recursively attaches the mouse listener of the parent row to
		 * every child component, so that hovering or double-clicking on
		 * the labels is handled by the same row.
		 */
		private void attachRecursively(final Component component, final EntryRow row){
			component.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
						openEditDialog();
				}

				@Override
				public void mouseEntered(final MouseEvent e){
					highlight(ROW_BACKGROUND_HOVER);
				}

				@Override
				public void mouseExited(final MouseEvent e){
					// Only clear the highlight when leaving the row as a
					// whole, not when moving between children.
					final Point p = SwingUtilities.convertPoint(component, e.getPoint(), row);
					if(p.x < 0 || p.y < 0 || p.x >= row.getWidth() || p.y >= row.getHeight())
						highlight(baseBackground);
				}
			});
			if(component instanceof Container container)
				for(final Component child : container.getComponents())
					attachRecursively(child, row);
		}

		private void openEditDialog(){
			final FLEFRecord record = entry.editRecord();
			if(record == null)
				return;
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
			if(handler == null)
				return;

			final Window owner = SwingUtilities.getWindowAncestor(IndividualDossierPanel.this);
			final BaseRecordDialog dialog = handler.createEditDialog(owner, model, record);
			dialog.setVisible(true);
			if(dialog.isSaved())
				refresh();
		}
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String modelUri = "/tests/TGMZ.flef";
		final String individualId = "I1";

		final String content;
		try(final InputStream is = IndividualDossierPanel.class.getResourceAsStream(modelUri)){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final IndividualDossierPanel panel = new IndividualDossierPanel(model);
			panel.setIndividual(individualId);

			final JFrame frame = new JFrame("Individual Dossier");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(420, 800);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
