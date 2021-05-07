import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

class NodeFunctions{
    private int Id;
    private InetAddress ip;
    private int port;
    private ArrayList<NodeFunctions> neighbors = new ArrayList<NodeFunctions>();
    private ArrayList<Integer> transactions = new ArrayList<Integer>();
    //private List<Client> clients = new ArrayList<Node>();
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
    public int randId(){
        return (int)(Math.random() * 1000); 
    }
    public String connectPacket(){
        int transationId = randId();
        return "CONNECT:"+this.transationId+":"+this.Id+":"+this.port;
    }
    public void addNeighbor(NodeFunctions n){
        this.neighbors.add(n);
    }
    public void addTransaction(int id){
        transactions.add(id);
    }
    public ArrayList<Integer> getTransactions(){
        return this.transactions;
    }
    public HashMap<String, String> getData(){
        return this.dataStorage;
    }
    public ArrayList<NodeFunctions> getNeighbors(){
        return this.neighbors;
    }
    public String[] splitPacket(String packet){
        String[] arrOfStr = packet.split(":", 5);
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
    public void broadcast(String packet){
        for(NodeFunctions n: neighbors){
            byte[] buffer = new byte[1024];
            buffer = packet.getBytes();
            InetAddress serverName = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
            server.send(packet);
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

        if(args.length == 1){
            node = new NodeFunctions(Integer.valueOf(args[0]));
            server = new DatagramSocket(node.getPort());
        }else if(args.length == 2){
            System.out.println("Making a connection");
            int destinationPort = Integer.valueOf(args[1]);
            node = new NodeFunctions(Integer.valueOf(args[0]), destinationPort);
            server = new DatagramSocket(node.getPort());

            NodeFunctions neighbor = new NodeFunctions(destinationPort);
            node.addNeighbor(neighbor);

                byte[] buffer = new byte[1024];
                String msg = node.connectPacket();
                buffer = msg.getBytes();
                InetAddress serverName = InetAddress.getByName("localhost");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverName, destinationPort);
                server.send(packet);
        }else{
            System.out.println("No arguments");
        }

        if(node != null){
            try{
                DatagramPacket packet = null;
                byte[] buffer = new byte[1024];
                System.out.println("Server is listening on port "+node.getPort());

                while(true){
                    packet = new DatagramPacket(buffer, buffer.length);
                    server.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());

                    String packetType = node.splitPacket(msg)[0];

                    if(packetType.equals("CONNECT")){
                        System.out.println("added new node");
                        int id = Integer.valueOf(node.splitPacket(msg)[1]);
                        int port = Integer.valueOf(node.splitPacket(msg)[2]);

                        NodeFunctions neighbor = new NodeFunctions(port);
                        node.addNeighbor(neighbor);
                    }else if(packetType.equals("GET")){
                        int transactionId = node.split(msg)[1];

                        if(!node.getTransactions().contains(transactionId)){
                            String key = node.splitPacket(msg)[2];
                            node.addTransactionId(transactionId);

                            if(node.getData().containsKey(key)){
                                 
                            }else{
                                node.broadcast(msg);
                            }
                        }
                    }else if(packetType.equals("SET")){
                        if(!node.isFull()){
                            String key = node.splitPacket(msg)[2];
                            String value = node.splitPacket(msg)[3];
                            int transactionId = node.split(msg)[1];

                            node.addData(key, value);
                            node.addTransaction(transactionId);
                        }else{

                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
