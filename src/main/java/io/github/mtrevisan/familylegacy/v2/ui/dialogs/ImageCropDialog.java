package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;


public class ImageCropDialog extends JDialog{
	private final BufferedImage image;
	private Rectangle cropRect;
	private final CropPanel cropPanel;

	public ImageCropDialog(Dialog parent, BufferedImage image){
		super(parent, "Select Crop Area", true);
		this.image = image;
		cropPanel = new CropPanel(image);
		initComponents();
		pack();
		setSize(600, 500);
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);

		add(cropPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		okBtn.addActionListener(e -> {
			cropRect = cropPanel.getCropRect();
			dispose();
		});
		cancelBtn.addActionListener(e -> {
			cropRect = null;
			dispose();
		});
	}

	public Rectangle getCrop(){
		return cropRect;
	}

	private static class CropPanel extends JPanel{
		private final BufferedImage image;
		private final int imgWidth;
		private final int imgHeight;
		private Rectangle rect;
		private Point start;
		private boolean drawing;

		public CropPanel(BufferedImage image){
			this.image = image;
			this.imgWidth = image.getWidth();
			this.imgHeight = image.getHeight();
			setPreferredSize(new Dimension(500, 400));
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

			MouseAdapter adapter = new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e){
					start = e.getPoint();
					rect = null;
					drawing = true;
					repaint();
				}

				@Override
				public void mouseDragged(MouseEvent e){
					if(start != null && drawing){
						int x = Math.min(start.x, e.getX());
						int y = Math.min(start.y, e.getY());
						int w = Math.abs(e.getX() - start.x);
						int h = Math.abs(e.getY() - start.y);
						rect = new Rectangle(x, y, w, h);
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e){
					drawing = false;
				}
			};
			addMouseListener(adapter);
			addMouseMotionListener(adapter);
		}

		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;

			int panelWidth = getWidth();
			int panelHeight = getHeight();
			double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
			int scaledW = (int)(imgWidth * scale);
			int scaledH = (int)(imgHeight * scale);
			int x = (panelWidth - scaledW) / 2;
			int y = (panelHeight - scaledH) / 2;
			g2.drawImage(image, x, y, scaledW, scaledH, null);

			if(rect != null){
				g2.setColor(Color.RED);
				g2.drawRect(rect.x, rect.y, rect.width, rect.height);
				g2.setColor(new Color(255, 0, 0, 50));
				g2.fillRect(rect.x, rect.y, rect.width, rect.height);
			}
		}

		public Rectangle getCropRect(){
			if(rect == null) return null;
			int panelWidth = getWidth();
			int panelHeight = getHeight();
			double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
			int scaledW = (int)(imgWidth * scale);
			int scaledH = (int)(imgHeight * scale);
			int offsetX = (panelWidth - scaledW) / 2;
			int offsetY = (panelHeight - scaledH) / 2;

			int imgX = (int)((rect.x - offsetX) / scale);
			int imgY = (int)((rect.y - offsetY) / scale);
			int imgW = (int)(rect.width / scale);
			int imgH = (int)(rect.height / scale);

			imgX = Math.clamp(imgX, 0, imgWidth - 1);
			imgY = Math.clamp(imgY, 0, imgHeight - 1);
			imgW = Math.min(imgW, imgWidth - imgX);
			imgH = Math.min(imgH, imgHeight - imgY);
			return new Rectangle(imgX, imgY, imgW, imgH);
		}
	}
}

