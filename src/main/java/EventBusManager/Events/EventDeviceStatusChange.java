package EventBusManager.Events;

import Utility.DeviceInfo;
import Utility.DeviceStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 1:01 AM
 */
public class EventDeviceStatusChange
{
    public Map<String, DeviceStatus> devNameStatusMap;

    public EventDeviceStatusChange(List<DeviceInfo> _devInfoList, DeviceStatus _deviceStatus)
    {
        this.devNameStatusMap = new HashMap<>();

        for(DeviceInfo devInfo : _devInfoList)
        {
            String devName = devInfo.getDevName();
            this.devNameStatusMap.put(devName, _deviceStatus);
        }
    }

    public EventDeviceStatusChange(Map<String, DeviceStatus> _devNameStatusMap)
    {
        this.devNameStatusMap = new HashMap<>(_devNameStatusMap);
    }

    @Override
    public String toString()
    {
        String str = "";

        for (Map.Entry<String, DeviceStatus> nameDevStatus : this.devNameStatusMap.entrySet())
        {
            str += "[ " + nameDevStatus.getKey() + " : " + nameDevStatus.getValue().name() +" ] ";
        }

        return str;
    }
}
