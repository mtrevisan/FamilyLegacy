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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


/**
 * Modal dialog that lets the user pick a subset of {@link SocialRelationCategory} values.
 * <p>
 * The dialog is intentionally minimal: a checkbox per category, plus
 * "Select all" / "Select none" convenience buttons and the standard
 * OK/Cancel pair. Each row shows the category's display label and a small
 * color swatch that matches the palette used by
 * {@link SocialNodeRenderer#colorForCategory}, so the user immediately
 * recognizes which visual group is being toggled.
 * <p>
 * The static {@link #show(Window, Set)} method runs the dialog modally and
 * returns the chosen set. It returns {@code null} if the user cancels, and
 * an empty set if the user confirms with no categories selected (which
 * means "no category restriction" in the filter model).
 * <p>
 * The dialog does not modify the input set: it works on an internal copy,
 * so the caller's data is never touched.
 */
public final class SocialCategorySelectionDialog extends JDialog{

	private final Map<SocialRelationCategory, JCheckBox> checkboxes = new EnumMap<>(SocialRelationCategory.class);

	private Set<SocialRelationCategory> result;


	private SocialCategorySelectionDialog(final Window owner, final Set<SocialRelationCategory> initial){
		super(owner, "Select categories", ModalityType.APPLICATION_MODAL);

		initComponents(initial);

		pack();

		setLocationRelativeTo(owner);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}


	/* ======================================================================
	 *                          Public entry point
	 * ====================================================================== */

	/**
	 * Shows the dialog modally and returns the chosen set of categories.
	 *
	 * @param owner   the parent window; may be {@code null}
	 * @param initial the initially selected categories; may be {@code null}
	 *                or empty
	 * @return the chosen categories, never {@code null} when confirmed;
	 * {@code null} when the user cancels
	 */
	public static Set<SocialRelationCategory> show(final Window owner, final Set<SocialRelationCategory> initial){
		final SocialCategorySelectionDialog dialog = new SocialCategorySelectionDialog(owner, initial);
		dialog.setVisible(true);

		return dialog.result;
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private void initComponents(final Set<SocialRelationCategory> initial){
		final Set<SocialRelationCategory> current = (initial != null && !initial.isEmpty()
			? EnumSet.copyOf(initial)
			: EnumSet.noneOf(SocialRelationCategory.class));

		final JPanel content = new JPanel(new MigLayout("ins 12,wrap 1", "[grow,fill]", "[]6[]6[]"));
		content.add(new JLabel("Include the following relationship categories:"));
		content.add(buildCategoryPanel(current), "growx");
		content.add(buildButtonsPanel(), "growx, align right");

		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
	}

	private JPanel buildCategoryPanel(final Set<SocialRelationCategory> current){
		final JPanel panel = new JPanel(new MigLayout("ins 4,wrap 1", "[grow,fill]", "[]2[]2[]2[]2[]2[]2[]"));
		panel.setBorder(BorderFactory.createLineBorder(new Color(210, 205, 195)));

		for(final SocialRelationCategory category : SocialRelationCategory.values()){
			final JPanel row = new JPanel(new MigLayout("ins 0", "[]2[]6[grow,fill]", "[]"));
			row.setOpaque(false);

			// Plain checkbox, no text and no icon
			final JCheckBox box = new JCheckBox(StringUtils.EMPTY, current.contains(category));
			box.setOpaque(false);
			checkboxes.put(category, box);

			// Color swatch as a small label, placed between the checkbox and
			// the category name.
			final JLabel swatch = new JLabel(new ColorSwatchIcon(SocialNodeRenderer.colorForCategory(category)));
			swatch.setOpaque(false);

			// Category name.
			final JLabel label = new JLabel(category.getDisplayLabel());

			row.add(box);
			row.add(swatch);
			row.add(label, "growx");

			// Make the swatch and the label clickable: clicking anywhere on the
			// row except the checkbox itself toggles the selection.
			final MouseAdapter toggle = new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					box.setSelected(!box.isSelected());
				}
			};
			swatch.addMouseListener(toggle);
			label.addMouseListener(toggle);

			panel.add(row, "growx");
		}
		return panel;
	}

	private JPanel buildButtonsPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 0", "[][][grow][][]", "[]"));

		final JButton selectAllBtn = new JButton("All");
		selectAllBtn.addActionListener(e -> setAll(true));

		final JButton selectNoneBtn = new JButton("None");
		selectNoneBtn.addActionListener(e -> setAll(false));

		final JButton okBtn = new JButton("OK");
		okBtn.addActionListener(e -> onConfirm());

		final JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(e -> onCancel());

		panel.add(selectAllBtn);
		panel.add(selectNoneBtn);
		panel.add(new JLabel(StringUtils.EMPTY), "growx");
		panel.add(okBtn);
		panel.add(cancelBtn);

		getRootPane()
			.setDefaultButton(okBtn);
		return panel;
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void setAll(final boolean selected){
		for(final JCheckBox box : checkboxes.values())
			box.setSelected(selected);
	}

	private void onConfirm(){
		final Set<SocialRelationCategory> picked = EnumSet.noneOf(SocialRelationCategory.class);
		for(final Map.Entry<SocialRelationCategory, JCheckBox> entry : checkboxes.entrySet())
			if(entry.getValue()
				.isSelected())
				picked.add(entry.getKey());
		result = picked;

		dispose();
	}

	private void onCancel(){
		result = null;

		dispose();
	}


	/* ======================================================================
	 *                          Color swatch icon
	 * ====================================================================== */

	/**
	 * A small square icon filled with the given color, used as the checkbox
	 * icon so that each category row carries a visual hint matching the
	 * palette of the network view.
	 */
	private static final class ColorSwatchIcon implements Icon{

		private static final int SIZE = 12;
		private static final int BORDER = 1;

		private final Color fill;


		ColorSwatchIcon(final Color fill){
			this.fill = fill;
		}

		@Override
		public void paintIcon(final Component c, final Graphics g, final int x, final int y){
			if(!(g instanceof Graphics2D g2))
				return;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(fill);
			g2.fillRect(x, y, SIZE, SIZE);
			g2.setColor(new Color(90, 90, 90, 160));
			g2.drawRect(x, y, SIZE - 1, SIZE - 1);
		}

		@Override
		public int getIconWidth(){
			return SIZE;
		}

		@Override
		public int getIconHeight(){
			return SIZE;
		}
	}

}
