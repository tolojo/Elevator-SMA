package Elevators;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.introspection.AddedBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Random;
import java.util.Vector;

public class Simulator extends Agent {
    AID myAid = getAID();
  Vector<AID> elevators = new Vector<>();
  int pisoMaximo = 6;

  public void setup() {
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("elevator");
    sd.setName(getLocalName() + "-simulator");
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
            DFAgentDescription[] result = DFService.search(
              this.getAgent(),
              dfd
            );
            //System.out.println(result.length + " results");
            if (result.length > 0) {
              for (int i = 0; i < result.length; ++i) {
                elevatorAux.addElement(result[i].getName());
              }
              for (int i = 0; i < result.length; ++i) {
                //System.out.println(elevatorAux.get(i));
              }
              elevators = elevatorAux;
            }

          } catch (FIPAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    );

    addBehaviour(new TickerBehaviour(this, 10000) {
        protected void onTick(){
            int elevatorsSize = elevators.size();
            if(elevatorsSize==1){
            System.out.println("Sou o unico na lista");
            return;
            }
            Random randg = new Random();
            int rElevator = randg.nextInt(elevatorsSize);
            System.out.println(elevators.get(rElevator).getName());

            while(elevators.get(rElevator).getName().equals(getName())){
                rElevator = randg.nextInt(elevatorsSize); //Vamos escolher um dos elevadores na DF que nao seja o agente Simulador
            }

            AID elevAid = elevators.get(rElevator);
            System.out.println("AID ESCOLHIDO = " + elevAid.getLocalName());
            int pisoDestino = randg.nextInt(pisoMaximo);
            int pisoInicial = randg.nextInt(pisoMaximo);
            while(pisoDestino == pisoInicial){
                pisoInicial = randg.nextInt(pisoMaximo);
            }
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setPerformative(ACLMessage.REQUEST);
            msg.addReceiver((AID) elevAid );
            msg.setContent(pisoInicial + "," + pisoDestino);
            System.out.println(msg.getContent());
            myAgent.send(msg);


        }
    });


  }
}
