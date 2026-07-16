public class Packet {
        private byte type;
        private byte code1;
        private byte code2;
        private byte time;
        private byte size1; 
        private byte size2; 
        private byte[] data;
        
        public Packet() {
            this.data = new byte[0];
        }
        

        void clear() {
            type = 0;
            code1 = 0;
            code2 = 0;
            time = 0;
            size1 = 0;
            size2 = 0;
            data = new byte[0];
        }
        
        void parseByte(byte[] rawData) {
            if (rawData == null || rawData.length < 6) {
                throw new IllegalArgumentException("Data must be at least 6 bytes");
            }
            
            type =  rawData[0];
            code1 = rawData[1];
            code2 = rawData[2];
            time =  rawData[3];
            size1 = rawData[4]; 
            size2 = rawData[5];  
            
            int dataSize = getDataSize();
            if (dataSize > 0 && rawData.length >= 6 + dataSize) {
                data = new byte[dataSize];
                System.arraycopy(rawData, 6, data, 0, dataSize);
            } else {
                data = new byte[0];
            }
        }
        
     
        int getDataSize() {
            return (size2 & 0xFF) << 8 | (size1 & 0xFF);
        }
        

        void setDataSize(int size) {
            if (size < 0 || size > 65535) {
                throw new IllegalArgumentException("Size must be between 0 and 65535");
            }
            size1 = (byte)(size & 0xFF);         
            size2 = (byte)((size >> 8) & 0xFF);   
        }

        public static byte[] buildPacketHeader(byte type, byte hxcode0, byte hxcode1, byte timeout, int dataSize) {
            byte[] header = new byte[6]; 
            
            header[0] = type;                   
            header[1] = hxcode0;                
            header[2] = hxcode1;                
            header[3] = timeout;                 
            header[4] = (byte)(dataSize & 0xFF); 
            header[5] = (byte)((dataSize >> 8) & 0xFF);
            
            return header;
        }


        public byte getType() {
            return type;
        }
        
        public byte getCode1() {
            return code1;
        }
        
        public byte getCode2() {
            return code2;
        }
        
        public byte getTime() {
            return time;
        }
        
        public byte getSize1() {
            return size1;
        }
        
        public byte getSize2() {
            return size2;
        }
        
        public byte[] getData() {
            return data.clone();  
        }
        
        public void setType(byte type) {
            this.type = type;
        }
        
        public void setCode1(byte code1) {
            this.code1 = code1;
        }
        
        public void setCode2(byte code2) {
            this.code2 = code2;
        }
        
        public void setTime(byte time) {
            this.time = time;
        }
        
        public void setData(byte[] data) {
            this.data = data != null ? data.clone() : new byte[0];
            setDataSize(this.data.length); 
        }
        
       
        byte[] toByteArray() {
            int totalSize = 6 + data.length;
            byte[] result = new byte[totalSize];
            
            result[0] = type;
            result[1] = code1;
            result[2] = code2;
            result[3] = time;
            result[4] = size1;  
            result[5] = size2;  
            
            if (data.length > 0) {
                System.arraycopy(data, 0, result, 6, data.length);
            }
            
            return result;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Packet[type=0x%02X, code1=0x%02X, code2=0x%02X, time=0x%02X, size=0x%04X(%d), dataLen=%d]",
                type & 0xFF, code1 & 0xFF, code2 & 0xFF, time & 0xFF,
                getDataSize(), getDataSize(), data.length
            );
        }
    }