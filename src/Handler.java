/**
 * Klasa Handler sluzy do odpowiedniego obsluzenia
 * komunikacji na weźle w zaleznosci od komunikatu do niego przychodzacego.
 * Przyjmuje w konstruktorze gniazdo, dostepne zasoby oraz o kontener
 * przechowujacy informacje o portach do ktorych mamy dostep.
 */



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Handler implements Runnable {

    Socket socket;
    Map<String, Integer> resourcesToAllocate = new HashMap<>();
    Map<String, Integer> resourcesInNode;
    Map<String, Integer> resourcesPotentialyAllocated;
    int clientId=0;
    Info info;
    List<String> informationsForClient = new ArrayList<>();
    boolean failed=false;

    public Handler(Socket socket,  Map<String, Integer> resourcesInNode, Info info){
        this.socket=socket;
        this.resourcesInNode=resourcesInNode;
        resourcesPotentialyAllocated= new HashMap<>(resourcesInNode);
        this.info=info;
    }


    /**
     * W metodzie run odbieramy komunikat i w zalezonosci od jego tresci przekazujemy go dalej.
     * W przypadku komunikatu od klienta wywolujemy metode clientMethod. W przypadku komunikatu "PORT"
     * Dodajemy informacje o porcie ktory sie podlaczyl do kontenera info.
     * W przypadku komunikatu "TERMINATE" zrywamy polaczenie.
     * W przypadku komunikatu "MISSING" wywolujemy metode methodForMissing
     * W przypadku komunikatu "ALLGOOD" aktualizujemy informacje o zasobach w wezle i przekazujemy dalej
     * komunikat "ALLGOOD" do innych podlaczonych wezlow.
     */
    @Override
    public void run() {
        try {
            PrintWriter out =new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String command = in.readLine();
            if(command.contains(":")){
               clientMethod(command, socket, out, in);
            }
            if(command.contains("PORT")){
                String[] portInfo = command.split(" ");
                info.add(portInfo[1], Integer.parseInt(portInfo[2]));
                System.out.println("Otrzymany port "+ portInfo[2]);
            }
            if(command.contains("TERMINATE")){
                socket.close();
            }
            if(command.contains("MISSING")) {
               methodForMissing(command, socket, out, in);
            }

            if(command.contains("ALLGOOD")){
                System.out.println("otrzymalem ALLGOOD");
                resourcesInNode=Map.copyOf(resourcesPotentialyAllocated);
                String[] allgoodInfo=command.split(" ");
                Info infoForAllGood = info;
                infoForAllGood.remove(allgoodInfo[1], Integer.parseInt(allgoodInfo[2]));
                if(infoForAllGood.listOfPorts.size()!=0) {
                    for (int i = 0; i < infoForAllGood.listOfGateways.size(); i++) {
                        Socket socket2 = new Socket(infoForAllGood.listOfGateways.get(i), infoForAllGood.listOfPorts.get(i));
                        PrintWriter out2 = new PrintWriter(socket2.getOutputStream(), true);
                        out2.println("ALLGOOD");
                        socket2.close();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Metoda ta sluzy do przetwarzania komunikatow otrzymanych od klienta.
     * Sprawdza czy wezel posiada wymagane zasoby. Jezeli nie to wysyla
     * komunikat MISSING do innego wezla do ktorego ma dostep i czeka na odpowiedz.
     * Jesli odpowiedzia jest komunikat ALLOCATED sprawdza kolejne zasoby az do
     * sprawdzenia wszytskich. Jesli otrzyma komunikat FAILED to wysyla informacje
     * do klienta i zrywa polaczenie.
     *
     * @param command - komenda otrzymana od węzła badz klienta
     * @param socket - gniazdo na ktorym odbieramy dany komunikat
     * @param out - strumien wyjsciowy do wysylania informacji
     * @param in - strumien wejsciowy do odbierania informaci
     * @throws IOException
     */



    public void clientMethod(String command, Socket socket, PrintWriter out,  BufferedReader in) throws IOException{

        String[] resources = command.split(" ");
        clientId=Integer.parseInt(resources[0]);
        info.addClientPort(String.valueOf(socket.getInetAddress()),socket.getPort());
        for(int i=1;i<resources.length;i++){
            String[] resorcesArray =resources[i].split(":");
            resourcesToAllocate.put(resorcesArray[0], Integer.parseInt(resorcesArray[1]));
        }

        for(String key : resourcesToAllocate.keySet()){
            System.out.println("poszukujemy tego klucza "+key);
            if(failed){
                break;
            }
            Socket socket2;
            PrintWriter out2;
            BufferedReader in2;
            if(resourcesInNode.containsKey(key)){
                System.out.println("Mamy zasob");
                System.out.println("liczba dostepbych portow "+info.listOfGateways.size());
                if(resourcesInNode.get(key)>=resourcesToAllocate.get(key)){
                    resourcesPotentialyAllocated.replace(key, resourcesInNode.get(key)-resourcesToAllocate.get(key));
                    System.out.println("ALLOCATED "+key+" "+resourcesToAllocate.get(key)+" "+socket.getLocalAddress() +" "+socket.getLocalPort());
                    informationsForClient.add("ALLOCATED "+key+" "+resourcesToAllocate.get(key)+" "+socket.getLocalAddress() +" "+socket.getLocalPort());
                }else{
                    if(info.listOfGateways.size()==0){
                        out.println("FAILED");
                        failed=true;
                        socket.close();
                        break;
                    }
                    resourcesPotentialyAllocated.replace(key, 0);
                    informationsForClient.add(key+" "+resourcesInNode.get(key)+" "+socket.getLocalAddress()+" "+socket.getLocalPort());
                    System.out.println("zasoby do zaalokowania "+resourcesToAllocate.get(key));
                    System.out.println("zasoby w wezle "+resourcesInNode.get(key));
                    int resourcesMissing = resourcesToAllocate.get(key)-resourcesInNode.get(key);
                    System.out.println("Brakujace zasoby "+resourcesMissing);
                    for(int i=0; i<info.listOfGateways.size(); i++) {
                        socket2 = new Socket(info.listOfGateways.get(i), info.listOfPorts.get(i));
                        System.out.println("Laczenie z wezlem na porcie "+info.listOfPorts.get(i));
                        out2 = new PrintWriter(socket2.getOutputStream(), true);
                        in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                        out2.println("MISSING " + key + " " + resourcesMissing+" "+socket.getLocalAddress()+" "+socket.getLocalPort());
                        System.out.println("Wysylamy MISSING");
                        String response=in2.readLine();
                        System.out.println("Dostalismy z powrotem "+response);
                        if(response.contains("ALLOCATED")){
                            System.out.println("odbieram ALLOCATED");
                            String information = response;
                            String[] information2 = information.split(" ");
                            informationsForClient.add(information2[1]+" "+information2[2]+" "+information2[3]+" "+information2[4]);
                            break;
                        }
                        if(response.contains("FAILED")){
                            System.out.println("odbieram FAILED");
                            out.println("FAILED");
                            failed=true;
                            socket.close();
                            break;
                        }
                        socket2.close();
                    }
                }
            }else{
                System.out.println("Nie mamy zasobow");
                if(info.listOfGateways.size()==0){
                    System.out.println("Wysylamy FAILED");
                    out.println("FAILED");
                    failed=true;
                    socket.close();
                    break;
                }
                for(int i=0; i<info.listOfGateways.size(); i++) {
                    socket2 = new Socket(info.listOfGateways.get(i), info.listOfPorts.get(i));
                    out2 = new PrintWriter(socket2.getOutputStream(), true);
                    in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                    System.out.println("Laczenie z wezlem na porcie "+info.listOfPorts.get(i));
                    out2.println("MISSING " + key + " " + resourcesToAllocate.get(key)+" "+socket.getLocalAddress()+" "+socket.getLocalPort());
                    System.out.println("Wysylamy MISSING");
                    String response=in2.readLine();
                    System.out.println("Dostalismy z powrotem "+response);
                    if(response.contains("ALLOCATED")){
                        System.out.println("odbieram ALLOCATED");
                        String[] info2 = response.split(" ");
                        informationsForClient.add(info2[1]+" "+info2[2]+" "+info2[3]+" "+info2[4]);
                        break;
                    }
                    if(response.contains("FAILED")){
                        System.out.println("odbieram FAILED");
                        out.println("FAILED");
                        failed=true;
                        socket.close();
                        break;
                    }
                    socket2.close();
                }
            }
        }
        if(failed==false){
            for(String string : informationsForClient){
                out.println(string);
            }
            out.println("ALLOCATED");
            socket.close();
            resourcesInNode=resourcesPotentialyAllocated;
            informationsForClient.clear();
            for(int i=0; i<info.listOfGateways.size(); i++) {
                Socket socket2 = new Socket(info.listOfGateways.get(i), info.listOfPorts.get(i));
                PrintWriter out2 = new PrintWriter(socket2.getOutputStream(), true);
                System.out.println("wysylam ALLGOOD");
                out2.println("ALLGOOD "+socket.getLocalAddress()+" "+socket.getLocalPort());
                socket2.close();
            }
        }

    }

    /**
     * Metoda ta sluzy do przetwarzania komunikatow MISSING otrzymywanych od innych wezlow.
     * Sprawdza czy posiada wymagane zasoby. Jesli tak to wysyla komunikat ALLOCATED do
     * pytajacego wezla. Jesli nie to wysyla dalej komunikat MISSING do pozostalych wezlow
     * do ktorych ma dostep. Jesli otrzyma z powrotem komunikat ALLOCATED wysyla go dalej do
     * wezla pytajacego. Jesli otrzyma komunikat FAILED wysyla go do wezla pytajcego.
     * Jesli wezel sam nie ma gdzie przeslac dalej komunikatu MISSING wysyla komunikat FAILED
     * do wezla pytajacego.
     *
     * @param command - komenda otrzymana od węzła badz klienta
     * @param socket - gniazdo na ktorym odbieramy dany komunikat
     * @param out -  strumien wyjsciowy do wysylania informacji
     * @param in - strumien wejsciowy do odbierania informaci
     * @throws IOException
     */


    public void methodForMissing(String command, Socket socket, PrintWriter out, BufferedReader in) throws IOException{
        Socket socket2;
        PrintWriter out2;
        BufferedReader in2;
        Info infoForMissing = info;
        String[] informations = command.split(" ");
        infoForMissing.remove(informations[3], Integer.parseInt(informations[4]));
        int failureCounter = 0;
        System.out.println("poszukujemy tego klucza "+informations[1]);
        if (resourcesInNode.containsKey(informations[1])) {
            System.out.println("zasoby w wezle "+resourcesInNode.get(informations[1]));
            System.out.println("zasoby do zaalokowania "+Integer.parseInt(informations[2]));
            if (resourcesInNode.get(informations[1]) >= Integer.parseInt(informations[2])) {
                resourcesPotentialyAllocated.replace(informations[1], resourcesInNode.get(informations[1]) - Integer.parseInt(informations[2]));
                System.out.println("Wysylam allocated");
                out.println("ALLOCATED " + informations[1] + " " + informations[2] + " " + socket.getLocalAddress() + " " + socket.getLocalPort());
            } else {
                if (infoForMissing.listOfPorts.size() == 0) {
                    System.out.println("Wysylam failed");
                    out.println("FAILED");
                }
                resourcesPotentialyAllocated.replace(informations[1], 0);
                for (int i = 0; i < infoForMissing.listOfGateways.size(); i++) {
                    socket2 = new Socket(infoForMissing.listOfGateways.get(i), infoForMissing.listOfPorts.get(i));
                    out2 = new PrintWriter(socket2.getOutputStream(), true);
                    in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                    int resourcesMising = Integer.parseInt(informations[2]) - resourcesInNode.get(informations[1]);
                    out2.println("MISSING " + informations[1] + " " + resourcesMising+" "+socket.getLocalAddress()+" "+socket.getLocalPort());
                    System.out.println("Wysylamy MISSING");
                    String response=in2.readLine();
                    System.out.println("Dostalismy z powrotem "+response);
                    if (response.contains("ALLOCATED")) {
                        String[] info3 = response.split(" ");
                        out.println("ALLOCATED " + info3[1] + " " + info3[2] + " " + info3[3] + " " + info3[4]+ "\n"+
                        "ALLOCATED "+informations[1]+" "+resourcesInNode.get(informations[1])+" "+socket.getLocalAddress()+" "+socket.getLocalPort());
                        break;
                    }
                    if (response.contains("FAILED")) {
                        failureCounter++;
                    }
                    socket2.close();
                }
                if (failureCounter == infoForMissing.listOfPorts.size()) {
                    out.println("FAILED");
                    resourcesPotentialyAllocated.clear();
                }
            }
        } else {
            System.out.println("nie znalazl sie");
            if (infoForMissing.listOfPorts.size() == 0) {
                System.out.println("Wysylam failed");
                out.println("FAILED");
            }
            for (int i = 0; i < infoForMissing.listOfGateways.size(); i++) {
                socket2 = new Socket(infoForMissing.listOfGateways.get(i), infoForMissing.listOfPorts.get(i));
                out2 = new PrintWriter(socket2.getOutputStream(), true);
                in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                int resourcesMising = Integer.parseInt(informations[2]);
                out2.println("MISSING " + informations[1] + " " + resourcesMising);
                String response=in2.readLine();
                if (response.contains("ALLOCATED")) {
                    String[] info3 = response.split(" ");
                    out.println("ALLOCATED " + info3[1] + " " + info3[2] + " " + info3[3] + " " + info3[4]);
                    break;
                }
                if (response.contains("FAILED")) {
                    failureCounter++;
                }
                socket2.close();
            }
            if (failureCounter == infoForMissing.listOfPorts.size()) {
                out.println("FAILED");
                resourcesPotentialyAllocated.clear();
            }
        }
    }


}
