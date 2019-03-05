package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 1:07 AM
 */
public class Routine
{
    public enum RoutineType
    {
        REGULAR,
        ROLLBACK
    }

    public enum RoutineExecutionStatus
    {
        NOT_ASSIGNED_YET,
        CONCURRENCY_FAILED,
        SAFETY_CHECK_FAILED,
        EXECUTION_FAILED,
        SUCCESSFUL
    }

    @JsonProperty
    public String routineName;

    @JsonProperty
    public List<Command> commandList;


    @JsonIgnore
    public Integer uniqueRoutineID;

    @JsonIgnore
    public RoutineType routineType;

    @JsonIgnore
    public Routine.RoutineExecutionStatus executionResult;

    @JsonIgnore
    public Boolean isDisposed;

    public Routine() // this default constructor is used for Json object generation. The JsonIgnore properties are initialized here
            // the rest properties are being added from the json string
    {
        this.routineType = RoutineType.REGULAR;
        this.uniqueRoutineID = -1; // the RoutineManager/Handler should assign a unique routine id
        this.executionResult = Routine.RoutineExecutionStatus.NOT_ASSIGNED_YET;
        this.isDisposed = false;
    }

    @JsonIgnore
    public Routine(String _routineName, List<Command> _commandList)
    {
        this.routineName = _routineName;
        this.commandList = new ArrayList<>(_commandList);
        this.uniqueRoutineID = -1; // the RoutineManager should assign a unique routine id
        this.routineType = RoutineType.REGULAR;
        this.executionResult = Routine.RoutineExecutionStatus.NOT_ASSIGNED_YET;
        this.isDisposed = false;
    }


    @JsonIgnore
    public void loadDeviceInfo(Map<String, DeviceInfo> _devNameDevInfoMap)
    {
        for(Command command : this.commandList)
        {
            command.loadDeviceInfo(_devNameDevInfoMap);
        }
    }

    @JsonIgnore
    public Routine getRollBackRoutine()
    {
        List<Command> rollBackCommandList = new ArrayList<>();

        for(Command command : this.commandList)
        {
            rollBackCommandList.add(command.getRollBackCommand());
        }

        Routine rollBackRoutine = new Routine(this.routineName, rollBackCommandList);
        rollBackRoutine.uniqueRoutineID = this.uniqueRoutineID;
        rollBackRoutine.routineType = RoutineType.ROLLBACK;

        return rollBackRoutine;
    }

    @JsonIgnore
    public Boolean isAllCommandInitializeDeviceInfo()
    {
        for(Command command : this.commandList)
        {
            if(command.deviceInfo == null)
                return false;
        }

        return true;
    }

    @JsonIgnore
    public synchronized void Dispose()
    {
        //TODO: implement it
        this.isDisposed = true;

        for(Command command : this.commandList)
        {
            command.Dispose();
        }

        this.commandList.clear();
        this.commandList = null;

    }

    @Override
    public String toString()
    {
        return "Routine{" +
                "routineName='" + routineName + '\'' +
                ", commandList=" + commandList +
                ", uniqueRoutineID=" + uniqueRoutineID +
                ", routineType=" + routineType +
                ", executionResult=" + executionResult +
                '}';
    }
}