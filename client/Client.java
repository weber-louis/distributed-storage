import java.net.*;
import java.io.*;
import java.util.Scanner;

class Node{

}

public class Client{
    private static int id;
    private static int port;
    private static int nodePort;
    private static DatagramSocket server = null;

    public static String randId(String type){
        String id = null;

        switch(type){
            case "get":
                id = "G"+(Math.random() * 1000);
            case "set":
                id = "S"+(Math.random() * 1000);
            case "response":
                id = "R"+(Math.random() * 1000);
            case "connect":
                id = "C"+(Math.random() * 1000);
        }
        return id;
    }
    public static String connectRequest(){
        String transactionId = randId("connect");
        return "CONNECT:"+transactionId+":null:"+id+":localhost:"+nodePort;
    }
    
    public static void main(String args[]) throws Exception{
        try{
            if(args[0] != null && args[1] != null){ 
                id = (int)(Math.random() * 1000);
                port = Integer.valueOf(args[0]);
                nodePort = Integer.valueOf(args[1]);
                server = new DatagramSocket(port);
                
                String packet = connectRequest();
                byte[] buffer = new byte[1024];
                buffer = packet.getBytes();
                InetAddress serverName = InetAddress.getByName("localhost");
                DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, nodePort);
                server.send(datapacket);

                System.out.println("works");
            }
        }catch(ArrayIndexOutOfBoundsException e){
            System.out.println("requires a port argument");
            System.exit(0);
        }
        while(true){
            Scanner sc = new Scanner(System.in);
            System.out.println("Please enter a request");
            String input = sc.nextLine();

            System.out.println(input);
            String packet = input;
            byte[] buffer = new byte[1024];
            buffer = packet.getBytes();
            InetAddress serverName = InetAddress.getByName("localhost");
            DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, nodePort);
            server.send(datapacket);
        }
    }
}
