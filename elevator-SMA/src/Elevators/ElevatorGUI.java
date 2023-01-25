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

            JButton btnNewButton = new JButton("Iniciar Simulação");
            btnNewButton.addActionListener(e -> {
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

                    OnStartSimulation(maxFloors, numOfElevators, maxCapacity);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(),
                            "Erro ao iniciar simulação", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException(ex);
                }
            });
            btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnNewButton.setBounds(10, 146, 176, 33);
            frame.getContentPane().add(btnNewButton);

            JButton btnPararSimulao = new JButton("Parar Simulação");
            btnPararSimulao.addActionListener(e -> {
                try {
                    OnStopSimulation();
                } catch (StaleProxyException ex) {
                    throw new RuntimeException(ex);
                }
            });
            btnPararSimulao.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnPararSimulao.setBounds(196, 146, 176, 33);
            frame.getContentPane().add(btnPararSimulao);

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

            JButton btnCriarTarefa = new JButton("Chamar Elevador");
            btnCriarTarefa.addActionListener(e -> {
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
            btnCriarTarefa.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnCriarTarefa.setBounds(10, 328, 176, 33);
            frame.getContentPane().add(btnCriarTarefa);

            JSeparator separator = new JSeparator();
            separator.setBounds(10, 209, 420, 27);
            frame.getContentPane().add(separator);

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setBounds(440, 11, 536, 350);
            frame.getContentPane().add(scrollPane);

            logPane = new JTextPane();
            logPane.setEditable(false);
            logPane.setFont(new Font("Tahoma", Font.PLAIN, 16));
            scrollPane.setViewportView(logPane);

            JButton btnLimparLogs = new JButton("Limpar Logs");
            btnLimparLogs.addActionListener(e -> {
                logPane.setText("");
            });
            btnLimparLogs.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnLimparLogs.setBounds(196, 328, 176, 33);
            frame.getContentPane().add(btnLimparLogs);
        }

        return frame;
    }

    private void OnStartSimulation(int maxFloors, int numElevators, int maxCapacity) throws ControllerException {
        for (int i = 1; i <= numElevators; i++) {
            AgentController elevatorAgent = platformController.createNewAgent("ElevatorAgent-" + i, "Elevators.ElevatorAgent",
                    new Object[]{simulatorAID, i, maxCapacity});
            elevatorAgent.start();
            agentControllers.add(elevatorAgent);
        }
        
        log("Simulação Iniciada: Edificio com " + maxFloors + " pisos e " + numElevators + " elevadores, cada um" +
                " com lotação " + maxCapacity);
        hasSimulationStarted = true;
    }
    
    private void OnStopSimulation() throws StaleProxyException {
        for (AgentController agent: agentControllers) {
            agent.kill();
        }
    }

    private void OnCallElevator(int currentFloor, int desiredFloor) {

    }

    public void log(String msg) {
        logPane.setText(logPane.getText() + "\n> " + msg);
    }
    
    public void prettyLog(int elevatorNum, int currentFloor) {
        StringBuilder stringToLog = new StringBuilder();
        for (int i = maxFloors; i >= 0; i--) {
            stringToLog.append("|");
            for (int j = 1; j <= numOfElevators; j++) {
                if (elevatorNum == j && currentFloor == i) stringToLog.append("[]|");
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
