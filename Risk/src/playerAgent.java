import java.util.ArrayList;
import java.util.Hashtable;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class playerAgent extends Agent{
    
  Hashtable<String, Integer> gameBoard = new Hashtable<String, Integer>();
  

  public void setup() {
    populateBoard();

    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("riskGame");
    sd.setName(getLocalName() + "-gamePlayer");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    } catch (FIPAException fe) {
      fe.printStackTrace();
    }
  }


  public void populateBoard(){
    //Populate ht with the values of the places
    //Index is the place acronym followed with the value of the place, ex: NA1 - Alaska
    //Value is the number of troops on each place

    /*
     * ligações entre territorios:
     * NA5-EU2
     * NA3-SA4
     * SA2-AF5
     * AF5-EU7
     * AF5-EU5
     * AF3-EU7
     * AF3-EU5
     * EU5-AS7
     * EU6-AS11
     * EU6-AS1
     * EU6-AS7
     * AS9-AU2
     * 
     */



     //North America
     gameBoard.put("NA1", 0);
     gameBoard.put("NA2", 0);
     gameBoard.put("NA3", 0);
     gameBoard.put("NA4", 0);
     gameBoard.put("NA5", 0);
     gameBoard.put("NA6", 0);
     gameBoard.put("NA7", 0);
     gameBoard.put("NA8", 0);
     gameBoard.put("NA9", 0);
 //-------------------------------//
     //South America
     gameBoard.put("SA1", 0);
     gameBoard.put("SA2", 0);
     gameBoard.put("SA3", 0);
     gameBoard.put("SA4", 0);
 //-------------------------------//
     //Europe
     gameBoard.put("EU1", 0);
     gameBoard.put("EU2", 0);
     gameBoard.put("EU3", 0);
     gameBoard.put("EU4", 0);
     gameBoard.put("EU5", 0);
     gameBoard.put("EU6", 0);
     gameBoard.put("EU7", 0);
 //-------------------------------//
     //Africa
     gameBoard.put("AF1", 0);
     gameBoard.put("AF2", 0);
     gameBoard.put("AF3", 0);
     gameBoard.put("AF4", 0);
     gameBoard.put("AF5", 0);
     gameBoard.put("AF6", 0);
 //-------------------------------//
     //Asia
     gameBoard.put("AS1", 0);
     gameBoard.put("AS2", 0);
     gameBoard.put("AS3", 0);
     gameBoard.put("AS4", 0);
     gameBoard.put("AS5", 0);
     gameBoard.put("AS6", 0);
     gameBoard.put("AS7", 0);
     gameBoard.put("AS8", 0);
     gameBoard.put("AS9", 0);
     gameBoard.put("AS10", 0);
     gameBoard.put("AS11", 0);
     gameBoard.put("AS12", 0);
 //-------------------------------//
     //Australia
     gameBoard.put("AU1", 0);
     gameBoard.put("AU2", 0);
     gameBoard.put("AU3", 0);
     gameBoard.put("AU4", 0);
 //-------------------------------//
  }
}
