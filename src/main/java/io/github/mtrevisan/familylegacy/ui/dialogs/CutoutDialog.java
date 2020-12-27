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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringJoiner;


//https://github.com/wzhwcp/CutOutPicture
//https://www.onooks.com/zoom-and-pan-in-java-using-affinetransform/
public class CutoutDialog extends JDialog{

	private static final int MIN_ZOOM_LEVEL = -20;
	private static final int MAX_ZOOM_LEVEL = 10;
	private static final double ZOOM_MULTIPLICATION_FACTOR = 1.2;
	private static final double INITIAL_ZOOM = 1.;


	private BufferedImage image;
	private final JLabel imageHolder = new JLabel();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private Runnable onCloseGracefully;

	private boolean initialized = false;
	private final AffineTransform coordTransform = new AffineTransform();
	private Point cutoutStartPoint;
	private Point cutoutEndPoint;
	private Point dragStartPoint;
	private Point dragEndPoint;
	private int zoomLevel = 0;


	public CutoutDialog(final Frame parent){
		super(parent, true);

		initComponents();
	}

	private void initComponents(){
		setTitle("Define cutout");

		final MyMouseListener listener = new MyMouseListener();
		imageHolder.addMouseListener(listener);
		imageHolder.addMouseMotionListener(listener);
		imageHolder.addMouseWheelListener(listener);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("debug", "[grow,shrink]", "[grow,shrink][][]"));
		add(imageHolder, "grow,shrink,wrap");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final String file, final Runnable onCloseGracefully) throws IOException{
		this.onCloseGracefully = onCloseGracefully;

		try(final ImageInputStream input = ImageIO.createImageInputStream(new File(file))){
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if(!readers.hasNext())
				throw new IllegalArgumentException("No reader for " + file);

			final ImageReader reader = readers.next();
			try{
				reader.setInput(input);

				image = reader.read(0);
			}
			finally{
				reader.dispose();
			}
		}

		repaint();
	}

	@Override
	public void paint(final Graphics g){
		super.paint(g);

		final Graphics g2 = imageHolder.getGraphics();
		if(g2 instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g2.create();

			//image:
			if(!initialized){
				final int x = (int)(imageHolder.getWidth() - (image.getWidth() * INITIAL_ZOOM)) / 2;
				final int y = (int)(imageHolder.getHeight() - (image.getHeight() * INITIAL_ZOOM)) / 2;
				coordTransform.translate(x, y);
				coordTransform.scale(INITIAL_ZOOM, INITIAL_ZOOM);

				initialized = true;
			}
			graphics2D.setTransform(coordTransform);
			graphics2D.drawImage(image, 0, 0, this);

			//cutout box:
			if(cutoutEndPoint != null){
				graphics2D.setColor(Color.RED);
				cutoutEndPoint.x = limit(cutoutEndPoint.x, imageHolder.getWidth() - 1);
				cutoutEndPoint.y = limit(cutoutEndPoint.y, imageHolder.getHeight() - 1);
				final double zoomFactor = coordTransform.getScaleX();
				final int x = (int)((Math.min(cutoutStartPoint.x, cutoutEndPoint.x)) / zoomFactor);
				final int y = (int)((Math.min(cutoutStartPoint.y, cutoutEndPoint.y)) / zoomFactor);
				final int width = (int)(Math.abs(cutoutEndPoint.x - cutoutStartPoint.x) * zoomFactor);
				final int height = (int)(Math.abs(cutoutEndPoint.y - cutoutStartPoint.y) * zoomFactor);
				graphics2D.drawRect(x, y, width, height);
			}

			graphics2D.dispose();
		}
	}

	private int limit(final int value, final int max){
		return Math.min(Math.max(value, 0), max);
	}

	class MyMouseListener extends MouseAdapter implements MouseWheelListener{

		@Override
		public void mousePressed(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					dragStartPoint = evt.getPoint();
					dragEndPoint = null;
				}
				else{
					final int x = (int)(evt.getX() - coordTransform.getTranslateX());
					final int y = (int)(evt.getY() - coordTransform.getTranslateY());
					cutoutStartPoint = new Point(x, y);
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent evt){
			if(SwingUtilities.isLeftMouseButton(evt)){
				if(evt.isControlDown()){
					//pan
					try{
						dragEndPoint = evt.getPoint();
						final Point2D dragStart = transformPoint(dragStartPoint);
						final Point2D dragEnd = transformPoint(dragEndPoint);
						final double dx = dragEnd.getX() - dragStart.getX();
						final double dy = dragEnd.getY() - dragStart.getY();
						coordTransform.translate(dx, dy);
						dragStartPoint = dragEndPoint;
						dragEndPoint = null;
					}
					catch(final NoninvertibleTransformException ignored){}
				}
				else{
					//cutout start point
//					final double zoomFactor = coordTransform.getScaleX();
					final int x = (int)(evt.getX() - coordTransform.getTranslateX());
					final int y = (int)(evt.getY() - coordTransform.getTranslateY());
					cutoutEndPoint = new Point(x, y);
				}

				repaint();
			}
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent evt){
			if(evt.isControlDown()){
				//zoom
				final int wheelRotation = evt.getWheelRotation();
				double zoomFactor = 0.;
				if(wheelRotation > 0 && zoomLevel < MAX_ZOOM_LEVEL){
					zoomLevel ++;
					zoomFactor = 1. / ZOOM_MULTIPLICATION_FACTOR;
				}
				else if(wheelRotation < 0 && zoomLevel > MIN_ZOOM_LEVEL){
					zoomLevel --;
					zoomFactor = ZOOM_MULTIPLICATION_FACTOR;
				}

				if(zoomFactor != 0.){
					try{
						final Point p = evt.getPoint();
						final Point2D p1 = transformPoint(p);
						coordTransform.scale(zoomFactor, zoomFactor);
						final Point2D p2 = transformPoint(p);
						coordTransform.translate(p2.getX() - p1.getX(), p2.getY() - p1.getY());

						repaint();
					}
					catch(final NoninvertibleTransformException ignored){}
				}
			}
		}

		private Point2D transformPoint(final Point point) throws NoninvertibleTransformException{
			final AffineTransform inverse = coordTransform.createInverse();
			final Point2D transformedPoint = new Point2D.Float();
			inverse.transform(point, transformedPoint);
			return transformedPoint;
		}

	}

	public String getCutoutCoordinates(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(Integer.toString(cutoutStartPoint.x));
		sj.add(Integer.toString(cutoutStartPoint.y));
		sj.add(Integer.toString(cutoutEndPoint.x));
		sj.add(Integer.toString(cutoutEndPoint.y));
		return sj.toString();
	}

	private void okAction(){
		//TODO
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.5.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();

		String file = "C:\\\\Users/mauro/Documents/My Genealogy Projects/Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)/Photos/Tosatto Luigia Maria.psd";
//		String file = "C:\\\\Users/mauro/Documents/My Genealogy Projects/Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)/Photos/Trevisan Mauro Ospitalization 20150304-10.jpg";

		EventQueue.invokeLater(() -> {
			final CutoutDialog dialog = new CutoutDialog(null);
			try{
				dialog.loadData(file, null);
			}
			catch(final IOException e){
				e.printStackTrace();
			}

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 480);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
