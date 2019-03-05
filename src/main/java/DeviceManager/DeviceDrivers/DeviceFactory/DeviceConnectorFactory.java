package DeviceManager.DeviceDrivers.DeviceFactory;

import DeviceManager.DeviceDrivers.DummyDevice.DummyDeviceConnector;
import DeviceManager.DeviceDrivers.TPLinkHS110.TPLinkHS110Connector;
import Utility.DeviceInfo;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:31 PM
 */
public class DeviceConnectorFactory
{
    public static DeviceConnector createDeviceConnector(DeviceInfo _devInfo)
    {
        switch(_devInfo.getDevType())
        {
            case DUMMY_DEVICE:
            {
                return new DummyDeviceConnector(_devInfo);
            }
            case TPLINK_HS110:
            {
                return new TPLinkHS110Connector(_devInfo);
            }
            default:
            {
                assert (false);
                System.out.println("Inside DeviceConnectorFactory, Unknown Device Type: _devInfo = " + _devInfo);
                System.exit(1);
            }
        }

        return null;
    }
}
