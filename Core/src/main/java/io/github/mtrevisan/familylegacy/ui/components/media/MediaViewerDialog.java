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
package io.github.mtrevisan.familylegacy.ui.components.media;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions;
import io.github.mtrevisan.familylegacy.imaging.MediaRegions.Region;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.PagedDocuments;
import io.github.mtrevisan.familylegacy.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.images.RegionOverlay;
import io.github.mtrevisan.familylegacy.ui.images.ScaledImage;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


/**
 * Shows an image at its full size, with the regions that tell who is in it: all of them are drawn over the image (they can be hidden), the
 * mouse over them says who they are, and a click opens the editor of the person.
 * <p>
 * Where several regions overlap (a face inside the group it belongs to) the tooltip lists them all, the smallest first; a click on a point that
 * has more than one person asks which one. A document that has pages (a PDF, a multi-page TIFF) is browsed page by page (the spinner, or Page Up
 * and Page Down), each with its own regions; the pages are rendered away from the event dispatch thread.
 * </p>
 */
public class MediaViewerDialog extends JDialog{

	private static final float MAX_SCREEN_FRACTION = 0.75f;
	private static final int MIN_IMAGE_SIDE = 200;
	private static final int PAGE_CACHE_SIZE = 6;
	private static final String ACTION_PREVIOUS_PAGE = "previousPage";
	private static final String ACTION_NEXT_PAGE = "nextPage";

	private static final String[] HTML_SPECIAL = {"&", "<", ">"};
	private static final String[] HTML_ESCAPED = {"&amp;", "&lt;", "&gt;"};


	private final FLEFModel model;
	private final FLEFRecord media;

	private final ScaledImage imageHolder = ScaledImage.createViewOnly();
	private final JCheckBox showRegions = new JCheckBox(I18N.t("dialog.media.viewer.show_regions"), true);
	private final JLabel regionsLabel = new JLabel();
	private final JLabel hint = new JLabel(I18N.t("dialog.media.viewer.hint"));

	private final JSpinner pageSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
	private final JLabel pageCountLabel = new JLabel();
	private final JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
	/** The rendered pages are big (about 9 MB each for a PDF): only the last ones are kept. */
	private final Map<Integer, BufferedImage> pageCache = new LinkedHashMap<>(16, 0.75f, true){
		@Override
		protected boolean removeEldestEntry(final Map.Entry<Integer, BufferedImage> eldest){
			return (size() > PAGE_CACHE_SIZE);
		}
	};

	private File file;
	private boolean paged;
	private int pageCount = 1;
	private int page = 1;
	private boolean settingPage;
	private SwingWorker<PagedDocuments.Page, Void> renderWorker;

	private BufferedImage image;
	private List<Region> regions = List.of();


	/**
	 * Opens the viewer on a media (the call returns when the viewer is closed).
	 *
	 * @param parent	The window the viewer belongs to.
	 * @param model	The model that holds the media and the individuals.
	 * @param media	The media record.
	 */
	public static void open(final Window parent, final FLEFModel model, final FLEFRecord media){
		final MediaViewerDialog dialog = new MediaViewerDialog(parent, model, media);
		try{
			dialog.load();
		}
		catch(final IOException ioe){
			MessageDialogs.show(parent,
				I18N.tf("error.image", MediaPathDisplay.display(MediaReader.extractUri(media))),
				I18N.t("error.title"),
				JOptionPane.ERROR_MESSAGE);

			dialog.dispose();
			return;
		}

		dialog.setVisible(true);
		dialog.dispose();
	}


	/** Builds the viewer: the image is read by {@link #load()}. */
	MediaViewerDialog(final Window parent, final FLEFModel model, final FLEFRecord media){
		super(parent, I18N.t("dialog.media.viewer.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.media = media;

		initComponents();
		initLayout();
		GUIHelper.installEscapeToClose(this);
	}


	private void initComponents(){
		showRegions.addActionListener(e -> imageHolder.setRegionsVisible(showRegions.isSelected()));
		regionsLabel.setEnabled(false);

		pageSpinner.addChangeListener(e -> {
			if(!settingPage)
				showPage((Integer)pageSpinner.getValue());
		});
		pagePanel.add(new JLabel(I18N.t("dialog.media.viewer.page")));
		pagePanel.add(pageSpinner);
		pagePanel.add(pageCountLabel);
		pagePanel.setVisible(false);
		//Page Up and Page Down browse the pages wherever the focus is
		final InputMap inputs = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ACTION_PREVIOUS_PAGE);
		inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ACTION_NEXT_PAGE);
		getRootPane().getActionMap().put(ACTION_PREVIOUS_PAGE, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				showPage(page - 1);
			}
		});
		getRootPane().getActionMap().put(ACTION_NEXT_PAGE, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				showPage(page + 1);
			}
		});

		imageHolder.setRegionTooltip(this::tooltipFor);
		imageHolder.setRegionClickListener(this::openPeople);
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		setLayout(new MigLayout("ins " + GUIHelper.DIALOG_MARGIN + ",hidemode 3,fill", "[grow,fill]", "[][grow,fill][]"));

		final JPanel top = new JPanel(new MigLayout("ins 0,hidemode 3", "[][][grow,fill]", "[]"));
		top.add(showRegions);
		top.add(pagePanel, "gapleft 12");
		top.add(regionsLabel, "align right");
		add(top, "wrap");

		add(imageHolder, "grow,push,wrap");

		hint.setEnabled(false);
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> setVisible(false));
		getRootPane().setDefaultButton(close);
		final JPanel bottom = new JPanel(new MigLayout("ins 0", "[grow,fill][]", "[]"));
		bottom.add(hint);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttons.add(close);
		bottom.add(buttons);
		add(bottom);
	}


	/**
	 * Reads the image of the media and its regions.
	 *
	 * @throws IOException	If the media has no readable image.
	 */
	void load() throws IOException{
		final String uri = MediaReader.extractUri(media);
		final File found = (uri != null? FileHelper.loadFile(uri): null);
		if(found == null || !found.exists())
			throw new IOException(I18N.t("dialog.preferred.image.no_file"));

		final BufferedImage newImage;
		final boolean isPaged = PagedDocuments.isPaged(found.getName());
		int pages = 1;
		if(isPaged){
			//(the first page is read now, to know how big the viewer is and how many pages there are; the other ones when they are asked)
			final PagedDocuments.Page first = PagedDocuments.render(found, 1);
			if(first == null || first.image() == null)
				throw new IOException(I18N.tf("error.image", found.getAbsolutePath()));

			newImage = first.image();
			pages = first.pageCount();
		}
		else{
			newImage = ResourceHelper.readBufferedImage(found);
			if(newImage == null)
				throw new IOException(I18N.tf("error.image", found.getAbsolutePath()));
		}

		file = found;
		paged = isPaged;
		pageCount = pages;
		page = 1;
		pageCache.clear();
		pageCache.put(1, newImage);
		settingPage = true;
		try{
			final SpinnerNumberModel pageModel = (SpinnerNumberModel)pageSpinner.getModel();
			pageModel.setMaximum(pageCount);
			pageModel.setValue(1);
		}
		finally{
			settingPage = false;
		}
		pageCountLabel.setText(I18N.tf("dialog.media.viewer.of_pages", pageCount));
		pagePanel.setVisible(paged && pageCount > 1);
		hint.setText(I18N.t(paged && pageCount > 1? "dialog.media.viewer.hint.pages": "dialog.media.viewer.hint"));

		image = newImage;
		imageHolder.setRectangularImage(image);
		setTitle(I18N.tf("dialog.media.viewer.title.media", MediaPathDisplay.display(uri)));
		refreshRegions();
		fitToScreen();
		setLocationRelativeTo(getOwner());
	}

	/** @return how many pages the media has (1 for an image) */
	int pageCount(){
		return pageCount;
	}

	/** @return the page that is shown (1-based) */
	int currentPage(){
		return page;
	}

	/** @return the number of regions on the page that is shown */
	int regionCount(){
		return regions.size();
	}

	/**
	 * Shows a page of the document: at once if it was seen lately, otherwise when it is rendered (away from the event dispatch thread). A page
	 * that does not exist, or any page of a media with no pages, is ignored.
	 *
	 * @param requested	The 1-based page.
	 */
	void showPage(final int requested){
		if(!paged || requested < 1 || requested > pageCount || requested == page && image == pageCache.get(page))
			return;

		page = requested;
		settingPage = true;
		try{
			pageSpinner.setValue(page);
		}
		finally{
			settingPage = false;
		}
		if(renderWorker != null)
			renderWorker.cancel(true);

		final BufferedImage cached = pageCache.get(page);
		if(cached != null){
			displayPage(cached);

			return;
		}

		final int wanted = page;
		final File source = file;
		final SwingWorker<PagedDocuments.Page, Void> worker = new SwingWorker<>(){
			@Override
			protected PagedDocuments.Page doInBackground(){
				return PagedDocuments.render(source, wanted);
			}

			@Override
			protected void done(){
				//(the user may have gone to another page, or closed the viewer, meanwhile)
				if(isCancelled() || wanted != page)
					return;

				try{
					final PagedDocuments.Page rendered = get();
					if(rendered != null && rendered.image() != null){
						pageCache.put(wanted, rendered.image());
						displayPage(rendered.image());
					}
					else
						MessageDialogs.show(MediaViewerDialog.this, I18N.tf("error.image", source.getAbsolutePath()), I18N.t("error.title"),
							JOptionPane.ERROR_MESSAGE);
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ee){
					MessageDialogs.show(MediaViewerDialog.this, I18N.tf("error.image", source.getAbsolutePath()), I18N.t("error.title"),
						JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		renderWorker = worker;
		worker.execute();
	}

	private void displayPage(final BufferedImage pageImage){
		image = pageImage;
		imageHolder.setRectangularImage(pageImage);
		refreshRegions();
	}

	@Override
	public void dispose(){
		if(renderWorker != null)
			renderWorker.cancel(true);
		pageCache.clear();

		super.dispose();
	}

	/** Reads the regions again from the media: who is shown, and who has the region as the portrait, may have changed. */
	private void refreshRegions(){
		//(a media that has no pages is shown as its first page)
		final Integer shown = (paged? page: null);
		regions = MediaRegions.onPage(MediaRegions.of(model, media.getId()), shown);
		final List<RegionOverlay.Mark> marks = new ArrayList<>(regions.size());
		for(final Region region : regions)
			marks.add(new RegionOverlay.Mark(region, isPortrait(region)));
		imageHolder.setRegions(marks, shown);

		showRegions.setEnabled(!regions.isEmpty());
		regionsLabel.setText(regions.isEmpty()
			? I18N.t("dialog.media.viewer.no_regions")
			: I18N.tf("dialog.media.viewer.regions", regions.size()));
	}

	/** @return whether the region is the preferred image of one of its people */
	private boolean isPortrait(final Region region){
		for(final String individualId : region.individuals())
			if(isPortraitOf(region, individualId))
				return true;
		return false;
	}

	private boolean isPortraitOf(final Region region, final String individualId){
		return MediaRegions.isPortraitOf(model.getRecordById(individualId), media.getId(), region);
	}

	/** Fits the dialog to the image, in at most 75% of the screen (the image is scaled down to fit, never up). */
	private void fitToScreen(){
		imageHolder.setPreferredSize(new Dimension(Math.max(MIN_IMAGE_SIDE, image.getWidth()), Math.max(MIN_IMAGE_SIDE, image.getHeight())));
		pack();

		final int extraWidth = getWidth() - imageHolder.getWidth();
		final int extraHeight = getHeight() - imageHolder.getHeight();
		final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final int maxImageWidth = Math.max(MIN_IMAGE_SIDE, (int)(screen.width * MAX_SCREEN_FRACTION) - extraWidth);
		final int maxImageHeight = Math.max(MIN_IMAGE_SIDE, (int)(screen.height * MAX_SCREEN_FRACTION) - extraHeight);

		final double scale = Math.min(1., Math.min((double)maxImageWidth / image.getWidth(), (double)maxImageHeight / image.getHeight()));
		imageHolder.setPreferredSize(new Dimension(Math.max(MIN_IMAGE_SIDE, (int)(image.getWidth() * scale)),
			Math.max(MIN_IMAGE_SIDE, (int)(image.getHeight() * scale))));
		pack();
	}


	/** The tooltip for the regions under the mouse (smallest first): one line per person, the portraits marked. */
	String tooltipFor(final List<Region> under){
		final List<String> lines = new ArrayList<>(under.size());
		for(final Region region : under){
			for(final String individualId : region.individuals()){
				final String name = escape(nameOf(individualId));
				lines.add(isPortraitOf(region, individualId)? I18N.tf("dialog.media.viewer.portrait_of", name): name);
			}
		}
		return "<html>" + String.join("<br>", lines) + "</html>";
	}

	private static String escape(final String text){
		return StringUtils.replaceEach(text, HTML_SPECIAL, HTML_ESCAPED);
	}

	private String nameOf(final String individualId){
		final FLEFRecord individual = model.getRecordById(individualId);
		return (individual != null? IndividualHandler.getInstance().getDisplayText(individual, model): "[" + individualId + "]");
	}

	/** A click: the editor of the person; a choice among the people if there are several under the point. */
	private void openPeople(final List<Region> under, final Point point){
		final Set<String> people = new LinkedHashSet<>();
		for(final Region region : under)
			people.addAll(region.individuals());
		if(people.isEmpty())
			return;

		if(people.size() == 1){
			openIndividual(people.iterator().next());

			return;
		}

		final JPopupMenu menu = new JPopupMenu();
		for(final String individualId : people){
			final JMenuItem item = new JMenuItem(nameOf(individualId));
			item.addActionListener(e -> openIndividual(individualId));
			menu.add(item);
		}
		menu.show(imageHolder, point.x, point.y);
	}

	private void openIndividual(final String individualId){
		final FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null){
			MessageDialogs.show(this, I18N.t("error.record_not_found"), I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

			return;
		}

		IndividualHandler.getInstance().createEditDialog(this, model, individual)
			.setVisible(true);
		//(the name, and the preferred image, may have been changed)
		refreshRegions();
	}

}
