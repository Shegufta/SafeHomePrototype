package DeviceManager.DeviceDrivers.DummyDevice;

import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnector;
import Utility.DeviceInfo;
import Utility.DeviceStatus;
import Utility.DeviceType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:37 PM
 */
public class DummyDeviceConnector extends DeviceConnector
{
    DeviceStatus devStatus;
    DeviceInfo devInfo;

    public DummyDeviceConnector(DeviceInfo _devInfo)
    {
        super(_devInfo.getDevType());
        assert(DeviceType.DUMMY_DEVICE == _devInfo.getDevType());

        this.deviceType = DeviceType.DUMMY_DEVICE;
        this.devInfo = _devInfo;
        this.devStatus = DeviceStatus.OFF;
    }

    @Override
    public DeviceStatus turnON()
    {
        synchronized (devInfo)
        {
            this.devStatus = DeviceStatus.ON;
            return this.devStatus;
        }
    }

    @Override
    public DeviceStatus turnOFF()
    {
        synchronized (devInfo)
        {
            this.devStatus = DeviceStatus.OFF;
            return this.devStatus;
        }
    }

    @Override
    public DeviceStatus getCurrentStatus()
    {
        synchronized (devInfo)
        {
            return this.devStatus;
        }
    }

    @Override
    public  void Dispose()
    {
        //It is dummy... Nothing to do!
    }

    @Override
    public void ResetConnectionBestEffort()
    {
        //It is dummy... Nothing to do!
    }
}
