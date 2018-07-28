package thesis.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import thesis.util.HibernateManager;

public class UiLauncher {

	// private static Logger logger;
	static {
		System.setProperty("initiator", UiLauncher.class.getSimpleName());
		System.setProperty("current.date", (new SimpleDateFormat("yyyy-MM-dd HHmmss")).format(new Date()));
		System.setProperty("log4j.configurationFile", "META-INF/log4j.xml");
		// logger = LogManager.getLogger(UiLauncher.class);
	}

	public static void main(String[] args) {
		Display display = new Display();
		try {
			Shell shell = new Shell(display);
			shell.setLayout(new GridLayout(10, true));
			shell.setText("Thesis: Predict tags for SO Questions");
			shell.setBounds(display.getPrimaryMonitor()
					.getBounds());
			shell.setMaximized(true);
			try {
				new PredictorComposite(shell, SWT.NONE);

				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				}
			} finally {
				if (!shell.isDisposed())
					shell.dispose();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			HibernateManager.shutdown();
			display.dispose();
			System.exit(0);
		}
	}

}
