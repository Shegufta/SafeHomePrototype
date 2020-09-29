package Measurement;

import java.util.ArrayList;
import java.util.List;

public class SingleMeasurementResult {
  public MeasurementType res_type;
  public List<Float> results = new ArrayList<>();

  public SingleMeasurementResult(MeasurementType type) {
    res_type = type;
  }

  public SingleMeasurementResult(MeasurementType type, Float value) {
    res_type = type;
    results.add(value);
  }

  public void add_result(Float value) {
    results.add(value);
  }

  public List<Float> get_results() {
    return results;
  }
}
