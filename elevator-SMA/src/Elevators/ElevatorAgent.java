package Elevators;

import jade.core.AID;
import jade.core.Agent;
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

public class ElevatorAgent extends Agent {

    private enum AgentState {StandBy, MovingUp, MovingDown;}

    private final HashMap<String, Integer> elevatorLocation = new HashMap<>();
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private ArrayList<Request> currentRequests = new ArrayList<Request>();
    private ArrayList<Request> untakenRequests = new ArrayList<Request>();
    private final int movementDuration = 2;
    private final int movementCost = 5;
    private BlockingQueue<ACLMessage> tasksACL = new BlockingQueue<>(6);
    private Vector<AID> elevatorsAID = new Vector<>();
    private AgentState myState = AgentState.StandBy;
    private AID simulatorAID;
    private int numberOfMovs = 0;

    private int currentFloor, maxCapacity, myIndex, currentCapacity, destinationFloor;


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

        addBehaviour(new TickerBehaviour(this, 3000) {
            protected void onTick() {
                elevatorsAID = getAgents(myAgent);
            }
        });

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage aclMessage = myAgent.receive();
                try {
                    if (aclMessage != null) {
                        tasksACL.enqueue(aclMessage);
                        addBehaviour(new HandleRequestsBehaviour());
                    }


                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private class HandleRequestsBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage aclMessage = null;
            while (aclMessage == null) {
                try {
                    aclMessage = tasksACL.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (aclMessage != null) {
                if (aclMessage.getPerformative() == ACLMessage.REQUEST && myState == AgentState.StandBy) {
                    String[] splitFloors = aclMessage.getContent().split(",");
                    int requestFloor = Integer.parseInt(splitFloors[0]);
                    int destinationFloor = Integer.parseInt(splitFloors[1]);
                    int distance = abs(currentFloor - requestFloor);
                    Request request = new Request(requestFloor, destinationFloor);
                    boolean isChoosen = true;
                    String minAID = "";

                    isChoosen = checkCloserElevator(requestFloor,distance, request);


                    if (isChoosen) {
                        currentRequests.add(request);

                        while (currentRequests.size() != 0) {

                            try {
                                Request requestAux = currentRequests.get(0);
                                // mover elevador do piso atual para o piso do pedido
                                while (requestAux.getInitialFloor() != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + requestFloor + " a partir do piso " + currentFloor + " com " + currentCapacity + " pessoas");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    moveElevatorTo(requestAux.getInitialFloor());
                                }
                                currentCapacity++;
                                System.out.println(myAgent.getLocalName() + " recebeu pessoa no piso " + currentFloor + ", e vai para o piso " + destinationFloor);

                                // mover elevador do piso do pedido para o piso destino
                                while (requestAux.getDestinationFloor() != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + destinationFloor + " a partir do piso " + currentFloor + " com " + currentCapacity + " pessoas");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    moveElevatorTo(requestAux.getDestinationFloor());
                                }
                                if (currentCapacity > 0){
                                    currentCapacity--;
                                }

                                System.out.println(myAgent.getLocalName() + " chegou ao piso destino " + destinationFloor + " com " + currentCapacity + " pessoas");
                                informCurrentFloor(this.getAgent(), currentFloor, true);
                                myState = AgentState.StandBy;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    //receber mensagem de outros elevadores
                }

                if (aclMessage.getPerformative() == ACLMessage.INFORM) {
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
                } else {
                    String[] splitFloors = aclMessage.getContent().split(",");
                    int initialFloor = Integer.parseInt(splitFloors[0]);
                    int destinationFloor = Integer.parseInt(splitFloors[1]);
                    Request request = new Request(initialFloor, destinationFloor);
                    untakenRequests.add(request);
                    System.out.println(myAgent.getLocalName() + " adicionou novo pedido");
                }
            }

            aclMessage = null;
        }
    }

    @Override
    protected void takeDown() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFService.deregister(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        doDelete();
    }

    //Apanhar todos os agentes registados na df
    private Vector<AID> getAgents(Agent agent) {
        Vector<AID> elevatorAux = new Vector<>();
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("Elevator-Agent");
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

    // move o elevador na direçao do piso dado como argumento
    private void moveElevatorTo(int floorToGoTo) throws InterruptedException {
        // Descer andar
        if (floorToGoTo < currentFloor) {
            myState = AgentState.MovingDown;
            Thread.sleep(movementDuration * 1000L);
            currentFloor--;
        }
        // Subir andar
        else {
            myState = AgentState.MovingUp;
            Thread.sleep(movementDuration * 1000L);
            currentFloor++;
        }
        checkIfShouldAcceptRequests();
        if (currentCapacity != 0) {
            checkIfPeopleReachedItsFloor();
        }
    }

    // enviar mensagem aos outros agentes do piso onde se encontra
    private void informCurrentFloor(Agent agent, int floor, boolean informAllAgents) {
        ACLMessage msg = new ACLMessage();
        if (informAllAgents) {
            for (int i = 0; i < elevatorsAID.size(); i++)
                msg.addReceiver(elevatorsAID.get(i));
        }

        msg.addReceiver(simulatorAID);

        msg.setContent(agent.getLocalName() + "," + floor + "," + myIndex + "," + currentCapacity);

        msg.setPerformative(ACLMessage.INFORM);
        agent.send(msg);
    }

    private void checkIfShouldAcceptRequests() {
        System.out.println("Untaken Requests: " + untakenRequests);
        System.out.println("Current Requests: " + currentRequests);
        boolean isCloser;
        if (currentCapacity != maxCapacity) {
            for (int i = 0; i < untakenRequests.size(); i++) {
                Request requestAux = untakenRequests.get(i);
                System.out.println(requestAux.getInitialFloor() + " " + requestAux.getDestinationFloor());
                isCloser = checkCloserElevator(requestAux.getInitialFloor(),abs(currentFloor - requestAux.getInitialFloor()), requestAux);
                if(isCloser) {
                    if (myState == AgentState.MovingUp) {
                        System.out.println("a");
                        if (untakenRequests.get(i).getInitialFloor() <= untakenRequests.get(i).getDestinationFloor()) {
                            System.out.println("b");
                            if (untakenRequests.get(i).getInitialFloor() <= currentFloor) {
                                System.out.println("c");
                                currentRequests.add(requestAux);
                                currentCapacity++;
                                untakenRequests.remove(requestAux);
                                //untakenRequests.trimToSize();
                                System.out.println(this.getAID().getLocalName() + " entrou nova pessoa a querer subir para o piso ");

                            }
                        }
                    }
                    if (myState == AgentState.MovingDown) {
                        System.out.println("d");
                        if (untakenRequests.get(i).getInitialFloor() >= untakenRequests.get(i).getDestinationFloor()) {
                            System.out.println("e");
                            if (untakenRequests.get(i).getInitialFloor() >= currentFloor) {
                                System.out.println("f");
                                currentRequests.add(requestAux);
                                currentCapacity++;
                                untakenRequests.remove(requestAux);
                                //untakenRequests.trimToSize();
                                System.out.println(this.getAID().getLocalName() + " entrou nova pessoa a querer descer para o piso ");
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkIfPeopleReachedItsFloor() {
        for (int i = 0; i < currentRequests.size(); i++) {
            if (currentRequests.get(i).getDestinationFloor() == currentFloor) {
                Request requestAux = currentRequests.get(i);
                while(currentRequests.contains(requestAux)) {
                    currentRequests.remove(requestAux);
                }
                currentCapacity--;
                //currentRequests.trimToSize();
                System.out.println(this.getAID().getLocalName() + " chegou ao destino");
            }

        }
    }

    private boolean checkCloserElevator(int requestFloor, int distance, Request request){
        boolean isChosen = true;
        String minAID = "";
        for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
            int distanceAux = abs(entry.getValue() - requestFloor);
            if (distance > distanceAux) {
                isChosen = false;
                minAID = entry.getKey();
                sendRequestToCloserElevator(minAID,requestFloor, request);
            }
        }
        return isChosen;
    }

    private void sendRequestToCloserElevator(String minAID, int requestFloor, Request request){
        ACLMessage msgNextElevator = new ACLMessage(ACLMessage.REQUEST);
        msgNextElevator.setPerformative(ACLMessage.REQUEST);
        //enviar mensagem para o elevador mais próximo
        System.out.println(getAID().getLocalName() + " a enviar mensagem para o elevador mais proximo " + minAID);
        for (AID aid : elevatorsAID) {
            if (aid.getLocalName().equals(minAID)) {
                msgNextElevator.addReceiver(aid);
            }
        }
        untakenRequests.remove(request);
        msgNextElevator.setContent(requestFloor + "," + destinationFloor);
        this.send(msgNextElevator);

    }

    public void setState(AgentState state) {
        myState = state;
    }
}

