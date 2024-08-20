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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ScaledImage;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class PhotoCropDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3777867436237271707L;


	//record components:
	private final JPanel recordPanel = new JPanel();

	private ScaledImage imageHolder;

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private Map<String, Object> selectedRecord;

	private Consumer<Map<String, Object>> onCloseGracefully;


	public static PhotoCropDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new PhotoCropDialog(store, parent);
	}


	private PhotoCropDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}


	public PhotoCropDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
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
			onCloseGracefully.accept(selectedRecord);

		return true;
	}

	public void loadData(final int photoID, final String photoCrop) throws IOException{
		selectedRecord = store.get("media")
			.get(photoID);
		if(selectedRecord != null){
			final String filePath = extractRecordIdentifier(selectedRecord);

			loadData(filePath);

			if(photoCrop != null){
				//draw crop box
				final String[] crop = StringUtils.split(photoCrop);
				final Rectangle rect = new Rectangle(Integer.parseInt(crop[0]), Integer.parseInt(crop[1]),
					Integer.parseInt(crop[2]), Integer.parseInt(crop[3]));
				imageHolder.setCrop(rect);
			}
		}
	}

	public void loadData(final String filename) throws IOException{
		final File file = FileHelper.loadFile(filename);
		if(file == null || !file.exists())
			throw new IOException("File does not exists");

		loadData(file);
	}

	public void loadData(final File file) throws IOException{
		imageHolder.setRectangularImage(ResourceHelper.readImage(file));
	}

	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	public Rectangle getCrop(){
		return imageHolder.getCrop();
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put("media", medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		medias.put((Integer)media1.get("id"), media1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final PhotoCropDialog dialog = create(store, parent);
			try{
				dialog.loadData("/images/addPhoto.boy.jpg");
			}
			catch(final IOException e){
				e.printStackTrace();
			}

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

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
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
