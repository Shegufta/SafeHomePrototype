package Measurement;

import java.util.*;

public class MeasurementSingleton {
  private Boolean isDisposed;
  private static MeasurementSingleton singleton;
  private Map<MeasurementType, SingleMeasurementResult> all_results = new HashMap<>();

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
}
