package DeviceManager.DeviceDrivers.TPLinkHS110;

import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnector;
import DeviceManager.DeviceDrivers.TPLinkHS110.Implementation.HS110Client;
import Measurement.MeasurementSingleton;
import Measurement.MeasurementType;
import Utility.DeviceInfo;
import Utility.DeviceStatus;
import Utility.DeviceType;

import java.io.IOException;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:37 PM
 */

/**
 * FOR TP-LINK,the default port is 9999...
 */

public class TPLinkHS110Connector extends DeviceConnector
{
    DeviceInfo devInfo;
    HS110Client hs110Client;
    Boolean isDisposed;

    public TPLinkHS110Connector(DeviceInfo _devInfo)
    {
        super(_devInfo.getDevType());
        assert(DeviceType.TPLINK_HS110 == _devInfo.getDevType());

        this.deviceType = DeviceType.TPLINK_HS110;

        this.devInfo = _devInfo;
        this.hs110Client = new HS110Client(this.devInfo);
        this.isDisposed = false;
    }

    @Override
    public DeviceStatus turnON()
    {
        if(this.isDisposed)
            return DeviceStatus.TIMEOUT;

        try
        {
            if(null == this.hs110Client)
                this.hs110Client = new HS110Client(this.devInfo);

            synchronized (this.hs110Client)
            {
                long start_time = System.currentTimeMillis();
                this.hs110Client.on();
                MeasurementSingleton.getInstance().AddResult(
                    MeasurementType.SINGLE_CMD_EXEC_LATENCY,
                    (float) (System.currentTimeMillis() - start_time));
                return DeviceStatus.ON;
            }
        }
        catch (IOException ioEx)
        {
            System.out.println("\n\n-----------------NO-CONNECTION-----------------------");
            System.out.println("Inside TPLinkHS110Connector::turnON() " + ioEx );
            System.out.println("---------------------------------------------------------\n");
            return DeviceStatus.TIMEOUT;
        }
    }

    @Override
    public DeviceStatus turnOFF()
    {
        if(this.isDisposed)
            return DeviceStatus.TIMEOUT;

        try
        {
            if(null == this.hs110Client)
                this.hs110Client = new HS110Client(this.devInfo);

            synchronized (this.hs110Client)
            {
                long start_time = System.currentTimeMillis();
                this.hs110Client.off();
                MeasurementSingleton.getInstance().AddResult(
                    MeasurementType.SINGLE_CMD_EXEC_LATENCY,
                    (float) (System.currentTimeMillis() - start_time));
                return DeviceStatus.OFF;
            }
        }
        catch (IOException ioEx)
        {
            System.out.println("\n\n-----------------NO-CONNECTION-----------------------");
            System.out.println("Inside TPLinkHS110Connector::turnOFF() " + ioEx );
            System.out.println("---------------------------------------------------------\n");
            return DeviceStatus.TIMEOUT;
        }
    }

    @Override
    public DeviceStatus getCurrentStatus()
    {
        if(this.isDisposed)
            return DeviceStatus.TIMEOUT;

        try
        {
            if(null == this.hs110Client)
                this.hs110Client = new HS110Client(this.devInfo);

            synchronized (this.hs110Client)
            {
                if (this.hs110Client.isON())
                {
                    return DeviceStatus.ON;
                }
                else
                {
                    return DeviceStatus.OFF;
                }
            }
        }
        catch (IOException ioEx)
        {
            System.out.println("---------------------------------------------------------\n");
            System.out.println("Inside TPLinkHS110Connector::getCurrentStatus() " + ioEx );
            System.out.println("---------------------------------------------------------\n");
            return DeviceStatus.TIMEOUT;
        }
    }

    @Override
    public DeviceStatus simulateTIMEOUTonlyIn_DUMMY_DEVICE()
    {
        System.out.println("This function should only be called for DUMMY_DEVICE");
        System.out.println("Should not be called from : " + this.deviceType.name());
        System.out.println("FATAL ERROR... TERMINATING PROGRAM....");
        System.exit(1);

        return null;
    }

    @Override
    public void Dispose()
    {
        if(null != this.hs110Client)
        {
            synchronized (this.hs110Client)
            {
                try
                {
                    this.hs110Client.dispose();
                }
                catch (IOException ioEx)
                {
                    System.out.println("Inside TPLinkHS110Connector::Dispose() " + ioEx );
                }
                finally
                {
                    this.isDisposed = true;
                    this.hs110Client = null;
                }
            }
        }
    }

    @Override
    public void ResetConnectionBestEffort()
    {
        if(null != this.hs110Client)
        {
            synchronized (this.hs110Client)
            {
                try
                {
                    this.hs110Client.dispose();
                }
                catch(IOException ioEx)
                {
                    System.out.println("Inside TPLinkHS110Connector::ResetConnectionBestEffort() " + ioEx );
                }
                finally
                {
                    this.hs110Client = null;
                }
            }
        }
    }
}
