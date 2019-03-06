package DeviceManager.DeviceDrivers.DeviceFactory;

import Utility.DeviceStatus;
import Utility.DeviceType;


/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:31 PM
 */
public abstract class DeviceConnector
{
    public DeviceType deviceType;

    public DeviceConnector(DeviceType _deviceType)
    {
        this.deviceType = _deviceType;
    }

    public abstract DeviceStatus turnON();
    public abstract DeviceStatus turnOFF();
    public abstract DeviceStatus getCurrentStatus();
    public abstract DeviceStatus simulateTIMEOUTonlyIn_DUMMY_DEVICE(); // devices other than DUMMY_DEVICE should create just an empty function
    public abstract void ResetConnectionBestEffort();
    public abstract void Dispose();
}
