public class Main {
    public static byte[] Message(String recipientSey, String message) {
        byte[] seyBytes = recipientSey.getBytes();
        byte[] msgBytes = message.getBytes();
        
        byte[] data = new byte[20 + 1 + msgBytes.length];
    
        System.arraycopy(seyBytes, 0, data, 0, Math.min(20, seyBytes.length));
        

        data[20] = 0x01; // text_t
        
        System.arraycopy(msgBytes, 0, data, 21, msgBytes.length);
        
        return data;
    }

    public static void main(String[] args) {
        
        try {
            
            Options options = Options.of(
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,
                (byte)0x09, (byte)0x0A
            );
            
            MyClient client = new MyClient(options, "MY0SESSION0KEY012345");
            
            if (client.Connect("localhost", 3333) > 0) {
                
                client.startLoop();
                
                
                client.startBroadcast();
                
                
                Thread.sleep(1000);
               
                client.sendData(Message("MY0SESSION0KEY012345","Hello"));
    
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
     
    }
}


class MyClient extends ClientNetworkCore {
    
    public MyClient(Options options, String sey) {
        super(options, sey);
    }
    
    @Override
    public void Get(Packet packet) {
        if (packet.getDataSize() > 0) {
            byte[] data = packet.getData();
            System.out.println("Get Data: " + bytesToHex(data));
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}