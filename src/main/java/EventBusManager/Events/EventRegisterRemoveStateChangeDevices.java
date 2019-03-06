package EventBusManager.Events;

import Utility.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 10:14 PM
 */

public class EventRegisterRemoveStateChangeDevices
{
    public enum DeviceEventType
    {
        REGISTER,
        REMOVE,
        TURN_ON,
        TURN_OFF
    }

    public List<DeviceInfo> devInfoList;
    public DeviceEventType deviceEventType;

    public EventRegisterRemoveStateChangeDevices(List<DeviceInfo> _devInfoList, DeviceEventType _deviceEventType)
    {
        this.devInfoList = new ArrayList<>(_devInfoList);
        this.deviceEventType = _deviceEventType;
    }

    public EventRegisterRemoveStateChangeDevices(DeviceInfo _devInfo, DeviceEventType _deviceEventType)
    {
        this.devInfoList = new ArrayList<>();
        this.devInfoList.add(_devInfo);
        this.deviceEventType = _deviceEventType;
    }
}
