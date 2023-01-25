package Elevators;

import jade.core.AID;
import jade.wrapper.*;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ElevatorGUI {
    private JFrame frame = null;
    private JTextPane logPane;
    private final Vector<AgentController> agentControllers = new Vector<>();
    private PlatformController platformController;
    private final AID simulatorAID;
    private boolean hasSimulationStarted;
    private int maxFloors, numOfElevators, maxCapacity;
    private int[][] elevatorStatus;

    public ElevatorGUI(PlatformController platformController, AID simulatorAID) {
        this.platformController = platformController;
        this.simulatorAID = simulatorAID;
    }

    public JFrame getJFrame() throws StaleProxyException {
        if (frame == null) {
            frame = new JFrame("Simulador de Elevadores utilizando Agentes");
            frame.setBounds(100, 100, 1000, 410);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setLayout(null);

            JLabel lblNewLabel = new JLabel("Número de pisos no edificio");
            lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
            lblNewLabel.setBounds(81, 11, 364, 34);
            frame.getContentPane().add(lblNewLabel);

            JLabel lblNmeroDeElevadores = new JLabel("Número de elevadores existentes");
            lblNmeroDeElevadores.setFont(new Font("Tahoma", Font.PLAIN, 16));
            lblNmeroDeElevadores.setBounds(81, 56, 364, 34);
            frame.getContentPane().add(lblNmeroDeElevadores);

            JLabel lblCargaMximaDos = new JLabel("Carga máxima dos elevadores (num de pessoas)");
            lblCargaMximaDos.setFont(new Font("Tahoma", Font.PLAIN, 16));
            lblCargaMximaDos.setBounds(81, 101, 364, 34);
            frame.getContentPane().add(lblCargaMximaDos);

            JSpinner spinner = new JSpinner();
            spinner.setBounds(10, 20, 61, 20);
            frame.getContentPane().add(spinner);

            JSpinner spinner_1 = new JSpinner();
            spinner_1.setBounds(10, 65, 61, 20);
            frame.getContentPane().add(spinner_1);

            JSpinner spinner_2 = new JSpinner();
            spinner_2.setBounds(10, 110, 61, 20);
            frame.getContentPane().add(spinner_2);

            JCheckBox chckbxUseSmartElevators = new JCheckBox("Utilizar Elevadores Inteligentes");
            chckbxUseSmartElevators.setFont(new Font("Tahoma", Font.PLAIN, 16));
            chckbxUseSmartElevators.setBounds(10, 142, 280, 34);
            frame.getContentPane().add(chckbxUseSmartElevators);

            JButton btnStartSimulation = new JButton("Iniciar Simulação");
            btnStartSimulation.addActionListener(e -> {
                try {
                    maxFloors = (int) spinner.getValue();
                    numOfElevators = (int) spinner_1.getValue();
                    maxCapacity = (int) spinner_2.getValue();
                    if (maxFloors <= 1) {
                        throw new RuntimeException("Precisa indicar pelo menos 2 pisos.");
                    }
                    if (numOfElevators <= 0) {
                        throw new RuntimeException("Precisa indicar pelo menos 1 elevador.");
                    }
                    if (maxCapacity <= 0) {
                        throw new RuntimeException("Os elevadores precisam de uma capacidade de pelo menos 1 pessoa.");
                    }

                    OnStartSimulation(maxFloors, numOfElevators, maxCapacity, chckbxUseSmartElevators.isSelected());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(),
                            "Erro ao iniciar simulação", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException(ex);
                }
            });
            btnStartSimulation.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnStartSimulation.setBounds(10, 183, 176, 33);
            frame.getContentPane().add(btnStartSimulation);

            JButton btnStopSimulation = new JButton("Parar Simulação");
            btnStopSimulation.addActionListener(e -> {
                try {
                    OnStopSimulation();
                } catch (StaleProxyException ex) {
                    throw new RuntimeException(ex);
                }
            });
            btnStopSimulation.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnStopSimulation.setBounds(196, 183, 176, 33);
            frame.getContentPane().add(btnStopSimulation);

            JSpinner spinnerCurrentFloor = new JSpinner();
            spinnerCurrentFloor.setBounds(10, 247, 61, 20);
            frame.getContentPane().add(spinnerCurrentFloor);

            JLabel lblEstouNestePiso = new JLabel("Estou neste piso");
            lblEstouNestePiso.setFont(new Font("Tahoma", Font.PLAIN, 16));
            lblEstouNestePiso.setBounds(81, 238, 364, 34);
            frame.getContentPane().add(lblEstouNestePiso);

            JLabel lblQueroIrPara = new JLabel("Quero ir para este piso");
            lblQueroIrPara.setFont(new Font("Tahoma", Font.PLAIN, 16));
            lblQueroIrPara.setBounds(81, 283, 364, 34);
            frame.getContentPane().add(lblQueroIrPara);

            JSpinner spinnerDesiredFloor = new JSpinner();
            spinnerDesiredFloor.setBounds(10, 292, 61, 20);
            frame.getContentPane().add(spinnerDesiredFloor);

            JButton btnCallElevator = new JButton("Chamar Elevador");
            btnCallElevator.addActionListener(e -> {
                try {
                    int currentFloor = (int) spinnerCurrentFloor.getValue();
                    int desiredFloor = (int) spinnerDesiredFloor.getValue();
                    if (currentFloor < 0 || desiredFloor < 0) {
                        throw new RuntimeException("Não existem pisos negativos nesta simulação.");
                    }
                    if (currentFloor == desiredFloor) {
                        throw new RuntimeException("Não pode chamar o elevador para o mesmo piso.");
                    }

                    OnCallElevator(currentFloor, desiredFloor);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(),
                            "Erro ao chamar elevador", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException();
                }
            });
            btnCallElevator.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnCallElevator.setBounds(10, 328, 176, 33);
            frame.getContentPane().add(btnCallElevator);

            JSeparator separator = new JSeparator();
            separator.setBounds(10, 227, 420, 9);
            frame.getContentPane().add(separator);

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setBounds(440, 11, 536, 350);
            frame.getContentPane().add(scrollPane);

            logPane = new JTextPane();
            logPane.setEditable(false);
            logPane.setFont(new Font("Tahoma", Font.PLAIN, 16));
            scrollPane.setViewportView(logPane);

            JButton btnClearLogs = new JButton("Limpar Logs");
            btnClearLogs.addActionListener(e -> {
                logPane.setText("");
            });
            btnClearLogs.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnClearLogs.setBounds(196, 328, 176, 33);
            frame.getContentPane().add(btnClearLogs);
        }

        return frame;
    }

    private void OnStartSimulation(int maxFloors, int numElevators, int maxCapacity, boolean useSmartElevatores) throws ControllerException {
        String elevatorName = useSmartElevatores ? "SmartElevatorAgent-" : "ElevatorAgent-";
        String elevatorPath = useSmartElevatores ? "Elevators.SmartElevatorAgent" : "Elevators.ElevatorAgent";
        for (int i = 1; i <= numElevators; i++) {
            AgentController elevatorAgent = platformController.createNewAgent(elevatorName + i, elevatorPath,
                    new Object[]{simulatorAID, i, maxCapacity});
            elevatorAgent.start();
            agentControllers.add(elevatorAgent);
        }

        log("Simulação Iniciada: Edificio com " + maxFloors + " pisos e " + numElevators + " elevadores, cada um" +
                " com lotação " + maxCapacity);
        hasSimulationStarted = true;
        elevatorStatus = new int[maxFloors][numOfElevators];
    }

    private void OnStopSimulation() throws StaleProxyException {
        for (AgentController agent : agentControllers) {
            agent.kill();
        }
        hasSimulationStarted = false;
        elevatorStatus = null;
    }

    private void OnCallElevator(int currentFloor, int desiredFloor) {

    }

    public void log(String msg) {
        logPane.setText(logPane.getText() + "\n> " + msg);
    }

    public void moveElevator(int elevatorNum, int currentFloor) {
        for (int i = 0; i < elevatorStatus.length; i++)
            elevatorStatus[i][elevatorNum - 1] = 0;
        elevatorStatus[currentFloor][elevatorNum - 1] = 1;
        prettyLog();
    }

    private void prettyLog() {
        StringBuilder stringToLog = new StringBuilder();
        for (int i = maxFloors - 1; i >= 0; i--) {
            stringToLog.append("|");
            for (int j = 0; j < numOfElevators; j++) {
                if (elevatorStatus[i][j] == 1) stringToLog.append("[]|");
                else stringToLog.append("--|");
            }
            stringToLog.append("\n");
        }
        logPane.setText(stringToLog.toString());
    }

    public boolean hasSimulationStarted() {
        return hasSimulationStarted;
    }

    public int getMaxFloors() {
        return maxFloors;
    }

    public int getNumOfElevators() {
        return numOfElevators;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public Vector<AgentController> getAgentControllers() {
        return agentControllers;
    }
}
