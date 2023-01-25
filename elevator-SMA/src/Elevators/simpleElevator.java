package Elevators;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
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

public class simpleElevator extends Agent {

  BlockingQueue<ACLMessage> tasks = new BlockingQueue<>(6);
  Vector<AID> elevators = new Vector<>();
  int maxLoad = 4;
  int transCost = 5;
  int pisoAtual = 0; //representa o estado do agente
  HashMap<String, Integer> eleLoc = new HashMap<String, Integer>();
  private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();


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
    getAgents(this);
    sendFloor(this,pisoAtual);



    addBehaviour(
      new TickerBehaviour(this, 3000) {
        protected void onTick() {
          elevators = getAgents(this.myAgent);
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
          ACLMessage msg = myAgent.receive();

        if (msg != null) {
          if (msg.getPerformative() == ACLMessage.REQUEST) {
            int pisoDestino;
            int pisoInicial;
            String pisos = msg.getContent();

            //split mensagem
            String[] pisosSplit = pisos.split(",");
            pisoInicial = Integer.parseInt(pisosSplit[0]);
            pisoDestino = Integer.parseInt(pisosSplit[1]);
            int distancia = abs(pisoAtual - pisoInicial);
            Boolean escolhido = true;
            String minAID="";
            //verificação de elevador mais perto
            for (Map.Entry<String, Integer> entry : eleLoc.entrySet()){
              int distaux = 0;
              distaux = abs(entry.getValue() - pisoInicial);
              if (distancia>distaux){
                escolhido = false;
                minAID = entry.getKey();
              }
            }
            if(escolhido) {
              //verificação do piso do elevador com o piso do pedido
              if (pisoInicial != pisoAtual) {
                System.out.println("Elevador " + myAgent.getLocalName() + " movendo para o piso " + pisoInicial + " a partir do piso " + pisoAtual + ".");
                pisoAtual = pisoInicial;
              }
              System.out.println("Elevador " + myAgent.getLocalName() + " no piso nº" + pisoAtual + ", seguindo para o piso nª" + pisoDestino);
              pisoAtual = pisoDestino;
              System.out.println("Elevador " + myAgent.getLocalName() + " no piso final nº" + pisoDestino + " alcançado");
              sendFloor(this.getAgent(), pisoAtual);
            } else {

              ACLMessage msgEle = new ACLMessage(ACLMessage.REQUEST);
              msgEle.setPerformative(ACLMessage.REQUEST);
              //enviar mensagem para o elevador mais próximo
              System.out.println(myAgent.getLocalName() + " a enviar mensagem para o elevador mais proximo " + minAID);
              for (int i = 0; i<elevators.size(); ++i){
                if(elevators.get(i).getLocalName().equals(minAID)) {
                  msgEle.addReceiver(elevators.get(i));
                }
              }
              msgEle.setContent(pisoInicial + "," + pisoDestino);
              this.myAgent.send(msgEle);

            }


            //receber mensagem de outros elevadores
          } else if (msg.getPerformative() == ACLMessage.INFORM){
            String msgAgent;
            int pisoAgent;
            String info = msg.getContent();
            String[] msgSplit = info.split(",");
            msgAgent = msgSplit[0];
            pisoAgent = Integer.parseInt(msgSplit[1]);
            //se elevador não existir no hashmap
            if(!eleLoc.containsKey(msgAgent)) {
              eleLoc.put(msgAgent, pisoAgent);
              System.out.println(myAgent.getLocalName() + " adding " + msgAgent + " to floor " + pisoAgent);
            }
            //se o elevador já existir na hashmap
            else{
              eleLoc.put(msgAgent,pisoAgent);
              System.out.println(myAgent.getLocalName() + " is modifying " + msgAgent + " to floor " + pisoAgent);
            }

          }
        }
        msg = null;
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
      DFAgentDescription[] result = DFService.search(
              agent,
              dfd
      );
      //System.out.println(result.length + " results");
      if (result.length > 0) {
        for (int i = 0; i < result.length; ++i) {
          if(!result[i].getName().equals(agent.getName())) {
            elevatorAux.addElement(result[i].getName());
          }
        }

      }
    } catch (FIPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return elevatorAux;
  }

  //enviar mensagem aos outros agentes do piso onde se encontra
  private void sendFloor (Agent agent, int floor){
    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

    for (int i = 0; i<elevators.size(); ++i){
      msg.addReceiver(elevators.get(i));
    }
    msg.setContent(agent.getLocalName() + "," + floor);
    agent.send(msg);


  }
}



