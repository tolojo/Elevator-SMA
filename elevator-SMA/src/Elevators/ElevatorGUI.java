package Elevators;

import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

public class ElevatorGUI extends Agent {
    private JFrame frame = null;
    private Simulator simulator;
    private JTextPane logPane;

    @Override
    protected void setup() {
        try {
            getJFrame().setVisible(true);
        } catch (StaleProxyException ex) {
            Logger.getLogger(ElevatorGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                    int maxFloors = (int) spinner.getValue();
                    int numElevators = (int) spinner_1.getValue();
                    int maxCapacity = (int) spinner_2.getValue();
                    if (maxFloors <= 1 || numElevators <= 0 || maxCapacity <= 0) {
                        throw new RuntimeException("Invalid inputs!");
                    }

                    OnStartSimulation(maxFloors, numElevators, maxCapacity);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Erro! Utilize números acima de 0.",
                            "Erro nos campos introduzidos", JOptionPane.ERROR_MESSAGE);
                    throw new RuntimeException(ex);
                }
            });
            btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 17));
            btnNewButton.setBounds(10, 146, 176, 33);
            frame.getContentPane().add(btnNewButton);

            JButton btnPararSimulao = new JButton("Parar Simulação");
            btnPararSimulao.addActionListener(e -> {
                OnStopSimulation();
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
                        throw new RuntimeException("Invalid inputs!");
                    }
                    OnCallElevator(currentFloor, desiredFloor);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Erro! Utilize números iguais ou acima de 0.",
                            "Erro nos campos introduzidos", JOptionPane.ERROR_MESSAGE);
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
            logPane.setToolTipText("");
            logPane.setFont(new Font("Tahoma", Font.PLAIN, 12));
            scrollPane.setViewportView(logPane);
        }

        return frame;
    }

    private void OnStartSimulation(int maxFloors, int numElevators, int maxCapacity) throws StaleProxyException {
        Log("Simulação Iniciada: Edificio com " + maxFloors + " pisos e " + numElevators + " elevadores, cada um" +
                " com lotação " + maxCapacity);
        Runtime rt = Runtime.instance();
        ProfileImpl p = new ProfileImpl(false);
        AgentContainer container = rt.createMainContainer(p);
//        AgentController agent = container.createNewAgent("Simulator", "jade.Simulator", null);
//        agent.start();
    }

    private void OnStopSimulation() {

    }

    private void OnCallElevator(int currentFloor, int desiredFloor) {

    }

    public void Log(String msg) {
        logPane.setText(logPane.getText() + "\n> " + msg);
    }
}
