package Measurement;

import Utility.Command;
import Utility.LockTable;
import Utility.Routine;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

public class MeasurementSingleton {
  private Boolean isDisposed;
  private static MeasurementSingleton singleton;
  private Map<MeasurementType, SingleMeasurementResult> all_results = new HashMap<>();

  private static class DataHolder
  {
    //public float average = Float.MIN_VALUE;
    public List<Float> dataList;

    public boolean isHistogramMode;
    private float HISTOGRAM_KEY_RESOLUTION = 1000.0f; /// convert 3.14159 to 3.14100000... so now 3.14159 and 3.14161 are same... This will save space
    public Map<Float,Integer> globalHistogram;
    List<Float> cdfDataListInHistogramMode = new ArrayList<>();
    List<Float> cdfFrequencyListInHisotgramMode = new ArrayList<>();

    public Boolean isListFinalized;
    double globalItemCount;
    double globalSum;

    public DataHolder()
    {
      this.dataList = new ArrayList<>();
      this.globalHistogram = new HashMap<>();

      this.isListFinalized = false;
      this.isHistogramMode = false;
      this.globalItemCount = 0;
      this.globalSum = 0.0f;
    }

    public float getNthDataOrMinusOne(int N, float dummyMaxItemCount)
    {
      if( (N < 0) || (this.cdfListSize() == 0) || (this.cdfListSize() <= N))
        return -1;

      if (isHistogramMode)
        return cdfDataListInHistogramMode.get(N);
      else
        return dataList.get(N);
    }

    public float getNthCDFOrMinusOne(int N)
    {
      if( (N < 0) || (this.cdfListSize() == 0) || (this.cdfListSize() <= N))
        return -1;

      if(isHistogramMode)
        return cdfFrequencyListInHisotgramMode.get(N);
      else
      {
        float frequency = 1.0f / this.cdfListSize();
        return frequency * (N + 1.0f);
      }
    }

    public float getAverage()
    {
      return (float)((globalItemCount == 0.0)? 0.0 : this.globalSum/this.globalItemCount);
    }

    public int cdfListSize()
    {
      assert(this.isListFinalized); // should not call until finalize.
      if(this.isHistogramMode)
      {
        assert(!cdfDataListInHistogramMode.isEmpty());
        return cdfDataListInHistogramMode.size();
      }
      else
      {
        return (int)this.globalItemCount;
      }
    }

    public void addData(Map<Float,Integer> partialHistogram)
    {
      this.isHistogramMode = true;
      this.dataList = null; // to prevent it from being accidentally used!

      for(Map.Entry<Float, Integer> entry : partialHistogram.entrySet())
      {
        Float data = entry.getKey();
        Integer partialFrequency = entry.getValue();

        data = (float)((int)(data * HISTOGRAM_KEY_RESOLUTION))/ HISTOGRAM_KEY_RESOLUTION; /// convert 3.141592654 to 3.14100000
        /// convert 3.14159 to 3.141... so now 3.14159 and 3.14161 both are 3.141... both will go to the same Map-bucket. This will save huge space

        this.globalSum += (data * partialFrequency);
        this.globalItemCount += partialFrequency;

        Integer currentDataFrequency = globalHistogram.get(data);

        if(currentDataFrequency == null)
          globalHistogram.put(data, partialFrequency);
        else
          globalHistogram.put(data, (partialFrequency + currentDataFrequency));
      }
    }
  }

  private DataHolder incong_data = new DataHolder();

  private static int maxDataPoint = 5000;

  public MeasurementSingleton() {
    this.isDisposed = false;
  }

  public static synchronized MeasurementSingleton getInstance() {
    if (MeasurementSingleton.singleton == null) {
      MeasurementSingleton.singleton = new MeasurementSingleton();
    }
    return MeasurementSingleton.singleton;
  }

  public synchronized void AddResult(MeasurementType type, Float value) {
    if (all_results.containsKey(type)) {
      all_results.get(type).add_result(value);
    } else {
      all_results.put(type, new SingleMeasurementResult(type, value));
    }
  }

  public synchronized List<Float> GetResult(MeasurementType type) {
    if (all_results.containsKey(type)) return all_results.get(type).get_results();
    return null;
  }

  public void RecordIncongruance(final LockTable _lockTable) {
    Map<Float, Integer> res_data = isolationViolation(_lockTable);
    incong_data.addData(res_data);
  }

  public synchronized void Dispose() {
    // TODO: clean the class
    this.isDisposed = true;
  }

  private Map<Float, Integer> isolationViolation(final LockTable _lockTable) {
    Map<Float, Integer> isvltn5_routineLvlIsolationViolationTimePrcntHistogram = new HashMap<>();

    // Get from SafeHomeFamework
    List<Routine> allRtnList = _lockTable.getAllRoutineSet();

    Map<Routine, Set<Routine>> victimRtnAndAttackerRtnSetMap = new HashMap<>();
    Map<Routine, Set<Command>> victimRtnAndItsVictimCmdSetMap = new HashMap<>();
    Map<Routine, Long> victimRtnAndEarliestCollisionTimeMap = new HashMap<>();

    long earliestRtnStartTime = Long.MAX_VALUE;

    for (Routine rtn1 : allRtnList) {
      assert (!victimRtnAndAttackerRtnSetMap.containsKey(rtn1));
      victimRtnAndAttackerRtnSetMap.put(rtn1, new HashSet<>());

      assert (!victimRtnAndItsVictimCmdSetMap.containsKey(rtn1));
      victimRtnAndItsVictimCmdSetMap.put(rtn1, new HashSet<>());


      for (Command cmd1 : rtn1.commandList) {
        String devID = cmd1.devName;
        long spanStartTimeInclusive = cmd1.startTime;
        long spanEndTimeExclusive = rtn1.routineEndTime();
        earliestRtnStartTime = Math.min(earliestRtnStartTime, spanStartTimeInclusive);

        long earliestCollisionTime = Long.MAX_VALUE;

        for (Routine rtn2 : allRtnList) {
          if (rtn1 == rtn2)
            continue;

          boolean isAttackedByRtn2 = rtn2.isDevAccessStartsDuringTimeSpan(devID, spanStartTimeInclusive, spanEndTimeExclusive);

          if (isAttackedByRtn2) {
            victimRtnAndAttackerRtnSetMap.get(rtn1).add(rtn2);
            victimRtnAndItsVictimCmdSetMap.get(rtn1).add(cmd1);

            long collisionTime = rtn2.getCommandByDevID(devID).startTime;

            if (collisionTime < earliestCollisionTime)
              earliestCollisionTime = collisionTime;
          }
        }

        if (earliestCollisionTime < Long.MAX_VALUE) {
          assert (spanStartTimeInclusive <= earliestCollisionTime);

          if (!victimRtnAndEarliestCollisionTimeMap.containsKey(rtn1)) {
            // to analysis routine-timespan-level collision, we need only the first colliding command
            // hence this check is required. if we have already seen a command, there is no need to check for next violation
            // as in routine level, isolation-violation starts from the very first command-violation;
            victimRtnAndEarliestCollisionTimeMap.put(rtn1, earliestCollisionTime);
          }
        }
      }
    }


    for (Routine rtn1 : allRtnList) {
      float timeSpentInCollisionRatio = 0.0f;

      if (victimRtnAndEarliestCollisionTimeMap.containsKey(rtn1)) {// this routine has violation!
        long earliestCollisionTime = victimRtnAndEarliestCollisionTimeMap.get(rtn1);
        long spanStartTimeInclusive = rtn1.routineStartTime();
        long spanEndTimeExclusive = rtn1.routineEndTime();
        long expectedConsistencySpanRtn1 = spanEndTimeExclusive - spanStartTimeInclusive;
        long collisionTime = spanEndTimeExclusive - earliestCollisionTime;

        if (collisionTime > 0) {
          System.out.printf("Incongrance with rtn ID: %d\n" +
                  "    Overall start time %d \n" +
                  "    Start time %d (%d)\n" +
                  "    End time %d (%d) \n" +
                  "    Earlist collision time %d (%d)\n" +
                  "    collisionTime %d, spanTime %d\n",
              rtn1.uniqueRoutineID,
              earliestRtnStartTime,
              spanStartTimeInclusive, spanStartTimeInclusive - earliestRtnStartTime,
              spanEndTimeExclusive, spanEndTimeExclusive - earliestRtnStartTime,
              earliestCollisionTime, earliestCollisionTime - earliestRtnStartTime,
              collisionTime, expectedConsistencySpanRtn1);
        }

        timeSpentInCollisionRatio = ((float) collisionTime) / expectedConsistencySpanRtn1 * 100.0f;
      }


      Float data = timeSpentInCollisionRatio;
      isvltn5_routineLvlIsolationViolationTimePrcntHistogram.merge(data, 1, (a, b) -> a + b);
    }
    return isvltn5_routineLvlIsolationViolationTimePrcntHistogram;
  }

  private void writeCombinedStatInFile(
      final MeasurementType currentMeasurement,
      final String subDirPath,
      List<DataHolder> insertedInConsistencyOrder,
      List<String> consistencyHeader) {

    assert (insertedInConsistencyOrder.size() == consistencyHeader.size());

    String fileName = currentMeasurement.name() + ".dat";
    String filePath = subDirPath + File.separator + fileName;

    float maxItemCount = Integer.MIN_VALUE;

    String combinedCDFStr = "";
    for (int I = 0; I < consistencyHeader.size(); I++) {
      if (maxItemCount < insertedInConsistencyOrder.get(I).cdfListSize())
        maxItemCount = insertedInConsistencyOrder.get(I).cdfListSize();

      combinedCDFStr += "data\t" + consistencyHeader.get(I);

      if (I < (consistencyHeader.size() - 1))
        combinedCDFStr += "\t";
      else
        combinedCDFStr += "\n";
    }

    System.out.println("\tPrepared the header: " + combinedCDFStr + "\t\tnext working to extract the data...: maxItemCount = " + maxItemCount);

    for (int N = 0; N < maxItemCount; N++) {
      for (int I = 0; I < insertedInConsistencyOrder.size(); I++) {
        float data = insertedInConsistencyOrder.get(I).getNthDataOrMinusOne(N, maxItemCount);
        float CDF = insertedInConsistencyOrder.get(I).getNthCDFOrMinusOne(N);

        combinedCDFStr += data + "\t" + CDF;

        if (I < (insertedInConsistencyOrder.size() - 1))
          combinedCDFStr += "\t";
        else
          combinedCDFStr += "\n";
      }
    }

    try {
      Writer fileWriter = new FileWriter(filePath);
      fileWriter.write(combinedCDFStr);
      fileWriter.close();
    } catch (Exception ex) {
      System.out.println("\n\nERROR: cannot write file " + filePath);
      System.exit(1);
    }
  }

  private void DataFinalize(DataHolder data_holder) {
    System.out.println("\tFinalizing data\n");
    if (!data_holder.isHistogramMode) {
      System.out.println("\n\n ERROR: Inside MeasurementCollector.java... The new approach should not execute this part of code... Terminating...");
      System.exit(1);
    }

    int currentHistogramSize  = (int) data_holder.globalItemCount;
    final int maxDataPointForHistogram = maxDataPoint;

    System.out.println("\t\tHistogram mode: currentHistogramSize = " + currentHistogramSize + " | maxDataPointForHistogram = " + maxDataPointForHistogram);

    double cdfListSize = data_holder.globalItemCount;

    if (maxDataPointForHistogram < currentHistogramSize) {
      cdfListSize = 0;

      float[] tempDataArray = new float[maxDataPointForHistogram];

      Set<Integer> uniqueIndexSet = new HashSet<>();
      Random rand = new Random();

      while(uniqueIndexSet.size() < maxDataPointForHistogram) {
        int randHistogramPosition = 1 + rand.nextInt(currentHistogramSize); // random number from 1 to currentHistogramSize

        if(!uniqueIndexSet.contains(randHistogramPosition))
          uniqueIndexSet.add(randHistogramPosition);
      }

      assert(tempDataArray.length == uniqueIndexSet.size());

      int trimmedIndex = 0;
      for(int randHistogramPosition : uniqueIndexSet) {
        int I = 0;
        for(Map.Entry<Float, Integer> entry : data_holder.globalHistogram.entrySet())
        {
          Float data = entry.getKey();
          Integer frequency = entry.getValue();

          I += frequency;

          if( randHistogramPosition <= I)
          {
            tempDataArray[trimmedIndex++] = data;
            break;
          }
        }
      }

      /*
       * SBA:
       * DO NOT RESET globalItemCount and globalSum... you already have the entire dataset..
       * why not calculate the average based on that?
       * It will give more accurate result!
       * */

      //data_holder.globalItemCount = 0; //SBA: DO NOT RESET
      //data_holder.globalSum = 0.0f; //SBA: DO NOT RESET
      data_holder.globalHistogram.clear();

      for(float data : tempDataArray)
      {
        Integer currentFrequency = data_holder.globalHistogram.get(data);

        if(currentFrequency == null)
          data_holder.globalHistogram.put(data, 1);
        else
          data_holder.globalHistogram.put(data, currentFrequency + 1);

        cdfListSize++;
        //data_holder.globalItemCount++; //SBA: DO NOT RESET
        //data_holder.globalSum += data; //SBA: DO NOT RESET
      }
    }


    List<Float> sortedDataSequenceFromHistogram = new ArrayList<>();

    for(float data : data_holder.globalHistogram.keySet())
    {
      sortedDataSequenceFromHistogram.add(data);
    }

    Collections.sort(sortedDataSequenceFromHistogram);

    int indexTracker = 1;
    float frequencyMultiplyer = 0.0f;
    if(0 < cdfListSize )
      frequencyMultiplyer = (float)(1.0 / cdfListSize);


    //final float frequencyMultiplyer = (float)(1.0 / data_holder.globalItemCount); // NOTE: this is total item count... not the CDF list size... that CDF list has not been initialized yet!

    for(float sortedData : sortedDataSequenceFromHistogram)
    {
      float frequency = data_holder.globalHistogram.get(sortedData);

      data_holder.cdfDataListInHistogramMode.add(sortedData);
      data_holder.cdfFrequencyListInHisotgramMode.add(indexTracker * frequencyMultiplyer);

      if(1 < frequency)
      {
        indexTracker = indexTracker + (int)frequency - 1;
        data_holder.cdfDataListInHistogramMode.add(sortedData);
        data_holder.cdfFrequencyListInHisotgramMode.add(indexTracker * frequencyMultiplyer);
      }

      indexTracker++;
    }

    data_holder.globalHistogram.clear();
    data_holder.globalHistogram = null;

    data_holder.isListFinalized = true;
  }

  public void getFinalIncongruenceResult(String consistency_type, String folder) {
    DataFinalize(incong_data);
    List<DataHolder> data_list = new ArrayList<>();
    data_list.add(incong_data);

    List<String> consistency_header = new ArrayList<>();
    consistency_header.add(consistency_type);

    writeCombinedStatInFile(MeasurementType.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
                            folder, data_list, consistency_header);
  }
}
