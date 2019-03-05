package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 12:54 AM
 */
public class Command
{
    //public List<DeviceInfo> deviceInfoList;   //SBA: for now lets assume a command contains only a single device.
                                                //i.e. for the first version, lets forget about the commands such as "Turn On All Lights"
                                                //In an alternate design, such commands might be converted into a routine by the RoutineHandler.
                                                //I think, that will create a simple design.
    @JsonProperty
    public String devName;

    @JsonProperty
    public DeviceStatus targetStatus;

    @JsonProperty
    public CommandPriority commandPriority;



    @JsonIgnore
    public DeviceInfo deviceInfo;

    @JsonIgnore
    public DeviceStatus beforeExecutionStatus;

    @JsonIgnore
    public DeviceStatus afterExecutionStatus;

    @JsonIgnore
    public Boolean isDisposed;

    public Command() // pass the rest 3 attribute through JSON
    {
        this.deviceInfo = null;
        this.beforeExecutionStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        this.afterExecutionStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        this.isDisposed = false;
    }// need this default constructor for JSON parsing

    @JsonIgnore
    public Command(String _devName, DeviceInfo _deviceInfo, DeviceStatus _targetStatus, CommandPriority _commandPriority)
    {
        this.devName = _devName;
        this.deviceInfo = _deviceInfo;
        this.targetStatus = _targetStatus;
        this.commandPriority = _commandPriority;

        this.beforeExecutionStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        this.afterExecutionStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        this.isDisposed = false;
    }


    @Override
    public String toString()
    {
        return "Command{" +
                "devName='" + devName + '\'' +
                ", deviceInfo=" + deviceInfo +
                ", commandPriority=" + commandPriority +
                ", beforeExecutionStatus=" + beforeExecutionStatus +
                ", targetStatus=" + targetStatus +
                ", afterExecutionStatus=" + afterExecutionStatus +
                '}';
    }

    /**
     *
     * @return rollBack command
     */
    @JsonIgnore
    public Command getRollBackCommand()
    {
        assert(this.beforeExecutionStatus != DeviceStatus.COMMAND_NOT_EXECUTED_YET); //Cannot Rollback if you dont know the previous state!
        assert(this.deviceInfo != null);
        assert(0 == this.deviceInfo.getDevName().compareTo(this.devName));

        Command rollBackCommand = new Command(this.devName, this.deviceInfo, this.beforeExecutionStatus, CommandPriority.BEST_EFFORT); // Rollback should be best effort
        rollBackCommand.beforeExecutionStatus = rollBackCommand.targetStatus;

        return rollBackCommand;
    }

    @JsonIgnore
    public void loadDeviceInfo(Map<String, DeviceInfo> _devNameDevInfoMap)
    {
        if(!_devNameDevInfoMap.containsKey(this.devName))
        {
            System.out.println("Fatal Error: the device : " + this.devName + " is not present in " + _devNameDevInfoMap);
            assert(false);
            System.exit(1);
        }

        this.deviceInfo = _devNameDevInfoMap.get(this.devName);
    }

    @JsonIgnore
    public synchronized void Dispose()
    {
        this.isDisposed = true;
        //TODO: implement
    }
}
