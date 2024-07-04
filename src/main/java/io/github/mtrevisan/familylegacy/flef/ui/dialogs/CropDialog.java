/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ScaledImage;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.function.Consumer;


public final class CropDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3777867436237271707L;


	//record components:
	private final JPanel recordPanel = new JPanel();

	private ScaledImage imageHolder;

	private Consumer<Object> onCloseGracefully;


	public CropDialog(final Frame parent){
		super(parent, true);

		initComponents();
	}


	public CropDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;

		return this;
	}

	private void initComponents(){
		initRecordComponents();

		initLayout();
	}

	private void initRecordComponents(){
		setTitle("Define crop");

		imageHolder = new ScaledImage();
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		initRecordLayout(recordPanel);

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[grow]"));
		add(recordPanel, "grow");
	}

	private void initRecordLayout(final Container recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[grow,fill]"));

		recordPanel.add(imageHolder, "grow");
	}

	private void closeAction(final ActionEvent evt){
		if(closeAction())
			setVisible(false);
	}

	private boolean closeAction(){
		if(onCloseGracefully != null)
			onCloseGracefully.accept(this);

		return true;
	}

	public void loadData(final File file) throws IOException{
		imageHolder.setRectangularImage(ResourceHelper.readImage(file));
	}

	public void loadData(final File file, final Rectangle crop) throws IOException{
		loadData(file);

		imageHolder.setCropRectangle(crop);
	}

	public Rectangle getCropRectangle(){
		return imageHolder.getCropRectangle();
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){}
			};
			EventBusService.subscribe(listener);

			final CropDialog dialog = new CropDialog(parent)
				.withOnCloseGracefully(d -> System.out.println("crop: " + ((CropDialog)d).getCropRectangle()));
			final File file = new File("\\resources\\images\\addPhoto.boy.jpg");
			try{
				dialog.loadData(file);
			}
			catch(final IOException e){
				e.printStackTrace();
			}

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(420, 295);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
