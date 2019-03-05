package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 12:54 AM
 */
public class DeviceInfo
{
    @JsonProperty
    private String devName;

    @JsonProperty
    private DeviceType devType;

    @JsonProperty
    private String ipAddr;

    @JsonProperty
    private Integer port;

    @JsonProperty
    private Boolean isPersistentTCP;

    //@JsonProperty
    //private Integer socketTimeoutMS;

    @JsonIgnore // For now lets IGNORE this one from the JSON string
    private Integer socketTimeoutMS = -1;

    public DeviceInfo(){}//need this blank constructor for JSON parsing

    @JsonIgnore
    public DeviceInfo(String _ipAddr, Integer _port, String _devName, DeviceType _devType, Boolean _isPersistentTCP)
    {
        this.ipAddr = _ipAddr;
        this.port = _port;
        this.devName = _devName;
        this.devType = _devType;
        this.isPersistentTCP = _isPersistentTCP;
    }

    @JsonIgnore
    public String getIpAddr()
    {
        return ipAddr;
    }

    @JsonIgnore
    public void setIpAddr(String ipAddr)
    {
        this.ipAddr = ipAddr;
    }

    @JsonIgnore
    public Integer getPort()
    {
        return port;
    }

    @JsonIgnore
    public void setPort(Integer port)
    {
        this.port = port;
    }

    @JsonIgnore
    public String getDevName()
    {
        return devName;
    }

    @JsonIgnore
    public void setDevName(String devName)
    {
        this.devName = devName;
    }

    @JsonIgnore
    public DeviceType getDevType()
    {
        return devType;
    }

    @JsonIgnore
    public void setDevType(DeviceType devType)
    {
        this.devType = devType;
    }

    @JsonIgnore
    public Boolean isPersistentTCP()
    {
        return isPersistentTCP;
    }

    @JsonIgnore
    public void setPersistentTCP(Boolean isPersistentTCP)
    {
        this.isPersistentTCP = isPersistentTCP;
    }

    @JsonIgnore
    public Integer getSocketTimeoutMS()
    {
        return socketTimeoutMS;
    }

    @JsonIgnore
    public void setSocketTimeoutMS(Integer socketTimeoutMS)
    {
        this.socketTimeoutMS = socketTimeoutMS;
    }


    @Override
    public String toString()
    {
        return "DeviceInfo{" +
                "devName='" + devName + '\'' +
                ", devType=" + devType +
                ", ipAddr='" + ipAddr + '\'' +
                ", port=" + port +
                ", isPersistentTCP=" + isPersistentTCP +
                ", socketTimeoutMS=" + socketTimeoutMS +
                '}';
    }
}
