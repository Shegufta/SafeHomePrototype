package TpLinkPingMeasurement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/13/2019
 * @time 6:00 PM
 */
public class TestResultContainer
{
    @JsonProperty
    String wifiRouterName;
    @JsonProperty
    Integer HDstreamCount;
    @JsonProperty
    Integer socketTimeoutMS;
    @JsonProperty
    Integer pingCountPerDevice;
    @JsonProperty
    Integer totalTimeoutCount;
    @JsonProperty
    Boolean isParallelPing;
    @JsonProperty
    Boolean isPersistentTCP;
    @JsonProperty
    Map<Integer, List<Integer>> devCntLatencyMSListMap;


    public TestResultContainer() // default constructor for JSON
    {
        this.totalTimeoutCount = 0;
    }

    @JsonIgnore
    public TestResultContainer(String _wifiRouterName,
                               Integer _HDstreamCount,
                               Integer _pingCountPerDevice,
                               Integer _socketTimeoutMS,
                               Boolean _isParallelPing,
                               Boolean _isPersistentTCP)
    {
        this.wifiRouterName = _wifiRouterName;
        this.HDstreamCount = _HDstreamCount;
        this.pingCountPerDevice = _pingCountPerDevice;
        this.socketTimeoutMS = _socketTimeoutMS;
        this.isParallelPing = _isParallelPing;
        this.isPersistentTCP = _isPersistentTCP;
        this.totalTimeoutCount = 0;
        this.devCntLatencyMSListMap = new HashMap<>();
    }

    @JsonIgnore
    public void addToList(Integer totalDeviceCount, Integer latencyMS)
    {
        if(!this.devCntLatencyMSListMap.containsKey(totalDeviceCount))
        {
            this.devCntLatencyMSListMap.put(totalDeviceCount, new ArrayList<>());
        }
        this.devCntLatencyMSListMap.get(totalDeviceCount).add(latencyMS);
    }

    @JsonIgnore
    public void increaseTimeoutCount()
    {
        this.totalTimeoutCount++;
    }
}
