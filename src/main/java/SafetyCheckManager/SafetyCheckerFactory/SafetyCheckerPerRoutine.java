package SafetyCheckManager.SafetyCheckerFactory;

import EventBusManager.Events.EventSftyCkrDevMngrMsg;
import Utility.DeviceStatus;
import Utility.Routine;
import Utility.SafetyCheckerType;

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
        //MSG from lower layer
        //TO RUI: Dummy code, this code handles the device event changes ON/OFF/TIMEOUT/REMOVED
        //Implement your logic

        System.out.println("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println("@ DEVICE STATUS CHANGED....");
        for(Map.Entry<String, DeviceStatus> entry : _devNameStatusMap.entrySet())
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
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
    }

    @Override
    public void doAwesomeSafetyChecking(Routine _routineToCheck)
    {
        // TO RUI: this at this stage you need to resolve the concurrency controller
        // Feel free to use your own series of functions
        // this one is just a dummy function!

        Boolean isSafe = true;

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
