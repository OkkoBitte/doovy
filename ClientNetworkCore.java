import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public abstract class ClientNetworkCore {
    private int isConnect = 0;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int timeout;
    private Options option;
    private String sessionKey;
    private SecureRandom random = new SecureRandom();
    
    
    private List<Packet> internalPackets = new ArrayList<>();
    

    private volatile boolean running = false;
    private volatile boolean broadcasting = false;
    private Thread loopThread;
    private Thread broadcastThread;

    public ClientNetworkCore(Options options, String sey) {
        this.option = options;
        this.sessionKey = sey;
        this.timeout = options.getClientTypeUnsigned();
        this.internalPackets = new ArrayList<>();
    }
    
    public int IsConnect() {
        return isConnect;
    }
    
    public int Connect(String host, int port) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            
            serverAddress = InetAddress.getByName(host);
            serverPort = port;
            
            isConnect = 1;
            System.out.println("[doovy] UDP conn to " + host + ":" + port);
            startLoop();
            sendAuth();
            return isConnect;
            
        } catch (UnknownHostException e) {
            System.err.println("un-host " + host);
            isConnect = 0;
            return -1;
        } catch (SocketException e) {
            System.err.println("not create socket: " + e.getMessage());
            isConnect = 0;
            return -1;
        }
    }
    
    public abstract void Get(Packet packet);


    public void startLoop() {
        if (loopThread != null && loopThread.isAlive()) {
          return;
        }
        
        running = true;
        loopThread = new Thread(this::Loop);
        loopThread.setName("ClientNetworkCore-Loop");
        loopThread.start();
        System.out.println("Server loop is threading....");
    }
    

    public void stopLoop() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Stop threading client loop");
    }
    
    public void startBroadcast() {
        if (broadcastThread != null && broadcastThread.isAlive()) {
            return;
        }
        
        broadcasting = true;
        broadcastThread = new Thread(this::BroadCast);
        broadcastThread.setName("ClientNetworkCore-Broadcast");
        broadcastThread.start();
    }
    
    public void stopBroadcast() {
        broadcasting = false;
        if (broadcastThread != null) {
            try {
                broadcastThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void Loop() {
        while (running && IsConnect() == 1) {
            try {
                byte[] data = receive();
                
                if (data == null || data.length < 6) {
                    Thread.sleep(10);
                    continue;
                }
                
                Packet packet = new Packet();
                packet.parseByte(data);
                
                if (packet.getType() == 0x01) { // management
                    internalPackets.removeIf(pack ->
                        packet.getCode1() == pack.getCode1() &&
                        packet.getCode2() == pack.getCode2()
                    );
                } else if (packet.getType() == 0x02) { // control
                    if (packet.getDataSize() > 0) {
                        byte[] pData = packet.getData();
                        if (pData != null && pData.length > 0) {
                            if (pData[0] == 0x57) { // auth
                                sendAuth();
                            } else if (pData[0] == (byte)0xFF) { // close
                                disconnect();
                            }
                        }
                    }
                } else if (packet.getType() == 0x03) { // data
                    sendMenegmand(packet);
                    Get(packet);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Err while Loop: " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }

    }


    private void BroadCast() {
        while (broadcasting && IsConnect() == 1) {
            try {
                Thread.sleep(1000); 
                sendHere();
          
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Err while Broadcast: " + e.getMessage());
            }
        }

    }

    private void sendAuth() {
        try {
            byte[] authData = new byte[31];
            int offset = 0;
            authData[offset++] = (byte)0x57;
            
            byte[] optionsData = option.toArray();
            System.arraycopy(optionsData, 0, authData, offset, 10);
            offset += 10;
            
            byte[] seyData = sessionKey.getBytes();
            int seyLen = Math.min(seyData.length, 20);
            System.arraycopy(seyData, 0, authData, offset, seyLen);

            byte[] header = Packet.buildPacketHeader(
                (byte)0x02,
                (byte)random.nextInt(256),
                (byte)random.nextInt(256),
                (byte)0x0A,
                authData.length
            );

            byte[] fullPacket = new byte[header.length + authData.length];
            System.arraycopy(header, 0, fullPacket, 0, header.length);
            System.arraycopy(authData, 0, fullPacket, header.length, authData.length);

            Packet authPacket = new Packet();
            authPacket.parseByte(fullPacket);
            send(authPacket);
            
          
            
        } catch (Exception e) {
            System.err.println("sendAuth: " + e.getMessage());
        }
    }


    private void sendMenegmand(Packet packet) {
        try {
            byte[] headerMenegmand = Packet.buildPacketHeader(
                (byte)0x01,
                (byte)packet.getCode1(),
                (byte)packet.getCode2(),
                (byte)0x0A,
                0
            );
            
     
            Packet mgmtPacket = new Packet();
            mgmtPacket.setType((byte)0x01);
            mgmtPacket.setCode1(packet.getCode1());
            mgmtPacket.setCode2(packet.getCode2());
            mgmtPacket.setTime((byte)0x0A);
            mgmtPacket.setData(new byte[0]);
            
            send(mgmtPacket);
            
        } catch (Exception e) {
            System.err.println("While sendMenegmand: " + e.getMessage());
        }
    }


    private void sendHere() {
        try {
            byte[] headerMenegmand = Packet.buildPacketHeader(
                (byte)0x02,
                (byte)random.nextInt(256),
                (byte)random.nextInt(256),
                (byte)0x0A,
                1
            );
            
            Packet herePacket = new Packet();
            herePacket.setType((byte)0x02);
            herePacket.setCode1(headerMenegmand[1]);
            herePacket.setCode2(headerMenegmand[2]);
            herePacket.setTime((byte)0x0A);
            herePacket.setData(new byte[]{(byte)0xA0}); 
            
            send(herePacket);
            
        } catch (Exception e) {
            System.err.println("While sendHere: " + e.getMessage());
        }
    }

    private int send(byte[] data) {
        if (socket == null || isConnect != 1) {
            System.err.println("Dond send: socket is dont connection");
            return -1;
        }
        
        try {
            DatagramPacket dpacket = new DatagramPacket(
                data,
                data.length,
                serverAddress,
                serverPort
            );
            socket.send(dpacket);
            return data.length;
        } catch (IOException e) {
            System.err.println("While send UDP: " + e.getMessage());
            return -1;
        }
    }

    public int send(Packet packet) {
        if (packet == null) return -1;
        byte[] data = packet.toByteArray();
        internalPackets.add(packet);
        return send(data);
    }
    public int sendData(byte[] data){
        if(data.length>0){
            Packet dataPacket = new Packet();
            dataPacket.setType((byte)0x03);
            dataPacket.setCode1((byte)random.nextInt(256));
            dataPacket.setCode2((byte)random.nextInt(256));
            dataPacket.setTime((byte)0xA0);
            dataPacket.setData(data);

            send(dataPacket);
        }
        return 0;
    }
    private byte[] receive() {
        if (socket == null || isConnect != 1) {
            return null;
        }
        
        try {
            byte[] buffer = new byte[65507];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            byte[] receivedData = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, receivedData, 0, packet.getLength());
            
            return receivedData;
            
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            System.err.println("While read UDP: " + e.getMessage());
            return null;
        }
    }

    public void disconnect() {
        running = false;
        broadcasting = false;
        
        if (socket != null) {
            socket.close();
            socket = null;
        }
        isConnect = 0;
        internalPackets.clear();
        stopLoop();
        System.out.println("Do Dissconect");
    }

    public void setTimeout(int timeoutMs) {
        this.timeout = timeoutMs;
        if (socket != null) {
            try {
                socket.setSoTimeout(timeoutMs);
            } catch (SocketException e) {
                System.err.println("While set Timeout: " + e.getMessage());
            }
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public String getServerAddress() {
        return serverAddress != null ? serverAddress.getHostAddress() : null;
    }

    public int getServerPort() {
        return serverPort;
    }

    public List<Packet> getInternalPackets() {
        return new ArrayList<>(internalPackets);
    }
}