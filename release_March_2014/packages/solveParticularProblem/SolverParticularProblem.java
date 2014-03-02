package solveParticularProblem;

import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dimension;

public class SolverParticularProblem
{
    boolean packFrame = false;

    /**
     * Construct and show the application.
     */
    public SolverParticularProblem()
    {
        MainFrame frame = new MainFrame("IDP-IP");
        // Validate frames that have preset sizes
        // Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame) {
            frame.pack();
        } else {
            frame.validate();
        }

        // Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation((screenSize.width  - frameSize.width ) / 2,
                          (screenSize.height - frameSize.height) / 2);
        
        //Set the "Run" button as the default button.
        frame.getRootPane().setDefaultButton(frame.run_button);

        frame.setVisible(true);
    }

    //*****************************************************************************************************
    
    /**
     * Application entry point.
     */
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(new Runnable()
    	{
    		public void run() {
    			try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
    			catch (Exception exception) { exception.printStackTrace(); }    			
    			new SolverParticularProblem();
    		}
    	});
    }
}