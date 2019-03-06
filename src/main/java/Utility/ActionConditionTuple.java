package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/5/2019
 * @time 10:54 AM
 */
public class ActionConditionTuple
{
    @JsonProperty
    private String actionDevName;

    @JsonProperty
    public DeviceStatus actionDevStatus;

    @JsonProperty
    private String conditionDevName;

    @JsonProperty
    public DeviceStatus conditionDevStatus;

    public ActionConditionTuple() { } // need this default constructor for JSON parser

    @JsonIgnore
    public ActionConditionTuple(String actionDevName, DeviceStatus actionDevStatus, String conditionDevName, DeviceStatus conditionDevStatus)
    {
        this.actionDevName = actionDevName;
        this.actionDevStatus = actionDevStatus;
        this.conditionDevName = conditionDevName;
        this.conditionDevStatus = conditionDevStatus;
    }

    @JsonIgnore
    public DevNameDevStatusTuple getAction()
    {
        return new DevNameDevStatusTuple(this.actionDevName, this.actionDevStatus);
    }

    @JsonIgnore
    public DevNameDevStatusTuple getCondition()
    {
        return new DevNameDevStatusTuple(this.conditionDevName, conditionDevStatus);
    }

    @Override
    public String toString()
    {
        return "ActionConditionTuple{" +
                "actionDevName='" + actionDevName + '\'' +
                ", actionDevStatus=" + actionDevStatus +
                ", conditionDevName='" + conditionDevName + '\'' +
                ", conditionDevStatus=" + conditionDevStatus +
                '}';
    }
}
