package Utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
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

    public final String KEY_DEVICE_LIST_JSON_PATH = "deviceListJSONpath";
    public final String KEY_ROUTINE_LIST_JSON_PATH = "routineListJSONpath";
    public final String KEY_SAFETY_LIST_JSON_PATH = "safetyListJSONpath";
    public final String KEY_SOCKET_TIMEOUT_MS = "socketTimeoutMS";
    public final String KEY_HEARTBEAT_INTERVAL_MS = "heartBeatIntervalMS";

    public Integer socketTimeoutMS = -1;
    public Integer heartBeatIntervalMS = -1;

    public Map<String, DeviceInfo> devNameDevInfoMap;
    public Map<String, Routine> routineNameRoutineDetailsMap;
    HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap;

    private static SystemParametersSingleton singleton;

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

    private Boolean validateConditionVsRequiredActionsMap(HashMap<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> conditionVsRequiredActionsMap)
    {
        //TODO (rui): validate if all device names are present in the Device List
        //TODO: validate if there are conflicting action rules etc.

        return true;
    }

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
            //////////////////////////////////////_LOAD_SINGLE_PARAMETERS_////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////


            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_DEVICES_//////////////////////////////////////////////////
            String deviceListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_DEVICE_LIST_JSON_PATH));
            List<DeviceInfo> devListFromJson = objectMapper.readValue(new File(deviceListJsonPath), new TypeReference<List<DeviceInfo>>(){} );

            devNameDevInfoMap = new HashMap<>();

            for(DeviceInfo devInfo : devListFromJson)
            {
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
            String safetyListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_SAFETY_LIST_JSON_PATH));
            List<ActionConditionTuple> safetyListFromJson = objectMapper.readValue(new File(safetyListJsonPath), new TypeReference<List<ActionConditionTuple>>(){} );

            this.conditionVsRequiredActionsMap = new HashMap<>();

            for(ActionConditionTuple actionConditionTuple : safetyListFromJson)
            {
                DevNameDevStatusTuple condition = actionConditionTuple.getCondition();
                if(!this.conditionVsRequiredActionsMap.containsKey(condition))
                {
                    this.conditionVsRequiredActionsMap.put(condition, new ArrayList<>());
                }
                this.conditionVsRequiredActionsMap.get(condition).add(actionConditionTuple.getAction());
            }

//            for(Map.Entry<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> tuple : conditionVsRequiredActionsMap.entrySet())
//            {
//                System.out.println("CONDITION = " + tuple.getKey() + " || list =  " + tuple.getValue());
//            }

            if(!this.validateConditionVsRequiredActionsMap(this.conditionVsRequiredActionsMap))
            {
                System.out.println("Failed validating Device Map...");
                System.out.println("To Rui: instead of exiting the program, you might remove the conflicts");
                System.out.println("Feel free to do any change");
                assert(false); // assert does not work with some IDE. Hence I am using System.exit(1)
                System.exit(1);
            }
            //////////////////////////////////////_LOAD_SAFETY_RULES_/////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////////////


            //////////////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////_LOAD_ROUTINES_/////////////////////////////////////////////////
            String routineListJsonPath = this.getOSindependentPath(properties.getProperty(this.KEY_ROUTINE_LIST_JSON_PATH));
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

    /**
     * @param routineName : name of the routine
     * @return if routine present, return the routine. Otherwise return null.
     */
    public Routine getRoutine(String routineName)
    {
        Routine routine = this.routineNameRoutineDetailsMap.getOrDefault(routineName, null);

        if(routine != null)
            routine.refreshRoutine();

        return routine;
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
    public Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> getSafetyRules()
    {
        return this.conditionVsRequiredActionsMap;
    }

    /**
     *
     * @return Return all the influenced dev states of a specific device state
     */
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


    /**
     *
     * @return Map of safety rules related to a set of devices
     */
    public Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> getSafetyRules(List<DevNameDevStatusTuple> devset)
    {
        // TODO (rui): This func is for optimization.
        return this.conditionVsRequiredActionsMap;
    }


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
