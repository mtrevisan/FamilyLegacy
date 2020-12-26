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
import io.github.mtrevisan.familylegacy.ui.utilities.ImageDrawer;
import io.github.mtrevisan.familylegacy.ui.utilities.ScaledImageLabel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringJoiner;


//https://github.com/wzhwcp/CutOutPicture
public class CutoutDialog extends JDialog{

	private static final double DATE_HEIGHT = 17.;
	private static final double DATE_ASPECT_RATIO = 270 / 248.;
	private static final Dimension DATE_SIZE = new Dimension((int)(DATE_HEIGHT / DATE_ASPECT_RATIO), (int)DATE_HEIGHT);


	private final ScaledImageLabel imageHolder = new ScaledImageLabel(ImageDrawer.ALIGNMENT_X_CENTER, ImageDrawer.ALIGNMENT_Y_TOP);
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private Runnable onCloseGracefully;

	//cutout coordinates:
	private int x1, y1;
	private int x2, y2;


	public CutoutDialog(final Frame parent){
		super(parent, true);

		initComponents();
	}

	private void initComponents(){
		setTitle("Define cutout");

		final MyMouseListener listener = new MyMouseListener();
		imageHolder.addMouseListener(listener);
		imageHolder.addMouseMotionListener(listener);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("debug", "[grow]", "[top]"));
		add(imageHolder, "grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final String file, final Runnable onCloseGracefully) throws IOException{
		this.onCloseGracefully = onCloseGracefully;

		try(final ImageInputStream input = ImageIO.createImageInputStream(new File(file))){
			//get the reader
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if(!readers.hasNext())
				throw new IllegalArgumentException("No reader for: " + file);

			final ImageReader reader = readers.next();
			try{
				reader.setInput(input);

				final BufferedImage image = reader.read(0);

				imageHolder.setIcon(new ImageIcon(image));
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

			graphics2D.setColor(Color.RED);
			x2 = limit(x2, imageHolder.getWidth() - 1);
			y2 = limit(y2, imageHolder.getHeight() - 1);
			final int x = Math.min(x1, x2);
			final int y = Math.min(y1, y2);
			final int width = Math.abs(x1 - x2);
			final int height = Math.abs(y1 - y2);
			graphics2D.drawRect(x, y, width, height);

			graphics2D.dispose();
		}
	}

	private int limit(final int value, final int max){
		return Math.min(Math.max(value, 0), max);
	}

	private void setStartPoint(final int x, final int y){
		x1 = x;
		y1 = y;
	}

	private void setEndPoint(final int x, final int y){
		x2 = x;
		y2 = y;
	}

	class MyMouseListener extends MouseAdapter{

		@Override
		public void mousePressed(final MouseEvent e){
			if(SwingUtilities.isLeftMouseButton(e))
				setStartPoint(e.getX(), e.getY());
		}

		@Override
		public void mouseDragged(final MouseEvent e){
			if(SwingUtilities.isLeftMouseButton(e)){
				setEndPoint(e.getX(), e.getY());

				repaint();
			}
		}
	}

	public String getCutoutCoordinates(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(Integer.toString(x1));
		sj.add(Integer.toString(y1));
		sj.add(Integer.toString(x2));
		sj.add(Integer.toString(y2));
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
			dialog.setSize(500, 430);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
