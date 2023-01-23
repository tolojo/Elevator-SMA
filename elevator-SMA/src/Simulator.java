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

  Vector<AID> elevators = new Vector<>();
  int pisoMáximo = 6;

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
            System.out.println(result.length + " results");
            if (result.length > 0) {
              for (int i = 0; i < result.length; ++i) {
                elevatorAux.addElement(result[i].getName());
              }
              for (int i = 0; i < result.length; ++i) {
                System.out.println(elevatorAux.get(i));
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
            Random randg = new Random();
            int rElevator = randg.nextInt(elevatorsSize);

            while(rElevator == 0){
                rElevator = randg.nextInt(elevatorsSize); //Vamos escolher um dos elevadores na DF que nao seja o agente Simulador
            }
            AID elevAid = elevators.get(rElevator);
            int pisoDestino = randg.nextInt(pisoMáximo);
            ACLMessage msg = new ACLMessage();
            msg.setPerformative(ACLMessage.REQUEST);
            msg.addReceiver((AID) elevAid );
            msg.setContent(""+pisoDestino);
            send(msg);


        }
    });


  }
}
