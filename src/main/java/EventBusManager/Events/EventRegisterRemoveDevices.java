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

public class EventRegisterRemoveDevices
{
    public List<DeviceInfo> devInfoList;
    public Boolean isRegisterEvent;

    public EventRegisterRemoveDevices(List<DeviceInfo> _devInfoList, Boolean _isRegisterEvent)
    {
        this.devInfoList = new ArrayList<>(_devInfoList);
        this.isRegisterEvent = _isRegisterEvent;
    }
}
