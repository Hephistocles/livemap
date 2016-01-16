package main.java.testclj;

import storm.trident.operation.CombinerAggregator;
import storm.trident.tuple.TridentTuple;

/**
 * Created by christoph on 12/01/16.
 */
public class AlphaIntensity implements CombinerAggregator<Double> {
    public Double init(TridentTuple tuple) {
        return tuple.getDouble(0);
    }

    public Double combine(Double val1, Double val2) {
        return val1 + val2 - (val2 * val1);
    }

    public Double zero() {
        return 0.0;
    }
}
