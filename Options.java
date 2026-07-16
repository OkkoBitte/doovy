

public record Options(byte[] full) {
    
    // INDEXES
    public static final int CLIENT_TYPE = 0;
    public static final int CONNECTION_TYPE = 1;
    public static final int ACTION = 2;
    public static final int CONNECTION_MODE = 3;

    
    public Options {
        if (full == null || full.length != 10) {
            throw new IllegalArgumentException("Options must be exactly 10 bytes");
        }
        full = full.clone();
    }
    
   
    public static Options of(byte... bytes) {
        if (bytes.length != 10) {
            throw new IllegalArgumentException("Need exactly 10 bytes");
        }
        return new Options(bytes);
    }
    
   
    public static Options empty() {
        return new Options(new byte[10]);
    }
    
    
    public byte getClientType() { return full[CLIENT_TYPE]; }
    public byte getConnectionType() { return full[CONNECTION_TYPE]; }
    public byte getAction() { return full[ACTION]; }
    public byte getConnectionMode() { return full[CONNECTION_MODE]; }
    
    
    public int getClientTypeUnsigned() { 
        return full[CLIENT_TYPE] & 0xFF; 
    }
    



    public byte[] toArray() {
        return full.clone();
    }
    


    

    public String toHexString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : full) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}