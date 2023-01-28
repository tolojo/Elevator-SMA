package Elevators;

import jade.core.AID;
import jade.core.Agent;
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
    private int currentFloor, maxCapacity, myIndex, curCapacity;


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

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {

                    System.out.println("Performative"+aclMessage.getPerformative());


                    if (aclMessage.getPerformative() == ACLMessage.REQUEST && myState == AgentState.StandBy) {
                        String[] splitFloors = aclMessage.getContent().split(",");
                        int initialFloor = Integer.parseInt(splitFloors[0]);
                        int destinationFloor = Integer.parseInt(splitFloors[1]);
                        int distance = abs(currentFloor - initialFloor);
                        Request request = new Request(initialFloor,destinationFloor);
                        boolean isChoosen = true;
                        String minAID = "";
                        //verificação de elevador mais perto
                        if(curCapacity>=maxCapacity){
                            for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
                                int distanceAux = abs(entry.getValue() - initialFloor);
                                if (distance > distanceAux) {
                                    minAID = entry.getKey();
                                }
                            }
                            System.out.println("Pedido delegado para o agente: "+ minAID);
                            isChoosen = false;
                        }

                        for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
                            int distanceAux = abs(entry.getValue() - initialFloor);
                            if (distance > distanceAux) {
                                isChoosen = false;
                                minAID = entry.getKey();
                            }
                        }

                        if (isChoosen) {
                            currentRequests.add(request);
                            curCapacity++;
                            try {
                                //verificação do piso do elevador com o piso do pedido
                                while (initialFloor != currentFloor) {
                                    System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + initialFloor + " a partir do piso " + currentFloor + ".");
                                    numberOfMovs++;
                                    informCurrentFloor(myAgent, currentFloor, false);
                                    // Descer andar
                                    if (initialFloor < currentFloor) {
                                        myState = AgentState.MovingDown;
                                        Thread.sleep(movementDuration * 1000L);
                                        currentFloor--;
                                        checkFloorOut(currentFloor);
                                        checkFloorIn(currentFloor,myState);
                                        // Subir andar
                                    } else {
                                        myState = AgentState.MovingUp;
                                        Thread.sleep(movementDuration * 1000L);
                                        currentFloor++;
                                        checkFloorOut(currentFloor);
                                        checkFloorIn(currentFloor,myState);
                                    }
                                }

                                System.out.println(myAgent.getLocalName() + " está no piso " + currentFloor + ", e vai para o piso " + destinationFloor);
                                while (currentRequests.size()>0) {
                                    destinationFloor = currentRequests.get(0).getDestinationFloor();
                                    while (destinationFloor != currentFloor) {
                                        System.out.println(myAgent.getLocalName() + " movendo-se para o piso " + initialFloor + " a partir do piso " + currentFloor + ".");
                                        numberOfMovs++;
                                        informCurrentFloor(myAgent, currentFloor, false);
                                        // Descer andar
                                        if (destinationFloor < currentFloor) {
                                            myState = AgentState.MovingDown;
                                            Thread.sleep(movementDuration * 1000L);
                                            currentFloor--;
                                            checkFloorOut(currentFloor);
                                            checkFloorIn(currentFloor, myState);
                                            // Subir andar
                                        } else {
                                            myState = AgentState.MovingUp;
                                            Thread.sleep(movementDuration * 1000L);
                                            currentFloor++;
                                            checkFloorOut(currentFloor);
                                            checkFloorIn(currentFloor, myState);
                                        }
                                    }
                                }

                                System.out.println(myAgent.getLocalName() + " chegou ao piso destino " + destinationFloor);
                                myState = AgentState.StandBy;
                                informCurrentFloor(this.getAgent(), currentFloor, true);
                                myState = AgentState.StandBy;
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
                        Request request = new Request(initialFloor,destinationFloor);
                        untakenRequests.add(request);
                        System.out.println(myAgent.getLocalName() + " adicionou novo pedido");


                    }
                }
                aclMessage = null;
            }
        });
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

    //enviar mensagem aos outros agentes do piso onde se encontra
    private void informCurrentFloor(Agent agent, int floor, boolean informAllAgents) {
        ACLMessage msg = new ACLMessage();
        if (informAllAgents) {
            for (int i = 0; i < elevatorsAID.size(); i++) {

                msg.addReceiver(elevatorsAID.get(i));
            }
        }

        msg.addReceiver(simulatorAID);
        msg.setContent(agent.getLocalName() + "," + floor + "," + myIndex + ","+ curCapacity);
        msg.setPerformative(ACLMessage.INFORM);
        agent.send(msg);
    }

    private void checkFloorIn(int curFloor, AgentState state){
        for (int i = 0; i<untakenRequests.size(); i++){
            if (state == AgentState.MovingUp
                    && untakenRequests.get(i).getInitialFloor() - untakenRequests.get(i).getDestinationFloor() < 0
                    && untakenRequests.get(i).getInitialFloor() == curFloor) {

                System.out.println(this.getAID().getLocalName() + " detetada nova pessoa a subir");
                currentRequests.add(untakenRequests.get(i));
                untakenRequests.remove(i);
                untakenRequests.trimToSize();

            } else if (state == AgentState.MovingDown
                    && untakenRequests.get(i).getInitialFloor() - untakenRequests.get(i).getDestinationFloor() > 0
                    && untakenRequests.get(i).getInitialFloor() == curFloor) {

                System.out.println(this.getAID().getLocalName() + " detetada nova pessoa a descer");
                currentRequests.add(untakenRequests.get(i));
                untakenRequests.remove(i);
                untakenRequests.trimToSize();
            }
        }

    }

    private void checkFloorOut(int curFloor){
        for (int i = 0; i<currentRequests.size(); i++){
           if (currentRequests.get(i).getDestinationFloor() == curFloor){
               currentRequests.remove(i);
               currentRequests.trimToSize();
               System.out.println(this.getAID().getLocalName() + " destino alcançado");
               curCapacity--;
           }
        }

    }

    public void setState(AgentState state) {
        myState = state;
    }
}

