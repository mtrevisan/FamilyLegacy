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
package io.github.mtrevisan.familylegacy.ui.tools.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReportText;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Modal dialog that shows the consanguinity report: the couples whose partners are related and the inbred individuals, with the
 * coefficients of Wright. The report itself is read-only, but every reference in it (the partners, the common ancestors, the
 * relationships that unite the couple, the children, the inbred individuals and their identifiers) is a link: a click opens the editor
 * of that record, and the text is written again afterwards, so that a name that was corrected is the corrected one.
 */
public class ConsanguinityReportDialog extends JDialog{

	private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);
	/** The size of the text of the report: what is more scrolls. */
	static final int PREFERRED_WIDTH = 760;
	static final int PREFERRED_TEXT_HEIGHT = 380;

	private final ConsanguinityReport report;
	private final Function<String, String> names;
	private final FLEFModel model;

	private final JTextPane area = new JTextPane();
	/** Where the links are in the text: start, end (excluded) and identifier of the record. */
	private final List<Link> links = new ArrayList<>();


	private record Link(int start, int end, String recordId){}


	/**
	 * @param owner  the parent window; may be {@code null}
	 * @param model  the model the records of the report are in: the links open their editors
	 * @param report the report to show
	 * @param names  the name to show of an individual
	 */
	public ConsanguinityReportDialog(final Window owner, final FLEFModel model, final ConsanguinityReport report,
			final Function<String, String> names){
		super(owner, I18N.t("dialog.consanguinity_report.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);

		if(report == null)
			throw new IllegalArgumentException("Report must not be null");

		this.report = report;
		this.names = names;
		this.model = model;

		initComponents();

		//the size is final before the dialog is centered: the minimum one is set before packing, not after
		setMinimumSize(new Dimension(700, 420));
		pack();
		setLocationRelativeTo(owner);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}


	/** @return	The text of the report as it is shown. */
	public String reportText(){
		//the text of the document, whose positions are the ones of the links: getText() writes the lines as the system does
		try{
			return area.getDocument().getText(0, area.getDocument().getLength());
		}
		catch(final BadLocationException ignored){
			return "";
		}
	}

	/**
	 * @param offset	A position in the text of the report.
	 * @return	The identifier of the record that the text at that position is a link to; {@code null} if it is not a link.
	 */
	public String recordIdAt(final int offset){
		for(final Link link : links)
			if(offset >= link.start() && offset < link.end())
				return link.recordId();
		return null;
	}


	private void initComponents(){
		final JPanel main = new JPanel(new MigLayout("ins 12,fill", "[grow]", "[]8[grow]12[]"));

		final JLabel header = new JLabel(buildHeaderText());
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		main.add(header, "growx,wrap");

		area.setEditable(false);
		area.setFont(MONO_FONT);
		area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		fillText();
		area.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e))
					openLinkAt(e);
			}
		});
		area.addMouseMotionListener(new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				area.setCursor(recordIdAt(area.viewToModel2D(e.getPoint())) != null
					? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
					: Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			}
		});
		area.setToolTipText(I18N.t("dialog.consanguinity_report.links.tooltip"));
		final JScrollPane scroll = new JScrollPane(area);
		//the text area asks for the height of the whole report: the dialog has a size of its own, and the report scrolls
		scroll.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_TEXT_HEIGHT));
		main.add(scroll, "grow,push,wrap");

		final JButton closeButton = new JButton(I18N.t("button.close"));
		closeButton.addActionListener(e -> dispose());
		final JPanel footer = new JPanel(new MigLayout("ins 0", "[grow][]", "[]"));
		footer.add(new JLabel(), "growx");
		footer.add(closeButton);
		main.add(footer, "growx");

		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);

		getRootPane().setDefaultButton(closeButton);
	}

	/** Writes the report in the text area, the references as links. */
	private void fillText(){
		links.clear();
		area.setText("");
		final Color linkColor = linkColor();
		final var document = area.getStyledDocument();
		final SimpleAttributeSet plain = new SimpleAttributeSet();
		final SimpleAttributeSet link = new SimpleAttributeSet();
		StyleConstants.setForeground(link, linkColor);
		StyleConstants.setUnderline(link, true);
		try{
			for(final ConsanguinityReportText.Segment segment : ConsanguinityReportText.segments(report, names)){
				final int start = document.getLength();
				//a reference to a record that is no longer in the model is written, not linked
				final boolean linked = (segment.isReference() && model != null && model.hasRecord(segment.recordId()));
				document.insertString(start, segment.text(), (linked? link: plain));
				if(linked)
					links.add(new Link(start, start + segment.text().length(), segment.recordId()));
			}
		}
		catch(final BadLocationException ignored){
			//the offsets are the ones of the document just written: it cannot happen
		}
		area.setCaretPosition(0);
	}

	private void openLinkAt(final MouseEvent event){
		final String recordId = recordIdAt(area.viewToModel2D(event.getPoint()));
		if(recordId == null || model == null || !model.hasRecord(recordId))
			return;

		ToolDialogs.openRecordEditor(this, model, model.getRecordById(recordId));
		//the editor may have changed a name or removed a record
		fillText();
	}

	private static Color linkColor(){
		final Color color = UIManager.getColor("Component.linkColor");
		return (color != null? color: new Color(0x0645AD));
	}

	private String buildHeaderText(){
		return I18N.tf("dialog.consanguinity_report.header", String.valueOf(report.unions().size()),
			String.valueOf(report.inbredIndividuals().size()));
	}

}
