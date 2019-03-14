package TpLinkPingMeasurement;

import DeviceManager.DeviceDrivers.TPLinkHS110.TPLinkHS110Connector;
import Utility.DeviceInfo;
import Utility.DeviceStatus;
import Utility.DeviceType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/13/2019
 * @time 5:49 PM
 */
public class CustomizedTpLinkPingTester implements Runnable
{
    DeviceInfo devInfo;
    TPLinkHS110Connector connector;
    public Integer latencyMS = -1;
    Boolean isDisposed;

    Thread thread;

    public static final Integer TIMEOUT_SIGNAL = -1234;

    public CustomizedTpLinkPingTester(String _ipAddr, Integer _port, String _devName, DeviceType _devType, Boolean _isPersistentTCP, Integer socketTimeoutMS)
    {
        if(_devType != DeviceType.TPLINK_HS110)
        {
            System.out.println("current version is only for TPLINK_HS110 devices");
            assert(false);
            System.exit(1);
        }

        this.isDisposed = false;
        this.devInfo = new DeviceInfo( _ipAddr, _port, _devName, _devType, _isPersistentTCP);
        devInfo.setSocketTimeoutMS(socketTimeoutMS);
        this.connector = new TPLinkHS110Connector(this.devInfo);
    }

    public Integer measurePingTime()
    {
        this.latencyMS = (int)System.currentTimeMillis();
        DeviceStatus devStatus = connector.getCurrentStatus();

        if(devStatus == DeviceStatus.TIMEOUT)
            this.latencyMS = TIMEOUT_SIGNAL;
        else
            this.latencyMS = (int)(System.currentTimeMillis() - (long)this.latencyMS);

        return this.latencyMS;
    }

    public Thread startPingThread()
    {
        if(this.isDisposed)
            return null;

        this.resetPingTime();
        this.thread = new Thread(this, "ThreadName = " + this.devInfo.getDevName());
        this.thread.start();

        return this.thread;
    }

    public Integer resetPingTime()
    {
        Integer pingTime = this.latencyMS;
        this.latencyMS = -1;
        return pingTime;
    }

    public void Dispose()
    {
        this.isDisposed = true;
        this.connector.Dispose();
    }

    @Override
    public void run()
    {
        this.measurePingTime();
    }
}
