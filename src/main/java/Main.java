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
import java.util.stream.Collectors;
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

    private static SHUNKMORN shrunk_type = SHUNKMORN.FIX;

    public static Routine getRoutine(String routineName)
    {
        return SystemParametersSingleton.getInstance().getRoutine(routineName);
    }

    private static List<AbstractMap.SimpleEntry<Integer, String>> getOneMorningWorkload() {
        int fst_lst_rtn_interval = 15000;

        // Add all routines into workload
        List<String> workload = SystemParametersSingleton.getInstance().getRoutineNameList();

        // Shuffle list
        Collections.shuffle(workload.subList(0, workload.size() - 1));
        assert(!workload.isEmpty());

        // Get a list of random registration time, sort, and assign
        List<Integer> reg_times = IntStream.range(0, workload.size() - 2)
            .mapToObj(i -> ThreadLocalRandom.current().nextInt(0, fst_lst_rtn_interval + 1))
            .sorted()
            .collect(Collectors.toList());

        reg_times.add(0, 0);
        reg_times.add(fst_lst_rtn_interval);

        assert(workload.size() == reg_times.size());

        List<AbstractMap.SimpleEntry<Integer, String>> res_workload = new ArrayList<>();
        for (int i = 0; i < workload.size() - 1; ++i) {
            res_workload.add(new AbstractMap.SimpleEntry<>(reg_times.get(i + 1) - reg_times.get(i), workload.get(i)));
            System.out.printf("\t Routine %s starts at %d, following sleep %d%n",
                workload.get(i), reg_times.get(i), reg_times.get(i + 1) - reg_times.get(i));
        }
        res_workload.add(new AbstractMap.SimpleEntry<>(0, workload.get(workload.size() - 1)));
        System.out.printf("\t Routine %s starts at %d%n", workload.get(workload.size() - 1), 0);

      return res_workload;
    }

    private static List<AbstractMap.SimpleEntry<Integer, String>> getOneShrunkMorningWorkload() {
        /** Get the workload of one run

         Return: a routine list with trailing time set up.
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

        String parent_folder = "/Users/ruiyang/Developer/research/asid/expr/cdf/1006-prototype-10run/";
        String folder = parent_folder + "benchmarking-123.0/";
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        List<CONSISTENCY_TYPE> test_consistency_types = new ArrayList<CONSISTENCY_TYPE>() {{
            add(CONSISTENCY_TYPE.STRONG);
            add(CONSISTENCY_TYPE.RELAXED_STRONG);
            add(CONSISTENCY_TYPE.EVENTUAL);
            add(CONSISTENCY_TYPE.WEAK);
        }};

        Map<CONSISTENCY_TYPE, Integer> waiting_time_map = new HashMap<CONSISTENCY_TYPE, Integer>() {{
            put(CONSISTENCY_TYPE.STRONG, 100000);
            put(CONSISTENCY_TYPE.RELAXED_STRONG, 70000);
            put(CONSISTENCY_TYPE.EVENTUAL, 30000);
            put(CONSISTENCY_TYPE.WEAK, 30000);
        }};

        for (CONSISTENCY_TYPE consistency_type: test_consistency_types) {
            /* Shrunk morning scenario */
            for (int i = 0; i < 10; ++i) {
                ConcurrencyControllerSingleton.getInstance().setConsistencyType(consistency_type);
                System.out.printf("\n\n****** Run %d for %s*******\n", i, consistency_type.name());
                System.out.printf("%d threads are running\n", Thread.getAllStackTraces().keySet().size());
                List<AbstractMap.SimpleEntry<Integer, String>> t_rtns = getOneMorningWorkload();
                for (AbstractMap.SimpleEntry<Integer, String> t_rtn : t_rtns) {
                    int trailing_time = t_rtn.getKey();
                    rtn = getRoutine(t_rtn.getValue());
                    System.out.println("\nExecuting routine " + rtn.routineName + "....");
                    safeHomeManager.sendMsgToRoutineManager(rtn);
                    Thread.sleep(trailing_time);
                }

                Thread.sleep(waiting_time_map.getOrDefault(consistency_type, 20000));
                // Current execution do not provide correct WV lockTable because it only register one
                LockTable lock_table = ConcurrencyControllerSingleton.getInstance().getLockTable();
                MeasurementSingleton.getInstance().RecordMetrics(lock_table);
                ConcurrencyControllerSingleton.getInstance().clearLockTable();
            }

            ////////////////////  Data Collection  //////////////////
            System.out.printf("Average waiting time: %s \n\twith prct %s %%\n",
                MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME),
                MeasurementSingleton.getInstance().GetResult(MeasurementType.WAIT_TIME_VS_E2E));
        }

        MeasurementSingleton.getInstance().getFinalMetricResults(test_consistency_types, folder);

        System.out.println("Exiting SafeHome program....");
        safeHomeManager.Dispose();
    }
}
