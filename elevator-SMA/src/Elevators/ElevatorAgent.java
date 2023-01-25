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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static java.lang.Math.abs;

public class ElevatorAgent extends Agent {

    BlockingQueue<ACLMessage> tasksACL = new BlockingQueue<>(6);
    Vector<AID> elevatorsAID = new Vector<>();
    int maxCapacity = 4;
    int movementCost = 5;
    int currentFloor = 0; //representa o estado do agente
    HashMap<String, Integer> elevatorLocation = new HashMap<>();
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

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
        
        getAgents(this);
        informCurrentFloor(this, currentFloor);

        addBehaviour(
                new TickerBehaviour(this, 3000) {
                    protected void onTick() {
                        elevatorsAID = getAgents(myAgent);
                    }
                }
        );

    /*Behaviour receiveTask = new CyclicBehaviour() {
      @Override
      public void action() {
        ACLMessage task = myAgent.receive();
        if(task != null){
        try {
          tasks.enqueue(task);
          task = null;
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }}
      }
    };

  addBehaviour(tbf.wrap(receiveTask));
     */
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {
                    if (aclMessage.getPerformative() == ACLMessage.REQUEST) {
                        int destinationFloor;
                        int initialFloor;
                        String floors = aclMessage.getContent();

                        //split mensagem
                        String[] splitFloors = floors.split(",");
                        initialFloor = Integer.parseInt(splitFloors[0]);
                        destinationFloor = Integer.parseInt(splitFloors[1]);
                        int distance = abs(currentFloor - initialFloor);
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
                            //verificação do piso do elevador com o piso do pedido
                            if (initialFloor != currentFloor) {
                                System.out.println("Elevador " + myAgent.getLocalName() + " movendo para o piso " + initialFloor + " a partir do piso " + currentFloor + ".");
                                currentFloor = initialFloor;
                            }
                            System.out.println("Elevador " + myAgent.getLocalName() + " no piso nº" + currentFloor + ", seguindo para o piso nª" + destinationFloor);
                            currentFloor = destinationFloor;
                            System.out.println("Elevador " + myAgent.getLocalName() + " no piso final nº" + destinationFloor + " alcançado");
                            informCurrentFloor(this.getAgent(), currentFloor);
                        } else {

                            ACLMessage msgEle = new ACLMessage(ACLMessage.REQUEST);
                            msgEle.setPerformative(ACLMessage.REQUEST);
                            //enviar mensagem para o elevador mais próximo
                            System.out.println(myAgent.getLocalName() + " a enviar mensagem para o elevador mais proximo " + minAID);
                            for (int i = 0; i < elevatorsAID.size(); ++i) {
                                if (elevatorsAID.get(i).getLocalName().equals(minAID)) {
                                    msgEle.addReceiver(elevatorsAID.get(i));
                                }
                            }
                            msgEle.setContent(initialFloor + "," + destinationFloor);
                            this.myAgent.send(msgEle);

                        }


                        //receber mensagem de outros elevadores
                    } else if (aclMessage.getPerformative() == ACLMessage.INFORM) {
                        String msgAgent;
                        int pisoAgent;
                        String info = aclMessage.getContent();
                        String[] msgSplit = info.split(",");
                        msgAgent = msgSplit[0];
                        pisoAgent = Integer.parseInt(msgSplit[1]);
                        //se elevador não existir no hashmap
                        if (!elevatorLocation.containsKey(msgAgent)) {
                            elevatorLocation.put(msgAgent, pisoAgent);
                            System.out.println(myAgent.getLocalName() + " adding " + msgAgent + " to floor " + pisoAgent);
                        }
                        //se o elevador já existir na hashmap
                        else {
                            elevatorLocation.put(msgAgent, pisoAgent);
                            System.out.println(myAgent.getLocalName() + " is modifying " + msgAgent + " to floor " + pisoAgent);
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
    private void informCurrentFloor(Agent agent, int floor) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        for (AID aid : elevatorsAID) {
            msg.addReceiver(aid);
        }
        msg.setContent(agent.getLocalName() + "," + floor);
        agent.send(msg);
    }
}



