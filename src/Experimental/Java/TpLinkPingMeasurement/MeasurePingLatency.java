package TpLinkPingMeasurement;

import Utility.DeviceType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static TpLinkPingMeasurement.CustomizedTpLinkPingTester.TIMEOUT_SIGNAL;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/13/2019
 * @time 5:17 PM
 */
public class MeasurePingLatency
{
    public static void main(String[] args) throws InterruptedException, IOException
    {
        String wifiRouterName = "TPLinkC7";
        Integer HDstreamCount = 0;
        Boolean isParallelPing = false;
        Boolean isPersistentTCP = true;
        Integer socketTimeoutMS = 2000;
        Integer pingCountPerDevice = 100;


        TestResultContainer testResultContainer = new TestResultContainer(
                wifiRouterName,
                HDstreamCount,
                pingCountPerDevice,
                socketTimeoutMS,
                isParallelPing,
                isPersistentTCP);


        Integer runWithDevCountUpTo = 8;

        for(Integer devCount = 1 ; devCount <= runWithDevCountUpTo ; ++devCount )
        {
            Map<String, CustomizedTpLinkPingTester> nameTesterMap = prepareDevices(isPersistentTCP, socketTimeoutMS, devCount);

            for(int smplCount = 1 ; smplCount <= pingCountPerDevice ; ++smplCount)
            {
                System.out.println("batch " + devCount + "/" + runWithDevCountUpTo + " - Sample : " + smplCount);

                if(isParallelPing)
                {
                    List<Thread> threadList = new ArrayList<>();

                    for(Map.Entry<String, CustomizedTpLinkPingTester> tuple : nameTesterMap.entrySet())
                    {
                        threadList.add(tuple.getValue().startPingThread());
                    }

                    for(Thread thread : threadList)
                    {
                        thread.join();
                    }

                    for(Map.Entry<String, CustomizedTpLinkPingTester> tuple : nameTesterMap.entrySet())
                    {
                        Integer pingMS = tuple.getValue().latencyMS; // ping have been already measured inside the run() of the startPingThread()

                        if(pingMS == TIMEOUT_SIGNAL)
                        {
                            System.out.println( tuple.getKey() + " - Timeout");
                            testResultContainer.increaseTimeoutCount();
                        }
                        else
                        {
                            testResultContainer.addToList(devCount, pingMS);
                        }
                    }
                }
                else
                {
                    for(Map.Entry<String, CustomizedTpLinkPingTester> tuple : nameTesterMap.entrySet())
                    {

                        Integer pingMS = tuple.getValue().measurePingTime();

                        if(pingMS == TIMEOUT_SIGNAL)
                        {
                            System.out.println( tuple.getKey() + " - Timeout");
                            testResultContainer.increaseTimeoutCount();
                        }
                        else
                        {
                            testResultContainer.addToList(devCount, pingMS);
                        }
                    }
                }
            }


            System.out.println("Disposing.....");
            for(Map.Entry<String, CustomizedTpLinkPingTester> tuple : nameTesterMap.entrySet())
            {
                tuple.getValue().Dispose();
                Thread.sleep(100);
            }
            Thread.sleep(1000);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String fileName = System.currentTimeMillis() + String.valueOf(HDstreamCount) + isParallelPing + ".log";

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), testResultContainer);

    }

    private static Map<String, CustomizedTpLinkPingTester> prepareDevices(Boolean _isPersistentTCP, Integer _socketTimeoutMS, Integer _deviceCount)
    {
        String ipAddr = "";
        Integer port = 0000;
        String devName = "";
        DeviceType devType = DeviceType.TPLINK_HS110;
        Boolean isPersistentTCP = _isPersistentTCP;
        Integer socketTimeoutMS = _socketTimeoutMS;

        Map<String, CustomizedTpLinkPingTester> nameTesterMap = new HashMap<>();
        List<CustomizedTpLinkPingTester> devList = new ArrayList<>();

        ipAddr = "192.168.0.31";
        devName = "wyzeCam";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.40";
        devName = "airFreshener";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.42";
        devName = "exhaustFan";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.44";
        devName = "extra";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.50";
        devName = "bedroomLight";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.51";
        devName = "diningLight";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.52";
        devName = "livingLight";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        ipAddr = "192.168.0.53";
        devName = "kitchenLight";
        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        //WARNING: tv and heater times out... the switch is a older model HS110 V1, maybe that is causing this issue.
        //WARNING: try not to use these two devices

//        ipAddr = "192.168.0.41";
//        devName = "tv";
//        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));
//
//        ipAddr = "192.168.0.43";
//        devName = "heater";
//        devList.add(new CustomizedTpLinkPingTester(ipAddr, port, devName, devType, isPersistentTCP, socketTimeoutMS));

        if(devList.size() < _deviceCount)
        {
            System.out.println("NOT ENOUGH DEVICES.... ADD MORE...");
            assert (false);
            System.exit(1);
        }

        for(int index = 0 ; index < _deviceCount ; ++index)
        {
            nameTesterMap.put(devList.get(index).devInfo.getDevName(), devList.get(index));
        }

        return nameTesterMap;
    }

}
