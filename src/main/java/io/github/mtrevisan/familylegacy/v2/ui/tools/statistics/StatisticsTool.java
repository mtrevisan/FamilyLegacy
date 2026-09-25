package io.github.mtrevisan.familylegacy.v2.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ProgressDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;


/**
 * Opens the statistics dialog. The computation runs in a background
 * worker and reports progress through a {@link ProgressDialog}, so the
 * EDT stays free and the user sees what is happening.
 * <p>
 * The result dialog receives the fully computed {@link Statistics}
 * object, so it can be opened, closed, and reopened without recomputing.
 */
public final class StatisticsTool implements ToolOperation{

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsTool.class);


	@Override
	public String getName(){
		return "Statistics…";
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final FLEFModel model = context.model();
		if(model == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No model loaded.", "Statistics", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final ProgressDialog progress = new ProgressDialog(context.owner(),
			"Statistics", "Preparing…");

		final SwingWorker<Statistics, Void> worker = new SwingWorker<>(){
			@Override
			protected Statistics doInBackground(){
				return new StatisticsCalculator(model)
					.compute(progress::update);
			}

			@Override
			protected void done(){
				progress.close();
				try{
					final Statistics stats = get();
					new StatisticsDialog(context.owner(), stats, model)
						.setVisible(true);
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					LOGGER.error("Statistics computation failed", ex);
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					JOptionPane.showMessageDialog(context.owner(),
						"Statistics computation failed:\n" + cause.getMessage(),
						"Statistics", JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		worker.execute();
		progress.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
