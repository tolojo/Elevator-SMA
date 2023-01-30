package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.AgentState;
import jade.core.NotFoundException;
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

public class SmartElevatorAgent extends Agent {
    private enum AgentState {StandBy, MovingUp, MovingDown;}

    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final int movementDuration = 2;
    private ArrayList<Request> currentRequests = new ArrayList<Request>();
    private ArrayList<Request> untakenRequests = new ArrayList<Request>();
    ArrayList<Floor> mostCalledFloors = new ArrayList<>();
    private BlockingQueue<ACLMessage> tasksACL = new BlockingQueue<>(6);
    private Vector<AID> elevatorsAID = new Vector<>();
    private int currentFloor, maxCapacity, myIndex, currentCapacity, destinationFloor;
    private int movementCost = 5;
    private int numberOfMovs = 0;
    private AID simulatorAID;
    private AgentState myState = AgentState.StandBy;
    private Behaviour moveToMostCalled;
    private HashMap<String, Integer> elevatorLocation = new HashMap<>();
    private long planThreshold = 5000L;

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
                /*
                if (aclMessage != null && aclMessage.getPerformative() == ACLMessage.REQUEST) {
                    if (myState == AgentState.StandBy) { // verificar tbm se o andar do pedido esta a caminho do destino
                        // accept and process request
                    }
                    // se este elevador nao puder aceitar o pedido, manda para o proximo elevador
                    else {
                        int nextIndex;
                        if (myIndex == elevatorsAID.size()) nextIndex = 0;
                        else nextIndex = myIndex + 1;

                        AID elevAid = elevatorsAID.get(nextIndex);
                        ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
                        aclMessage.setPerformative(ACLMessage.REQUEST);
                        aclMessage.addReceiver((AID) elevAid);
                        aclMessage.setContent(currentFloor + "," + desiredFloor);
                        send(aclMessage);
                    }
                }
                */
            }
        });


        moveToMostCalled = new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(planThreshold);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (!mostCalledFloors.isEmpty()) {
                    Floor floorAux = mostCalledFloors.get(0);
                    for (int i = 0; i < mostCalledFloors.size(); i++) {
                        if (floorAux.getFrequencyCalled() < mostCalledFloors.get(i).getFrequencyCalled()) {
                            floorAux = mostCalledFloors.get(i);
                        }
                    }
                    while (myState == SmartElevatorAgent.AgentState.StandBy && floorAux.getFloor() != currentFloor) {
                        try {
                            informCurrentFloor(myAgent, currentFloor, false);
                            System.out.println("Elevador " + myAgent.getLocalName() + " movendo para o piso de origem mais chamado: " + floorAux.getFloor() + " a partir do piso " + currentFloor + " .");
                            numberOfMovs++;
                            if (floorAux.getFloor() < currentFloor) {
                                Thread.sleep(movementDuration * 1000);
                                currentFloor--;
                            } else {
                                Thread.sleep(movementDuration * 1000);
                                currentFloor++;
                            }


                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        addBehaviour(tbf.wrap(moveToMostCalled));
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
                if (aclMessage.getPerformative() == ACLMessage.REQUEST) {
                    String[] splitFloors = aclMessage.getContent().split(",");
                    int requestFloor = Integer.parseInt(splitFloors[0]);
                    int destinationFloor = Integer.parseInt(splitFloors[1]);
                    int distance = abs(currentFloor - requestFloor);
                    Request request = new Request(requestFloor, destinationFloor);
                    currentRequests.add(request);
                    currentCapacity++;

                    boolean isChoosen = true;
                    String minAID = "";
                    //verificação de elevador mais perto
                    for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
                        int distanceAux = abs(entry.getValue() - requestFloor);
                        if (distance > distanceAux) {
                            isChoosen = false;
                            minAID = entry.getKey();
                        }
                    }

                    if (isChoosen) {
                        while (currentRequests.size() != 0) {
                            tbf.interrupt();
                            try {
                                Request requestAux = currentRequests.get(0);
                                //atualizar o andar mais chamado
                                updateMostCalled(requestAux);
                                //verificação do piso do elevador com o piso do pedido
                                while (requestAux.getInitialFloor() != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + requestFloor + " a partir do piso " + currentFloor + " com " + currentCapacity + " pessoas");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    moveElevatorTo(requestAux.getInitialFloor());
                                }

                                System.out.println(myAgent.getLocalName() + " recebeu pessoa no piso " + currentFloor + ", e vai para o piso " + destinationFloor);
                                while (requestAux.getDestinationFloor() != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + destinationFloor + " a partir do piso " + currentFloor + " com " + currentCapacity + " pessoas");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    moveElevatorTo(requestAux.getDestinationFloor());
                                }
                                System.out.println(myAgent.getLocalName() + " chegou ao piso destino " + destinationFloor + " com " + currentCapacity + " pessoas");
                                informCurrentFloor(this.getAgent(), currentFloor, true);
                                myState = SmartElevatorAgent.AgentState.StandBy;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        ACLMessage msgNextElevator = new ACLMessage(ACLMessage.REQUEST);
                        currentCapacity--;
                        msgNextElevator.setPerformative(ACLMessage.REQUEST);
                        //enviar mensagem para o elevador mais próximo
                        System.out.println(myAgent.getLocalName() + " a enviar mensagem para o elevador mais proximo " + minAID);
                        for (AID aid : elevatorsAID) {
                            if (aid.getLocalName().equals(minAID)) {
                                msgNextElevator.addReceiver(aid);
                            }
                        }
                        currentRequests.remove(currentRequests.size() - 1);
                        msgNextElevator.setContent(requestFloor + "," + destinationFloor);
                        myAgent.send(msgNextElevator);
                    }
                    addBehaviour(tbf.wrap(moveToMostCalled));

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
                } else /*(aclMessage.getPerformative() == ACLMessage.REQUEST && myState != AgentState.StandBy)*/ {
                    String[] splitFloors = aclMessage.getContent().split(",");
                    int initialFloor = Integer.parseInt(splitFloors[0]);
                    int destinationFloor = Integer.parseInt(splitFloors[1]);
                    Request request = new Request(initialFloor, destinationFloor);
                    untakenRequests.add(request);
                    System.out.println(myAgent.getLocalName() + " adicionou novo pedido");
                }
                aclMessage = null;
            }
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
            e.printStackTrace();
        }
        return elevatorAux;
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

    // move o elevador na direçao do piso dado como argumento
    private void moveElevatorTo(int floorToGoTo) throws InterruptedException {
        // Descer andar
        if (floorToGoTo < currentFloor) {
            myState = SmartElevatorAgent.AgentState.MovingDown;
            Thread.sleep(movementDuration * 1000L);
            currentFloor--;
        }
        // Subir andar
        else {
            myState = SmartElevatorAgent.AgentState.MovingUp;
            Thread.sleep(movementDuration * 1000L);
            currentFloor++;
        }
        checkIfShouldAcceptRequests();
        if (currentCapacity != 0) {
            checkIfPeopleReachedItsFloor();
        }
    }


    private void checkIfShouldAcceptRequests() {
        System.out.println("Untaken Requests: " + untakenRequests);
        System.out.println("Current Requests: " + currentRequests);
        if (currentCapacity != maxCapacity) {
            for (int i = 0; i < untakenRequests.size(); i++) {
                Request requestAux = untakenRequests.get(i);
                System.out.println(requestAux.getInitialFloor() + " " + requestAux.getDestinationFloor());
                if (myState == SmartElevatorAgent.AgentState.MovingUp) {
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
                if (myState == SmartElevatorAgent.AgentState.MovingDown) {
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

    private void checkIfPeopleReachedItsFloor() {
        for (int i = 0; i < currentRequests.size(); i++) {
            if (currentRequests.get(i).getDestinationFloor() == currentFloor) {
                Request requestAux = currentRequests.get(i);
                while (currentRequests.contains(requestAux)) {
                    currentRequests.remove(requestAux);
                }
                currentCapacity--;
                //currentRequests.trimToSize();
                System.out.println(this.getAID().getLocalName() + " chegou ao destino");
            }
        }
    }

    public void setState(SmartElevatorAgent.AgentState state) {
        myState = state;
    }

    public void updateMostCalled(Request request) {
        if (mostCalledFloors.isEmpty()) {
            Floor floor = new Floor(request.getInitialFloor(), 1);
            mostCalledFloors.add(floor);
        } else {
            for (int i = 0; i < mostCalledFloors.size(); i++) {
                if (mostCalledFloors.get(i).getFloor() == request.getInitialFloor()) {
                    mostCalledFloors.get(i).setFrequencyCalled(mostCalledFloors.get(i).getFrequencyCalled() + 1);
                }
            }
        }
    }
}
