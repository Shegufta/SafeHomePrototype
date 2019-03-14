package DeviceManager.DeviceDrivers.TPLinkHS110.Implementation;

import Utility.DeviceInfo;
import Utility.DeviceType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class HS110Client
{
    private static final Charset ASCII = Charset.forName("ASCII");
    
    private static final byte KEY = (byte) 0xAB;
    
    private static final byte[] ON = encrypt("{\"system\":{\"set_relay_state\":{\"state\":1}}}");
    
    private static final byte[] OFF = encrypt("{\"system\":{\"set_relay_state\":{\"state\":0}}}");
    
    private static final byte[] SYSINFO = encrypt("{\"system\":{\"get_sysinfo\":{}}}");
    
    private static final byte[] EMETER = encrypt("{ \"emeter\":{ \"get_realtime\":null } }");
    
    private static final byte[] LED_ON = encrypt("{\"system\":{\"set_led_off\":{\"off\": 0}}}");
    
    private static final byte[] LED_OFF = encrypt("{\"system\":{\"set_led_off\":{\"off\": 1}}}");
    
    //private String address;
    private DeviceInfo devInfo;
    private InetSocketAddress inetSocketAddress;
    private Integer socketTimeoutMS;
    
    private final Integer port = 9999;


    private Socket socket = null; // SBA
    private OutputStream out = null; //SBA
    private InputStream in = null; //SBA
    private Boolean isDisposed = true;
    public Boolean isConnectionEstablishedForPersistentConnection;


    private final ObjectMapper mapper = new ObjectMapper();
    
    public HS110Client(DeviceInfo _devInfo)
    {
        assert(DeviceType.TPLINK_HS110 == _devInfo.getDevType());
        assert(0 <= _devInfo.getSocketTimeoutMS());

        this.devInfo = _devInfo;
        this.devInfo.setPort(this.port); // Default TP-Link port is 9999.
        this.inetSocketAddress = new InetSocketAddress(this.devInfo.getIpAddr(), this.port);
        this.socketTimeoutMS = this.devInfo.getSocketTimeoutMS();
        this.isConnectionEstablishedForPersistentConnection = false;
        this.isDisposed = false;

        // setup the JSON decoder
        this.mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        this.mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // register response types
        this.mapper.registerSubtypes(HS110Response.class);

        if(this.devInfo.isPersistentTCP())
        {
            try
            {
                this.sbaInitSocket();
            }
            catch(IOException ioEx)
            {
                //System.out.println("Inside public HS110Client(DeviceInfo _devInfo) : " + ioEx);
            }
        }
    }


    public void dispose() throws IOException
    {
        this.isDisposed = true;
        this.sbaCloseSockets();
    }

    private void sbaInitSocket() throws IOException
    {

        if(null == this.socket)
        {
            this.socket = new Socket();
            this.socket.connect(this.inetSocketAddress, this.socketTimeoutMS );
            this.socket.setSoTimeout(this.socketTimeoutMS);
            this.socket.setKeepAlive(true);
        }

        if(null == this.out)
            this.out = socket.getOutputStream();

        if(null == this.in)
            this.in = socket.getInputStream();

        this.isConnectionEstablishedForPersistentConnection = true;
    }

    public synchronized void sbaCloseSockets() throws IOException
    {
        if(null != this.in)
        {
            this.in.close();
            this.in = null;
        }

        if(null != this.out)
        {
            this.out.close();
            this.out = null;
        }

        if(null != this.socket)
        {
            this.socket.close();
            this.socket = null;
        }

        this.isConnectionEstablishedForPersistentConnection = false;
    }

    
    private <T> T parse(String input, Class<T> type) throws IOException
    {
        if (input == null) return null;
        //Discuss with @intrbiz to use a Logging System
        //System.out.println("Parsing: " + input);
        try (JsonParser p = this.mapper.getFactory().createParser(input))
        {
            return this.mapper.readValue(p, type);
        }
    }
    
    public byte[] send(byte[] message) throws IOException
    {
        byte[] ret;

        if (!this.devInfo.isPersistentTCP())
        {
            this.socket = new Socket();
            this.socket.connect(this.inetSocketAddress, this.socketTimeoutMS);
            this.socket.setSoTimeout(this.socketTimeoutMS);
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
        }
        else
        {
            if(!this.isConnectionEstablishedForPersistentConnection)
                this.sbaInitSocket();

        }

        // write
        this.out.write(message);
        this.out.flush();
        // read
        byte[] buffer = new byte[4096];
        int r = in.read(buffer);
        if (r == -1)
        {
            this.sbaCloseSockets(); // does not matter if it is persistent or not. The socket fails, hence close it.
            return null;
        }

        ret = new byte[r];
        System.arraycopy(buffer, 0, ret, 0, r);

        if (!this.devInfo.isPersistentTCP())
        {
            this.sbaCloseSockets();
        }

        return ret;
    }
    
    public String sendMessage(String message) throws Exception
    {
        return decrypt(send(encrypt(message)));
    }
    
    public HS110Response setLEDState(boolean on) throws Exception
    {
        return parse(decrypt(send(on ? LED_ON : LED_OFF)), HS110Response.class);
    }
    
    public HS110Response ledOn() throws Exception
    {
        return parse(decrypt(send(LED_ON)), HS110Response.class);
    }
    
    public HS110Response ledOff() throws Exception
    {
        return parse(decrypt(send(LED_OFF)), HS110Response.class);
    }
    
    public HS110Response setRelayState(boolean on) throws Exception
    {
        return parse(decrypt(send(on ? ON : OFF)), HS110Response.class);
    }
    
    public HS110Response on() throws IOException
    {
        return parse(decrypt(send(ON)), HS110Response.class);
    }
    
    public HS110Response off() throws IOException
    {
        return parse(decrypt(send(OFF)), HS110Response.class);
    }

    public GetSysInfo getCurrentPlugStatus() throws IOException
    {
        HS110Response response = this.sysInfo();
        if(response == null)
        {
            return null;
        }
        HS110System system = response.getSystem();
        GetSysInfo sysInfo = (GetSysInfo)system.getSysInfo();

        return sysInfo;
    }

    public boolean isON() throws Exception
    {
        GetSysInfo getSysInfo = this.getCurrentPlugStatus();

        if(null == getSysInfo)
            throw new Exception("cannot read from socket, null received");

        return (1 == getSysInfo.getRelayState());
    }
    
    public GetRealtime consumption() throws Exception
    {
        HS110Response response = parse(decrypt(send(EMETER)), HS110Response.class);
        if (response == null || response.getEmeter() == null || response.getEmeter().getRealtime() == null) 
            return null;
        return response.getEmeter().getRealtime();
    }
    
    public HS110Response sysInfo() throws IOException
    {
        return parse(decrypt(send(SYSINFO)), HS110Response.class);
    }
    
    private static byte[] encrypt(String message)
    {
        byte[] data = message.getBytes(ASCII);
        byte[] enc = new byte[data.length + 4];
        ByteBuffer.wrap(enc).putInt(data.length);
        System.arraycopy(data, 0, enc, 4, data.length);
        byte key = KEY;
        for (int i = 4; i < enc.length; i ++)
        {
            enc[i] = (byte) (enc[i] ^ key);
            key = enc[i];
        }
        return enc;
    }
    
    private static String decrypt(byte[] data)
    {
        if (data == null) return null;
        byte key = KEY;
        byte nextKey = 0;
        for (int i = 4; i < data.length; i++)
        {
            nextKey = data[i];
            data[i] = (byte) (data[i] ^ key);
            key = nextKey;
        }

        String returnStr = new String(data, 4, data.length - 4, ASCII); //SBA for debug purpose

        return returnStr;
    }
    
    public static String toHex(byte[] b)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++)
        {
            if ((b[i] & 0xF0) == 0) sb.append("0");
            sb.append(Integer.toHexString(b[i] & 0xFF));
        }
        return sb.toString();
    }

}
