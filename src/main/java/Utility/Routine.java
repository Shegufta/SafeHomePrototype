package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:02 AM
 */
public class Routine implements Comparator<Routine>
{
    @JsonProperty
    public String routineName;

    @JsonProperty
    public List<Command> commandList;


    @JsonIgnore
    public int uniqueRoutineID;

    @JsonIgnore
    public Boolean isDisposed;

    @JsonIgnore
    public long registrationTime = 0;


    @JsonIgnore
    public boolean isCmmandListINITIALIZED;

    @JsonIgnore
    public Map<String, Boolean> devIdIsMustMap;

    @JsonIgnore
    public Map<String, Command> devIDCommandMap;

    @JsonIgnore
    public Map<Integer, Command> indexCommandMap;

    @JsonIgnore
    public Map<Command, Integer> commandIndexMap;

    @JsonIgnore
    final int ID_NOT_ASSIGNED_YET = -1;

    @JsonIgnore
    public float backToBackCmdExecutionWithoutGap;

    @JsonIgnore
    public Map<String, List<Integer>> tempPerDevPreSet; // used to calculate the recursive insertion method (EV)

    @JsonIgnore
    public Map<String, List<Integer>> tempPerDevPostSet; // used to calculate the recursive insertion method (EV)

    public Routine()
    {
        this.initialize();
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
    private void initialize()
    {
        this.uniqueRoutineID = ID_NOT_ASSIGNED_YET;
        this.commandList = new ArrayList<>();
        this.devIdIsMustMap = new HashMap<>();
        //this.deviceSet = new HashSet<>();
        this.devIDCommandMap = new HashMap();
        this.indexCommandMap = new HashMap();
        this.commandIndexMap = new HashMap();

        this.tempPerDevPreSet = new HashMap<>();
        this.tempPerDevPostSet = new HashMap<>();

        this.backToBackCmdExecutionWithoutGap = 0.0f;
        this.isDisposed = false;
        this.isCmmandListINITIALIZED = false; // while reading from JSON, the Routine class will not be properly initialized
        // To initialized it, you need to execute the addCommand method for each commands
    }


    @JsonIgnore
    public Set<String> getAllDevIDSet()
    {
        return this.devIDCommandMap.keySet(); //this.deviceSet;
    }

    @JsonIgnore
    public float getBackToBackCmdExecutionTimeWithoutGap()
    {
        return this.backToBackCmdExecutionWithoutGap;
    }

    @JsonIgnore
    public void clearTempPrePostSet()
    {// used to calculate the recursive insertion method (EV)
        for(String devID : this.devIDCommandMap.keySet())
        {
            tempPerDevPreSet.get(devID).clear();
            tempPerDevPostSet.get(devID).clear();
        }
    }

    @JsonIgnore
    public void initializeCommandList()
    {
        assert(isCmmandListINITIALIZED != true);

        List<Command> commandListCopy = new ArrayList(this.commandList);
        this.commandList.clear();

        for(Command cmd : commandListCopy)
        {
            this.addCommand(cmd);
        }

        this.isCmmandListINITIALIZED = false;
    }

    @JsonIgnore
    public void addCommand(Command cmd)
    {
        assert(!devIDCommandMap.containsKey(cmd.devName));

        tempPerDevPreSet.put(cmd.devName, new ArrayList<>());// used to calculate the recursive insertion method (EV)
        tempPerDevPostSet.put(cmd.devName, new ArrayList<>());// used to calculate the recursive insertion method (EV)

        devIdIsMustMap.put(cmd.devName, cmd.isMust());

        backToBackCmdExecutionWithoutGap += cmd.durationMilliSec;  // i.e., summing up the core execution times of all commands

        int cmdInsertionIndex = commandList.size();
        this.indexCommandMap.put(cmdInsertionIndex, cmd);
        this.commandIndexMap.put(cmd, cmdInsertionIndex);
        this.devIDCommandMap.put(cmd.devName, cmd);

        this.commandList.add(cmd);
    }


    @JsonIgnore
    public float getStretchRatio()
    {
        float gap = 0.0f;

        for(int idx = 0 ; idx < this.commandList.size()-1 ; ++idx)
        {
            Command firstCommand = this.commandList.get(idx);
            Command nextCommand = this.commandList.get(idx + 1);

            gap += nextCommand.startTime  - firstCommand.getCmdEndTime();
        }

        if( this.backToBackCmdExecutionWithoutGap == 0.0f)
            return 0.0f;

        return (gap + backToBackCmdExecutionWithoutGap)/backToBackCmdExecutionWithoutGap;
    }



    @JsonIgnore
    public Command getCommandByDevID(String devID)
    {
        assert(this.devIDCommandMap.containsKey(devID));

        return this.devIDCommandMap.get(devID);
    }

    @JsonIgnore
    int getCommandIndex(String devID)
    {
        assert(this.devIDCommandMap.containsKey(devID));

        Command cmd = this.devIDCommandMap.get(devID);

        return this.commandIndexMap.get(cmd);
    }

    @JsonIgnore
    String getDevID(int cmdIdx)
    {
        return this.commandList.get(cmdIdx).devName;
    }

    @JsonIgnore
    public long lockStartTime(String _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        return this.devIDCommandMap.get(_devID).startTime;
    }

    @JsonIgnore
    public long lockEndTime(String _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        Command cmd = this.devIDCommandMap.get(_devID);
        return cmd.startTime + cmd.durationMilliSec;
    }

    @JsonIgnore
    public long lockDuration(String _devID)
    {
        assert(this.devIDCommandMap.containsKey(_devID));

        return this.devIDCommandMap.get(_devID).durationMilliSec;
    }

    @JsonIgnore
    public long routineStartTime()
    {
        return this.commandList.get(0).startTime;
    }

    @JsonIgnore
    public float getEndToEndLatency()
    {
        return this.routineEndTime() - this.registrationTime;
    }

    @JsonIgnore
    private float getRoutineExecutionTime()
    {
        return this.routineEndTime() - this.routineStartTime();
    }


    @JsonIgnore
    public float getE2EvsWaittime()
    {
        float endToEndLatency = getEndToEndLatency();
        float waitTime = getStartDelay();

        assert(0.0 < endToEndLatency);

        if(waitTime == 0.0f)
            return 0.0f;
        else
            return (endToEndLatency/waitTime)*100.0f;
    }

    @JsonIgnore
    public float getWaittimevsE2E()
    {
        float endToEndLatency = getEndToEndLatency();
        float waitTime = getStartDelay();

        assert(0.0 < endToEndLatency);

        return (waitTime * 100.0f / endToEndLatency);
    }

    @JsonIgnore
    public float getLatencyOverheadPrcnt()
    {
        float endToEndLatency = getEndToEndLatency();

        assert(backToBackCmdExecutionWithoutGap != 0.0f);

        return (endToEndLatency / backToBackCmdExecutionWithoutGap) * 100.0f;


        /*
        float waitTime = getStartDelay();

        assert(0.0 < endToEndLatency);

        if(waitTime == 0.0f)
            return 0.0f;
        else
            return (endToEndLatency/waitTime)*100.0f;

        */
        //return (overhead/endToEndLatency)*100.0f;

        /*
        * TODO: wait/endtoend
        *  2) latencyOverhead endtoend/minextime
        * 3)TODO: fix dev utilization, + add new dev utilization
        * */
    }

    @JsonIgnore
    public float getStartDelay()
    {
        return this.routineStartTime() - this.registrationTime;
    }

    @JsonIgnore
    public long routineEndTime()
    {
        int lastIdx = commandList.size() - 1;
        assert(0 <= lastIdx);

        Command lastCmd = commandList.get(lastIdx);

        return lastCmd.getCmdEndTime();//lastCmd.startTime + lastCmd.duration;
    }

    @JsonIgnore
    public boolean isCommittedByGivenTime(long targetTime)
    {
        //NOTE: end time is exclusive, and start time is inclusive
        return ( this.routineEndTime() == targetTime || this.routineEndTime() < targetTime);
    }

    @JsonIgnore
    public boolean isCandidateCmdInsidePreLeaseZone(String _devID, long _candidateCmdStartTime, int _candidateCmdDuration)
    {
        long cmdStartTime = getCommandByDevID(_devID).startTime;
        long routineStartTime = this.routineStartTime();
        long candidateCmdEndTime = _candidateCmdStartTime + _candidateCmdDuration;

        if(routineStartTime <= _candidateCmdStartTime && _candidateCmdStartTime <= cmdStartTime)
            return true;

        if(routineStartTime <= candidateCmdEndTime && candidateCmdEndTime <= cmdStartTime)
            return true;

        return false;
    }

    @JsonIgnore
    public boolean isCandidateCmdInsidePostLeaseZone(String _devID, long _candidateCmdStartTime, long _candidateCmdDuration)
    {
        long cmdEndTime = getCommandByDevID(_devID).getCmdEndTime();
        long routineEndTime = this.routineEndTime();
        long candidateCmdEndTime = _candidateCmdStartTime + _candidateCmdDuration;

        if(cmdEndTime <= _candidateCmdStartTime && _candidateCmdStartTime <= routineEndTime)
            return true;

        if(cmdEndTime <= candidateCmdEndTime && candidateCmdEndTime <= routineEndTime)
            return true;

        return false;
    }

    @JsonIgnore
    public boolean isDevAccessStartsDuringTimeSpan(String devId, int startTimeInclusive, int endTimeExclusive)
    {
        assert(startTimeInclusive < endTimeExclusive);

        if(!this.devIDCommandMap.containsKey(devId))
            return false;

        long cmdStartTimeInclusive = getCommandByDevID(devId).startTime;

        if(startTimeInclusive <= cmdStartTimeInclusive && cmdStartTimeInclusive < endTimeExclusive)
            return true;


        return false;
    }


    @JsonIgnore
    public boolean isRoutineOverlapsWithGivenTimeSpan(int startTimeInclusive, int endTimeExclusive)
    {
        assert(startTimeInclusive < endTimeExclusive);

        long rtnStartTimeInclusive = this.routineStartTime();
        long rtnEndTimeExclusive = this.routineEndTime();

        if(startTimeInclusive <= rtnStartTimeInclusive && rtnStartTimeInclusive < endTimeExclusive)
            return true;

        if(rtnStartTimeInclusive <= startTimeInclusive && startTimeInclusive < rtnEndTimeExclusive)
            return true;


        return false;
    }


    @JsonIgnore
    public Routine getDeepCopy()
    {
        Routine deepCopyRoutine = new Routine();
        deepCopyRoutine.uniqueRoutineID = this.uniqueRoutineID;
        deepCopyRoutine.registrationTime = this.registrationTime;
        deepCopyRoutine.routineName = this.routineName;

        deepCopyRoutine.commandList = new ArrayList(this.commandList);
        /*
        for(Command cmd : this.commandList)
        {// SBA: do not call addCommand from here. Let the RoutineManager call the  initializeCommandList() function
        Here simply copy the commandList
            deepCopyRoutine.addCommand(cmd.getDeepCopy());
        }
        */

        return deepCopyRoutine;
    }

    @Override
    public String toString()
    {
        String str = "";

        str += "{ Routine ID:" + this.uniqueRoutineID;
        str += "; routineName:" + this.routineName;
        str += "; delay:" + this.getStartDelay();
        str += "; registrationTime: " + this.registrationTime;
        str += "; backTobackExc: " + this.backToBackCmdExecutionWithoutGap;
        str += "; expectedEndWithoutAnyGap: " + (int)(this.registrationTime + this.backToBackCmdExecutionWithoutGap);
        str += "; stretchRatio: " + this.getStretchRatio() + " || ";

        for(Command cmd : this.commandList)
        {
            str += cmd + " || ";
        }

        str += " }";

        return str;
    }

    @Override
    public int compare(Routine a, Routine b)
    {
        if(a.registrationTime == b.registrationTime)
        {
            assert(a.uniqueRoutineID != b.uniqueRoutineID);

            return (a.uniqueRoutineID < b.uniqueRoutineID) ? -1 : ( (a.uniqueRoutineID == b.uniqueRoutineID)? 0 : 1 );
        }
        else
        {
            return (a.registrationTime < b.registrationTime) ? -1 : ( (a.registrationTime == b.registrationTime)? 0 : 1 );
        }
    }

    @JsonIgnore
    public synchronized void Dispose()
    {
        //TODO: implement it
        this.isDisposed = true;

        if(commandList != null)
        {
            for(Command command : this.commandList)
            {
                if(command != null)
                {
                    command.Dispose();
                }
            }

            this.commandList.clear();
            this.commandList = null;
        }
    }

    @JsonIgnore
    public List<RoutineExecutionRecipe> getRoutineExecuteRecipe()
    {
        List<RoutineExecutionRecipe> recipeList = new ArrayList<>();

        long prevCmdEndTime = this.registrationTime;

        for(int I = 0 ; I < this.commandList.size() ; I++)
        {
            Command cmd = this.commandList.get(I);

            assert(cmd.startTime != cmd.NOT_INITIALIZED_INT);

            long sleepDurationMilliSec = cmd.startTime - prevCmdEndTime;
            assert(0 <= sleepDurationMilliSec);

            RoutineExecutionRecipe recipeStart = new RoutineExecutionRecipe(
                    sleepDurationMilliSec,
                    cmd.deviceInfo,
                    cmd.targetStatus,
                    this.uniqueRoutineID,
                    I,
                    RoutineExecutionRecipe.beginning);

            if(cmd.durationMilliSec <= 0)
                recipeStart.subCmdType = RoutineExecutionRecipe.single;

            recipeList.add(recipeStart);

            if(0 < cmd.durationMilliSec)
            {
                sleepDurationMilliSec = cmd.durationMilliSec;
                RoutineExecutionRecipe recipeEnd = new RoutineExecutionRecipe(
                        sleepDurationMilliSec,
                        cmd.deviceInfo,
                        cmd.endStatus,
                        this.uniqueRoutineID,
                        I,
                        RoutineExecutionRecipe.end);

                recipeList.add(recipeEnd);
            }

            prevCmdEndTime = cmd.getCmdEndTime();
        }

        return recipeList;
    }
}