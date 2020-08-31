package Utility;

public class RoutineExecutionRecipe
{
    public long sleepMilliSecBeforeExecutingCmd;
    public DeviceInfo deviceInfo;
    public DeviceStatus targetStatus;
    public int uniqueRoutineID;
    public int cmdIdx;
    public final static String beginning = "BG";
    public final static String end = "ED";
    public final static String single = "SG";
    public String subCmdType;

    public RoutineExecutionRecipe(long _sleepMilliSecBeforeExecutingCmd,
                                  DeviceInfo _deviceInfo,
                                  DeviceStatus _targetStatus,
                                  int _uniqueRoutineID,
                                  int _cmdIdx,
                                  String _subCmdType)
    {
        this.sleepMilliSecBeforeExecutingCmd = _sleepMilliSecBeforeExecutingCmd;
        this.deviceInfo = _deviceInfo;
        this.targetStatus = _targetStatus;
        this.uniqueRoutineID = _uniqueRoutineID;
        this.cmdIdx = _cmdIdx;
        this.subCmdType = _subCmdType;
    }

    @Override
    public String toString()
    {
        String str = "";

        str += "\t ### -t:" + System.currentTimeMillis() +
                " [ rtnID = " + this.uniqueRoutineID +
                ", cmdIdx = " + this.cmdIdx +
                ", subCmdType = " + this.subCmdType +
                ", devName = " + this.deviceInfo.getDevName() +
                ", targetStatus = " + this.targetStatus +
                ", sleepBeforeExecutionMillisec = " + this.sleepMilliSecBeforeExecutingCmd +
                " ]";

        return str;
    }
}
