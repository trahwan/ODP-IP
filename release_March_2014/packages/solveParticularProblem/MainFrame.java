package solveParticularProblem;


import general.General;
import inputOutput.Input;
import inputOutput.SolverNames;
import inputOutput.ValueDistribution;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import mainSolver.MainSolver;
import mainSolver.Result;

//*****************************************************************************************************

public class MainFrame extends JFrame
{
	public MainFrame() {
		try { jbInit();	} catch (Exception ex) {ex.printStackTrace(); }
	}
	JButton run_button = new JButton();

	ButtonGroup radioButtonGroup2 = new ButtonGroup();
	
	JTextField numOfAgents_textField = new JTextField();
	JTextField inputAndOutputPathAndFolderName_textField = new JTextField();
	JTextField inputFileName_textField = new JTextField();
	JTextField ipAcceptableRatio_textField = new JTextField();

	JPanel contentPane;
	JPanel jPanel1 = new JPanel();
	JPanel jPanel5 = new JPanel();
	JPanel jPanel7 = new JPanel();
	JPanel jPanel9 = new JPanel();

	JRadioButton runCplex_radioButton = new JRadioButton();
	JRadioButton runDP_radioButton = new JRadioButton();
	JRadioButton runIDP_radioButton = new JRadioButton();
	JRadioButton runODP_radioButton = new JRadioButton();
	JRadioButton runIP_radioButton = new JRadioButton();
	JRadioButton runODPIP_radioButton = new JRadioButton();
	
	JLabel jLabel1 = new JLabel();
	JLabel jLabel2 = new JLabel();
	JLabel jLabel6 = new JLabel();
	JLabel jLabel7 = new JLabel();
	JLabel jLabel10 = new JLabel();
	JLabel jLabel11 = new JLabel();
	JLabel jLabel14 = new JLabel();
	JLabel jLabel15 = new JLabel();
	JLabel jLabel16 = new JLabel();

	JCheckBox ipOrdersIntegerPartitionsAscendingly_checkBox = new JCheckBox();
	JCheckBox printDetailedResultsOfIPToFiles_checkBox = new JCheckBox();
	JCheckBox printTimeTakenByIPForEachSubspace_checkBox = new JCheckBox();

	public JTextArea textArea = new JTextArea();	
	TitledBorder titledBorder1 = new TitledBorder("");
	JScrollPane jScrollPane1 = new JScrollPane();	

	//*****************************************************************************************************

	/**
	 * This is the main method (it gets executed when pressing the "Run" button)
	 */
	private void main()
	{
		//Read the user input
		Input input = readInputFromGUI(); //read the input parameters from the GUI.
		if( input == null ) return; // If "input" is null, then there was an error in the input, and no action is taken

		//Run the CSG algorithm(s)
		Result result = (new MainSolver()).solve( input );

		//Print the results on the GUI
		printResultOnGUI(input, result);
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method reads the user input and fills it in an object of "Input"
	 */
	public Input readInputFromGUI()
	{
		Input input = new Input();
		input.initInput();

		//Read the number of agents
		input.numOfAgents = (new Integer(numOfAgents_textField.getText())).intValue();
		if( input.numOfAgents > 25 ){
			JOptionPane.showMessageDialog(null,	"The number of coalition structures cannot be handled by java for more than 25 agents!","Alert", JOptionPane.ERROR_MESSAGE);
			return(null); //Return null to indicate that there was an error in reading the input.
		}
		
		// Select the algorithm you want to run
		if( runDP_radioButton.isSelected()   ) input.solverName = SolverNames.DP;
		else if( runIDP_radioButton.isSelected()  ) input.solverName = SolverNames.IDP;
		else if( runODP_radioButton.isSelected()  ) input.solverName = SolverNames.ODP;
		else if( runIP_radioButton.isSelected()   ) input.solverName = SolverNames.IP;
		else if( runODPIP_radioButton.isSelected()) input.solverName = SolverNames.ODPIP;
		else if( runCplex_radioButton.isSelected()) input.solverName = SolverNames.CPLEX;

		// Printing options
		if (printDetailedResultsOfIPToFiles_checkBox.isSelected()) input.printInterimResultsOfIPToFiles = true; else input.printInterimResultsOfIPToFiles = false;
		if (printTimeTakenByIPForEachSubspace_checkBox.isSelected()) input.printTimeTakenByIPForEachSubspace = true; else input.printTimeTakenByIPForEachSubspace = false;

		// only relevant when running IP or ODP-IP
		if (ipOrdersIntegerPartitionsAscendingly_checkBox.isSelected()) input.orderIntegerPartitionsAscendingly = true; else input.orderIntegerPartitionsAscendingly = false;

		input.acceptableRatio = (new Double(ipAcceptableRatio_textField.getText())).doubleValue(); // only relevant when running IP or ODP-IP

		//set the folders in which the coalition values are stored, or read from.
		input.folderInWhichCoalitionValuesAreStored = inputAndOutputPathAndFolderName_textField.getText();

		// we will simply set the output folder to be the same as the input folder (this is only relevant if there are any results to be placed in an output folder) 
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd (HH-mm-ss)" );
		input.outputFolder = input.folderInWhichCoalitionValuesAreStored+"/"+SolverNames.toString(input.solverName)+" "+simpleDateFormat.format(calendar.getTime());;					
		
		input.readCoalitionValuesFromFile = true;
		input.readCoalitionValuesFromFile( inputFileName_textField.getText() );

		return( input );
	}

	//*****************************************************************************************************
	
	/**
	 * This method prints the search outcome (e.g. solution value, num of solutions searched, ...)
	 */
	public void printResultOnGUI(Input input, Result result)
	{
		// if running ILOG's CPLEX
		if ( input.solverName == SolverNames.CPLEX ){
			textArea.append("----------------------------------------------------\n    CPLEX ("+input.numOfAgents+" agents, "+ValueDistribution.toString(input.valueDistribution)+" distribution)\n----------------------------------------------------\n");
			if( input.numOfRunningTimes == 1 ){
				textArea.append("\nThe run time of CPLEX (in milliseconds):\n"+result.cplexTime+"\n");
				textArea.append("\nThe best coalition structure found by CPLEX is:\n"+General.convertArrayToString(result.cplexBestCSFound)+"\n");
				textArea.append("\nThe value of this coalition structure is:\n"+result.cplexValueOfBestCSFound+"\n");
			}else
				textArea.append("\nThe average run time of Cplex (in milliseconds):\n"+result.cplexTime+" +/- "+result.cpleXTime_confidenceInterval+"\n");
		}
		// else, if running DP, IDP, or ODP
		if ( (input.solverName == SolverNames.DP) || (input.solverName == SolverNames.IDP) || (input.solverName == SolverNames.ODP) ){
			textArea.append("----------------------------------------------------\n    "+SolverNames.toString(input.solverName)+" ("+input.numOfAgents+" agents, "+ValueDistribution.toString(input.valueDistribution)+" distribution)\n----------------------------------------------------\n");
			textArea.append("\nThe run time of "+SolverNames.toString(input.solverName)+" for every size (in milliseconds):\n");
			for(int size=2; size<=input.numOfAgents; size++)
				textArea.append("    - Size "+size+" took "+result.dpTimeForEachSize[size]+"\n");
			textArea.append("\nThe total run time of "+SolverNames.toString(input.solverName)+" (in milliseconds):\n"+result.dpTime+"\n");
			textArea.append("\nThe coalition structure found by "+SolverNames.toString(input.solverName)+":\n"+General.convertArrayToString(result.get_dpBestCSFound())+"\n");
			textArea.append("\nThe value of this coalition structure is:\n"+ result.get_dpValueOfBestCSFound()+"\n\n");
		}
		// else, if running IP or ODP-IP
		if ( (input.solverName == SolverNames.IP) || (input.solverName == SolverNames.ODPIP) ){
			textArea.append("----------------------------------------------------\n    "+SolverNames.toString(input.solverName)+" ("+input.numOfAgents+" agents, "+ValueDistribution.toString(input.valueDistribution)+" distribution)\n----------------------------------------------------\n");
			if( input.numOfRunningTimes == 1 ){
				textArea.append("\nThe time for IP to scan the input (in milliseconds):\n"+result.ipTimeForScanningTheInput+"\n");			
				textArea.append("\nThe total run time of "+SolverNames.toString(input.solverName)+" (in milliseconds):\n"+result.ipTime+"\n");
				textArea.append("\nThe best coalition structure found by "+SolverNames.toString(input.solverName)+" is:\n"+General.convertArrayToString(result.get_ipBestCSFound())+"\n");
				textArea.append("\nThe value of this coalition structure is:\n"+result.get_ipValueOfBestCSFound()+"\n");
				if( input.solverName == SolverNames.IP ){
					textArea.append("\nThe number of expansions made by IP:\n"+result.ipNumOfExpansions+"\n");
					textArea.append("\nBased on this, the percentage of search-space that was searched by "+SolverNames.toString(input.solverName)+" is:\n"+(double)(result.ipNumOfExpansions*100)/result.totalNumOfExpansions+"%\n");
				}
			}else{
				textArea.append("\nThe average time for "+SolverNames.toString(input.solverName)+" to scan the input (in milliseconds):\n"+result.ipTimeForScanningTheInput+" +/- "+result.ipTimeForScanningTheInput_confidenceInterval+"\n");			
				textArea.append("\nThe average run time of "+SolverNames.toString(input.solverName)+" (in milliseconds):\n"+result.ipTime+" +/- "+result.ipTime_confidenceInterval+"\n");
				if( input.solverName == SolverNames.IP ){
					textArea.append("\nThe average number of expansions made by "+SolverNames.toString(input.solverName)+":\n"+result.ipNumOfExpansions+" +/- "+result.ipNumOfExpansions_confidenceInterval+"\n");
					textArea.append("\nBased on this, the average percentage of search-space that was searched by "+SolverNames.toString(input.solverName)+" is:\n"+(double)(result.ipNumOfExpansions*100)/result.totalNumOfExpansions+"% +/- "+(double)(result.ipNumOfExpansions_confidenceInterval*100)/result.totalNumOfExpansions+"%\n");
				}
			}
		}
	}

	//*****************************************************************************************************
	
	private void actionPerformed(ActionEvent e)
	{
		//if the "Run" button is pressed
		if (e.getActionCommand() == run_button.getActionCommand()) { main(); }
	}
	
	//*****************************************************************************************************
	
	private void jbInit() throws Exception
	{
		this.setResizable(false);
		setSize(new Dimension(990, 750));
		setTitle("Exact Algorithms for the Complete Set Partitioning Problem");
		textArea.setBorder(BorderFactory.createEtchedBorder());
		textArea.setText("");
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888         Content Pane          888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888		
		
		contentPane = (JPanel) getContentPane();
		contentPane.setLayout(null);
		contentPane.setMinimumSize(new Dimension(1, 1));
		contentPane.add(jLabel6);
		contentPane.add(jLabel7);
		contentPane.add(jLabel10);
		contentPane.add(jLabel11);
		contentPane.add(jLabel14);
		
		contentPane.add(jScrollPane1);
		jScrollPane1.getViewport().add(textArea);
		jScrollPane1.getViewport().setBackground(Color.black);
		jScrollPane1.setBounds(new Rectangle(15, 15, 616, 690));

		contentPane.add(run_button);
		run_button.setBounds(new Rectangle(650, 14, 61, 26));
		run_button.setMnemonic('R');
		run_button.setText("Run");
		run_button.addActionListener(new applicationGUI_actionAdapter(this));
		
		contentPane.add(jLabel1);
		jLabel1.setBounds(new Rectangle(740, 15, 95, 21));
		jLabel1.setText("Number of agents");
		
		contentPane.add(numOfAgents_textField);
		numOfAgents_textField.setBounds(new Rectangle(840, 15, 38, 22));
		numOfAgents_textField.setText("");

		contentPane.add(jPanel1);
		contentPane.add(jPanel5);
		contentPane.add(jPanel7);
		contentPane.add(jPanel9);

		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel1           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel1.setBounds(new Rectangle(650, 55, 315, 155));
		jPanel1.setBorder(BorderFactory.createEtchedBorder());
		jPanel1.setLayout(null);
		
		jLabel14.setText("    Input file (and Input/output folder)   ");
		jLabel14.setBounds(new Rectangle(670, 45, 210, 18));
		jLabel14.setOpaque(true);	

		jPanel1.add(jLabel15);
		jLabel15.setBounds(new Rectangle(10, 7, 290, 50));
		jLabel15.setText("<html>The path and name of the folder containing the coalition values (any output file(s) will be placed in the same folder)</html>");

		jPanel1.add(inputAndOutputPathAndFolderName_textField);
		inputAndOutputPathAndFolderName_textField.setBounds(new Rectangle(10, 55, 290, 22));
		inputAndOutputPathAndFolderName_textField.setText("e.g., D:/InputFolder");
		
		jPanel1.add(jLabel16);
		jLabel16.setBounds(new Rectangle(10, 75, 290, 50));
		jLabel16.setText("<html>Name (with extension) of file containing coalition values</html>");
		
		jPanel1.add(inputFileName_textField);
		inputFileName_textField.setBounds(new Rectangle(10, 120, 290, 22));
		inputFileName_textField.setText("e.g., coalitionValues.txt");
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel7           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel7.setBounds(new Rectangle(650, 230, 315, 165));
		jPanel7.setBorder(BorderFactory.createEtchedBorder());
		jPanel7.setLayout(null);
	
		jLabel10.setText("    Select an algorithm");
		jLabel10.setBounds(new Rectangle(670, 220, 135, 18));
		jLabel10.setOpaque(true);
		
		jPanel7.add(runDP_radioButton);
		radioButtonGroup2.add(runDP_radioButton);
		runDP_radioButton.setBounds(new Rectangle(6, 10, 270, 22));
		runDP_radioButton.setText("Run DP [Yeh, 1986]");

		jPanel7.add(runIDP_radioButton);
		radioButtonGroup2.add(runIDP_radioButton);
		runIDP_radioButton.setBounds(new Rectangle(6, 35, 270, 22));
		runIDP_radioButton.setText("Run IDP [Rahwan & Jennings, 2008]");
		
		jPanel7.add(runODP_radioButton);
		radioButtonGroup2.add(runODP_radioButton);
		runODP_radioButton.setBounds(new Rectangle(6, 60, 270, 22));
		runODP_radioButton.setText("Run ODP [Rahwan et al., 2014]");
		
		jPanel7.add(runIP_radioButton);
		radioButtonGroup2.add(runIP_radioButton);
		runIP_radioButton.setBounds(new Rectangle(6, 85, 270,22));
		runIP_radioButton.setText("Run IP [Rahwan et al., 2007]");

		jPanel7.add(runODPIP_radioButton);
		radioButtonGroup2.add(runODPIP_radioButton);
		runODPIP_radioButton.setBounds(new Rectangle(6, 110, 270,22));
		runODPIP_radioButton.setText("Run ODP-IP [Rahwan et al., 2014]");
		runODPIP_radioButton.setSelected(true);
		
		jPanel7.add(runCplex_radioButton);
		radioButtonGroup2.add(runCplex_radioButton);
		runCplex_radioButton.setBounds(new Rectangle(6, 135, 270,22));
		runCplex_radioButton.setText("Run ILOG's CPLEX");
		runCplex_radioButton.setEnabled(false);;		

		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel5           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888		
		
		jPanel5.setBounds(new Rectangle(650, 415, 315, 95));
		jPanel5.setBorder(BorderFactory.createEtchedBorder());
		jPanel5.setLayout(null);	

		jLabel11.setText("    Printing options");
		jLabel11.setBounds(new Rectangle(660, 405, 107, 18));
		jLabel11.setOpaque(true);
		
		jPanel5.add(printDetailedResultsOfIPToFiles_checkBox);
		printDetailedResultsOfIPToFiles_checkBox.setText("<html>Print (to an output file) the interim results of IP or ODP-IP to output file<html>");
		printDetailedResultsOfIPToFiles_checkBox.setBounds(new Rectangle(6, 20, 303, 22));
		
		jPanel5.add(printTimeTakenByIPForEachSubspace_checkBox);
		printTimeTakenByIPForEachSubspace_checkBox.setText("<html>Print (to an output file) the time taken by IP to search each subspace<html>");
		printTimeTakenByIPForEachSubspace_checkBox.setBounds(new Rectangle(6, 55, 303, 22));
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel9           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel9.setBounds(new Rectangle(650, 530, 315, 68));
		jPanel9.setBorder(BorderFactory.createEtchedBorder());
		jPanel9.setLayout(null);
		
		jLabel7.setText("    IP & ODP-IP options    ");
		jLabel7.setBounds(new Rectangle(660, 520, 128, 18));
		jLabel7.setOpaque(true);	

		jPanel9.add(jLabel2);
		jLabel2.setBounds(new Rectangle(10, 10, 260, 21));
		jLabel2.setText("Acceptable Ratio (%) at which IP (or ODP-IP) stops:");

		jPanel9.add(ipAcceptableRatio_textField);
		ipAcceptableRatio_textField.setBounds(new Rectangle(271, 10, 33, 22));
		ipAcceptableRatio_textField.setText("100");
		
		jPanel9.add(ipOrdersIntegerPartitionsAscendingly_checkBox);
		ipOrdersIntegerPartitionsAscendingly_checkBox.setText("Order integers ascendingly (instead of descendingly)");
		ipOrdersIntegerPartitionsAscendingly_checkBox.setBounds(new Rectangle(6, 35, 303, 22));
	}
	
	//*****************************************************************************************************
	
	class applicationGUI_actionAdapter implements ActionListener {
		private MainFrame adaptee;
		applicationGUI_actionAdapter(MainFrame adaptee) {
			this.adaptee = adaptee;
		}
		public void actionPerformed(ActionEvent e) {
			adaptee.actionPerformed(e);
		}
	}
	
	public MainFrame(String title) {
		super(title);
		try { setDefaultCloseOperation(EXIT_ON_CLOSE);	jbInit(); }
		catch (Exception exception) {	exception.printStackTrace(); }
	}
}