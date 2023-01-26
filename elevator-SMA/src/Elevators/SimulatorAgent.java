package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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
    private Behaviour searchForElevatorsBehaviour, createCallsBehaviour, receiveMsgBehaviour;
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
            elevatorGUI = new ElevatorGUI(this);
            elevatorGUI.getJFrame().setVisible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        searchForElevatorsBehaviour = new SearchForElevatorsBehaviour(this, 3000);
        createCallsBehaviour = new CreateCallsBehaviour(this, 10000);
        receiveMsgBehaviour = new ReceiveMsgBehaviour();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                if (elevatorGUI.hasSimulationStarted() && flag == 0) {
                    maxFloors = elevatorGUI.getMaxFloors();
                    numOfElevators = elevatorGUI.getNumOfElevators();
                    maxCapacity = elevatorGUI.getMaxCapacity();
                    addBehaviour(searchForElevatorsBehaviour);
                    addBehaviour(createCallsBehaviour);
                    addBehaviour(receiveMsgBehaviour);
                    flag = 1;
                }
                if (!elevatorGUI.hasSimulationStarted() && flag == 1) {
                    removeBehaviour(searchForElevatorsBehaviour);
                    removeBehaviour(createCallsBehaviour);
                    removeBehaviour(receiveMsgBehaviour);
                    flag = 0;
                }
            }
        });
    }

    public void callElevator(int currentFloor, int desiredFloor) {
        Random random = new Random();
        int randomElevator = random.nextInt(numOfElevators);

        //Vamos escolher um dos elevadores na DF que nao seja o agente Simulador
        while (elevatorsAID.get(randomElevator).getName().equals(getName()))
            randomElevator = random.nextInt(numOfElevators);

        AID elevAid = elevatorsAID.get(randomElevator);
        ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
        aclMessage.setPerformative(ACLMessage.REQUEST);
        aclMessage.addReceiver((AID) elevAid);
        aclMessage.setContent(currentFloor + "," + desiredFloor);
        send(aclMessage);
        System.out.println("> Novo pedido no piso " + currentFloor + " para o piso " + desiredFloor +
                ". Elevador escolhido: " + elevAid.getLocalName());
        elevatorGUI.newCallForElevator(currentFloor, desiredFloor);
    }

    private class SearchForElevatorsBehaviour extends TickerBehaviour {

        public SearchForElevatorsBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            try {
                Vector<AID> elevatorAux = new Vector<>();
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Elevator-Agent");
                dfd.addServices(sd);
                DFAgentDescription[] result = DFService.search(this.getAgent(), dfd);
                if (result.length > 0) {
                    for (DFAgentDescription dfAgentDescription : result)
                        elevatorAux.addElement(dfAgentDescription.getName());
                    elevatorsAID = elevatorAux;
                }

            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private class CreateCallsBehaviour extends TickerBehaviour {

        public CreateCallsBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (numOfElevators <= 1) return;
            Random random = new Random();
            int currentFloor = random.nextInt(maxFloors);
            int desiredFloor = random.nextInt(maxFloors);
            while (desiredFloor == currentFloor) currentFloor = random.nextInt(maxFloors);
            callElevator(currentFloor, desiredFloor);
        }
    }

    private class ReceiveMsgBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage aclMessage = myAgent.receive();
            if (aclMessage != null && aclMessage.getPerformative() == ACLMessage.INFORM) {
                String info = aclMessage.getContent();
                String[] msgSplit = info.split(",");
                int receivedFloor = Integer.parseInt(msgSplit[1]);
                int agentIndex = Integer.parseInt(msgSplit[2]);
                elevatorGUI.moveElevator(agentIndex, receivedFloor);

                //TODO Precisa receber o numero de pessoas dentro do elevador
                //TODO Precisa receber que elevador foi atribuido a cada pedido
            }
        }
    }
}
