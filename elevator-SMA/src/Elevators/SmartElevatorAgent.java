package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.AgentState;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static java.lang.Math.abs;

public class SmartElevatorAgent extends Agent{
    private enum AgentState {StandBy, MovingUp, MovingDown;}
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final int movementDuration = 2;
    private final HashMap<Integer, Integer> currentRequests = new HashMap<>();
    ArrayList<Floor> mostCalledFloors = new ArrayList<>();
    private BlockingQueue<ACLMessage> tasksACL = new BlockingQueue<>(6);
    private Vector<AID> elevatorsAID = new Vector<>();
    private int currentFloor, maxCapacity, myIndex;
    private int movementCost = 5;
    private int numberOfMovs = 0;
    private AID simulatorAID;
    private AgentState myState = AgentState.StandBy;

    private HashMap<String, Integer> elevatorLocation = new HashMap<>();

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

        simulatorAID = (AID) getArguments()[0];
        myIndex = (int) getArguments()[1];
        maxCapacity = (int) getArguments()[2];

        getAgents(this);
        informCurrentFloor(this, currentFloor, true);

        addBehaviour(
                new TickerBehaviour(this, 3000) {
                    protected void onTick() {
                        elevatorsAID = getAgents(myAgent);
                    }
                }
        );

        addBehaviour(
                new TickerBehaviour(this, 3000) {
                    protected void onTick() {
                        elevatorsAID = getAgents(myAgent);
                    }
                }
        );

        Behaviour moveToMostCalled = new TickerBehaviour(this,5000) {
            @Override
            public void onTick() {
                if (!mostCalledFloors.isEmpty()) {
                    Floor floorAux = mostCalledFloors.get(0);
                    for (int i = 0; i < mostCalledFloors.size(); i++) {
                        if (floorAux.getFrequencyCalled() < mostCalledFloors.get(i).getFrequencyCalled()) {
                            floorAux = mostCalledFloors.get(i);
                        }
                    }
                    if (myState.equals(SmartElevatorAgent.AgentState.StandBy)) {
                        try {
                            while (floorAux.getFloor() != currentFloor) {
                                System.out.println("Elevador " + myAgent.getLocalName() + " movendo para o piso de origem mais chamado" + floorAux.getFloor() + " a partir do piso " + currentFloor + " .");
                                numberOfMovs++;
                                if (floorAux.getFloor() < currentFloor) {
                                    Thread.sleep(movementDuration * 1000);
                                    currentFloor--;
                                } else {
                                    Thread.sleep(movementDuration * 1000);
                                    currentFloor++;
                                }
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }


                    }
                }
            }

        };

        addBehaviour(tbf.wrap(moveToMostCalled));

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {
                    if (aclMessage.getPerformative() == ACLMessage.REQUEST && myState == SmartElevatorAgent.AgentState.StandBy) {
                        String[] splitFloors = aclMessage.getContent().split(",");
                        int initialFloor = Integer.parseInt(splitFloors[0]);
                        int destinationFloor = Integer.parseInt(splitFloors[1]);
                        int distance = abs(currentFloor - initialFloor);
                        currentRequests.put(initialFloor, destinationFloor);
                        boolean isChoosen = true;
                        String minAID = "";
                        //verificação de elevador mais perto
                        for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
                            int distanceAux = abs(entry.getValue() - initialFloor);
                            if (distance > distanceAux) {
                                isChoosen = false;
                                minAID = entry.getKey();
                            }
                        }

                        if (isChoosen) {
                            try {


                                //verificação do piso do elevador com o piso do pedido
                                while (initialFloor != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + initialFloor + " a partir do piso " + currentFloor + ".");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    // Descer andar
                                    if (initialFloor < currentFloor) {
                                        myState = SmartElevatorAgent.AgentState.MovingDown;
                                        Thread.sleep(movementDuration * 1000L);
                                        currentFloor--;
                                        // Subir andar
                                    } else {
                                        Thread.sleep(movementDuration * 1000L);
                                        myState = SmartElevatorAgent.AgentState.MovingUp;
                                        currentFloor++;
                                    }
                                }

                                System.out.println(myAgent.getLocalName() + " está no piso " + currentFloor + ", e vai para o piso " + destinationFloor);
                                while (destinationFloor != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + initialFloor + " a partir do piso " + currentFloor + ".");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    // Descer andar
                                    if (destinationFloor < currentFloor) {
                                        Thread.sleep(movementDuration * 1000L);
                                        currentFloor--;
                                        // Subir andar
                                    } else {
                                        Thread.sleep(movementDuration * 1000L);
                                        currentFloor++;
                                    }
                                }
                                myState = SmartElevatorAgent.AgentState.StandBy;
                                System.out.println(myAgent.getLocalName() + " chegou ao piso destino " + destinationFloor);
                                informCurrentFloor(this.getAgent(), currentFloor, true);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            ACLMessage msgNextElevator = new ACLMessage(ACLMessage.REQUEST);
                            msgNextElevator.setPerformative(ACLMessage.REQUEST);
                            //enviar mensagem para o elevador mais próximo
                            System.out.println(myAgent.getLocalName() + " a enviar mensagem para o elevador mais proximo " + minAID);
                            for (AID aid : elevatorsAID) {
                                if (aid.getLocalName().equals(minAID)) {
                                    msgNextElevator.addReceiver(aid);
                                }
                            }
                            msgNextElevator.setContent(initialFloor + "," + destinationFloor);
                            myAgent.send(msgNextElevator);
                        }

                        //receber mensagem de outros elevadores
                    } else if (aclMessage.getPerformative() == ACLMessage.INFORM) {
                        String[] msgSplit = aclMessage.getContent().split(",");
                        String msgAgent = msgSplit[0];
                        int receivedFloor = Integer.parseInt(msgSplit[1]);
                        //se elevador não existir no hashmap
                        if (!elevatorLocation.containsKey(msgAgent)) {
                            elevatorLocation.put(msgAgent, receivedFloor);
                            System.out.println(myAgent.getLocalName() + " adding " + msgAgent + " to floor " + receivedFloor);
                        }
                        //se o elevador já existir na hashmap
                        else {
                            elevatorLocation.put(msgAgent, receivedFloor);
                            System.out.println(myAgent.getLocalName() + " is modifying " + msgAgent + " to floor " + receivedFloor);
                        }
                    }
                }
                aclMessage = null;
            }
        });


    }

    //Apanhar todos os agentes registados na df
    private Vector<AID> getAgents(Agent agent) {
        Vector<AID> elevatorAux = new Vector<>();
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("elevator");
            dfd.addServices(sd);
            DFAgentDescription[] result = DFService.search(agent, dfd);
            //System.out.println(result.length + " results");
            for (DFAgentDescription dfAgentDescription : result) {
                if (!dfAgentDescription.getName().equals(agent.getName())) {
                    elevatorAux.addElement(dfAgentDescription.getName());
                }
            }

        } catch (FIPAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return elevatorAux;
    }

    //enviar mensagem aos outros agentes do piso onde se encontra
    private void informCurrentFloor(Agent agent, int floor, boolean informAllAgents) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        if (informAllAgents) {
            for (AID aid : elevatorsAID) {
                msg.addReceiver(aid);
            }
        }

        msg.addReceiver(simulatorAID);
        msg.setContent(agent.getLocalName() + "," + floor + "," + myIndex);
        agent.send(msg);
    }

    public void setState(AgentState state) {
        myState = state;
    }
}
