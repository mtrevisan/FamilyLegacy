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
import io.github.mtrevisan.familylegacy.ui.utilities.ScaledImage;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;


//https://github.com/wzhwcp/CutOutPicture
//https://www.onooks.com/zoom-and-pan-in-java-using-affinetransform/
public class CutoutDialog extends JDialog{

	private final ScaledImage imageHolder = new ScaledImage();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private Consumer<CutoutDialog> onCloseGracefully;


	public CutoutDialog(final Frame parent){
		super(parent, true);

		initComponents();
	}

	private void initComponents(){
		setTitle("Define cutout");

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("", "[grow]", "[grow,fill][][]"));
		add(imageHolder, "grow,wrap");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final String file, final Consumer<CutoutDialog> onCloseGracefully) throws IOException{
		this.onCloseGracefully = onCloseGracefully;

		try(final ImageInputStream input = ImageIO.createImageInputStream(new File(file))){
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if(!readers.hasNext())
				throw new IllegalArgumentException("No reader for " + file);

			final ImageReader reader = readers.next();
			try{
				reader.setInput(input);

				final BufferedImage image = reader.read(0);

				imageHolder.setImage(image);
			}
			finally{
				reader.dispose();
			}
		}

		repaint();
	}

	public Point getCutoutStartPoint(){
		return imageHolder.getCutoutStartPoint();
	}

	public Point getCutoutEndPoint(){
		return imageHolder.getCutoutEndPoint();
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
