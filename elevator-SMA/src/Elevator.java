import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
public class Elevator extends Agent{

    BlockingQueue<Integer> tasks = new BlockingQueue<>(6);

    int maxLoad = 4;
    int transCost = 5;
    int pisoAtual= 0; //representa o estado do agente


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
      }


}
