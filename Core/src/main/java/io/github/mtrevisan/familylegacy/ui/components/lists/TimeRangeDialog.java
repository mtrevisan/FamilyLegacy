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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;


// TODO test
/**
 * Dialog for selecting a time range within an audio document with waveform visualization.
 */
public class TimeRangeDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(TimeRangeDialog.class);

	private JTextField startField;
	private JTextField endField;
	private WaveformPanel waveformPanel;
	private JScrollPane waveformScrollPane;

	private boolean isSaved;
	private long totalDurationMicros;


	public static TimeRangeDialog create(final Window parent){
		final TimeRangeDialog dialog = new TimeRangeDialog(parent);
		dialog.initialize();

		return dialog;
	}


	private TimeRangeDialog(final Window parent){
		super(parent, ModalityType.APPLICATION_MODAL);
	}


	private void initialize(){
		initComponents();
		initLayout();

		pack();

		setMinimumSize(new Dimension(600, 400));
		setLocationRelativeTo(getOwner());
	}

	private void initComponents(){
		setTitle(I18N.t("dialog.preferred.time_range.title"));

		startField = new JTextField(10);
		endField = new JTextField(10);

		waveformPanel = new WaveformPanel();
		waveformScrollPane = new JScrollPane(waveformPanel);
		waveformScrollPane.setBorder(BorderFactory.createEtchedBorder());
		waveformScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
	}

	private void initLayout(){
		setLayout(new MigLayout("ins 10,hidemode 3,fill,wrap 1", "[grow,fill]", "[grow,push]10[]10[]"));

		add(waveformScrollPane, "grow,push");

		final JPanel contentPanel = GUIHelper.createLabelFieldPanel(0, "[grow,fill]");
		GUIHelper.addComponent(contentPanel, new JLabel(I18N.t("dialog.preferred.time_range.start")));
		GUIHelper.addComponent(contentPanel, startField);
		GUIHelper.addComponent(contentPanel, new JLabel(I18N.t("dialog.preferred.time_range.end")));
		GUIHelper.addComponent(contentPanel, endField);
		add(contentPanel, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			this::save,
			() -> setVisible(false));
		add(buttonPanel, "growx");
	}

	public void loadData(final String uri, final String start, final String end){
		isSaved = false;
		startField.setText(start != null ? start : StringUtils.EMPTY);
		endField.setText(end != null ? end : StringUtils.EMPTY);

		loadAudioWaveform(uri);
	}

	private void loadAudioWaveform(final String uri){
		if(StringUtils.isBlank(uri)){
			waveformPanel.setAudioData(null, 0);
			return;
		}

		try{
			final File file = new File(uri);
			if(!file.exists()){
				waveformPanel.setAudioData(null, 0);
				return;
			}

			try(final AudioInputStream audioStream = AudioSystem.getAudioInputStream(file)){
				final var format = audioStream.getFormat();
				final long frames = audioStream.getFrameLength();
				totalDurationMicros = (frames != javax.sound.sampled.AudioSystem.NOT_SPECIFIED && format.getFrameRate() > 0)
					? (long)(frames / format.getFrameRate() * 1_000_000.0)
					: 0L;

				final byte[] audioBytes = audioStream.readAllBytes();
				waveformPanel.setAudioData(audioBytes, format.getSampleSizeInBits());
			}
		}
		catch(final Exception e){
			LOGGER.error("Error loading audio waveform", e);
			waveformPanel.setAudioData(null, 0);
		}
	}

	public void save(){
		isSaved = true;

		setVisible(false);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public String getStart(){
		return startField.getText().trim();
	}

	public String getEnd(){
		return endField.getText().trim();
	}

	/**
	 * Custom panel for rendering audio waveform and selecting time regions.
	 */
	private static class WaveformPanel extends JPanel{

		private byte[] audioData;
		private int sampleSizeInBits;
		private Point startDrag;
		private Point endDrag;

		public WaveformPanel(){
			setBackground(Color.WHITE);

			final MouseAdapter mouseAdapter = new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent e){
					startDrag = e.getPoint();
					endDrag = startDrag;
					repaint();
				}

				@Override
				public void mouseDragged(final MouseEvent e){
					endDrag = e.getPoint();
					repaint();
				}
			};
			addMouseListener(mouseAdapter);
			addMouseMotionListener(mouseAdapter);
		}

		public void setAudioData(final byte[] audioData, final int sampleSizeInBits){
			this.audioData = audioData;
			this.sampleSizeInBits = sampleSizeInBits;
			this.startDrag = null;
			this.endDrag = null;
			repaint();
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(audioData == null || audioData.length == 0){
				g.setColor(Color.GRAY);
				g.drawString("No waveform available", 20, 30);
				return;
			}

			final Graphics2D g2 = (Graphics2D)g.create();
			final int width = getWidth();
			final int height = getHeight();
			final int centerY = height / 2;

			g2.setColor(new Color(100, 150, 220));
			final int bytesPerSample = Math.max(1, sampleSizeInBits / 8);
			final int step = Math.max(1, audioData.length / width / bytesPerSample);

			for(int x = 0; x < width; x++){
				final int index = x * step * bytesPerSample;
				if(index >= audioData.length)
					break;

				int sample = 0;
				if(bytesPerSample == 2){
					sample = (audioData[index + 1] << 8) | (audioData[index] & 0xFF);
				}
				else{
					sample = audioData[index];
				}

				final int sampleHeight = (int)((double)sample / 32768. * centerY);
				g2.drawLine(x, centerY - sampleHeight, x, centerY + sampleHeight);
			}

			if(startDrag != null && endDrag != null){
				final int x = Math.min(startDrag.x, endDrag.x);
				final int widthSel = Math.abs(startDrag.x - endDrag.x);
				g2.setColor(new Color(0, 120, 215, 60));
				g2.fillRect(x, 0, widthSel, height);
				g2.setColor(new Color(0, 120, 215, 200));
				g2.setStroke(new BasicStroke(1.5f));
				g2.drawRect(x, 0, widthSel, height);
			}

			g2.dispose();
		}
	}

}
