import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client{
    private static int id;
    private static int port;
    private static int nodePort = 0;
    private static DatagramSocket server = null;

    //Get a random ID depending on the type of query
    public static String randId(String type){
        String id = null;

        switch(type){
            case "get":
                id = "G"+(int)(Math.random() * 1000);
            case "set":
                id = "S"+(int)(Math.random() * 1000);
            case "response":
                id = "R"+(int)(Math.random() * 1000);
            case "connect":
                id = "C"+(int)(Math.random() * 1000);
        }
        return id;
    }
    public static String connectRequest(){
        String transactionId = randId("connect");
        return "CONNECT:"+transactionId+":null:"+id+":localhost:"+port;
    }
    
    public static String[] splitPacket(String packet){
        String[] arrOfStr = packet.split(":", 7);
        return arrOfStr;
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

                System.out.println("You are now connected to Node "+nodePort);
            }
        }catch(ArrayIndexOutOfBoundsException e){
            System.out.println("requires multiple port arguments");
            System.exit(0);
        }

        if(nodePort != 0){
            while(true){
                Scanner sc = new Scanner(System.in);
                System.out.print("Please choose either GET or SET: ");
                String input = sc.nextLine();
                
                //If the user wants to GET data
                if(input.toLowerCase().equals("get")){
                    String key = null;
                    String queryID = randId("get");

                    System.out.print("Enter a key: ");
                    key = sc.nextLine();
                    
                    String query = "GET:"+queryID+":"+id+":"+key;
                    System.out.println("\""+query+"\" will be send to the node");

                    //Send the GET query to the connected node
                    byte[] buffer = new byte[8000];
                    buffer = query.getBytes();
                    InetAddress serverName = InetAddress.getByName("localhost");
                    DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, nodePort);
                    server.send(datapacket);

                    buffer = new byte[8000];
                    
                    //Waiting for the response query from the connected node
                    while(true){
                        datapacket = new DatagramPacket(buffer, buffer.length); 
                        server.receive(datapacket);
                        String response = new String(datapacket.getData(), 0, datapacket.getLength());
                        System.out.println("Response from node "+nodePort+" -> "+splitPacket(response)[3]);
                        break;
                    }
                }else if(input.toLowerCase().equals("set")){
                    String key = null;
                    String value = null;
                    String queryID = randId("set");

                    System.out.print("Enter a key: ");
                    key = sc.nextLine();
                    System.out.print("Enter a value: ");
                    value = sc.nextLine();
                    
                    String query = "SET:"+queryID+":"+id+":"+key+":"+value;
                    System.out.println("\""+query+"\" will be send to the node");

                    //Send the SET query to the connected node
                    byte[] buffer = new byte[8000];
                    buffer = query.getBytes();
                    InetAddress serverName = InetAddress.getByName("localhost");
                    DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, nodePort);
                    server.send(datapacket);
                }
            }
        }else{
            System.out.println("Node has not been specified");
            System.exit(0);
        }
    }
}
