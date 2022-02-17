/**
 * Klasa węzła dla projektu.
 * Wezel zakłada, że podane parametry są poprawne oraz, że jest ich odpowiednia
 * liczba. Nie jest sprawdzana poprawność wywołania. Jesli wezel dostaje w parametrze
 * adres ip oraz port wezla do ktorego ma sie podlaczyc to wysyla temu węzłowi komunikat "PORT"
 * oraz dodaje do kontenera adres ip oraz port podany w parametrze "-gateway".
 * ze swoim adresem ip oraz portem. Dla kazdego polaczenia tworzy oddzielny wątek przy pomocy klasy Handler.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkNode {


    public static void main(String[] args){
        Info info = new Info();


        String gateway = null;
        int port = 0;
        String identifier = null;
        int tcpPort = 0;
        Map<String, Integer> resourcesInNode = new HashMap<>();


            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-ident":
                        identifier = args[++i];
                        break;
                    case "-tcpport":
                        tcpPort = Integer.parseInt(args[++i]);
                        break;
                    case "-gateway":
                        String[] gatewayArray = args[++i].split(":");
                        gateway = gatewayArray[0];
                        port = Integer.parseInt(gatewayArray[1]);
                        info.add(gateway, port);
                        break;
                    default:
                        String[] res = args[i].split(":");
                        resourcesInNode.put(res[0], Integer.parseInt(res[1]));
                }
            }


        if(port!=0) {
            Socket socket = null;
            try {
                socket = new Socket(gateway, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("PORT "+socket.getLocalAddress()+" "+tcpPort);
                System.out.println("Wysylam PORT "+ socket.getLocalAddress()+" "+tcpPort);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        try {
            ServerSocket serverSocket = new ServerSocket(tcpPort);


            while(true) {

                Socket socket = serverSocket.accept();

                System.out.println("Conected with port"+ socket.getPort());

                new Thread(new Handler(socket, resourcesInNode, info)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}





