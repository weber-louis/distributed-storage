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
        if(dataStorage.size() <= 5){
            this.dataStorage.put(key, value);
        }
    }
    public boolean isFull(){
        return (dataStorage.size() == 5)? true:false;
    }
    //Send a query to all the neighbors
    public void broadcast(String packet) throws IOException{
        for(NodeFunctions n: this.neighbors){
            byte[] buffer = new byte[8000];
            buffer = packet.getBytes();
            InetAddress serverName = InetAddress.getByName("localhost");
            DatagramPacket datapacket = new DatagramPacket(buffer, buffer.length, serverName, n.getPort());
            this.server.send(datapacket);
            System.out.println("Broadcasting -> \""+packet+"\"");
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

        if(args.length == 1){ //If the node runs individually
            node = new NodeFunctions(Integer.valueOf(args[0]));
            server = new DatagramSocket(node.getPort());
            node.setServer(server);
        }else if(args.length == 2){ //If the node wants to connect to another node
            System.out.println("Making a connection");
            int destinationPort = Integer.valueOf(args[1]);
            node = new NodeFunctions(Integer.valueOf(args[0]), destinationPort);
            server = new DatagramSocket(node.getPort());

            //Add the node to the neighbors list
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
                    //Listening for packets
                    buffer = new byte[8000];
                    packet = new DatagramPacket(buffer, buffer.length);
                    server.receive(packet);

                    //Packet content
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    String packetType = node.splitPacket(msg)[0];

                    //Handle different packet types
                    if(packetType.equals("CONNECT")){ //If the node receives a CONNECT packet
                        if(!node.splitPacket(msg)[2].equals("null")){ //If it's a node to node connection
                            String id = node.splitPacket(msg)[1];
                            System.out.println("Connected with node "+id);
                            int port = Integer.valueOf(node.splitPacket(msg)[5]);

                            //Add the node to the neighbors list
                            NodeFunctions neighbor = new NodeFunctions(port);
                            node.addNeighbor(neighbor);
                        }else if(!node.splitPacket(msg)[3].equals("null")){ //If it's a client to node connection
                            int clientID = Integer.valueOf(node.splitPacket(msg)[3]);
                            int clientPort = Integer.valueOf(node.splitPacket(msg)[5]);

                            //Add the client to the client list 
                            Client client = new Client(clientID, clientPort);
                            node.addClient(client);
                            System.out.println("Client "+clientID+" is now connected");
                        }
                        
                    }else if(packetType.equals("GET")){ //If the node receives a GET packet
                        String transactionId = node.splitPacket(msg)[1];
                        int clientId = Integer.valueOf(node.splitPacket(msg)[2]); 
                        System.out.println("Received GET query -> \""+msg+"\"");

                        if(!node.getTransactions().contains(transactionId)){//To prevent an infinite loop
                            String key = node.splitPacket(msg)[3];
                            node.addTransaction(transactionId);

                            //If the node contains the key
                            if(node.getData().containsKey(key)){
                                String randomId = node.randId("response");                                
                                String value = node.getData().get(key);
                                String resp = "RESPONSE:"+randomId+":"+clientId+":"+value;

                                //If the node contains the client
                                if(node.hasClient(Integer.valueOf(clientId))){
                                    Client client = null;
                                    for(Client c: node.getClients()){
                                        if(c.getId() == Integer.valueOf(clientId)){
                                            client = c;
                                        }
                                    }
                                    //Send a response packet to the client
                                    int destinationPort = client.getPort();
                                    buffer = new byte[8000];
                                    buffer = resp.getBytes();
                                    InetAddress serverName = InetAddress.getByName("localhost");
                                    packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
                                    server.send(packet);
                                    System.out.println("Sending response to the client");
                                }else{ //Redirect the response packet all the neighbor nodes
                                    node.broadcast(resp); 
                                }
                            }else{
                                node.broadcast(msg);
                            }
                        }
                    }else if(packetType.equals("SET")){ //If the node receives a SET packet
                        System.out.println("Received SET query -> \""+msg+"\"");

                        //If the node has no space
                        if(!node.isFull()){
                            String key = node.splitPacket(msg)[3];
                            String value = node.splitPacket(msg)[4];
                            String transactionId = node.splitPacket(msg)[1];

                            //Add the data to the node
                            node.addData(key, value);
                            node.addTransaction(transactionId);
                            System.out.println("Storing the value \"" + node.getData().get(key)+"\" with the key \""+key+"\" in this node");
                        }else if(node.getNeighbors().size() > 0){ //If the node has neighbors
                            int randomNum = (int)(Math.random() * node.getNeighbors().size());
                            NodeFunctions randomNeighbor = node.getNeighbors().get(randomNum);
                            int neighborPort = randomNeighbor.getPort();
                            
                            //Redirect the packet to a random neighbor
                            InetAddress serverName = InetAddress.getByName("localhost");
                            buffer = new byte[8000];
                            buffer = msg.getBytes();
                            packet = new DatagramPacket(buffer, buffer.length, serverName, neighborPort);
                            server.send(packet);
                        }else{
                            System.out.println("No data space");
                        }
                    }else if(packetType.equals("RESPONSE")){ //If the node receives a RESPONSE packet
                        String transactionId = node.splitPacket(msg)[1];
                        String clientId = node.splitPacket(msg)[2];
                        System.out.println("Received RESPONSE query -> \""+msg+"\"");

                        if(!node.getTransactions().contains(transactionId)){
                            node.addTransaction(transactionId);
                            //If the node has the client, then redirect the response packet to the client
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
                                System.out.println("Sending response to the client");
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
