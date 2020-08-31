package Utility;

import java.util.*;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 18-Jul-19
 * @time 12:02 AM
 */
public class LockTable
{
    public Map<String, List<Routine>> lockTable;
    public Map<String, Routine> devID_lastAccesedRtn_Map = null;
    public Map<String, List<Routine>> perDevRoutineListForWeakScheduling = null;
    List<Routine> weakSchedulingSpecialLockTable = null;

    public CONSISTENCY_TYPE consistencyType;
    public long safeHomeStartTime;

    public LockTable(List<String> devIDlist, CONSISTENCY_TYPE _consistencyType, long _safeHomeStartTime)
    {
        this.lockTable = new HashMap<>();

        safeHomeStartTime = _safeHomeStartTime;
        //this.CURRENT_TIME = 0;
        //this.ROUTINE_ID = 0;
        this.consistencyType = _consistencyType;
        this.devID_lastAccesedRtn_Map = new HashMap<>();

        for(String devID : devIDlist)
        {
            this.lockTable.put(devID, new ArrayList<>());
        }
    }

    public List<Routine> getAllRoutineSet()
    {
        if(this.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            assert(weakSchedulingSpecialLockTable != null);
            return weakSchedulingSpecialLockTable;
        }
        else
        {
            List<Routine> allRoutineList = new ArrayList<>();

            for(List<Routine> lineage : lockTable.values())
            {
                for(Routine routine : lineage)
                {
                    if(!allRoutineList.contains(routine))
                        allRoutineList.add(routine);
                }
            }

            return allRoutineList;
        }
    }


    @Override
    public String toString()
    {
        String str = "------------------------------------------------------\n";
        str += "CONSISTENCY: " + this.consistencyType + "\n";
        //for(Map.Entry<DEV_ID, List<Routine>> entry : this.lockTable.entrySet())
        for(String devID : lockTable.keySet())
        {// print the rows alphabetically
            if(!this.lockTable.containsKey(devID))
                continue;
            //DEV_ID devID = entry.getKey();
            str += "\n " + devID + " : ";

            for(Routine rtn : this.lockTable.get(devID))
            {
                str += "[<R" + rtn.uniqueRoutineID + "|C"+ rtn.getCommandIndex(devID) +">:" + rtn.lockStartTime(devID) + ":";
                str += rtn.lockStartTime(devID) + rtn.lockDuration(devID) + ") ";
            }
        }
        str += "\n------------------------------------------------------\n";

        return str;
    }


    public class LazySchedulingHelper
    {
        public String devID;
        public boolean isLocked;
        public long lockReleaseTime;

        public LazySchedulingHelper(String _devID)
        {
            this.devID = _devID;
            this.isLocked = false;
        }
    }

    private long lazy_ReleaseEarliestCommandLock(Map<String, LazySchedulingHelper> lockTableHelper, long simulationStartTime, List<Routine> _waitingList)
    {
        long earliestLockReleaseTime = Integer.MAX_VALUE;
        String earliestReleaseDevID = null;

        for(LazySchedulingHelper RShlpr : lockTableHelper.values())
        {
            if(RShlpr.isLocked)
            {
                if(RShlpr.lockReleaseTime < earliestLockReleaseTime)
                {
                    earliestLockReleaseTime = RShlpr.lockReleaseTime;
                    earliestReleaseDevID = RShlpr.devID;
                }
            }
        }

        if(earliestReleaseDevID != null)
        { // search for another waiting routine that was registered before earliestLockReleaseTime
            // and can acquire all locks before current command ends
            for(Routine waitingRtn : _waitingList)
            {
                if(waitingRtn.registrationTime < earliestLockReleaseTime)
                {
                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRtn))
                    {
                        //canAnotherRoutineAcquireAllLocksBeforeThisCommandEnds = true;
                        long anotherRoutineCanStartAtTime = waitingRtn.registrationTime;
                        return anotherRoutineCanStartAtTime;
                    }
                }
                else
                    break;
            }


            // No other routine will start before this command, hence release this lock
            lockTableHelper.get(earliestReleaseDevID).isLocked = false; // release lock
        }

        return (earliestReleaseDevID == null) ? simulationStartTime : earliestLockReleaseTime;
    }

    private boolean isAllLockReleased(Map<String, LazySchedulingHelper> lockTableHelper)
    {
        for(LazySchedulingHelper RShlpr : lockTableHelper.values())
        {
            if(RShlpr.isLocked)
            {
                return false;
            }
        }

        return true;
    }


    private boolean lazy_canAcquireAllLocks(Map<String, LazySchedulingHelper> lockTableHelper, Routine rtn)
    {
        for(String devId : rtn.getAllDevIDSet())
        {
            if(lockTableHelper.get(devId).isLocked)
                return false;
        }

        return true;
    }

    private void lazy_acquireAllLock(Map<String, LazySchedulingHelper> lockTableHelper, Routine rtn)
    {
        for(Command cmd : rtn.commandList)
        {
            lockTableHelper.get(cmd.devName).isLocked = true;
            lockTableHelper.get(cmd.devName).lockReleaseTime = cmd.getCmdEndTime();
        }
    }

    //private void lazyScheduling(final List<Routine> rtnList, int _simulationStartTime)
    private void lazyScheduling(final List<Routine> rtnList)
    {
        Map<String, LazySchedulingHelper> lockTableHelper = new HashMap<>();

        for(String devID : this.lockTable.keySet())
        {
            lockTableHelper.put( devID, new LazySchedulingHelper(devID) );
        }

        List<Routine> waitingList = new ArrayList<>(rtnList); // NOTE: do not change the rtnList
        Collections.sort(waitingList, new Routine());// sort based on arrival time. ties are broken by routine id. smaller id first

        while(!waitingList.isEmpty())
        {
            long lockReleaseTime = lazy_ReleaseEarliestCommandLock(lockTableHelper, this.safeHomeStartTime, waitingList);
            boolean isAllLockReleased = this.isAllLockReleased(lockTableHelper);
            List<Routine> scheduledRtnList = new ArrayList<>();


            if(consistencyType == CONSISTENCY_TYPE.LAZY)
            {
                for(Routine waitingRoutine : waitingList)
                {
                    if( !isAllLockReleased &&(lockReleaseTime < waitingRoutine.registrationTime))
                        break;

                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRoutine))
                    {
                        long maxTime = Math.max(lockReleaseTime, waitingRoutine.registrationTime);

                        registerRoutineFromExactTime(waitingRoutine, maxTime); // register in the lock table
                        lazy_acquireAllLock(lockTableHelper ,waitingRoutine);

                        scheduledRtnList.add(waitingRoutine);
                    }
                }
            }
            else if(consistencyType == CONSISTENCY_TYPE.LAZY_FCFS)
            { // stop checking after first routine from the waiting list
                Routine firstWaitingRoutine = waitingList.get(0); // get the first routine

                if(!isAllLockReleased)
                {
                    for(Routine earlyComerNonConflictingRtn : waitingList)
                    {
                        if(lockReleaseTime < earlyComerNonConflictingRtn.registrationTime)
                            break;

                        if(lazy_canAcquireAllLocks( lockTableHelper , earlyComerNonConflictingRtn))
                        {
                            firstWaitingRoutine = earlyComerNonConflictingRtn;
                        }

                    }
                }

                if(lazy_canAcquireAllLocks( lockTableHelper , firstWaitingRoutine))
                {
                    long maxTime = Math.max(lockReleaseTime, firstWaitingRoutine.registrationTime);

                    registerRoutineFromExactTime(firstWaitingRoutine, maxTime); // register in the lock table
                    lazy_acquireAllLock(lockTableHelper ,firstWaitingRoutine);

                    scheduledRtnList.add(firstWaitingRoutine);
                }

            }
            else if(consistencyType == CONSISTENCY_TYPE.LAZY_PRIORITY)
            {
                List<Routine> candidateList = new ArrayList<>();

                for(Routine waitingRoutine : waitingList)
                {
                    if( !isAllLockReleased &&(lockReleaseTime < waitingRoutine.registrationTime))
                        break;

                    if(lazy_canAcquireAllLocks( lockTableHelper , waitingRoutine))
                    {
                        candidateList.add(waitingRoutine);
                    }
                }

                if(!candidateList.isEmpty())
                {
                    List<Routine> existingCandidateList = new ArrayList<>();
                    List<Long> waitTimeList = new ArrayList<>(); // for routines, that arrive before or at "lockReleaseTime"

                    List<Routine> futureCandidateList = new ArrayList<>();
                    List<Long> rightToCurreneScanLineArrivalIntervalList = new ArrayList<>(); // for routines, that arrive after "lockReleaseTime"

                    for (Routine candidateRtn : candidateList)
                    {
                        long waitTime = lockReleaseTime - candidateRtn.registrationTime;

                        if (0 <= waitTime)
                        {//for routines, that arrive before or at "lockReleaseTime"
                            waitTimeList.add(waitTime);
                            existingCandidateList.add(candidateRtn);
                        }
                        else
                        {// for routines, that arrive after "lockReleaseTime"
                            long arrivalInterval = candidateRtn.registrationTime - lockReleaseTime;
                            rightToCurreneScanLineArrivalIntervalList.add(arrivalInterval);
                            futureCandidateList.add(candidateRtn);
                        }
                    }

                    Routine candidateRoutine = null;

                    if(!waitTimeList.isEmpty())
                    {// select from left side of "lockReleaseTime"

                        Collections.sort(waitTimeList, Collections.reverseOrder()); // sort in descending order
                        long targetWaitTime = waitTimeList.get(0);

                        for (Routine rtn : existingCandidateList)
                        {
                            long waitTime = lockReleaseTime - rtn.registrationTime;

                            if (waitTime == targetWaitTime)
                            {
                                candidateRoutine = rtn;
                                break;
                            }
                        }
                    }
                    else
                    { // select from right side of "lockReleaseTime"
                        Collections.sort(rightToCurreneScanLineArrivalIntervalList); // sort in ascending order;
                        long targetArrivalTime = rightToCurreneScanLineArrivalIntervalList.get(0);

                        for (Routine rtn : futureCandidateList)
                        {
                            long arrivalInterval = rtn.registrationTime - lockReleaseTime;

                            if(targetArrivalTime == arrivalInterval)
                            {
                                candidateRoutine = rtn;
                                break;
                            }
                        }
                    }

                    long maxTime = Math.max(lockReleaseTime, candidateRoutine.registrationTime);

                    registerRoutineFromExactTime(candidateRoutine, maxTime); // register in the lock table
                    lazy_acquireAllLock(lockTableHelper ,candidateRoutine);

                    scheduledRtnList.add(candidateRoutine);
                }

            }
            else
            {
                System.out.println("\n\nERROR: LockTable.java: should not execute this line");
                assert(false);
                System.exit(1); // trigger if assertion is turned off!
            }


            for(Routine rtn : scheduledRtnList)
            {
                waitingList.remove(rtn);
            }
        }
    }



    //private void weakScheduling(List<Routine> rtnList, int _simulationStartTime)
    private void weakScheduling(List<Routine> rtnList)
    {
        //Collections.shuffle(rtnList); // shuffle the rtn list. in weak visibility, routine can be executed in any order.

        this.lockTable = null; // so that accidentally no one uses it while in WeakScheduling mode
        this.perDevRoutineListForWeakScheduling = new HashMap<>();

        weakSchedulingSpecialLockTable = new ArrayList<>();

        for(int I = 0 ; I < rtnList.size() ; I++)
        {
            weakSchedulingSpecialLockTable.add(I, rtnList.get(I));

            //weakSchedulingSpecialLockTable.get(I).registrationTime = _simulationStartTime;
            //weakSchedulingSpecialLockTable.get(I).arrivalSequenceForWeakScheduling = I;

            assert(!weakSchedulingSpecialLockTable.get(I).commandList.isEmpty());

            //weakSchedulingSpecialLockTable.get(I).commandList.get(0).startTime = _simulationStartTime;
            weakSchedulingSpecialLockTable.get(I).commandList.get(0).startTime = weakSchedulingSpecialLockTable.get(I).registrationTime;

            for(int cmdIdx = 1 ; cmdIdx < weakSchedulingSpecialLockTable.get(I).commandList.size() ; cmdIdx++)
            {
                long prevCmdEndTime = weakSchedulingSpecialLockTable.get(I).commandList.get(cmdIdx-1).getCmdEndTime();
                weakSchedulingSpecialLockTable.get(I).commandList.get(cmdIdx).startTime = prevCmdEndTime;
            }

            for(Command cmd : rtnList.get(I).commandList)
            {
                String devID = cmd.devName;

                if(!perDevRoutineListForWeakScheduling.containsKey(devID))
                    perDevRoutineListForWeakScheduling.put(devID, new ArrayList<>());

                perDevRoutineListForWeakScheduling.get(devID).add(rtnList.get(I));

                if(!this.devID_lastAccesedRtn_Map.containsKey(devID))
                {
                    this.devID_lastAccesedRtn_Map.put(devID, rtnList.get(I));
                }
                else if(this.devID_lastAccesedRtn_Map.get(devID).getCommandByDevID(devID).getCmdEndTime() < rtnList.get(I).getCommandByDevID(devID).getCmdEndTime())
                {
                    this.devID_lastAccesedRtn_Map.put(devID, rtnList.get(I));
                }
            }
        }
    }

    private void initiateNonWV_devID_lastAccesedRtn_Map()
    {
        assert(lockTable != null); // SBA: this should never called from WV....
        assert(this.consistencyType != CONSISTENCY_TYPE.WEAK); // SBA: this should never called from WV....

        for(Map.Entry<String, List<Routine>> entry : lockTable.entrySet())
        {
            String devID = entry.getKey();
            List<Routine> lineage = entry.getValue();

            if(!lineage.isEmpty())
            {
                int lastIndex = lineage.size() - 1;
                Routine lastAccessedRtn = lineage.get(lastIndex);
                this.devID_lastAccesedRtn_Map.put(devID, lastAccessedRtn);
            }
        }
    }


    public void register(Routine rtn)
    {
        List<Routine> rtnList = new ArrayList<>();
        rtnList.add(rtn);

        register(rtnList);
    }

    public void register(List<Routine> rtnList)
    {
        if(this.consistencyType == CONSISTENCY_TYPE.LAZY
        || this.consistencyType == CONSISTENCY_TYPE.LAZY_FCFS
                || this.consistencyType == CONSISTENCY_TYPE.LAZY_PRIORITY
        )
        {
            //lazyScheduling(rtnList, _simulationStartTime);
            lazyScheduling(rtnList);
            initiateNonWV_devID_lastAccesedRtn_Map();
        }

        if(this.consistencyType == CONSISTENCY_TYPE.WEAK)
        {
            //this.weakScheduling(rtnList, _simulationStartTime);
            this.weakScheduling(rtnList); // NOTE: devID_lastAccesedRtn_Map is initiated inside this function.
        }


        long startTime = 0;
        Map<Float, Integer> EV_executionLatencyHistogram = null;


        for(Routine rtn : rtnList)
        {
            //this.register(rtn, _simulationStartTime);
            this.registerHelper(rtn);
        }

        initiateNonWV_devID_lastAccesedRtn_Map();

    }

    //private void register(Routine rtn, int _simulationStartTime)
    private void registerHelper(Routine rtn)
    {
        //rtn.registrationTime = _simulationStartTime;

        Set<String> devIDset = new HashSet<>();
        for(Command cmd : rtn.commandList)
        {
            if(devIDset.contains(cmd.devName))
            {
                System.out.println("a routine should use a device only once!");
                assert(false);
                return;
            }
            else
            {
                if( !this.lockTable.keySet().contains(cmd.devName))
                {
                    System.out.println(cmd.devName + " is used in routine, but not registered in the lock table");
                    assert(false);
                    return;
                }
                devIDset.add(cmd.devName);
            }
        }

        switch(this.consistencyType)
        {
            case SUPER_STRONG:
                case STRONG:
            {
                //this.registerStrong(rtn, _simulationStartTime);
                this.registerStrong(rtn);
                break;
            }
            case RELAXED_STRONG: //PSV
            {
                //registerRelaxedStepByStep(rtn, _simulationStartTime);
                registerRelaxedStepByStep(rtn);
                break;
            }
            case EVENTUAL: // EV
            {
                //this.insertRecursively(rtn, 0, _simulationStartTime, new HashSet<>(), new HashSet<>());
                this.insertRecursively(rtn, 0, rtn.registrationTime, new HashSet<>(), new HashSet<>());
                break;
            }
            default:
            {
                assert(false);
            }
        }
    }


    //private void registerStrong(Routine rtn, int _simulationStartTime)
    private void registerStrong(Routine rtn)
    {
        long registeredRtnMaxEndTime = this.safeHomeStartTime;

        /*
        for(Routine existingRtn : this.getAllRoutineSet())
        {
            int existingRtnEndTime = existingRtn.routineEndTime();

            if(registeredRtnMaxEndTime < existingRtnEndTime)
                registeredRtnMaxEndTime = existingRtnEndTime;
        }
        */

        for(String devId: this.lockTable.keySet())
        {
            for(Routine existingRtn : lockTable.get(devId))
            {
                long existingRtnEndTime = existingRtn.routineEndTime();

                if(registeredRtnMaxEndTime < existingRtnEndTime)
                    registeredRtnMaxEndTime = existingRtnEndTime;
            }
        }

        long routineStartTime = Math.max(registeredRtnMaxEndTime, rtn.registrationTime);

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

    //private void registerRelaxedStepByStep(Routine rtn, int _simulationStartTime)
    private void registerRelaxedStepByStep(Routine rtn)
    {
        long overlappintRtnMaxEndTime = this.safeHomeStartTime;

        for(String devId: rtn.getAllDevIDSet())
        {
            for(Routine existingRtnSharingSameDev : lockTable.get(devId))
            {
                long existingRtnEndTime = existingRtnSharingSameDev.routineEndTime();

                if(overlappintRtnMaxEndTime < existingRtnEndTime)
                    overlappintRtnMaxEndTime = existingRtnEndTime;
            }
        }

        long routineStartTime = Math.max(overlappintRtnMaxEndTime, rtn.registrationTime);

        this.registerRoutineFromExactTime(rtn, routineStartTime);

    }

//    private void registerWeak(Routine rtn, int currentTime)
//    {
//        this.registerRoutineFromExactTime(rtn, currentTime);
//    }
//
    private void registerRoutineFromExactTime(Routine rtn, long initialTime)
    {// commands will be registered without any gap
        int commandIdx = 0;
        rtn.commandList.get(commandIdx).startTime = initialTime;
        String devID = rtn.getDevID(commandIdx);
        this.lockTable.get(devID).add(rtn); // insert in the list

        for(commandIdx = 1 ; commandIdx < rtn.commandList.size() ; ++commandIdx)
        {
            rtn.commandList.get(commandIdx).startTime = rtn.commandList.get(commandIdx - 1).getCmdEndTime();

            devID = rtn.getDevID(commandIdx);
            this.lockTable.get(devID).add(rtn); // insert in the list
        }
    }

    private boolean isNoOverlap(Set<Integer> set1, Set<Integer> set2)
    {
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection.isEmpty();
    }

    private Set<Integer> getPreSet(String devID, int index)
    {// pre set should be the set of routine ids  0<= preset < index

        Set<Integer> preSet = new HashSet<>();

        if(EFFICIENT_PRE_POST_SET_CALCULATION)
        {
            if(0 < index)
            {
                preSet.add(this.lockTable.get(devID).get(index - 1).uniqueRoutineID);
                preSet.addAll(this.lockTable.get(devID).get(index - 1).tempPerDevPreSet.get(devID));
            }
        }
        else
        {
            for(int idx = 0 ; idx < index ; ++idx)
            {
                assert(idx < this.lockTable.get(devID).size());
                preSet.add(this.lockTable.get(devID).get(idx).uniqueRoutineID);
            }
        }

        return preSet;
    }

    private Set<Integer> getPostSet(String devID, int index)
    {// post set should be the set of routine ids index <= postSet < end of list

        Set<Integer> postSet = new HashSet<>();

        if(EFFICIENT_PRE_POST_SET_CALCULATION)
        {
            if(index < this.lockTable.get(devID).size() )
            {
                postSet.add(this.lockTable.get(devID).get(index).uniqueRoutineID);
                postSet.addAll(this.lockTable.get(devID).get(index).tempPerDevPostSet.get(devID));
            }
        }
        else
        {
            for (int idx = index; idx < this.lockTable.get(devID).size(); ++idx)
            {
                postSet.add(this.lockTable.get(devID).get(idx).uniqueRoutineID);
            }
        }

        return postSet;
    }

    public boolean EFFICIENT_PRE_POST_SET_CALCULATION = true;

    public boolean insertRecursively(Routine rtn, int commandIdx, long insertionStartTime, Set<Integer> _preSet, Set<Integer> _postSet)
    {
        assert(this.isNoOverlap(_preSet, _postSet));

        if(rtn.commandList.size() == commandIdx)
            return true;

        String devID = rtn.getDevID(commandIdx);
        long commandStartTime = insertionStartTime;
        int cmdDuration = rtn.commandList.get(commandIdx).durationMilliSec;

        while(true)
        {
            CmdInsertionData cmdInsertionData = getLockTableEmptyPlaceIndex(devID, commandStartTime, cmdDuration );
            commandStartTime = cmdInsertionData.cmdStartTime; // the commandStartTime might drift from "insertionStartTime"
            int lockTableInsertionIndex = cmdInsertionData.cmdInsertIndex;

            Set<Integer> preSet = new HashSet<>(_preSet);
            preSet.addAll(getPreSet( devID ,lockTableInsertionIndex));

            Set<Integer> postSet = new HashSet<>(_postSet);
            postSet.addAll(getPostSet(devID, lockTableInsertionIndex));

            if(isNoOverlap(preSet, postSet))
            {
                long nextCmdMinimumStartTime = commandStartTime + rtn.commandList.get(commandIdx).durationMilliSec;
                boolean deepDive = insertRecursively(rtn, commandIdx + 1, nextCmdMinimumStartTime, preSet, postSet);/// call recursion

                if(deepDive)
                {
                    long startTime;
                    if((0 == commandIdx) && 1 < rtn.commandList.size())
                    {
                        long secondCmdStartTime = rtn.commandList.get(1).startTime;
                        long nextLockStartTime = Long.MAX_VALUE;
                        if(lockTableInsertionIndex != lockTable.get(devID).size())
                        {
                            nextLockStartTime = this.lockTable.get(devID).get(lockTableInsertionIndex).lockStartTime(devID);
                        }

                        long earliestEndTime = Math.min(secondCmdStartTime, nextLockStartTime);

                        startTime = (earliestEndTime - rtn.commandList.get(commandIdx).durationMilliSec); // set command Start Time
                    }
                    else
                    {
                        startTime = commandStartTime; // set command Start Time
                    }

                    rtn.commandList.get(commandIdx).startTime = startTime;
                    this.lockTable.get(devID).add(lockTableInsertionIndex, rtn); // insert in the list

                    if(EFFICIENT_PRE_POST_SET_CALCULATION)
                    {
                        for (int idx = 0; idx < this.lockTable.get(devID).size(); ++idx)
                        {
                            int id = this.lockTable.get(devID).get(idx).uniqueRoutineID;

                            if(idx < lockTableInsertionIndex)
                            {
                                this.lockTable.get(devID).get(idx).tempPerDevPostSet.get(devID).add(rtn.uniqueRoutineID);
                                this.lockTable.get(devID).get(lockTableInsertionIndex).tempPerDevPreSet.get(devID).add(id);
                            }
                            else if(lockTableInsertionIndex < idx)
                            {
                                this.lockTable.get(devID).get(idx).tempPerDevPreSet.get(devID).add(rtn.uniqueRoutineID);
                                this.lockTable.get(devID).get(lockTableInsertionIndex).tempPerDevPostSet.get(devID).add(id);
                            }
                        }
                    }

                    return true;
                }

            }

            if(lockTableInsertionIndex == lockTable.get(devID).size())
            {// already reached at the last, but still could not insert... return false
                return false;
            }
            else
            {
                commandStartTime = lockTable.get(devID).get(lockTableInsertionIndex).lockEndTime(devID);
            }

        }

    }


    public CmdInsertionData getLockTableEmptyPlaceIndex(String _devID, long _scanStartTime, int _targetCmdDuration)
    {
        boolean isPreLeaseAllowed = SystemParametersSingleton.getInstance().IS_PRE_LEASE_ALLOWED;
        boolean isPostLeaseAllowed = SystemParametersSingleton.getInstance().IS_POST_LEASE_ALLOWED;

        int index;
        long scanStartTime = _scanStartTime;

        for(index = 0 ; index < lockTable.get(_devID).size() ; ++index)
        {
            Routine currentRtnInLineage = lockTable.get(_devID).get(index);
            long cmdStartTime = currentRtnInLineage.lockStartTime(_devID);
            long cmdEndTime = currentRtnInLineage.lockEndTime(_devID);

            assert(cmdStartTime < cmdEndTime);

            if(cmdEndTime < scanStartTime)
            {
                continue;
            }
            else
            {
                if(cmdStartTime <= scanStartTime)
                {// overlap with the scan line
                    scanStartTime = cmdEndTime; // shift the scan line
                    continue;
                }
                else
                {
                    long emptySlot = cmdStartTime - scanStartTime;

                    if( _targetCmdDuration <= emptySlot)
                    {

                        if(!isPreLeaseAllowed)
                        {
                            // donot need to check if currentRtnInLineage is committed!
                            if(currentRtnInLineage.isCandidateCmdInsidePreLeaseZone(_devID, scanStartTime, _targetCmdDuration )) // SBA: Never allow something before another routine
                            //if(true)// SBA: but it is a temporary lease, right? so i think, its ok
                            {
                                scanStartTime = cmdEndTime; // shift the scan line after the command
                                continue;
                            }
                        }

                        if(!isPostLeaseAllowed && (0 < index))
                        {
                            // lineage=>   [rtn 1 c1] [r4 c9] [prevRtnInLineage cx] [r_currentCandidate, c_Candidate (this command will be inserted)] [currentRtnInLineage, c*].....
                            Routine prevRtnInLineage = lockTable.get(_devID).get(index - 1);
                            // check if prevRtnInLineage is committed...
                            if(!prevRtnInLineage.isCommittedByGivenTime(scanStartTime))
                            {// prevRtnInLineage not committed... now check if it the new command overlaps with prevRtnInLineage's postLeaseZone
                                if(prevRtnInLineage.isCandidateCmdInsidePostLeaseZone(_devID, scanStartTime, _targetCmdDuration ))
                                {
                                    scanStartTime = prevRtnInLineage.routineEndTime(); // shift the scan line after the command
                                    continue;
                                }
                            }
                        }

                        return new CmdInsertionData(scanStartTime, index);
                    }
                    else
                    {
                        scanStartTime = cmdEndTime; // shift the scan line
                    }

                }
            }

        }

        //dont need to check "pre" at this point... if code comes here, this should be the last command.
        // hence, consider the post-lease flag of this command's previous routine.
        if(!isPostLeaseAllowed && (0 < index))
        {
            // lineage=>   [rtn 1 c1] [r4 c9] [prevRtnInLineage cx] [r_currentCandidate, c_Candidate (this command will be inserted)] [currentRtnInLineage, c*].....
            Routine prevRtnInLineage = lockTable.get(_devID).get(index - 1);
            // check if prevRtnInLineage is committed...
            if(!prevRtnInLineage.isCommittedByGivenTime(scanStartTime))
            {// prevRtnInLineage not committed... now check if it the new command overlaps with prevRtnInLineage's postLeaseZone
                if(prevRtnInLineage.isCandidateCmdInsidePostLeaseZone(_devID, scanStartTime, _targetCmdDuration ))
                {
                    scanStartTime = prevRtnInLineage.routineEndTime(); // shift the scan line after the command
                }
            }
        }


        return new CmdInsertionData(scanStartTime, index);
    }

    public class CmdInsertionData
    {
        long cmdStartTime;
        int cmdInsertIndex;
        public CmdInsertionData(long _cmdStartTime, int _cmdInsertIndex)
        {
            this.cmdStartTime = _cmdStartTime;
            this.cmdInsertIndex = _cmdInsertIndex;
        }

        @Override
        public String toString()
        {
            String str = " cmdStartTime = " + this.cmdStartTime + " | cmdInsertIndex = " + this.cmdInsertIndex;
            return str;
        }
    }
}
