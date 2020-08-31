package Utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/4/2019
 * @time 2:49 PM
 */
public class SystemParametersSingleton
{
    public final String PROPERTY_FILE_NAME = "conf/SafeHomePrototype.config";

    private final String KEY_DEVICE_LIST_JSON_PATH = "deviceListJSONpath";
    private final String KEY_ROUTINE_LIST_JSON_PATH = "routineListJSONpath";
    private final String KEY_SAFETY_LIST_JSON_PATH = "safetyListJSONpath";
    private final String KEY_SOCKET_TIMEOUT_MS = "socketTimeoutMS";
    private final String KEY_HEARTBEAT_INTERVAL_MS = "heartBeatIntervalMS";
    private final String CONSISTENCY_TYPE_STR = "consistencyType";

    public final boolean IS_PRE_LEASE_ALLOWED = true;
    public final boolean IS_POST_LEASE_ALLOWED = true;

    public Integer socketTimeoutMS = -1;
    public Integer heartBeatIntervalMS = -1;

    public Map<String, DeviceInfo> devNameDevInfoMap;
    private Map<String, Routine> routineNameRoutineDetailsMap;
    //HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap;
    //HashMap<String, HashMap<DeviceStatus, List<DevNameDevStatusTuple>>> actionVsRelatedConditionsMap;

    private static SystemParametersSingleton singleton;

    public static List<String> devIDList;
    public static CONSISTENCY_TYPE consistencyType;

    private String routineListJsonPath = "";

    private String getOSindependentPath(String filePath)
    {
        String osIndependentPath = filePath;

        if(osIndependentPath.contains("/"))
        {
            osIndependentPath.replace('/', File.separatorChar);
        }
        else
        {
            osIndependentPath.replace('\\', File.separatorChar);
        }


        return osIndependentPath;
    }

    private Boolean validateDevNameDevInfoMap(Map<String, DeviceInfo> _devNameDevInfoMap)
    {
        //TODO: check if multiple devices contain same ip:port etc.

        return true;
    }

    private Boolean validateRoutineNameRoutineDetailsMap(Map<String, Routine> routineNameRoutineDetailsMap)
    {
        //TODO: check if a command's deviceInfo is null, if a command contains illegal value "e.g. Timeout" etc.

        return true;
    }

    /*
    private Boolean validateConditionVsRequiredActionsMap(
            HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap) {
        //TODO: validate if all device names are present in the Device List

        return true;
    }
    */

    private SystemParametersSingleton()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Properties properties = new Properties();

        try
        {
            properties.load(new FileInputStream(PROPERTY_FILE_NAME));

            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_SINGLE_PARAMETERS_////////////////////////////////////////
            this.socketTimeoutMS = Integer.valueOf( properties.getProperty(this.KEY_SOCKET_TIMEOUT_MS) );
            this.heartBeatIntervalMS = Integer.valueOf( properties.getProperty(this.KEY_HEARTBEAT_INTERVAL_MS) );

            String consistencyTypeStr =  properties.getProperty(this.CONSISTENCY_TYPE_STR);

            try
            {
                SystemParametersSingleton.consistencyType = Enum.valueOf(CONSISTENCY_TYPE.class, consistencyTypeStr);
            }
            catch (IllegalArgumentException ilex)
            {
                System.out.println("conf/SafeHomePrototype.config: consistencyType = " + consistencyTypeStr +
                        "is not a member of Enum CONSISTENCY_TYPE \n" + ilex.toString());

                System.exit(1);
            }
            //////////////////////////////////////_LOAD_SINGLE_PARAMETERS_////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////


            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_DEVICES_//////////////////////////////////////////////////
            String deviceListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_DEVICE_LIST_JSON_PATH));
            List<DeviceInfo> devListFromJson = objectMapper.readValue(new File(deviceListJsonPath), new TypeReference<List<DeviceInfo>>(){} );

            devNameDevInfoMap = new HashMap<>();

            SystemParametersSingleton.devIDList = new ArrayList();

            for(DeviceInfo devInfo : devListFromJson)
            {
                SystemParametersSingleton.devIDList.add(devInfo.getDevName());

                if(this.devNameDevInfoMap.containsKey(devInfo.getDevName()))
                {
                    System.out.println("the device : " + devInfo.getDevName() + " appears more than once... double check " + deviceListJsonPath);
                    assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                    System.exit(1);
                }

                devInfo.setSocketTimeoutMS(this.socketTimeoutMS); //SBA: WARNING: DO NOT forget this line
                this.devNameDevInfoMap.put(devInfo.getDevName(), devInfo);
            }

            if(!this.validateDevNameDevInfoMap(this.devNameDevInfoMap))
            {
                System.out.println("Failed validating Device Map...");
                assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                System.exit(1);
            }
            //////////////////////////////////////_LOAD_DEVICES_//////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////


            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_SAFETY_RULES_/////////////////////////////////////////////
            /*
            String safetyListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_SAFETY_LIST_JSON_PATH));
            List<ActionConditionTuple> safetyListFromJson = objectMapper.readValue(new File(safetyListJsonPath), new TypeReference<List<ActionConditionTuple>>(){} );

            this.conditionVsRequiredActionsMap = new HashMap<>();
            this.actionVsRelatedConditionsMap = new HashMap<>();

            for(ActionConditionTuple actionConditionTuple : safetyListFromJson)
            {
                DevNameDevStatusTuple condition = actionConditionTuple.getCondition();
                DevNameDevStatusTuple action = actionConditionTuple.getAction();

                System.out.println("Validating rules: if " + condition.toString() + " then " +  action.toString());

                if (!this.conditionVsRequiredActionsMap.isEmpty()) {
                    boolean isValid = validateSingleSafetyRules(
                        condition, action, new HashMap<>(this.conditionVsRequiredActionsMap), new HashMap<>(this.actionVsRelatedConditionsMap));
                }

                // Update condition -> List<action> map
                if(!this.conditionVsRequiredActionsMap.containsKey(condition)) {
                    this.conditionVsRequiredActionsMap.put(condition, new ArrayList<>());
                }
                this.conditionVsRequiredActionsMap.get(condition).add(action);

                // Update action -> List<condition> map
                String act_dev_name = action.getDevName();
                DeviceStatus act_dev_stat = action.getDevStatus();
                if (!this.actionVsRelatedConditionsMap.containsKey(act_dev_name)) {
                    this.actionVsRelatedConditionsMap.put(act_dev_name, new HashMap<>());
                    this.actionVsRelatedConditionsMap.get(act_dev_name).put(act_dev_stat, new ArrayList<>());
                } else if (!this.actionVsRelatedConditionsMap.get(act_dev_name).containsKey(act_dev_stat)) {
                    this.actionVsRelatedConditionsMap.get(act_dev_name).put(act_dev_stat, new ArrayList<>());
                }

                this.actionVsRelatedConditionsMap.get(act_dev_name).get(act_dev_stat).add(condition);
            }


            if(!this.validateConditionVsRequiredActionsMap(this.conditionVsRequiredActionsMap))
            {
                System.out.println("Failed validating Device Map...");
                System.out.println("To Rui: instead of exiting the program, you might remove the conflicts");
                System.out.println("Feel free to do any change");
                assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                System.exit(1);
            }
            */
            //////////////////////////////////////_LOAD_SAFETY_RULES_/////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////


            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_ROUTINES_/////////////////////////////////////////////////
            routineListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_ROUTINE_LIST_JSON_PATH));
            List<Routine> routineListFromJson = objectMapper.readValue(new File(routineListJsonPath), new TypeReference<List<Routine>>(){} );

            this.routineNameRoutineDetailsMap = new HashMap<>();

            for(Routine routine: routineListFromJson)
            {
                if(this.routineNameRoutineDetailsMap.containsKey(routine.routineName))
                {
                    System.out.println("the routine : " + routine.routineName + " appears more than once... double check " + routineListJsonPath);
                    assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                    System.exit(1);
                }

                routine.loadDeviceInfo(this.devNameDevInfoMap);// SBA: WARNING: DO NOT FORGET this step!
                this.routineNameRoutineDetailsMap.put(routine.routineName, routine);
            }

            if(!this.validateRoutineNameRoutineDetailsMap(this.routineNameRoutineDetailsMap))
            {
                System.out.println("Failed validating Routine Map...");
                assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                System.exit(1);
            }
            //////////////////////////////////////_LOAD_ROUTINES_/////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////

            /////////////////////////////////////_VALIDATE_ROUTINES_//////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // This is part of the static checking (Checker in high-level design)
            /*
            for (final String routine_name: this.routineNameRoutineDetailsMap.keySet()) {
                this.routineNameRoutineDetailsMap.put(
                        routine_name,
                        staticSafetyCheckPerRoutine(
                                this.routineNameRoutineDetailsMap.get(routine_name),
                                this.conditionVsRequiredActionsMap));
            }
            */

            /////////////////////////////////////_VALIDATE_ROUTINES_//////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////



            System.out.println("************************************************************");
            System.out.println("* Devices, Routines and Safety Rules Loaded Successfully....");
            System.out.println("* socketTimeoutMS = " + this.socketTimeoutMS);
            System.out.println("* heartBeatIntervalMS = " + this.heartBeatIntervalMS);
            System.out.println("************************************************************");


        }
        catch (IOException ioEx)
        {
            System.out.println("ERROR: File Read Error... " + ioEx);

            System.exit(1);
        }

    }

    private boolean validateSingleSafetyRules(
        DevNameDevStatusTuple condition, DevNameDevStatusTuple action,
        HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap,
        HashMap<String, HashMap<DeviceStatus, List<DevNameDevStatusTuple>>> actionVsRelatedConditionsMap) {

        /* Starting validating conflict rules. Below is supposed to be part of static checking in high-level design.*/
        // Category 1: Each condition could only ``enforce'' at most one condition of each device. TODO: any exception?
        //             If Multiple, only keep the first one.
        if (conditionVsRequiredActionsMap.containsKey(condition)) {
            for (final DevNameDevStatusTuple existing_action : conditionVsRequiredActionsMap.get(condition)) {
                if (action.equals(existing_action)) {
                    System.out.println("STATIC CHECKING -- DUPILICATED RULES");
                    return false;
                }
                if (action.getDevName().equals(existing_action.getDevName()) &&
                    !action.getDevStatus().equals(existing_action.getDevStatus())) {
                    System.out.println("STATIC CHECKING -- Conflict Safety rules with existing rules with same condition");
                    return false;
                }
            }
        }

        // Category 2: The condition sets of two different states of the same dev should always be the same.
        //             If violated, send out notification. (maybe remove the later one?)

        final String act_dev_name = action.getDevName();
        final DeviceStatus act_dev_stat = action.getDevStatus();

        if (!actionVsRelatedConditionsMap.containsKey(act_dev_name)) {
            return true;
        }

        final Map<DeviceStatus, List<String>> dev_set = getConditionDevSetMapForOneActionDev(actionVsRelatedConditionsMap.get(act_dev_name));
        if (dev_set.containsKey(act_dev_stat)) {
            dev_set.get(act_dev_stat).add(condition.getDevName());
        } else {
            dev_set.put(act_dev_stat, Collections.singletonList(condition.getDevName()));
        }

        final List<String> dev_set_max = getLargestDevSetForOneActionDev(dev_set);

//        for (Map.Entry<DeviceStatus, List<String>> entry : dev_set.entrySet()) {
//            System.out.println("-----------------------");
//            System.out.println("Device: " + act_dev_name + " Status: " + entry.getKey().toString() + " con_set: " + dev_set.toString());
//        }

        for (final DeviceStatus dev_stat : dev_set.keySet()) {
            if (!dev_set_max.containsAll(dev_set.get(dev_stat))) {
                System.out.println("STATIC CHECKING -- IF A THEN B  AND IF C THEN NOT B conflict happens for " + act_dev_name + act_dev_stat);
                return false;
            }
        }

        return true;
    }

    private List<String> getLargestDevSetForOneActionDev(Map<DeviceStatus, List<String>> dev_set) {
        List<String> res = new ArrayList<>();
        int max_size = 0;
        for (final List<String> set: dev_set.values()) {
            if (set.size() > max_size) { res = new ArrayList<>(set);}
        }
        return res;
    }

    private Map<DeviceStatus, List<String>> getConditionDevSetMapForOneActionDev(
        Map<DeviceStatus, List<DevNameDevStatusTuple>> deviceStatusListHashMap) {
        final Map<DeviceStatus, List<String>> res = new HashMap<>();
        for (final DeviceStatus stat: deviceStatusListHashMap.keySet()) {
            res.put(stat, new ArrayList<>());
            final List<DevNameDevStatusTuple> conditions = deviceStatusListHashMap.get(stat);
            for (final DevNameDevStatusTuple cond : conditions) {
                res.get(stat).add(cond.getDevName());
            }
        }
        return res;
    }

    private HashMap<DevNameDevStatusTuple,List<DevNameDevStatusTuple>> getValidSafetyRules(
            HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap) {

        // TODO: This func is for optimization of group safety rules. (Unfinished)

        /* Starting validating conflict rules. Below is supposed to be part of static checking in high-level design.*/
        // Category 1: Each condition could only ``enforce'' at most one condition of each device.
        //             If Multiple, only keep the first one.
        for (final List<DevNameDevStatusTuple> action_list : conditionVsRequiredActionsMap.values()) {
            final Set<String> act_devs = new HashSet<>();
            Iterator<DevNameDevStatusTuple> itr = action_list.iterator();
            while (itr.hasNext()) {
                final String dev_name = itr.next().getDevName();
                if (act_devs.contains(dev_name)) { itr.remove(); } else { act_devs.add(dev_name); }
            }
        }
        // Category 2: The condition sets of two different states of the same dev should always be the same.
        //             If violated, send out notification. (maybe remove the later one?)

        // Category 3: TODO: loop detection

        return new HashMap<>(conditionVsRequiredActionsMap);
    }



    /*

    // @param rt: the routine waiting for static checking
     // @param safety_rules: existing safety rules
     // @return A routine that satisfied all the safety rules. (The missing actions will be silently added.)
    private RoutinePrototype staticSafetyCheckPerRoutine(RoutinePrototype rt,
                                                         HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> safety_rules) {

        // TODO: Needs modification for intentionally duplicated or repetitive cmd in long-running routines

        // LinkedHashSet does not work well for Command. Thu,s here use List and existing_targets together to track.
        Boolean isSafe = true;
        List<CommandPrototype> cmd_list = rt.commandList;
        List<CommandPrototype> res_list = new ArrayList<>();
        Set<DevNameDevStatusTuple> existing_targets = new HashSet<>();

        for (final CommandPrototype cmd: cmd_list) {
            // Get the pre-requests (all actions)
            DevNameDevStatusTuple cmd_dev_stat = new DevNameDevStatusTuple(cmd.devName, cmd.targetStatus);
            List<DevNameDevStatusTuple> pre_requets = getPreReqPerDevState(cmd_dev_stat, safety_rules);
            for (final DevNameDevStatusTuple req: pre_requets) {
                if (!existing_targets.contains(req)) {
                    // There is pre-request not guaranteed inside routine.
                    isSafe = false;
                    res_list.add(devStateToCommand(req));
                    existing_targets.add(req);
                }
            }
            if (!existing_targets.contains(cmd_dev_stat)) {
                res_list.add(cmd);
                existing_targets.add(cmd_dev_stat);
            }
        }

        RoutinePrototype res_routine = new RoutinePrototype(rt.routineName, res_list);
        res_routine.uniqueRoutineID = rt.uniqueRoutineID;

        if (!isSafe) {
            System.out.println("**************************************");
            System.out.println("STATIC CHECKING ---- Routine Modified with new Routine: \n" +res_routine);
        }

        return res_routine;
    }
    */


    /*
    private List<CommandPrototype> devStatesToCommands(List<DevNameDevStatusTuple> pre_requets) {
        List<CommandPrototype> res_cmds = new ArrayList<>();
        for (final DevNameDevStatusTuple req : pre_requets) {
            res_cmds.add(devStateToCommand(req));
        }
        return res_cmds;
    }

    private CommandPrototype devStateToCommand(DevNameDevStatusTuple req) {
        // TODO: add more mechanism for command priority.
        return new CommandPrototype(req.getDevName(),
            getDeviceInfo(req.getDevName()),
            req.getDevStatus(),
            CommandPriority.MUST);
    }
    */

    private List<DevNameDevStatusTuple> getPreReqPerDevState(
            DevNameDevStatusTuple target_dev_stat,
            HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> safety_rules) {
        List<DevNameDevStatusTuple> res = new ArrayList<>();
        for (final DevNameDevStatusTuple condition: safety_rules.keySet()) {
            if (condition.equals(target_dev_stat)) {
                return safety_rules.get(target_dev_stat);
            }
        }
        return res;
    }

    /**
     * @param routineName : name of the routine
     * @return if the routine is found present, return a deep copy of the routine. Otherwise return null.
     */
    public Routine getRoutine(String routineName) {
        Routine routine = this.routineNameRoutineDetailsMap.getOrDefault(routineName, null);

        if (routine == null)
        {
            throw new IllegalArgumentException("Routine " + routineName + " not found in " + routineListJsonPath);
        }

        Routine deepCopy = routine.getDeepCopy(); // Return a copy of the routine

        return deepCopy; // Return the deep copy. Note that, the Routine manager need to create another deep copy
    }

    /**
     *
     * @return List of all routines
     */
    public List<Routine> getRoutineList()
    {
        List<Routine> routineList = new ArrayList<>(this.routineNameRoutineDetailsMap.values());
        return routineList;
    }

    public List<String> getRoutineNameList()
    {
        List<String> routineNameList = new ArrayList<>(this.routineNameRoutineDetailsMap.keySet());
        return routineNameList;
    }

    public Boolean isRoutinePresent(String routineName)
    {
        return this.getRoutineNameList().contains(routineName);
    }

    /**
     *
     * @return List of All deviceInfo
     */
    public List<DeviceInfo> getDeviceInfoList()
    {
        List<DeviceInfo> deviceInfoList = new ArrayList<>(this.devNameDevInfoMap.values());
        return deviceInfoList;
    }

    public DeviceInfo getDeviceInfo(String deviceName)
    {
        return this.devNameDevInfoMap.getOrDefault(deviceName, null);
    }

    /**
     *
     * @return Map of safety rules
     */
    /*
    public Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> getSafetyRules()
    {
        return this.conditionVsRequiredActionsMap;
    }
    */

    /**
     *
     * @return Return all the influenced dev states of a specific device state
     */
    /*
    public Map<String, DeviceStatus> getConditionsOfOneDevStat(DevNameDevStatusTuple dev_stat) {
        Map<String, DeviceStatus> condition_list = new HashMap<>();
        for (Map.Entry<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> single_rule:
                this.conditionVsRequiredActionsMap.entrySet()) {
            if (single_rule.getValue().contains(dev_stat)) {
                condition_list.put(single_rule.getKey().getDevName(), single_rule.getKey().getDevStatus());
            }
        }
        return condition_list;
    }
    */


    /**
     *
     * @return Map of safety rules related to a set of devices
     */
    /*
    public Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> getSafetyRules(List<DevNameDevStatusTuple> devset)
    {
        // TODO (rui): This func is for optimization.
        return this.conditionVsRequiredActionsMap;
    }
    */


    public static synchronized SystemParametersSingleton getInstance()
    {
        if(null == SystemParametersSingleton.singleton)
        {
            SystemParametersSingleton.singleton = new SystemParametersSingleton();
        }

        return SystemParametersSingleton.singleton;
    }


    public static void main(String[] args)
    {
        SystemParametersSingleton.getInstance();
    }
}
