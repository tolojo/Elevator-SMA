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

public class SmartElevatorAgent extends Agent{
    private enum AgentState {StandBy, MovingUp, MovingDown;}
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    private final int movementDuration = 2;
    private ArrayList<Request> currentRequests = new ArrayList<Request>();
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

        Behaviour moveToMostCalled = new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(5000L);
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

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {
                    if (aclMessage.getPerformative() == ACLMessage.REQUEST) {
                        boolean isChoosen = false;
                        String[] splitFloors = aclMessage.getContent().split(",");
                        int initialFloor = Integer.parseInt(splitFloors[0]);
                        int destinationFloor = Integer.parseInt(splitFloors[1]);
                        int distance = abs(currentFloor - initialFloor);
                        Request request = new Request(initialFloor,destinationFloor);
                        String minAID = "";

                        //verificação de elevador mais perto
                        for (Map.Entry<String, Integer> entry : elevatorLocation.entrySet()) {
                            int distanceAux = abs(entry.getValue() - initialFloor);
                            if (distance > distanceAux) {
                                isChoosen = false;
                                minAID = entry.getKey();
                            }
                        }

                        if(myState == SmartElevatorAgent.AgentState.StandBy) {
                            currentRequests.add(request);
                            isChoosen = true;
                        }

                        if (myState == SmartElevatorAgent.AgentState.MovingUp){
                            boolean canAccept = false;
                            for (int i = 0; i < currentRequests.size(); i++) {
                                Request rAux = currentRequests.get(i);
                                if(initialFloor>currentFloor && initialFloor < rAux.getDestinationFloor())
                                    canAccept = true;
                            }
                           if (canAccept){
                               currentRequests.add(request);
                           }

                        }

                        if (myState == SmartElevatorAgent.AgentState.MovingDown){
                            boolean canAccept = false;
                            for (int i = 0; i < currentRequests.size(); i++) {
                                Request rAux = currentRequests.get(i);
                                if(initialFloor<currentFloor && initialFloor > rAux.getDestinationFloor())
                                    canAccept = true;
                            }
                            if (canAccept){
                                currentRequests.add(request);
                            }

                        }


                        if (isChoosen) {
                                tbf.interrupt();
                            try {
                                //atualizar o andar mais chamado
                                updateMostCalled(request);
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
                                currentRequests.remove(0);
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
                        addBehaviour(tbf.wrap(moveToMostCalled));

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
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        if (informAllAgents) {
            for (AID aid : elevatorsAID) {
                msg.addReceiver(aid);
            }
        }

        msg.addReceiver(simulatorAID);
        msg.setContent(agent.getLocalName() + "," + floor + "," + myIndex + ","+ currentRequests.size());
        agent.send(msg);
    }

    public void setState(AgentState state) {
        myState = state;
    }

    public void updateMostCalled(Request request){
        if(mostCalledFloors.isEmpty()){
            Floor floor = new Floor(request.getInitialFloor(),1);
            mostCalledFloors.add(floor);
        }else {
        for (int i = 0; i < mostCalledFloors.size(); i++) {
            if(mostCalledFloors.get(i).getFloor() == request.getInitialFloor()){
                mostCalledFloors.get(i).setFrequencyCalled(mostCalledFloors.get(i).getFrequencyCalled()+1);
            }
        }
    }
    }
}
