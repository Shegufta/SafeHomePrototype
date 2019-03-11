import DeviceManager.DeviceConnectionManagerSingleton;
import EventBusManager.Events.EventRegisterRemoveStateChangeDevices;
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
        //assert(false); //SBA: Check if assertion is working on your IDE. If it is turn OFF, see the readme (how to turn it ON for intellij)


        SafeHomeManager safeHomeManager = new SafeHomeManager();

        Routine turn_on_oven = SystemParametersSingleton.getInstance().getRoutine("turn_on_oven");
        Routine theater_mode = SystemParametersSingleton.getInstance().getRoutine("theater_mode");
        Routine open_window = SystemParametersSingleton.getInstance().getRoutine("open_window");

        System.out.println("Turn on Fire Alarm & Exhaust Fan!");
        safeHomeManager.turnOnOffUnplugDevice("exhaust_fan", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
        safeHomeManager.turnOnOffUnplugDevice("fire_alarm", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
        Thread.sleep(6000);

        System.out.println("Turn On oven....");
        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);
        System.out.println("Again sleeping");
        Thread.sleep(6000);
        System.out.println("wakeup");
        
        System.out.println("Turn ON tv!");
        safeHomeManager.turnOnOffUnplugDevice("tv", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);

        System.out.println("Again sleeping looooooooooooooooooong");
        Thread.sleep(6000);
        System.out.println("wakeup");

        System.out.println("Turn OFF tv!");
        safeHomeManager.turnOnOffUnplugDevice("tv", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_OFF);

        System.out.println("Again sleeping");
        Thread.sleep(6000);
        System.out.println("wakeup");

        System.out.println("Unplug Fire Alarm !");
        safeHomeManager.turnOnOffUnplugDevice("fire_alarm", EventRegisterRemoveStateChangeDevices.DeviceEventType.UNPLUG);
        Thread.sleep(6000);

        System.out.println("Turn On oven AGAIN!....");
        System.out.println("TO RUI: KNOWN ISSUE: never use same routine twice... it will crash the code");
        System.out.println("I have detected the bug, I will fix it later");
        System.out.println("For example, if you want to call the routine turn_on_oven again, create a new instance");
        System.out.println("Below you will get an example of how to use the same routine twice (this is temporary, I will fix it soon)");

        turn_on_oven = SystemParametersSingleton.getInstance().getRoutine("turn_on_oven");
        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);


        ////////////////////////////////////////////////////////////////////////////////////////


        System.out.println("Again sleeping");
        Thread.sleep(30000); // To run it for longer time, put a higher value
        System.out.println("Exiting SafeHome program....");
        safeHomeManager.Dispose();



    }
}
