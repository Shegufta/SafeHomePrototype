package Utility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/5/2019
 * @time 6:04 PM
 */
public class DevNameDevStatusTuple
{
    @JsonProperty
    private String devName;

    @JsonProperty
    private DeviceStatus devStatus;

    public DevNameDevStatusTuple(){} // Default Constructor for JSON

    @JsonIgnore
    public DevNameDevStatusTuple(String devName, DeviceStatus devStatus)
    {
        this.devName = devName;
        this.devStatus = devStatus;
    }

    @Override
    public String toString()
    {
        return "[" +
                "devName='" + devName + '\'' +
                ", devStatus=" + devStatus +
                ']';
    }

    @Override
    public int hashCode()
    {
        int modifiedHashCode = this.devName.hashCode() + this.devStatus.hashCode();
        return modifiedHashCode;
    }


    @Override
    public boolean equals(Object obj)
    {
        // If the object is compared with itself then return true
        if (obj == this)
        {
            return true;
        }

        // Check if obj is an instance of DevNameDevStatusTuple or not
          //"null instanceof [type]" also returns false
        if (!(obj instanceof DevNameDevStatusTuple)) {
            return false;
        }

        // typecast obj to Complex so that we can compare data members
        DevNameDevStatusTuple incomingInstance = (DevNameDevStatusTuple) obj;

        boolean isEqual = (
                (0 == this.devName.compareTo(incomingInstance.devName))
                &&
                        (this.devStatus == incomingInstance.devStatus)
        );

        return isEqual;
    }

    public String getDevName() {
        return this.devName;
    }

    public DeviceStatus getDevStatus() {
        return this.devStatus;
    }
}

/**
 * https://stackoverflow.com/questions/9440380/using-an-instance-of-an-object-as-a-key-in-hashmap-and-then-access-it-with-exac
 * https://www.geeksforgeeks.org/overriding-equals-method-in-java/
 */