package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.Random;
import java.util.Vector;

public class SimulatorAgent extends Agent {

    private ElevatorGUI elevatorGUI;
    private AID myAid = getAID();
    private Vector<AID> elevatorsAID = new Vector<>();
    private int maxFloors, numOfElevators, maxCapacity;
    private int flag;

    public void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Elevator-Agent");
        sd.setName("Elevator-Service");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        try {
            elevatorGUI = new ElevatorGUI(getContainerController().getPlatformController(), getAID());
            elevatorGUI.getJFrame().setVisible(true);
        } catch (ControllerException e) {
            throw new RuntimeException(e);
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                if (elevatorGUI.hasSimulationStarted() && flag == 0) {
                    maxFloors = elevatorGUI.getMaxFloors();
                    numOfElevators = elevatorGUI.getNumOfElevators();
                    maxCapacity = elevatorGUI.getMaxCapacity();
                    startSimulation();
                    flag = 1;
                }
                if (!elevatorGUI.hasSimulationStarted() && flag == 1) {
                    flag = 0;
                }
            }
        });
    }

    private void startSimulation() {
        addBehaviour(
                new TickerBehaviour(this, 3000) {
                    protected void onTick() {
                        try {
                            Vector<AID> elevatorAux = new Vector<>();
                            DFAgentDescription dfd = new DFAgentDescription();
                            ServiceDescription sd = new ServiceDescription();
                            sd.setType("Elevator-Agent");
                            dfd.addServices(sd);
                            DFAgentDescription[] result = DFService.search(this.getAgent(), dfd);
                            if (result.length > 0) {
                                for (DFAgentDescription dfAgentDescription : result) {
                                    elevatorAux.addElement(dfAgentDescription.getName());
                                }
                                elevatorsAID = elevatorAux;
                            }

                        } catch (FIPAException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // Gerar pedidos aleatorios
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                if (numOfElevators <= 1) {
                    return;
                }
                Random random = new Random();
                int randomElevator = random.nextInt(numOfElevators);

                //Vamos escolher um dos elevadores na DF que nao seja o agente Simulador
                while (elevatorsAID.get(randomElevator).getName().equals(getName())) {
                    randomElevator = random.nextInt(numOfElevators);
                }

                AID elevAid = elevatorsAID.get(randomElevator);
                int destinationFloor = random.nextInt(maxFloors);
                int initialFloor = random.nextInt(maxFloors);
                while (destinationFloor == initialFloor) {
                    initialFloor = random.nextInt(maxFloors);
                }
                ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
                aclMessage.setPerformative(ACLMessage.REQUEST);
                aclMessage.addReceiver((AID) elevAid);
                aclMessage.setContent(initialFloor + "," + destinationFloor);
                myAgent.send(aclMessage);
                System.out.println("> Novo pedido no piso " + initialFloor + " para o piso " + destinationFloor +
                        ". Elevador escolhido: " + elevAid.getLocalName());
            }
        });
        
        // Recebe mensagens de informaçao dos elevadores para mostrar na GUI
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = myAgent.receive();
                if (aclMessage != null && aclMessage.getPerformative() == ACLMessage.INFORM) {
                    String info = aclMessage.getContent();
                    String[] msgSplit = info.split(",");
                    int receivedFloor = Integer.parseInt(msgSplit[1]);
                    int agentIndex = Integer.parseInt(msgSplit[2]);
                    elevatorGUI.moveElevator(agentIndex, receivedFloor);
                }
            }
        });
    }
}
