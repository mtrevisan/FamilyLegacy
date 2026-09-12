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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree;

import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.Serial;
import java.util.List;
import java.util.Locale;


/**
 * Modal dialog that reports the pedigree collapses detected in the current
 * ancestor tree.
 * <p>
 * The dialog shows one section per collapse, with the individual name, the
 * number of occurrences, and the list of paths through which the individual
 * is reached from the root. Paths are rendered in two forms: the compact
 * {@code F / M} code and a human-readable translation
 * ({@code father > mother}).
 * <p>
 * The dialog is read-only: it does not modify the model nor the tree.
 */
public class PedigreeCollapseDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8823418394192481052L;


	private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);


	private final List<PedigreeCollapse> collapses;


	/**
	 * Constructor.
	 *
	 * @param owner      the parent window; may be {@code null}
	 * @param collapses  the collapse list (must not be {@code null})
	 */
	public PedigreeCollapseDialog(final Window owner, final List<PedigreeCollapse> collapses){
		super(owner, "Pedigree collapse report", ModalityType.APPLICATION_MODAL);
		if(collapses == null)
			throw new IllegalArgumentException("Collapses must not be null");

		this.collapses = List.copyOf(collapses);

		initComponents();
		pack();
		setMinimumSize(new Dimension(620, 420));
		setLocationRelativeTo(owner);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}


	private void initComponents(){
		final JPanel main = new JPanel(new MigLayout("ins 12,fill", "[grow]", "[]8[grow]12[]"));

		// Header
		final JLabel header = new JLabel(buildHeaderText());
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		main.add(header, "growx,wrap");

		// Report area
		final JTextArea area = new JTextArea(buildReportText());
		area.setEditable(false);
		area.setFont(MONO_FONT);
		area.setLineWrap(false);
		area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		final JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(BorderFactory.createTitledBorder("Collapses"));
		main.add(scroll, "grow,push,wrap");

		// Footer
		final JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());

		final JPanel footer = new JPanel(new MigLayout("ins 0", "[grow][]", "[]"));
		footer.add(new JLabel(" "), "growx");
		footer.add(closeButton);
		main.add(footer, "growx");

		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);

		getRootPane()
			.setDefaultButton(closeButton);
	}


	/* ======================================================================
	 *                          Formatting
	 * ====================================================================== */

	private String buildHeaderText(){
		if(collapses.isEmpty())
			return "No pedigree collapse detected in the current tree.";
		if(collapses.size() == 1)
			return "1 pedigree collapse detected in the current tree.";
		return collapses.size() + " pedigree collapses detected in the current tree.";
	}

	private String buildReportText(){
		if(collapses.isEmpty())
			return "All ancestors appear exactly once. The tree is fully expanded.";

		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < collapses.size(); i ++){
			final PedigreeCollapse collapse = collapses.get(i);
			if(i > 0)
				sb.append('\n');
			sb.append(String.format(Locale.ROOT, "[%d] %s%n", i + 1, collapse.displayName()));
			sb.append(String.format(Locale.ROOT, "    Occurrences: %d%n", collapse.occurrenceCount()));
			sb.append(String.format(Locale.ROOT, "    Deepest generation reached: %d%n", collapse.maxGeneration()));
			sb.append("    Paths from the root:\n");
			for(final PedigreePath path : collapse.paths())
				sb.append(String.format(Locale.ROOT, "      %-10s  %s%n",
					path.code(), path.describe()));
		}
		return sb.toString();
	}

}
