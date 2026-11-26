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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Supplier;


/**
 * A guided tour: a small window, not modal, that explains one step after the other and outlines, over the real window of the
 * application, the part (usually a menu) the step is about.
 * <p>
 * Esc, like closing the window, ends the tour; so does the last step.
 */
public final class GuidedTourDialog extends JDialog{

	/** The gap between the outlined part and the tour window, and from the borders of the screen. */
	private static final int GAP = 10;
	private static final Color HIGHLIGHT = new Color(0xF5, 0x9E, 0x0B);


	/**
	 * One step of the tour.
	 *
	 * @param title	the title of the step.
	 * @param text	the text of the step (HTML).
	 * @param target	what to outline for the step, evaluated when the step is shown (it may return {@code null}, or a component that is
	 * 	not visible: nothing is outlined then); {@code null} for a step about nothing in particular.
	 */
	public record Step(String title, String text, Supplier<? extends Component> target){}


	private static GuidedTourDialog current;

	private final JFrame owner;
	private final List<Step> steps;
	private final Highlight highlight = new Highlight();
	private final Component previousGlassPane;
	private final ComponentAdapter ownerListener = new ComponentAdapter(){
		@Override
		public void componentResized(final ComponentEvent e){
			SwingUtilities.invokeLater(GuidedTourDialog.this::refresh);
		}

		@Override
		public void componentMoved(final ComponentEvent e){
			SwingUtilities.invokeLater(GuidedTourDialog.this::refresh);
		}
	};

	private final JLabel titleLabel = new JLabel();
	private final JLabel textLabel = new JLabel();
	private final JLabel progressLabel = new JLabel();
	private final JButton backButton = new JButton(I18N.t("dialog.tour.back"));
	private final JButton nextButton = new JButton();
	private int index;


	private GuidedTourDialog(final JFrame owner, final List<Step> steps, final Runnable onClosed){
		super(owner, I18N.t("dialog.tour.title"), ModalityType.MODELESS);

		this.owner = owner;
		this.steps = steps;
		previousGlassPane = owner.getGlassPane();

		GUIHelper.installEscapeToClose(this);

		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 3.f));
		textLabel.setVerticalAlignment(JLabel.TOP);
		progressLabel.setEnabled(false);

		final JButton skipButton = new JButton(I18N.t("dialog.tour.skip"));
		skipButton.addActionListener(e -> dispose());
		backButton.addActionListener(e -> show(index - 1));
		nextButton.addActionListener(e -> {
			if(index == steps.size() - 1)
				dispose();
			else
				show(index + 1);
		});

		final JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
		content.add(titleLabel, BorderLayout.NORTH);
		content.add(textLabel, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		buttons.add(skipButton);
		buttons.add(backButton);
		buttons.add(nextButton);
		final JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		footer.add(progressLabel, BorderLayout.WEST);
		footer.add(buttons, BorderLayout.EAST);
		content.add(footer, BorderLayout.SOUTH);
		setContentPane(content);
		getRootPane().setDefaultButton(nextButton);

		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(final WindowEvent e){
				owner.removeComponentListener(ownerListener);
				owner.setGlassPane(previousGlassPane);
				previousGlassPane.setVisible(false);
				owner.repaint();
				if(current == GuidedTourDialog.this)
					current = null;
				if(onClosed != null)
					onClosed.run();
			}
		});

		owner.setGlassPane(highlight);
		owner.addComponentListener(ownerListener);
		show(0);
	}


	/**
	 * Starts the tour (the one that is already running is closed first); the tour ends when the last step is left, or by Esc.
	 *
	 * @param owner	the main window, where the parts are outlined.
	 * @param steps	the steps.
	 * @param onClosed	what to do when the tour ends, for any reason (may be {@code null}).
	 */
	public static void show(final JFrame owner, final List<Step> steps, final Runnable onClosed){
		if(steps.isEmpty())
			return;

		if(current != null)
			current.dispose();
		current = new GuidedTourDialog(owner, steps, onClosed);
		current.setVisible(true);
	}

	private void show(final int step){
		index = Math.max(0, Math.min(step, steps.size() - 1));
		final Step shown = steps.get(index);
		titleLabel.setText(shown.title());
		textLabel.setText(shown.text());
		progressLabel.setText(I18N.tf("dialog.tour.progress", index + 1, steps.size()));
		backButton.setEnabled(index > 0);
		nextButton.setText(I18N.t(index == steps.size() - 1? "dialog.tour.finish": "dialog.tour.next"));
		pack();
		refresh();
		nextButton.requestFocusInWindow();
	}

	/** Outlines the part of the current step and places the window next to it. */
	private void refresh(){
		if(!isDisplayable())
			return;

		final Supplier<? extends Component> supplier = steps.get(index).target();
		final Component target = (supplier != null? supplier.get(): null);
		final Rectangle area = (target != null && target.isShowing()
			? SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), highlight)
			: null);
		highlight.setArea(area);
		highlight.setVisible(true);
		highlight.repaint();

		final Rectangle frameBounds = owner.getBounds();
		final Point location;
		if(area != null){
			final Point below = new Point(area.x, area.y + area.height + GAP);
			SwingUtilities.convertPointToScreen(below, highlight);
			location = below;
		}
		else
			location = new Point(frameBounds.x + (frameBounds.width - getWidth()) / 2, frameBounds.y + (frameBounds.height - getHeight()) / 2);
		//kept inside the window of the application
		location.x = Math.max(frameBounds.x + GAP, Math.min(location.x, frameBounds.x + frameBounds.width - getWidth() - GAP));
		location.y = Math.max(frameBounds.y + GAP, Math.min(location.y, frameBounds.y + frameBounds.height - getHeight() - GAP));
		setLocation(location);
	}


	/** The glass pane of the application window: it draws a frame around the part of the step, and lets every mouse event through. */
	private static final class Highlight extends JComponent{

		private static final int PADDING = 3;

		private Rectangle area;


		void setArea(final Rectangle area){
			this.area = area;
		}

		@Override
		public boolean contains(final int x, final int y){
			return false;
		}

		@Override
		protected void paintComponent(final Graphics g){
			if(area == null)
				return;

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(HIGHLIGHT.getRed(), HIGHLIGHT.getGreen(), HIGHLIGHT.getBlue(), 50));
				g2.fillRoundRect(area.x - PADDING, area.y - PADDING, area.width + 2 * PADDING, area.height + 2 * PADDING, 10, 10);
				g2.setColor(HIGHLIGHT);
				g2.setStroke(new BasicStroke(3.f));
				g2.drawRoundRect(area.x - PADDING, area.y - PADDING, area.width + 2 * PADDING, area.height + 2 * PADDING, 10, 10);
			}
			finally{
				g2.dispose();
			}
		}

	}

}
