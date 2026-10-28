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
package io.github.mtrevisan.familylegacy.ui.assistant.app;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantIndicator;
import io.github.mtrevisan.familylegacy.ui.components.projections.ResearchAssistantPort;

import javax.swing.JComponent;
import java.awt.Window;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


/** The Research Assistant as the main window sees it: found by the window as a service provider. */
public final class FrameAssistantPort implements ResearchAssistantPort{

	@Override
	public void install(final Supplier<FLEFModel> modelSupplier, final Supplier<String> scopeSupplier){
		DefaultResearchAssistant.getInstance().install(modelSupplier, scopeSupplier);
	}

	@Override
	public void open(){
		DefaultResearchAssistant.getInstance().open();
	}

	@Override
	public Indicator createIndicator(final Window window, final Supplier<FLEFModel> modelSupplier,
			final Supplier<String> scopeSupplier, final BooleanSupplier modalDialogShowing){
		final AssistantIndicator indicator = new AssistantIndicator(DefaultResearchAssistant.getInstance(),
			() -> new AssistantContext(window, null, modelSupplier.get(), scopeSupplier.get()), modalDialogShowing);
		return new Indicator(){
			@Override
			public JComponent component(){
				return indicator.component();
			}

			@Override
			public void refreshSoon(){
				indicator.refreshSoon();
			}
		};
	}

}
