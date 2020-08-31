package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:03 AM
 */
public class Command
{
    @JsonProperty
    public String devName;

    @JsonProperty
    public DeviceStatus targetStatus;

    @JsonProperty
    public CommandPriority commandPriority;

    @JsonProperty
    public int durationMilliSec;

    @JsonProperty
    public DeviceStatus endStatus;


    @JsonIgnore
    public DeviceInfo deviceInfo;

    @JsonIgnore
    final public String NOT_INITIALIZED_YET = "not_initialized_yet";

    @JsonIgnore
    final public int NOT_INITIALIZED_INT = -1;


    @JsonIgnore
    public long startTime;

    @JsonIgnore
    public Boolean isDisposed;

    public Command()
    {
        this.devName = NOT_INITIALIZED_YET;
        this.commandPriority = CommandPriority.UNKNOWN;
        this.deviceInfo = null;
        this.isDisposed = false;
        this.startTime = NOT_INITIALIZED_INT;
        this.durationMilliSec = NOT_INITIALIZED_INT;
    }//// need this default constructor for JSON parsing

    @JsonIgnore
    public Command(String _devName, CommandPriority _commandPriority, DeviceStatus _targetStatus, int _duration, DeviceStatus _endStatus)
    {
        this.devName = _devName;
        this.commandPriority = _commandPriority;
        this.targetStatus = _targetStatus;
        this.durationMilliSec = _duration;
        this.endStatus = _endStatus;
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
    public boolean isMust()
    {
        return (this.commandPriority == CommandPriority.MUST);
    }

    @JsonIgnore
    public long getCmdEndTime()
    {
        return this.startTime + this.durationMilliSec;
    }

    @JsonIgnore
    public boolean isCmdOverlapsWithWatchTime(int queryTime)
    {
        boolean insideBound = false;

        if(this.startTime <= queryTime && queryTime < getCmdEndTime())
        { // NOTE: the start time is inclusive, whereas the end time is exclusive. e.g.   [3,7)
            insideBound = true;
        }

        return insideBound;
    }

    @JsonIgnore
    public int compareTimeline(int queryTime)
    {
        if(this.getCmdEndTime() <= queryTime)
            return -1; //  Cmd ends before query

        if(this.startTime <= queryTime && queryTime < getCmdEndTime())
            return 0; // cmd overlaps

        return 1; // cmd starts after query
    }

    @JsonIgnore
    public Command getDeepCopy()
    {
        Command deepCopyCommand = new Command(
                this.devName,
                this.commandPriority,
                this.targetStatus,
                this.durationMilliSec,
                this.endStatus);

        deepCopyCommand.startTime = this.startTime;
        deepCopyCommand.isDisposed = this.isDisposed;
        deepCopyCommand.deviceInfo = this.deviceInfo;
        return deepCopyCommand;
    }



    @Override
    public String toString()
    {
        return "Command{" +
                "devName='" + devName + '\'' +
                ",startTime=" + startTime +
                ",endTime=" + getCmdEndTime() +
                ", deviceInfo=" + deviceInfo +
                ", commandPriority=" + commandPriority +
                ", targetStatus=" + targetStatus +
                ", durationMilliSec=" + durationMilliSec +
                ", endStatus=" + endStatus +
                '}';
    }

    @JsonIgnore
    public synchronized void Dispose()
    {
        this.isDisposed = true;
        //TODO: implement
    }
}
