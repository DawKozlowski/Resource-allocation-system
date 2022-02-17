/**
 * Klasa dla kontenera przechowujacego informacje o adresach IP
 * oraz portach do ktorych dany wezel ma dostep.
 */


import java.util.ArrayList;
import java.util.List;

public class Info {

     public List<Integer> listOfPorts = new ArrayList<>();
    public List<String> listOfGateways = new ArrayList<>();



      int clientPort;
      String clientGateway;

    /**
     * Metoda czysci wszystkie kolekcje przechowujace informacje
     */
     public void clear(){
         listOfPorts.clear();
         listOfGateways.clear();
         clientPort=0;
     }



    /**
     * Metoda dodaje do kolekcji adres IP oraz port
     *
     * @param gateway
     * @param port
     */
    public void add(String gateway, int port){
         listOfGateways.add(gateway);
         listOfPorts.add(port);
    }



    /**
     * Metoda usuwa z kolekcji adres IP oraz port
     *
     * @param gateway
     * @param port
     */
    public void remove(String gateway, int port){
        listOfGateways.remove(gateway);
        listOfPorts.remove(Integer.valueOf(port));
    }



    /**
     * Metoda ustawia adres IP oraz port klienta
     *
     * @param gateway
     * @param port
     */
    public  void addClientPort(String gateway ,int port){
        clientGateway = gateway;
         clientPort=port;
    }


}
