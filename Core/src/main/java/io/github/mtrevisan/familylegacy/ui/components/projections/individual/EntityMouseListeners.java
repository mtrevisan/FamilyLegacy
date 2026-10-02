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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.MultiLineLabel;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Installs the standard interaction listeners on an entity panel
 * (individual or group).
 * <p>
 * The class encapsulates a pattern shared by {@code IndividualPanel} and
 * {@code GroupPanel}:
 * <ul>
 *   <li><b>single click on the name label</b> (only for the secondary
 *       boxes of the tree) re-roots the view on the entity; the hand
 *       cursor appears only when the pointer is over the text, not over
 *       the whole label;</li>
 *   <li><b>single click anywhere else on the panel</b> selects the
 *       entity: the panel is highlighted with a thicker border, and any
 *       detail panel observing the selection is populated;</li>
 *   <li><b>double click</b> opens the edit dialog for the entity;</li>
 *   <li><b>hover</b> repaints the panel with a lighter border.</li>
 * </ul>
 * <p>
 * The listeners are installed recursively on the panel and every child,
 * because in Swing mouse events are delivered only to the deepest
 * component under the pointer: a listener attached to the panel alone
 * would never fire when the pointer is over the name label or the photo.
 */
public final class EntityMouseListeners{

	private final JComponent root;
	private final MultiLineLabel nameLabel;
	private final Supplier<FLEFRecord> recordSupplier;
	private final Consumer<String> onRootSelected;
	private final Consumer<FLEFRecord> onSelected;
	private final Consumer<FLEFRecord> onEdit;
	private final Consumer<Boolean> onHoverChanged;


	private EntityMouseListeners(final Builder b){
		this.root = b.root;
		this.nameLabel = b.nameLabel;
		this.recordSupplier = b.recordSupplier;
		this.onRootSelected = b.onRootSelected;
		this.onSelected = b.onSelected;
		this.onEdit = b.onEdit;
		this.onHoverChanged = b.onHoverChanged;
	}


	public static Builder builder(final JComponent root){
		return new Builder(root);
	}

	/**
	 * Installs all listeners. Must be called once per panel, after the
	 * child components have been added.
	 */
	public void install(){
		if(nameLabel != null && onRootSelected != null)
			installNameLabelListener();

		installPanelListener();
	}


	private void installNameLabelListener(){
		nameLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				final FLEFRecord record = recordSupplier.get();
				if(record == null || !SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1)
					return;

				onRootSelected.accept(record.getId());

				// Consume so the panel-level single-click listener does not
				// also fire: clicking the name is a navigation gesture, not
				// a selection one.
				e.consume();
			}

			@Override
			public void mouseExited(final MouseEvent e){
				nameLabel.setCursor(Cursor.getDefaultCursor());
			}
		});

		nameLabel.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				nameLabel.setCursor(nameLabel.isTextHit(e.getPoint())
					? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
					: Cursor.getDefaultCursor());
			}
		});
	}

	private void installPanelListener(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.isConsumed())
					return;

				final FLEFRecord record = recordSupplier.get();
				if(record == null)
					return;

				// Right-click: select the entity so that the popup menu that
				// opens right afterwards operates on the clicked target. The
				// event is NOT consumed, because the PopupMouseAdapter that is
				// attached later (in withListener) must still receive it and
				// show the popup. The panel adapter is registered first, so
				// this branch fires before the popup is displayed.
				if(SwingUtilities.isRightMouseButton(e)){
					if(onSelected != null)
						onSelected.accept(record);

					return;
				}

				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				if(e.getClickCount() == 2){
					if(onEdit != null)
						onEdit.accept(record);
				}
				else if(e.getClickCount() == 1 && onSelected != null){
					onSelected.accept(record);

					// Consume the single-click selection so that an
					// enclosing container with its own listener does not
					// also process the same event.
					e.consume();
				}
			}

			@Override
			public void mouseEntered(final MouseEvent e){
				onHoverChanged.accept(true);
			}

			@Override
			public void mouseExited(final MouseEvent e){
				onHoverChanged.accept(false);
			}
		};

		attachRecursive(root, adapter);
	}

	/**
	 * Attaches the given listener to the component and, recursively, to
	 * every descendant. Used to propagate a listener that must react to
	 * mouse events no matter which child is under the pointer, because in
	 * Swing mouse events do not bubble up from a child to its parent.
	 *
	 * @param component the root of the subtree
	 * @param listener  the listener to attach
	 */
	public static void attachRecursive(final Component component, final MouseListener listener){
		component.addMouseListener(listener);

		if(component instanceof Container container)
			for(final Component child : container.getComponents())
				attachRecursive(child, listener);
	}


	public static final class Builder{

		private final JComponent root;
		private MultiLineLabel nameLabel;
		private Supplier<FLEFRecord> recordSupplier;
		private Consumer<String> onRootSelected;
		private Consumer<FLEFRecord> onSelected;
		private Consumer<FLEFRecord> onEdit;
		private Consumer<Boolean> onHoverChanged;

		private Builder(final JComponent root){
			this.root = Objects.requireNonNull(root, "Root component must not be null");
		}

		/**
		 * Sets the name label that reacts to a single click by re-rooting
		 * the view. Pass {@code null} (or do not call this method) when
		 * the panel should not navigate on name click.
		 */
		public Builder nameLabel(final MultiLineLabel nameLabel){
			this.nameLabel = nameLabel;

			return this;
		}

		/**
		 * Supplies the record backing the panel at the moment of the
		 * click. Returning {@code null} disables every action, which is
		 * how the empty placeholder boxes ignore mouse input.
		 */
		public Builder recordSupplier(final Supplier<FLEFRecord> recordSupplier){
			this.recordSupplier = recordSupplier;

			return this;
		}

		/** Callback invoked on single click on the name label. */
		public Builder onRootSelected(final Consumer<String> onRootSelected){
			this.onRootSelected = onRootSelected;

			return this;
		}

		/** Callback invoked on single click anywhere on the panel. */
		public Builder onSelected(final Consumer<FLEFRecord> onSelected){
			this.onSelected = onSelected;

			return this;
		}

		/** Callback invoked on double click. */
		public Builder onEdit(final Consumer<FLEFRecord> onEdit){
			this.onEdit = onEdit;

			return this;
		}

		/**
		 * Callback invoked when the pointer enters or leaves the panel.
		 * The caller is responsible for repainting when the state changes.
		 */
		public Builder onHoverChanged(final Consumer<Boolean> onHoverChanged){
			this.onHoverChanged = onHoverChanged;

			return this;
		}

		public EntityMouseListeners build(){
			Objects.requireNonNull(recordSupplier, "recordSupplier must not be null");

			if(onHoverChanged == null)
				onHoverChanged = h -> {};

			return new EntityMouseListeners(this);
		}

	}

}
