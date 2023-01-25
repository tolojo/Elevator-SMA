package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;
import java.util.Vector;

public class SimulatorAgent extends Agent {
    AID myAid = getAID();
    Vector<AID> elevatorsAID = new Vector<>();
    int maxFloors = 6;

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

        addBehaviour(
                new TickerBehaviour(this, 3000) {
                    protected void onTick() {
                        try {
                            Vector<AID> elevatorAux = new Vector<>();
                            DFAgentDescription dfd = new DFAgentDescription();
                            ServiceDescription sd = new ServiceDescription();
                            sd.setType("elevator");
                            dfd.addServices(sd);
                            DFAgentDescription[] result = DFService.search(this.getAgent(), dfd);
                            //System.out.println(result.length + " results");
                            if (result.length > 0) {
                                for (DFAgentDescription dfAgentDescription : result) {
                                    elevatorAux.addElement(dfAgentDescription.getName());
                                }
                                elevatorsAID = elevatorAux;
                            }

                        } catch (FIPAException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
        );

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                int numOfElevators = elevatorsAID.size();
                if (numOfElevators == 1) {
                    System.out.println("Sou o unico na lista");
                    return;
                }
                Random random = new Random();
                int randomElevator = random.nextInt(numOfElevators);
                System.out.println(elevatorsAID.get(randomElevator).getName());

                while (elevatorsAID.get(randomElevator).getName().equals(getName())) {
                    randomElevator = random.nextInt(numOfElevators); //Vamos escolher um dos elevadores na DF que nao seja o agente Simulador
                }

                AID elevAid = elevatorsAID.get(randomElevator);
                System.out.println("AID ESCOLHIDO = " + elevAid.getLocalName());
                int destinationFloor = random.nextInt(maxFloors);
                int initialFloor = random.nextInt(maxFloors);
                while (destinationFloor == initialFloor) {
                    initialFloor = random.nextInt(maxFloors);
                }
                ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
                aclMessage.setPerformative(ACLMessage.REQUEST);
                aclMessage.addReceiver((AID) elevAid);
                aclMessage.setContent(initialFloor + "," + destinationFloor);
                System.out.println(aclMessage.getContent());
                myAgent.send(aclMessage);
            }
        });
    }
}
