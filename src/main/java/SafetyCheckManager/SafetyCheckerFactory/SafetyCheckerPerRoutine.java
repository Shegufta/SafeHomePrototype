package SafetyCheckManager.SafetyCheckerFactory;

import EventBusManager.Events.EventSftyCkrDevMngrMsg;
import Utility.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 11:20 PM
 */
public class SafetyCheckerPerRoutine extends SafetyChecker
{
    public SafetyCheckerPerRoutine(SafetyCheckerType _checkerType)
    {
        super(_checkerType);
        assert(SafetyCheckerType.PER_ROUTINE == _checkerType);
    }

    @Override
    public synchronized void eventHandler_DeviceStatusChange(Map<String, DeviceStatus> _devNameStatusMap)
    {
        System.out.println("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println("@ DEVICE STATUS CHANGED....");
        for(final Map.Entry<String, DeviceStatus> entry : _devNameStatusMap.entrySet())
        {
            String devName = entry.getKey();
            DeviceStatus currentStatus = entry.getValue();

            if(currentStatus == DeviceStatus.REMOVED)
            {
                this.devNameStatusMap.remove(devName);
                System.out.println("\t < deviceName " + devName + " : removed >");
            }
            else
            {
                DeviceStatus previousStatus = this.devNameStatusMap.getOrDefault(devName, DeviceStatus.UNKNOWN);
                this.devNameStatusMap.put(devName, currentStatus);
                System.out.println("\t< deviceName: "+ devName + " | previousStatus = " + previousStatus + " | currentStatus " + currentStatus + " >");
            }
        }

        // Check whether new change violates safety rules
        // (Needs to be done here, because there might be multiple changes in this function here with no order showing)
        Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> safety_rules =
                SystemParametersSingleton.getInstance().getSafetyRules();
        for (final Map.Entry<String, DeviceStatus> new_dev_stat: _devNameStatusMap.entrySet()) {
            if (!checkOneDevState(new DevNameDevStatusTuple(new_dev_stat.getKey(),
                    new_dev_stat.getValue()), safety_rules, this.devNameStatusMap)) {
                //TODO: what do we want to do for now?
                System.out.println("Check Failed!!!!!!!!\n");
            }
        }

        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
    }

    @Override
    public void doAwesomeSafetyChecking(Routine _routineToCheck)
    {
        // TO RUI: this at this stage you need to resolve the concurrency controller
        // Feel free to use your own series of functions
        // this one is just a dummy function!

        Boolean isSafe = true;

        Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> safety_rules =
                SystemParametersSingleton.getInstance().getSafetyRules();
        List<DevNameDevStatusTuple> target_dev_stats = getDevStatesFromRoutine(_routineToCheck);
        Map<String, DeviceStatus> all_dev_stats = new HashMap<>(this.devNameStatusMap);
        // TODO: null pointer handling for all_dev_state.

        for (final DevNameDevStatusTuple target_dev_stat: target_dev_stats) {
            // TODO: for optimization: limit range of safety rules.
            // For each command, scan all the safety rules
            if (!checkOneDevState(target_dev_stat, safety_rules, all_dev_stats)) {
                System.out.println("DYNAMIC SAFE CHECKER -- Not safe for DEVICE " + target_dev_stat.getDevName() +
                        " to STATUS " + target_dev_stat.getDevStatus().toString());
                isSafe = false;
            }

            // The next command check is based on successful execution of past commands.
            all_dev_stats.put(target_dev_stat.getDevName(), target_dev_stat.getDevStatus());
        }


        if(isSafe)
        {
            //If safe, then fill the CurrentStatus field of Each Command
            for(int index = 0 ; index < _routineToCheck.commandList.size() ; ++index)
            {
                assert(_routineToCheck.commandList.get(index).beforeExecutionStatus == DeviceStatus.COMMAND_NOT_EXECUTED_YET);
                
                String devName = _routineToCheck.commandList.get(index).deviceInfo.getDevName();
                assert(this.devNameStatusMap.containsKey(devName)); //Must register the device before execution of a routine. If you remove a device, remove all routines that touch that device
                DeviceStatus beforeExecutionStatus = this.devNameStatusMap.get(devName);



                if(beforeExecutionStatus != DeviceStatus.ON && beforeExecutionStatus != DeviceStatus.OFF)
                {
                    System.out.println("\n\n================================================");
                    System.out.println(" Device " + devName + " beforeExecutionStatus = " + beforeExecutionStatus.name());
                    System.out.println("THIS SHOULD NOT PASS THE SAFETY CHECK!");
                    //assert((beforeExecutionStatus == DeviceStatus.ON) || (beforeExecutionStatus == DeviceStatus.OFF)); //TODO: after test, uncomment the assertion
                    System.out.println("assert is commented out JUST FOR TEST purpose.");
                    System.out.println("For Test Purpose, lets assume that IF(beforeExecutionStatus == Timeout, then it is actually OFF)");
                    beforeExecutionStatus = DeviceStatus.OFF; // TODO: JUST FOR TEST PURPOSE!
                    System.out.println("================================================\n\n");

                }
                _routineToCheck.commandList.get(index).beforeExecutionStatus = beforeExecutionStatus;
            }

            Routine rollBackFormula = prepareRollbackFormula(_routineToCheck);
            this.sendMsgToDeviceManager(_routineToCheck, rollBackFormula); //submit to the next function
        }
        else
        {
            _routineToCheck.executionResult = Routine.RoutineExecutionStatus.SAFETY_CHECK_FAILED;
            this.sendMsgToConcurrencyController(_routineToCheck);
        }
    }

    private boolean checkOneDevState(final DevNameDevStatusTuple target_dev_stat,
                                     final Map<DevNameDevStatusTuple, List<DevNameDevStatusTuple>> safety_rules,
                                     final Map<String, DeviceStatus> all_dev_stats) {
        Boolean isSafe = true;
        for (final DevNameDevStatusTuple condition: safety_rules.keySet()){
            if (!target_dev_stat.equals(condition)) { continue; }
            // Get the expected device states for under that safety rule.
            for (final DevNameDevStatusTuple expected_devstate: safety_rules.get(condition)) {
                // Compare each expected device state with running state (per-routine level)
                if (!all_dev_stats.get(expected_devstate.getDevName()).equals(expected_devstate.getDevStatus())) {
                    isSafe = false;
                }
            }
        }
        return isSafe;
    }

    private List<DevNameDevStatusTuple> getDevStatesFromRoutine(Routine routine) {
        final List<DevNameDevStatusTuple> devstats = new ArrayList<>();
        for (final Command cmd: routine.commandList) {
            devstats.add(new DevNameDevStatusTuple(cmd.devName, cmd.targetStatus));
        }
        return devstats;
    }



    @Override
    public void receiveMsgFromConcurrencyController(Routine _routine)
    {
        //MSG from upper layer

        //TODO: SBA: here add a producer-consumer Queue... Multiple requests might come from the ConcurrencyCtlr
        this.doAwesomeSafetyChecking(_routine);
    }

    @Override
    public synchronized void receiveMsgFromDeviceManager(EventSftyCkrDevMngrMsg _eventSftyCkrDevMngrMsg)
    {
        //MSG from lower layer
        Routine.RoutineExecutionStatus routineExecutionStatus = Routine.RoutineExecutionStatus.SUCCESSFUL;
        if( _eventSftyCkrDevMngrMsg.isFailed())
        {
            routineExecutionStatus = Routine.RoutineExecutionStatus.EXECUTION_FAILED;
        }

        _eventSftyCkrDevMngrMsg.routine.executionResult = routineExecutionStatus;
        this.sendMsgToConcurrencyController(_eventSftyCkrDevMngrMsg.routine);
    }


    @Override
    public Routine prepareRollbackFormula(Routine _checkedRoutine)
    {
        // RUI: for per command, maybe we need to do something else...
        // In per command, to prepare the rollback, we need the previous commands
        // Therefore, _checkedRoutine.getRollBackRoutine(); might not help
        // May be you need to explicitly write code for it
        // For this reason I am keeping this function.

        Routine rollBackFormula = _checkedRoutine.getRollBackRoutine();

        return rollBackFormula;
    }


    @Override
    public synchronized void Dispose()
    {
        this.unregisterEventBus();
        this.isDisposed = true;
    }
}
