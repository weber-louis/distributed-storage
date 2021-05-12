import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

class Client{
    private int port;
    private int id;
    
    public Client(int id, int port){
        this.port = port;
        this.id = id;
    }
    public int getPort(){
        return this.port;
    }
    public int getId(){
        return this.id;
    }
}

class NodeFunctions{
    private int id;
    private InetAddress ip;
    private int port;
    private DatagramSocket server;
    private ArrayList<NodeFunctions> neighbors = new ArrayList<NodeFunctions>();
    private ArrayList<String> transactions = new ArrayList<String>();
    private ArrayList<Client> clients = new ArrayList<Client>();
    private HashMap<String, String> dataStorage = new HashMap<String, String>();

    public NodeFunctions(int port){
        this.port = port;
    }
    public NodeFunctions(int sourcePort, int destinationPort){
        this.port = sourcePort;
    }
    public int getPort(){
        return this.port;
    }
    public String responsePacket(){
        return null;
    }
    public String randId(String type){
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
    public String connectPacket(){
        String transationId = randId("connect");
        return "CONNECT:"+transationId+":"+this.id+":null:localhost:"+this.port;
    }
    public void setServer(DatagramSocket server){
        this.server = server;
    }
    public void addNeighbor(NodeFunctions n){
        this.neighbors.add(n);
    }
    public void addTransaction(String id){
        transactions.add(id);
    }
    public ArrayList<String> getTransactions(){
        return this.transactions;
    }
    public HashMap<String, String> getData(){
        return this.dataStorage;
    }
    public ArrayList<NodeFunctions> getNeighbors(){
        return this.neighbors;
    }
    public void addClient(Client client){
        this.clients.add(client);
    }
    public ArrayList<Client> getClients(){
        return this.clients;
    }
    public boolean hasClient(int clientId){
        for(Client c: this.clients){
            if(c.getId() == clientId){
                return true;
            }
        }
        return false;
    }
    public String[] splitPacket(String packet){
        String[] arrOfStr = packet.split(":", 7);
        return arrOfStr;
    }
    public void addData(String key, String value){
        if(dataStorage.size() <= 2){
            this.dataStorage.put(key, value);
        }
    }
    public boolean isFull(){
        return (dataStorage.size() == 2)? true:false;
    }
    public void broadcast(String packet) throws IOException{
        for(NodeFunctions n: this.neighbors){
            byte[] buffer = new byte[8000];
            buffer = packet.getBytes();
            InetAddress serverName = InetAddress.getByName("localhost");
            DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, n.getPort());
            this.server.send(datapacket);
            System.out.println("broadscast " + packet);
        }
    }
    public String toString(){
        return this.port+"";
    }
}

public class Node{
    public static void main(String args[]) throws IOException{
        NodeFunctions node = null;
        DatagramSocket server = null;
        DatagramPacket packet = null;

        if(args.length == 1){
            node = new NodeFunctions(Integer.valueOf(args[0]));
            server = new DatagramSocket(node.getPort());
            node.setServer(server);
        }else if(args.length == 2){
            System.out.println("Making a connection");
            int destinationPort = Integer.valueOf(args[1]);
            node = new NodeFunctions(Integer.valueOf(args[0]), destinationPort);
            server = new DatagramSocket(node.getPort());

            NodeFunctions neighbor = new NodeFunctions(destinationPort);
            node.addNeighbor(neighbor);
            node.setServer(server);

            //Send a connection Request to a specific port
            byte[] buffer = new byte[8000];
            String msg = node.connectPacket();
            buffer = msg.getBytes();
            InetAddress serverName = InetAddress.getByName("localhost");
            packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
            server.send(packet);
        }else{
            System.out.println("No arguments");
        }

        if(node != null){
            try{
                byte[] buffer = null;
                System.out.println("Server is listening on port "+node.getPort());

                while(true){
                    buffer = new byte[8000];
                    packet = new DatagramPacket(buffer, buffer.length);
                    server.receive(packet);

                    //Packet content
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    String packetType = node.splitPacket(msg)[0];

                    //Handle different packet types
                    if(packetType.equals("CONNECT")){
                        if(!node.splitPacket(msg)[2].equals("null")){
                            System.out.println("added new node");
                            String id = node.splitPacket(msg)[1];
                            int port = Integer.valueOf(node.splitPacket(msg)[5]);

                            NodeFunctions neighbor = new NodeFunctions(port);
                            node.addNeighbor(neighbor);
                        }else if(!node.splitPacket(msg)[3].equals("null")){
                            int clientID = Integer.valueOf(node.splitPacket(msg)[3]);
                            int clientPort = Integer.valueOf(node.splitPacket(msg)[5]);

                            System.out.println(clientPort);
                            Client client = new Client(clientID, clientPort);
                            node.addClient(client);
                            System.out.println("Client "+clientID+" is now connected");
                        }
                        
                    }else if(packetType.equals("GET")){
                        String transactionId = node.splitPacket(msg)[1];
                        int clientId = Integer.valueOf(node.splitPacket(msg)[2]); 

                        if(!node.getTransactions().contains(transactionId)){
                            String key = node.splitPacket(msg)[3];
                            node.addTransaction(transactionId);

                            if(node.getData().containsKey(key)){
                                String randomId = node.randId("response");                                
                                String value = node.getData().get(key);
                                String resp = "RESPONSE:"+randomId+":"+clientId+":"+value;

                                System.out.println(resp);
                                if(node.hasClient(Integer.valueOf(clientId))){
                                    Client client = null;
                                    for(Client c: node.getClients()){
                                        if(c.getId() == Integer.valueOf(clientId)){
                                            client = c;
                                        }
                                    }
                                    int destinationPort = client.getPort();
                                    buffer = new byte[8000];
                                    buffer = resp.getBytes();
                                    InetAddress serverName = InetAddress.getByName("localhost");
                                    packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
                                    server.send(packet);
                                }else{
                                    node.broadcast(resp); 
                                }
                            }else{
                                node.broadcast(msg);
                            }
                        }
                    }else if(packetType.equals("SET")){
                        if(!node.isFull()){
                            String key = node.splitPacket(msg)[3];
                            String value = node.splitPacket(msg)[4];
                            String transactionId = node.splitPacket(msg)[1];

                            node.addData(key, value);
                            node.addTransaction(transactionId);
                            System.out.println("result " + node.getData().get(key));
                        }else if(node.getNeighbors().size() > 0){
                            int randomNum = (int)(Math.random() * node.getNeighbors().size());
                            NodeFunctions randomNeighbor = node.getNeighbors().get(randomNum);
                            int neighborPort = randomNeighbor.getPort();
                            InetAddress serverName = InetAddress.getByName("localhost");

                            buffer = new byte[8000];
                            buffer = msg.getBytes();
                            packet = new DatagramPacket(buffer, buffer.length, serverName, neighborPort);
                            server.send(packet);
                        }else{
                            System.out.println("No data space");
                        }
                    }else if(packetType.equals("RESPONSE")){
                        String transactionId = node.splitPacket(msg)[1];
                        String clientId = node.splitPacket(msg)[2];

                        if(!node.getTransactions().contains(transactionId)){
                            node.addTransaction(transactionId);
                            //If the node has the client, then redirect the response packet to the client
                            System.out.println(node.splitPacket(msg)[3]);
                            if(node.hasClient(Integer.valueOf(clientId))){
                                Client client = null;
                                for(Client c: node.getClients()){
                                    if(c.getId() == Integer.valueOf(clientId)){
                                        client = c;
                                    }
                                }
                                int destinationPort = client.getPort();
                                InetAddress serverName = InetAddress.getByName("localhost");
                                buffer = new byte[8000];
                                buffer = msg.getBytes();
                                packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
                                server.send(packet);
                                System.out.println(msg);
                                System.out.println("Sending response");
                            }else{
                                node.broadcast(msg);
                            }
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
