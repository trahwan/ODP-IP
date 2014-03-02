package solveRandomProblems;

import general.General;
import inputOutput.Input;
import inputOutput.SolverNames;
import inputOutput.ValueDistribution;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import predefinedExperiments.PredefinedExperiments;

//*****************************************************************************************************

public class MainFrame extends JFrame
{
	public MainFrame() {
		try { jbInit();	} catch (Exception ex) {ex.printStackTrace(); }
	}
	private double[]   prevCoalitionValues;
	boolean generateNewCoalitionValues;

	JButton run_button = new JButton();

	ButtonGroup radioButtonGroup1 = new ButtonGroup();
	ButtonGroup radioButtonGroup2 = new ButtonGroup();
	ButtonGroup radioButtonGroup3 = new ButtonGroup();
	
	JTextField numOfAgents_textField = new JTextField();
	JTextField ipAcceptableRatio_textField = new JTextField();
	JTextField numOfRunningtimes_textField = new JTextField();

	JPanel contentPane;
	JPanel jPanel1 = new JPanel();
	JPanel jPanel3 = new JPanel();
	JPanel jPanel4 = new JPanel();
	JPanel jPanel5 = new JPanel();
	JPanel jPanel7 = new JPanel();
	JPanel jPanel9 = new JPanel();

	JRadioButton normal_radioButton = new JRadioButton();
	JRadioButton uniform_radioButton = new JRadioButton();
	JRadioButton NDCS_radioButton = new JRadioButton();
	JRadioButton exponential_radioButton = new JRadioButton();
	JRadioButton gamma_radioButton = new JRadioButton();
	JRadioButton beta_radioButton = new JRadioButton();
	JRadioButton agentBasedNormal_radioButton = new JRadioButton();
	JRadioButton agentBasedUniform_radioButton = new JRadioButton();
	JRadioButton modifiedNormal_radioButton = new JRadioButton();
	JRadioButton modifiedUniform_radioButton = new JRadioButton();
	JRadioButton solveCSGBasedOnUserInput_radioButton = new JRadioButton();
	JRadioButton solveCSGBasedOnPredefinedInput_radioButton = new JRadioButton();

	JRadioButton runCplex_radioButton = new JRadioButton();
	JRadioButton runDP_radioButton = new JRadioButton();
	JRadioButton runIDP_radioButton = new JRadioButton();
	JRadioButton runODP_radioButton = new JRadioButton();
	JRadioButton runIP_radioButton = new JRadioButton();
	JRadioButton runODPIP_radioButton = new JRadioButton();
	
	JLabel jLabel1 = new JLabel();
	JLabel jLabel2 = new JLabel();
	JLabel jLabel3 = new JLabel();
	JLabel jLabel5 = new JLabel();
	JLabel jLabel6 = new JLabel();
	JLabel jLabel7 = new JLabel();
	JLabel jLabel10 = new JLabel();
	JLabel jLabel11 = new JLabel();
	JLabel jLabel13 = new JLabel();
	JLabel jLabel14 = new JLabel();

	JCheckBox takeAverage_checkBox = new JCheckBox();
	JCheckBox keepCurrentValues_checkBox = new JCheckBox();
	JCheckBox readCoalitionValuesFromFile_checkBox = new JCheckBox();
	JCheckBox storeCoalitionValuesToFile_checkBox = new JCheckBox();
	JCheckBox ipOrdersIntegerPartitionsAscendingly_checkBox = new JCheckBox();
	JCheckBox printDetailedResultsOfIPToFiles_checkBox = new JCheckBox();
	JCheckBox printDetailsOfSubspaces_checkBox = new JCheckBox();
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
		if( solveCSGBasedOnPredefinedInput_radioButton.isSelected() ){ // If we want to run experiments from a pre-defined input
			prevCoalitionValues = null; // we won't be using any previous values
			PredefinedExperiments predefinedExperiments = new PredefinedExperiments();
			predefinedExperiments.run();
		}else{
			//Read the user input
			Input input = readInputFromGUI(); //read the input parameters from the GUI.
			if( input == null ) return; // If "input" is null, then there was an error in the input, and no action is taken
				
			//Set the name and path of the output folder
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd (HH-mm-ss)" );
			input.outputFolder = "D:/CSGtemp/"+input.numOfAgents+"agents "+simpleDateFormat.format(calendar.getTime());			

			if( keepCurrentValues_checkBox.isSelected() ) //If the user wants to keep the previous coalition values
				input.coalitionValues = prevCoalitionValues;
			else{				 
				prevCoalitionValues = null; //empty the memory allocated to previous values, and generate new values
				if( input.readCoalitionValuesFromFile ){
					input.readCoalitionValuesFromFile( 1 );
				}else{
					input.generateCoalitionValues();					
				}
			}
			//Run the CSG algorithm(s)
			Result result = (new MainSolver()).solve( input );

			//Print the results on the GUI
			printResultOnGUI(input, result);

			//Store the coalition values in case they were needed in the next run
			prevCoalitionValues = input.coalitionValues;
		}
	}
	
	//*****************************************************************************************************
	
	/**
	 * This method reads the user input and fills it in an object of "Input"
	 */
	public Input readInputFromGUI()
	{
		Input input = new Input();
		input.feasibleCoalitions = null; //You currently cannot set this from the GUI
		
		//Read the number of agents
		input.numOfAgents = (new Integer(numOfAgents_textField.getText())).intValue();
		if( input.numOfAgents > 25 ){
			JOptionPane.showMessageDialog(null,	"The number of coalition structures cannot be handled by java for more than 25 agents!","Alert", JOptionPane.ERROR_MESSAGE);
			return(null); //Return null to indicate that there was an error in reading the input.
		}
		//set the folders in which the coalition values are stored, or read from.
		input.folderInWhichCoalitionValuesAreStored = "D:/CSGtemp/coalitionValues";
		
		//Read whether to take the average of running multiple times
		if (takeAverage_checkBox.isSelected()) input.numOfRunningTimes = (new Integer(numOfRunningtimes_textField.getText())).intValue();
		else input.numOfRunningTimes = 1;

		if (ipOrdersIntegerPartitionsAscendingly_checkBox.isSelected()) input.orderIntegerPartitionsAscendingly = true; else input.orderIntegerPartitionsAscendingly = false;

		input.acceptableRatio = (new Double(ipAcceptableRatio_textField.getText())).doubleValue();

		//Read the distribution of coalition values to be generated
		if (uniform_radioButton.isSelected()) input.valueDistribution = ValueDistribution.UNIFORM;
		else if (normal_radioButton.isSelected()) input.valueDistribution = ValueDistribution.NORMAL;
		else if (NDCS_radioButton.isSelected()) input.valueDistribution = ValueDistribution.NDCS;
		else if (beta_radioButton.isSelected()) input.valueDistribution = ValueDistribution.BETA; 
		else if (exponential_radioButton.isSelected()) input.valueDistribution = ValueDistribution.EXPONENTIAL;
		else if (modifiedUniform_radioButton.isSelected()) input.valueDistribution = ValueDistribution.MODIFIEDUNIFORM;
		else if (modifiedNormal_radioButton.isSelected()) input.valueDistribution = ValueDistribution.MODIFIEDNORMAL;
		else if (agentBasedUniform_radioButton.isSelected()) input.valueDistribution = ValueDistribution.AGENTBASEDUNIFORM;
		else if (agentBasedNormal_radioButton.isSelected()) input.valueDistribution = ValueDistribution.AGENTBASEDNORMAL;
						
		// Select the algorithm you want to run
		if( runDP_radioButton.isSelected()   ) input.solverName = SolverNames.DP;
		else if( runIDP_radioButton.isSelected()  ) input.solverName = SolverNames.IDP;
		else if( runODP_radioButton.isSelected()  ) input.solverName = SolverNames.ODP;
		else if( runIP_radioButton.isSelected()   ) input.solverName = SolverNames.IP;
		else if( runODPIP_radioButton.isSelected()) input.solverName = SolverNames.ODPIP;
		else if( runCplex_radioButton.isSelected()) input.solverName = SolverNames.CPLEX;

		// Printing options
		if (printDetailedResultsOfIPToFiles_checkBox.isSelected()) input.printInterimResultsOfIPToFiles = true; else input.printInterimResultsOfIPToFiles = false;
		if (storeCoalitionValuesToFile_checkBox.isSelected()) input.storeCoalitionValuesInFile = true; else input.storeCoalitionValuesInFile = false;
		if (readCoalitionValuesFromFile_checkBox.isSelected()) input.readCoalitionValuesFromFile = true; else input.readCoalitionValuesFromFile = false;
		if (printDetailsOfSubspaces_checkBox.isSelected()) input.printDetailsOfSubspaces = true; else input.printDetailsOfSubspaces = false;
		if (printTimeTakenByIPForEachSubspace_checkBox.isSelected()) input.printTimeTakenByIPForEachSubspace = true; else input.printTimeTakenByIPForEachSubspace = false;

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
		setSize(new Dimension(1295, 750));
		setTitle("Exact Algorithms for the Complete Set Partitioning Problem");
		textArea.setBorder(BorderFactory.createEtchedBorder());
		textArea.setText("");
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888         Content Pane          888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888		
		
		contentPane = (JPanel) getContentPane();
		contentPane.setLayout(null);
		contentPane.setMinimumSize(new Dimension(1, 1));
		contentPane.add(jLabel3);
		contentPane.add(jLabel5);
		contentPane.add(jLabel6);
		contentPane.add(jLabel7);
		contentPane.add(jLabel10);
		contentPane.add(jLabel11);
		
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
		contentPane.add(jPanel3);
		contentPane.add(jPanel4);
		contentPane.add(jPanel5);
		contentPane.add(jPanel7);
		contentPane.add(jPanel9);

		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel1           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel1.setBounds(new Rectangle(650, 55, 290, 65));
		jPanel1.setBorder(BorderFactory.createEtchedBorder());
		jPanel1.setLayout(null);

		jLabel3.setText("    Problem to be solved");
		jLabel3.setBounds(new Rectangle(670, 45, 130, 18));
		jLabel3.setOpaque(true);	
		
		jPanel1.add(solveCSGBasedOnUserInput_radioButton);
		radioButtonGroup3.add(solveCSGBasedOnUserInput_radioButton);
		solveCSGBasedOnUserInput_radioButton.setText("Solve the CSG problem based on the user's input");
		solveCSGBasedOnUserInput_radioButton.setBounds(6, 10, 282, 22);
		solveCSGBasedOnUserInput_radioButton.addMouseListener(new MainFrame_solveCSGBasedOnUserInput_radioButton_mouseAdapter(this));
		solveCSGBasedOnUserInput_radioButton.setSelected(true);

		jPanel1.add(solveCSGBasedOnPredefinedInput_radioButton);
		radioButtonGroup3.add(solveCSGBasedOnPredefinedInput_radioButton);
		solveCSGBasedOnPredefinedInput_radioButton.setText("Get settings from: PredefinedExperiments.java");
		solveCSGBasedOnPredefinedInput_radioButton.setBounds(6, 35, 282, 22);
		solveCSGBasedOnPredefinedInput_radioButton.addMouseListener(new MainFrame_solveCSGBasedOnPredefinedInput_radioButton_mouseAdapter(this));
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel3           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel3.setBounds(new Rectangle(650, 135, 290, 265));
		jPanel3.setBorder(BorderFactory.createEtchedBorder());
		jPanel3.setLayout(null);		

		jLabel5.setText("    Generate random data ");
		jLabel5.setBounds(new Rectangle(670, 125, 132, 18));
		jLabel5.setOpaque(true);
		
		jPanel3.add(uniform_radioButton);
		radioButtonGroup1.add(uniform_radioButton);
		uniform_radioButton.setText("Uniform distribution");
		uniform_radioButton.setBounds(new Rectangle(6, 10, 270, 22));

		jPanel3.add(normal_radioButton);
		radioButtonGroup1.add(normal_radioButton);
		normal_radioButton.setText("Normal distribution");
		normal_radioButton.setBounds(new Rectangle(6, 35, 270, 22));
		normal_radioButton.setSelected(true);
		
		jPanel3.add(NDCS_radioButton);
		radioButtonGroup1.add(NDCS_radioButton);
		NDCS_radioButton.setText("NDCS (Normally Distributed Coalition Structures)");
		NDCS_radioButton.setBounds(new Rectangle(6,60,270,22));
		
		jPanel3.add(beta_radioButton);
		radioButtonGroup1.add(beta_radioButton);
		beta_radioButton.setText("Beta distribution");
		beta_radioButton.setBounds(new Rectangle(6,85,270,22));
		
		jPanel3.add(gamma_radioButton);
		radioButtonGroup1.add(gamma_radioButton);
		gamma_radioButton.setText("Gamma distribution");
		gamma_radioButton.setBounds(new Rectangle(6,110,270,22));
		
		jPanel3.add(exponential_radioButton);
		radioButtonGroup1.add(exponential_radioButton);
		exponential_radioButton.setText("Exponential distribution");
		exponential_radioButton.setBounds(new Rectangle(6,135,270,22));
		
		jPanel3.add(modifiedUniform_radioButton);
		radioButtonGroup1.add(modifiedUniform_radioButton);
		modifiedUniform_radioButton.setText("Modified Uniform distribution");
		modifiedUniform_radioButton.setBounds(new Rectangle(6,160,270,22));
		
		jPanel3.add(modifiedNormal_radioButton);
		radioButtonGroup1.add(modifiedNormal_radioButton);
		modifiedNormal_radioButton.setText("Modified Normal distribution");
		modifiedNormal_radioButton.setBounds(new Rectangle(6,185,270,22));
		
		jPanel3.add(agentBasedUniform_radioButton);
		radioButtonGroup1.add(agentBasedUniform_radioButton);
		agentBasedUniform_radioButton.setText("Agent-based Uniform distribution");
		agentBasedUniform_radioButton.setBounds(new Rectangle(6,210,270,22));
		
		jPanel3.add(agentBasedNormal_radioButton);
		radioButtonGroup1.add(agentBasedNormal_radioButton);
		agentBasedNormal_radioButton.setText("Agent-based Normal distribution");
		agentBasedNormal_radioButton.setBounds(new Rectangle(6,235,270,22));
		
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel7           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel7.setBounds(new Rectangle(650, 415, 290, 165));
		jPanel7.setBorder(BorderFactory.createEtchedBorder());
		jPanel7.setLayout(null);
	
		jLabel10.setText("    Select an algorithm");
		jLabel10.setBounds(new Rectangle(670, 405, 135, 18));
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
		//888888888888888888888888             jPanel4           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel4.setBounds(new Rectangle(955, 55, 315, 120));
		jPanel4.setBorder(BorderFactory.createEtchedBorder());
		jPanel4.setLayout(null);

		jLabel6.setText("    Solver settings");
		jLabel6.setBounds(new Rectangle(965, 45, 100, 18));
		jLabel6.setOpaque(true);
		
		jPanel4.add(keepCurrentValues_checkBox);
		keepCurrentValues_checkBox.setText("Use coalition values from the previous run");
		keepCurrentValues_checkBox.setBounds(new Rectangle(6, 10, 303, 22));
		
		jPanel4.add(storeCoalitionValuesToFile_checkBox);
		storeCoalitionValuesToFile_checkBox.setText("Store coalition values in a file");
		storeCoalitionValuesToFile_checkBox.setBounds(new Rectangle(6, 35, 303, 22));

		jPanel4.add(readCoalitionValuesFromFile_checkBox);
		readCoalitionValuesFromFile_checkBox.setText("Read coalition values from a file");
		readCoalitionValuesFromFile_checkBox.setBounds(new Rectangle(6, 60, 303, 22));
		
		jPanel4.add(takeAverage_checkBox);
		takeAverage_checkBox.setText("Show average after running a No. of times =");
		takeAverage_checkBox.setBounds(new Rectangle(6, 85, 257, 22));

		jPanel4.add(numOfRunningtimes_textField);
		numOfRunningtimes_textField.setBounds(new Rectangle(263, 85, 33, 22));
		numOfRunningtimes_textField.setText("");
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel5           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888		
		
		jPanel5.setBounds(new Rectangle(955, 190, 315, 110));
		jPanel5.setBorder(BorderFactory.createEtchedBorder());
		jPanel5.setLayout(null);	

		jLabel11.setText("    Printing options");
		jLabel11.setBounds(new Rectangle(965, 180, 107, 18));
		jLabel11.setOpaque(true);
		
		jPanel5.add(printDetailsOfSubspaces_checkBox);
		printDetailsOfSubspaces_checkBox.setText("Print details of subspaces (on console screen)");
		printDetailsOfSubspaces_checkBox.setBounds(new Rectangle(6, 10, 303, 22));

		jPanel5.add(printDetailedResultsOfIPToFiles_checkBox);
		printDetailedResultsOfIPToFiles_checkBox.setText("Print the interim results of IP to an output file");
		printDetailedResultsOfIPToFiles_checkBox.setBounds(new Rectangle(6, 35, 303, 22));
		
		jPanel5.add(printTimeTakenByIPForEachSubspace_checkBox);
		printTimeTakenByIPForEachSubspace_checkBox.setText("Print the time taken by IP to search each subspace");
		printTimeTakenByIPForEachSubspace_checkBox.setBounds(new Rectangle(6, 60, 303, 22));
		
		jPanel5.add(jLabel13);
		jLabel13.setText("(on console screen and to an output file)");
		jLabel13.setBounds(new Rectangle(27, 78, 250, 18));
		
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888
		//888888888888888888888888             jPanel9           888888888888888888888888
		//8888888888888888888888888888888888888888888888888888888888888888888888888888888

		jPanel9.setBounds(new Rectangle(955, 315, 315, 68));
		jPanel9.setBorder(BorderFactory.createEtchedBorder());
		jPanel9.setLayout(null);
		
		jLabel7.setText("    IP & ODP-IP options    ");
		jLabel7.setBounds(new Rectangle(965, 305, 128, 18));
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
	
	public void solveCSGBasedOnPredefinedInput_radioButton_mouseClicked(MouseEvent e)
	{
		numOfAgents_textField.setEnabled(false);
		numOfRunningtimes_textField.setEnabled(false);
		runIP_radioButton.setEnabled(false);
		runDP_radioButton.setEnabled(false);
		runIDP_radioButton.setEnabled(false);
		runODP_radioButton.setEnabled(false);
		runODPIP_radioButton.setEnabled(false);
		runCplex_radioButton.setEnabled(false);
		normal_radioButton.setEnabled(false);
		uniform_radioButton.setEnabled(false);
		NDCS_radioButton.setEnabled(false);
		exponential_radioButton.setEnabled(false);
		beta_radioButton.setEnabled(false);
		gamma_radioButton.setEnabled(false);
		agentBasedNormal_radioButton.setEnabled(false);
		agentBasedUniform_radioButton.setEnabled(false);
		modifiedNormal_radioButton.setEnabled(false);
		modifiedUniform_radioButton.setEnabled(false);
		takeAverage_checkBox.setEnabled(false);
		keepCurrentValues_checkBox.setEnabled(false);
		jLabel2.setEnabled(false);
		ipAcceptableRatio_textField.setEnabled(false);
		ipOrdersIntegerPartitionsAscendingly_checkBox.setEnabled(false);
		printDetailedResultsOfIPToFiles_checkBox.setEnabled(false);
		readCoalitionValuesFromFile_checkBox.setEnabled(false);
		storeCoalitionValuesToFile_checkBox.setEnabled(false);
		printDetailsOfSubspaces_checkBox.setEnabled(false);
		printTimeTakenByIPForEachSubspace_checkBox.setEnabled(false);
		jLabel13.setEnabled(false);
	}

	public void solveCSGBasedOnUserInput_radioButton_mouseClicked(MouseEvent e)
	{
		numOfAgents_textField.setEnabled(true);
		numOfRunningtimes_textField.setEnabled(true);
		runIP_radioButton.setEnabled(false);
		runDP_radioButton.setEnabled(false);
		runIDP_radioButton.setEnabled(false);
		runODP_radioButton.setEnabled(false);
		runODPIP_radioButton.setEnabled(false);
		normal_radioButton.setEnabled(true);
		uniform_radioButton.setEnabled(true);
		NDCS_radioButton.setEnabled(true);
		exponential_radioButton.setEnabled(true);
		beta_radioButton.setEnabled(true);
		gamma_radioButton.setEnabled(true);
		agentBasedNormal_radioButton.setEnabled(true);
		agentBasedUniform_radioButton.setEnabled(true);
		modifiedNormal_radioButton.setEnabled(true);
		modifiedUniform_radioButton.setEnabled(true);
		takeAverage_checkBox.setEnabled(true);
		keepCurrentValues_checkBox.setEnabled(true);
		ipOrdersIntegerPartitionsAscendingly_checkBox.setEnabled(true);
		printDetailedResultsOfIPToFiles_checkBox.setEnabled(true);
		readCoalitionValuesFromFile_checkBox.setEnabled(true);
		storeCoalitionValuesToFile_checkBox.setEnabled(true);
		printDetailsOfSubspaces_checkBox.setEnabled(true);
		printTimeTakenByIPForEachSubspace_checkBox.setEnabled(true);
		jLabel13.setEnabled(true);
		ipAcceptableRatio_textField.setEnabled(true);
		jLabel2.setEnabled(true);
		runIP_radioButton.setEnabled(true);
		runDP_radioButton.setEnabled(true);
		runIDP_radioButton.setEnabled(true);
		runODP_radioButton.setEnabled(true);
		runODPIP_radioButton.setEnabled(true);
		runCplex_radioButton.setEnabled(true);
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

//*****************************************************************************************************

class MainFrame_solveCSGBasedOnUserInput_radioButton_mouseAdapter extends MouseAdapter {
	private MainFrame adaptee;
	MainFrame_solveCSGBasedOnUserInput_radioButton_mouseAdapter(MainFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void mouseClicked(MouseEvent e) {
		adaptee.solveCSGBasedOnUserInput_radioButton_mouseClicked(e);
	}
}

class MainFrame_solveCSGBasedOnPredefinedInput_radioButton_mouseAdapter extends MouseAdapter {
	private MainFrame adaptee;
	MainFrame_solveCSGBasedOnPredefinedInput_radioButton_mouseAdapter(MainFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void mouseClicked(MouseEvent e) {
		adaptee.solveCSGBasedOnPredefinedInput_radioButton_mouseClicked(e);
	}
}