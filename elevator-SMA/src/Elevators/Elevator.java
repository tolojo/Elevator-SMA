package Elevators;

import Elevators.BlockingQueue;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Vector;

public class Elevator extends Agent {

  BlockingQueue<Integer> tasks = new BlockingQueue<>(6);
  Vector<AID> elevators = new Vector<>();
  int maxLoad = 4;
  int transCost = 5;
  int pisoAtual = 0; //representa o estado do agente

  public void setup() {
   

    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("elevator");
    sd.setName(getLocalName() + "-elevator");
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
    addBehaviour(new TickerBehaviour(this, 1000) {
      @Override
      protected void onTick() {
        ACLMessage msg = myAgent.receive();
        int pisoDestino;
        int pisoInicial;
        if(msg != null){
          String pisos = msg.getContent();
          //split mensagem
          String [] pisosSplit = pisos.split(",");
          pisoInicial = Integer.parseInt(pisosSplit[0]);
          pisoDestino = Integer.parseInt(pisosSplit[1]);

          //verificação do piso do elevador com o piso do pedido
          if(pisoInicial != pisoAtual){
            System.out.println("Elevador " + myAgent.getLocalName() + " movendo para o piso " + pisoInicial + " a partir do piso " + pisoAtual + ".");
            pisoAtual = pisoInicial;
          }
          System.out.println("Elevador " + myAgent.getLocalName() + " no piso nº" + pisoAtual + ", seguindo para o piso nª" + pisoDestino);
          pisoAtual = pisoDestino;
          System.out.println("Elevador " + myAgent.getLocalName() + " no piso final nº" + pisoDestino + " alcançado");

        }
      }
    });

  }
}
