import DeviceManager.DeviceConnectionManagerSingleton;
import SafeHomeManager.SafeHomeManager;
import Utility.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:51 PM
 */
public class Main
{

    public static void main(String [ ] args) throws InterruptedException
    {
        SafeHomeManager safeHomeManager = new SafeHomeManager();
        //assert(false); //SBA: Check if assertion is working on your IDE. If it is turn OFF, see the readme (how to turn it ON for intellij)

        //Routine routine1 = SystemParametersSingleton.getInstance().getRoutine("routine1");
        //this.sendMsgToRoutineManager(routine1);

        Routine routine3 = SystemParametersSingleton.getInstance().getRoutine("routine3");
        safeHomeManager.sendMsgToRoutineManager(routine3);
        ////////////////////////////////////////////////////////////////////////////////////////


        System.out.println("Again sleeping");
        Thread.sleep(30000); // To run it for longer time, put a higher value
        System.out.println("Exiting SafeHome program....");
        safeHomeManager.Dispose();


        /**
        SafeHomeManager safeHomeManager = new SafeHomeManager();

        DeviceInfo dummyDev1 = new DeviceInfo("a.b.c.d", 1234, "dummyDev1", DeviceType.DUMMY_DEVICE, true);
        DeviceInfo dummyDev2 = new DeviceInfo("a.b.c.d", 1234, "dummyDev2", DeviceType.DUMMY_DEVICE, true);
        DeviceInfo dummyDev3 = new DeviceInfo("a.b.c.d", 1234, "dummyDev3", DeviceType.DUMMY_DEVICE, true);
        DeviceInfo dummyDev4 = new DeviceInfo("a.b.c.d", 1234, "dummyDev4", DeviceType.DUMMY_DEVICE, true);
        DeviceInfo tpLink1 = new DeviceInfo("192.168.0.44", 1234, "tpLink1", DeviceType.TPLINK_HS110, true);



        List<DeviceInfo> devInfoList = new ArrayList<>();

        devInfoList.add(dummyDev1);
        devInfoList.add(dummyDev2);
        devInfoList.add(dummyDev3);
        devInfoList.add(dummyDev4);
        devInfoList.add(tpLink1);

        System.out.println("Register");
        safeHomeManager.RegisterDevices(devInfoList);

        System.out.println("\nRegistration Done, sleep for 6 second......................... REMOVE PLUG\n");
        Thread.sleep(6000);

        System.out.println("\n wakeup \n");

        List<Command> cmdList = new ArrayList<>();

        Command cmd1 = new Command(dummyDev1, DeviceStatus.ON, CommandPriority.MUST);
        cmdList.add(cmd1);

        Command cmdTpLink = new Command(tpLink1, DeviceStatus.ON, CommandPriority.MUST);
        cmdList.add(cmdTpLink);

        System.out.println("cmd sending ");
        safeHomeManager.tempFunction_Test_Safety(cmdList);

        System.out.println("cmd sending done");


//
//        List<DeviceInfo> removeList = new ArrayList<>();
//
//        removeList.add(dummyDev2);
//        System.out.println("remove");
//        safeHomeManager.RemoveDevices(removeList);
//
        System.out.println("Again sleeping");
        Thread.sleep(30000);
        System.out.println("disposing");
        safeHomeManager.Dispose();
        */
    }
}
