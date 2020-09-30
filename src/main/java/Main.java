import ConcurrencyManager.ConcurrencyControllerFactory.ConcurrencyController;
import ConcurrencyManager.ConcurrencyControllerSingleton;
import EventBusManager.Events.EventRegisterRemoveStateChangeDevices;
import Measurement.MeasurementSingleton;
import Measurement.MeasurementType;
import RoutineManager.RoutineManagerSingleton;
import SafeHomeManager.SafeHomeManager;
import Utility.*;

import java.io.File;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;


/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:51 PM
 */
public class Main
{

    public enum SHUNKMORN {
        SPREAD,
        COMPACT,
        FIX
    }

    private static SHUNKMORN shrunk_type = SHUNKMORN.COMPACT;

    public static Routine getRoutine(String routineName)
    {
        return SystemParametersSingleton.getInstance().getRoutine(routineName);
    }

    private static List<AbstractMap.SimpleEntry<Integer, String>> getOneShrunkMorningWorkload() {
        /** Get the workload of one run

         Return: a routine list with registration time set up.
         **/
        // Fetch through name instead of getRoutineList to avoid directly modifying original routine
        String[] rtn_names ={
            "daily-wakeup", "good-morning",
            "breakfast1", "breakfast2",
            "daylight-in", "eating-time",
            "news-time", "water-plants"};

        // Parameters for relatively spread
        int stt_gm = 0;
        int end_gm = 1200;
        int stt_daylight = 0;
        int end_daylight = 2100;

        if (shrunk_type == SHUNKMORN.COMPACT) {
            stt_gm = 600;
            end_gm = 780;
            stt_daylight = 1200;
            end_daylight = 1560;
        }

        // Good morning between 7:40 - 8:00
        int t_good_morning = ThreadLocalRandom.current().nextInt(stt_gm, end_gm + 1);
        // User1 make his breakfast after getting up but before 8:05
        int t_breakfast1 = ThreadLocalRandom.current().nextInt(t_good_morning + 10, 1501);
        // Outside brightness reach setting between 7:40 - 8:15
        int t_daylight = ThreadLocalRandom.current().nextInt(stt_daylight, end_daylight + 1);
        // Eating time starts after one of the breakfast is done but before 8:10
        int t_eating = ThreadLocalRandom.current().nextInt( Math.min(t_breakfast1 + 256, 1570), 2101);
        // News time could happen after good morning, but before 8:01
        int t_news = ThreadLocalRandom.current().nextInt( t_good_morning + 10, 1260);

        int[] t_startings;
        if (shrunk_type == SHUNKMORN.FIX) {
            t_startings = new int[]{0, 347, 573, 900, 1497, 1134, 948, 900};
        } else {
            t_startings = new int[]{300, t_good_morning, t_breakfast1, 1200, t_daylight, t_eating, t_news, 1200};
        }

        int[] sortedIndices = IntStream.range(0, t_startings.length)
            .boxed().sorted(Comparator.comparingInt(i -> t_startings[i]))
            .mapToInt(ele -> ele).toArray();
        System.out.println(Arrays.toString(sortedIndices));

        Arrays.sort(t_startings);


        List<AbstractMap.SimpleEntry<Integer, String>> res_workload = new ArrayList<>();

        System.out.println("\n\n-------- Routine sequence ---------");
        for (int i = 0; i < t_startings.length - 1; ++i) {
            int t_after = t_startings[i + 1] - t_startings[i];
            String rtn_name = rtn_names[sortedIndices[i]];
            res_workload.add(new AbstractMap.SimpleEntry<>(t_after, rtn_name));
            System.out.printf("\t Routine %s starts at %d, following sleep %d%n",
                              rtn_name, t_startings[i] - t_startings[0], t_after);
        }

        res_workload.add(new AbstractMap.SimpleEntry<>(0, rtn_names[sortedIndices[sortedIndices.length - 1]]));
        System.out.printf("\t Routine %s starts at %d%n",
                          rtn_names[sortedIndices[sortedIndices.length - 1]],
                          t_startings[t_startings.length - 1]);

        return res_workload;
    }

    public static void main(String [ ] args) throws InterruptedException
    {
        //assert(false); //SBA: Check if assertion is working on your IDE. If it is turn OFF, see the readme (how to turn it ON for intellij)


        SafeHomeManager safeHomeManager = new SafeHomeManager();
        Routine rtn;

        String parent_folder = "/Users/ruiyang/Developer/research/asid/expr/cdf/0929-prototype/";
        String folder = parent_folder + "benchmarking-123.0/";
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        /* Shrunk morning scenario */
        for (int i = 0; i < 10; ++i) {
            System.out.printf("****** Run %d *******\n", i);
            List<AbstractMap.SimpleEntry<Integer, String>> t_rtns = getOneShrunkMorningWorkload();
            for (AbstractMap.SimpleEntry<Integer, String> t_rtn : t_rtns) {
                int trailing_time = t_rtn.getKey();
                rtn = getRoutine(t_rtn.getValue());
                System.out.println("\nExecuting routine " + rtn.routineName + "....");
                safeHomeManager.sendMsgToRoutineManager(rtn);
                Thread.sleep(trailing_time);
            }

            Thread.sleep(20000);
            LockTable lock_table = ConcurrencyControllerSingleton.getInstance().getLockTable();
            MeasurementSingleton.getInstance().RecordIncongruance(lock_table);
            ConcurrencyControllerSingleton.getInstance().clearLockTable();
        }

        ////////////////////  Data Collection  //////////////////
        System.out.printf("Average waiting time: %s \n\twith prct %s %%\n",
            MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME),
            MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME_VS_E2E));

        MeasurementSingleton.getInstance().getFinalIncongruenceResult("EV", folder);

//        List<Float> res = MeasurementSingleton.getInstance().GetResult(MeasurementType.SINGLE_CMD_EXEC_LATENCY);
//        double res_avg = res.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
//        System.out.printf("%d samples. CMD execution time %s\n\t\t Avg %f\n",
//            res.size(),  res, res_avg);
//        System.out.println("Exiting SafeHome program....");
//        safeHomeManager.Dispose();

//        Routine daily_getup = getRoutine("daily-wakeup");
//        System.out.println("Daily wakeup routine....");
//        safeHomeManager.sendMsgToRoutineManager(daily_getup);
//
//        Thread.sleep(2000);
//
//        Routine good_morning = getRoutine("good-morning");
//        System.out.println("Good morning routine....");
//        safeHomeManager.sendMsgToRoutineManager(good_morning);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//        Routine turn_on_bedroomLight = getRoutine("turn_on_bedroomLight");
//        System.out.println("Turn on Bedroom Light....");
//        safeHomeManager.sendMsgToRoutineManager(turn_on_bedroomLight);
//
//        //System.out.println("sleep for 6 second");
//        //Thread.sleep(6000);
//        //System.out.println("wakeup");
//
//        Thread.sleep(1000);
//
//
//        Routine turn_on_air_freshener = getRoutine("turn_on_air_freshener");
//        System.out.println("Turn on air freshener....");
//        safeHomeManager.sendMsgToRoutineManager(turn_on_air_freshener);
//
//        //System.out.println("sleep for 6 second");
//        //Thread.sleep(6000);
//        //System.out.println("wakeup");
//
//        Thread.sleep(1000);
//
//        Routine bedroomRoutine2 = getRoutine("bedroomRoutine2");
//        System.out.println("Turn on air freshener and bedroom light");
//        safeHomeManager.sendMsgToRoutineManager(bedroomRoutine2);
//
//
//        System.out.println("sleep for 10 second");
//        Thread.sleep(10000);
//        System.out.println("wakeup");
//        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//        Routine turn_on_oven = getRoutine("turn_on_oven");
//        Routine theater_mode = getRoutine("theater_mode");
//        Routine open_window = getRoutine("open_window");
//
//        System.out.println("--------------------------------------------------------------------------------------");
//        //turn_on_oven = getRoutine("turn_on_oven");
//        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);
//
////        System.out.println("Turn on Fire Alarm & Exhaust Fan!");
////        safeHomeManager.turnOnOffUnplugDevice("exhaust_fan", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
////        safeHomeManager.turnOnOffUnplugDevice("fire_alarm", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
////        Thread.sleep(6000);
//
//        System.out.println("Turn On oven....");
//        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);
//        System.out.println("Again sleeping");
//        Thread.sleep(6000);
//        System.out.println("wakeup");
//
//        System.out.println("Turn ON tv!");
//        safeHomeManager.turnOnOffUnplugDevice("tv", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON);
//
//        System.out.println("Again sleeping looooooooooooooooooong");
//        Thread.sleep(6000);
//        System.out.println("wakeup");
//
//        System.out.println("Turn OFF tv!");
//        safeHomeManager.turnOnOffUnplugDevice("tv", EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_OFF);
//
//        System.out.println("Again sleeping");
//        Thread.sleep(6000);
//        System.out.println("wakeup");
//
//        System.out.println("Unplug Fire Alarm !");
//        safeHomeManager.turnOnOffUnplugDevice("fire_alarm", EventRegisterRemoveStateChangeDevices.DeviceEventType.UNPLUG);
//        Thread.sleep(6000);
//
//
//        turn_on_oven = getRoutine("turn_on_oven");
//        safeHomeManager.sendMsgToRoutineManager(turn_on_oven);
//
//
//        ////////////////////////////////////////////////////////////////////////////////////////
//
//
//        System.out.println("Again sleeping");
//        Thread.sleep(30000); // To run it for longer time, put a higher value
//
//        ////////////////////  Data Collection  //////////////////
//        System.out.printf("Average waiting time: %s with prct %s %%\n",
//            MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME),
//            MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME_VS_E2E));
//
        System.out.println("Exiting SafeHome program....");
        safeHomeManager.Dispose();
    }
}
