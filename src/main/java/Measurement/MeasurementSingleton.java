package Measurement;

import Utility.*;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

public class MeasurementSingleton {
  private Boolean isDisposed;
  private static MeasurementSingleton singleton;
  private Map<MeasurementType, SingleMeasurementResult> all_results = new HashMap<>();

  private class DataHolder
  {
    //public float average = Float.MIN_VALUE;
    public List<Float> dataList;

    public boolean isHistogramMode;
    private float HISTOGRAM_KEY_RESOLUTION = 1000.0f; /// convert 3.14159 to 3.14100000... so now 3.14159 and 3.14161 are same... This will save space
    public Map<Float,Integer> globalHistogram;
    List<Float> cdfDataListInHistogramMode = new ArrayList<>();
    List<Float> cdfFrequencyListInHisotgramMode = new ArrayList<>();
    List<Float> pdfDataListInHistogramMode = new ArrayList<>();
    List<Float> pdfFrequencyListInHisotgramMode = new ArrayList<>();


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

    public float getPDFNthDataOrMinusOne(int N, float dummyMaxItemCount)
    {
      if( (N < 0) || (this.pdfListSize() == 0) || (this.pdfListSize() <= N))
        return -1;

      if (isHistogramMode)
        return pdfDataListInHistogramMode.get(N);
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

    public float getNthPDFOrMinusOne(int N) {
      if ((N < 0) || (this.pdfListSize() == 0) || (this.pdfListSize() <= N))
        return -1;

      if (isHistogramMode) {
        return pdfFrequencyListInHisotgramMode.get(N);
      } else {
        float frequency = 1.0f / this.pdfListSize();
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

    public int pdfListSize()
    {
      assert(this.isListFinalized); // should not call until finalize.
      if(this.isHistogramMode) {
        assert(!pdfDataListInHistogramMode.isEmpty());
        return pdfDataListInHistogramMode.size();
      } else {
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

  private Map<CONSISTENCY_TYPE, String> header_map = new HashMap<CONSISTENCY_TYPE, String>() {{
    put(CONSISTENCY_TYPE.STRONG, "GSV");
    put(CONSISTENCY_TYPE.RELAXED_STRONG, "PSV");
    put(CONSISTENCY_TYPE.EVENTUAL, "EV");
    put(CONSISTENCY_TYPE.WEAK, "WV");
  }};

  private Map<CONSISTENCY_TYPE, DataHolder> incong_data = new HashMap<>();
  private Map<CONSISTENCY_TYPE, DataHolder> e2e_latency = new HashMap<>();
  private Map<CONSISTENCY_TYPE, DataHolder> parallel_dt = new HashMap<>();

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

  public synchronized void Dispose() {
    // TODO: clean the class
    this.isDisposed = true;
  }

  public void RecordMetrics(final LockTable _lockTable) {
    RecordE2ELatency(_lockTable);
    RecordIncongruance(_lockTable);
    RecordParallelDelta(_lockTable);
  }

  private void RecordE2ELatency(LockTable _lockTable) {
    // Collect e2e latency of all routines for that run
    List<Routine> allRtnList = _lockTable.getAllRoutineSet();
    Map<Float, Integer> e2eTimeHistogram = new HashMap<>();
    for(Routine routine : allRtnList) {
      float data = routine.getEndToEndLatency() / 10;
      int count = e2eTimeHistogram.getOrDefault(data, 0);
      e2eTimeHistogram.put(data, count + 1);
    }

    // Record all latencies into result collector
    CONSISTENCY_TYPE consistency_type = _lockTable.consistencyType;
    if (!e2e_latency.containsKey(consistency_type)) {
      e2e_latency.put(consistency_type, new DataHolder());
    }
    e2e_latency.get(consistency_type).addData(e2eTimeHistogram);
  }

  public void RecordIncongruance(final LockTable _lockTable) {
    // Collect final incongruance of all routines for that run
    Map<Float, Integer> res_data = isolationViolation(_lockTable);

    // Record all incongruance into result collector
    CONSISTENCY_TYPE consistency_type = _lockTable.consistencyType;
    if (!incong_data.containsKey(consistency_type)) {
      incong_data.put(consistency_type, new DataHolder());
    }
    incong_data.get(consistency_type).addData(res_data);
  }

  private Map<Float, Integer> isolationViolation(final LockTable _lockTable) {
    Map<Float, Integer> isvltn5_routineLvlIsolationViolationTimePrcntHistogram = new HashMap<>();

    // Get from SafeHomeFamework
    List<Routine> allRtnList = _lockTable.getAllRoutineSet();

    System.out.println("\n\n ***** Doing isolation checking! **** \n");
    for (Routine rtn: allRtnList) {
      System.out.println(rtn.toString());
    }

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

  public void RecordParallelDelta(final LockTable _lockTable) {
    // Collect parallel delta of all routines for that run
    Map<Float, Integer> res_data = parallelDelta(_lockTable);

    // Record all incongruance into result collector
    CONSISTENCY_TYPE consistency_type = _lockTable.consistencyType;
    if (!parallel_dt.containsKey(consistency_type)) {
      parallel_dt.put(consistency_type, new DataHolder());
    }
    parallel_dt.get(consistency_type).addData(res_data);
  }

  private Map<Float, Integer> parallelDelta(final LockTable _lockTable) {
    System.out.printf("Start measuring Parallel delta for consistency type %s\n", _lockTable.consistencyType.name());
    Map<Float, Integer> res_data = new HashMap<>();

    long minStartTimeInclusive = Long.MAX_VALUE;
    long maxEndTimeExclusive = Long.MIN_VALUE;

    for(Routine rtn : _lockTable.getAllRoutineSet()) {
      if(rtn.routineStartTime() < minStartTimeInclusive)
        minStartTimeInclusive = rtn.routineStartTime();

      if(maxEndTimeExclusive < rtn.routineEndTime())
        maxEndTimeExclusive = rtn.routineEndTime();
    }

    assert(minStartTimeInclusive < maxEndTimeExclusive);

    int totalTimeSpan = (int) (maxEndTimeExclusive - minStartTimeInclusive); // start time is inclusive, end time is exclusive. e.g.  J : [<R1|C1>:1:2) [<R0|C3>:3:4) [<R2|C0>:4:5)
    //this.parallelRtnCntList = new ArrayList<>(Collections.nCopies(totalTimeSpan, 0.0f));
    short[] histogram = new short[totalTimeSpan];

    System.out.printf("Total time span: %d with start %d end %d \n", totalTimeSpan, minStartTimeInclusive, maxEndTimeExclusive);

    for(Routine rtn : _lockTable.getAllRoutineSet()) {
      int startIdx = (int) (rtn.routineStartTime() - minStartTimeInclusive);
      int endIdx = (int) (rtn.routineEndTime() - minStartTimeInclusive);

      System.out.printf("rtn %s ID %d start idx: %d, end idx: %d\n",
          rtn.routineName, rtn.uniqueRoutineID, startIdx, endIdx);

      for(int I = startIdx ; I < endIdx ; I++) {
        histogram[I]++;
        //this.parallelRtnCntList.add(I, (this.parallelRtnCntList.get(I) + 1));
      }
    }

    short currentFreq = -1;

    for(short frequency : histogram) {
      // New Approach: just record the change in frequency...
      // e.g.  if the freq is 1 1 1 1 3 3 2 1 => then record 1,3,2,1...
      // i.e. just the changing points
      if(frequency != currentFreq) {
        System.out.printf("A different parallel level %d\n", frequency);
        currentFreq = frequency;

        Integer count = res_data.getOrDefault((float)frequency, 0);
        // here the count is the data. we have to count how many time these "count" appear
        res_data.put((float)frequency, count + 1);
      }
    }
    res_data.merge((float) 0, 1, Integer::sum);
    return res_data;
  }

  public void getFinalMetricResults(List<CONSISTENCY_TYPE> consistency_types, String folder) {
    getFinalE2EResult(consistency_types, folder);
    getFinalIncongruenceResult(consistency_types, folder);
    getFinalParallelDelta(consistency_types, folder);
  }

  private void getFinalE2EResult(List<CONSISTENCY_TYPE> consistency_types, String folder) {
    List<DataHolder> data_list = new ArrayList<>();
    List<String> consistency_header = new ArrayList<>();
    for (CONSISTENCY_TYPE consistency_type: consistency_types) {
      DataFinalize(e2e_latency.get(consistency_type));
      data_list.add(e2e_latency.get(consistency_type));
      consistency_header.add(header_map.get(consistency_type));
    }

    writeCombinedStatInFile(MeasurementType.E2E_RTN_TIME,
        folder, data_list, consistency_header);
    writeCombinedPDFInFile(MeasurementType.E2E_RTN_TIME,
        folder, data_list, consistency_header);
  }

  public void getFinalIncongruenceResult(List<CONSISTENCY_TYPE> consistency_types, String folder) {
    List<DataHolder> data_list = new ArrayList<>();
    List<String> consistency_header = new ArrayList<>();
    for (CONSISTENCY_TYPE consistency_type: consistency_types) {
      DataFinalize(incong_data.get(consistency_type));
      data_list.add(incong_data.get(consistency_type));
      consistency_header.add(header_map.get(consistency_type));
    }

    writeCombinedStatInFile(MeasurementType.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
        folder, data_list, consistency_header);
    writeCombinedPDFInFile(MeasurementType.ISVLTN5_RTN_LIFESPAN_COLLISION_PERCENT,
        folder, data_list, consistency_header);
  }

  private void getFinalParallelDelta(List<CONSISTENCY_TYPE> consistency_types, String folder) {
    List<DataHolder> data_list = new ArrayList<>();
    List<String> consistency_header = new ArrayList<>();
    for (CONSISTENCY_TYPE consistency_type: consistency_types) {
      DataFinalize(parallel_dt.get(consistency_type));
      data_list.add(parallel_dt.get(consistency_type));
      consistency_header.add(header_map.get(consistency_type));
    }

    writeCombinedStatInFile(MeasurementType.PARALLEL_DELTA,
        folder, data_list, consistency_header);
    writeCombinedPDFInFile(MeasurementType.PARALLEL_DELTA,
        folder, data_list, consistency_header);
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

  private void writeCombinedPDFInFile(
      final MeasurementType currentMeasurement,
      final String subDirPath,
      List<DataHolder> insertedInConsistencyOrder,
      List<String> consistencyHeader
  ) {
    assert(insertedInConsistencyOrder.size() == consistencyHeader.size());
    String fileName = currentMeasurement.name() + "_PDF.dat";
    String filePath = subDirPath + File.separator + fileName;

    float maxItemCount = Integer.MIN_VALUE;

    String combinedPDFStr = "";
    for(int I = 0 ; I < consistencyHeader.size() ; I++) {
      if( maxItemCount < insertedInConsistencyOrder.get(I).pdfListSize())
        maxItemCount = insertedInConsistencyOrder.get(I).pdfListSize();

      combinedPDFStr += "data\t" + consistencyHeader.get(I);

      if(I < (consistencyHeader.size() - 1))
        combinedPDFStr += "\t";
      else
        combinedPDFStr += "\n";
    }

    System.out.println("\tPrepared the header: " + combinedPDFStr + "\t\tnext working to extract the data...: maxItemCount = " + maxItemCount);

    for(int N = 0 ; N < maxItemCount ; N++) {
      for(int I = 0 ; I < insertedInConsistencyOrder.size() ; I++) {
        float data = insertedInConsistencyOrder.get(I).getPDFNthDataOrMinusOne(N, maxItemCount);
        float PDF = insertedInConsistencyOrder.get(I).getNthPDFOrMinusOne(N);

        combinedPDFStr += data + "\t" + PDF;

        if(I < (insertedInConsistencyOrder.size() - 1))
          combinedPDFStr += "\t";
        else
          combinedPDFStr += "\n";
      }
    }

    try {
      Writer fileWriter = new FileWriter(filePath);
      fileWriter.write(combinedPDFStr);
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
      data_holder.pdfDataListInHistogramMode.add(sortedData);
      data_holder.pdfFrequencyListInHisotgramMode.add(frequency);

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
}
