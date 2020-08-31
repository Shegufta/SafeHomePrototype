import EventBusManager.Events.EventRegisterRemoveStateChangeDevices;
import SafeHomeManager.SafeHomeManager;
import Utility.*;


/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:51 PM
 */
public class Main
{
    public static Routine getRoutine(String routineName)
    {
        return SystemParametersSingleton.getInstance().getRoutine(routineName);
    }

    public static void main(String [ ] args) throws InterruptedException
    {
        //assert(false); //SBA: Check if assertion is working on your IDE. If it is turn OFF, see the readme (how to turn it ON for intellij)


        SafeHomeManager safeHomeManager = new SafeHomeManager();

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Routine turn_on_bedroomLight = getRoutine("turn_on_bedroomLight");
        System.out.println("Turn on Bedroom Light....");
        safeHomeManager.sendMsgToRoutineManager(turn_on_bedroomLight);

        //System.out.println("sleep for 6 second");
        //Thread.sleep(6000);
        //System.out.println("wakeup");

        Thread.sleep(1000);


        Routine turn_on_air_freshener = getRoutine("turn_on_air_freshener");
        System.out.println("Turn on air freshener....");
        safeHomeManager.sendMsgToRoutineManager(turn_on_air_freshener);

        //System.out.println("sleep for 6 second");
        //Thread.sleep(6000);
        //System.out.println("wakeup");

        Thread.sleep(1000);

        Routine bedroomRoutine2 = getRoutine("bedroomRoutine2");
        System.out.println("Turn on air freshener and bedroom light");
        safeHomeManager.sendMsgToRoutineManager(bedroomRoutine2);


        System.out.println("sleep for 10 second");
        Thread.sleep(10000);
        System.out.println("wakeup");
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Routine turn_on_oven = getRoutine("turn_on_oven");
        Routine theater_mode = getRoutine("theater_mode");
        Routine open_window = getRoutine("open_window");

        System.out.println("--------------------------------------------------------------------------------------");
        //turn_on_oven = getRoutine("turn_on_oven");
        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);

//        System.out.println("Turn on Fire Alarm & Exhaust Fan!");
//        safeHomeManager.turnOnOffUnplugDevice("exhaust_fan", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
//        safeHomeManager.turnOnOffUnplugDevice("fire_alarm", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
//        Thread.sleep(6000);

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


        turn_on_oven = getRoutine("turn_on_oven");
        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);


        ////////////////////////////////////////////////////////////////////////////////////////


        System.out.println("Again sleeping");
        Thread.sleep(30000); // To run it for longer time, put a higher value
        System.out.println("Exiting SafeHome program....");
        safeHomeManager.Dispose();
    }
}
