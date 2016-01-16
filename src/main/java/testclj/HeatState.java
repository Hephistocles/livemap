package main.java.testclj;

import backtype.storm.tuple.Values;
import storm.trident.operation.BaseAggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.state.BaseQueryFunction;
import storm.trident.testing.MemoryMapState;
import storm.trident.tuple.TridentTuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by christoph on 12/01/16.
 */
public class HeatState extends BaseQueryFunction<MemoryMapState, List<HeatState.MapPoint>> {

    public List<List<HeatState.MapPoint>> batchRetrieve(MemoryMapState state, List<TridentTuple> inputs) {
        List<List<HeatState.MapPoint>> results = new ArrayList<List<HeatState.MapPoint>>();

        // for now assuming we have a single request (should look at inputs otherwise to disambiguate)

        Iterator<List<Object>> ts = state.getTuples();
        List<HeatState.MapPoint> l = new ArrayList<HeatState.MapPoint>();
        while (ts.hasNext()) {
            List<Object> t = ts.next();
            l.add(new HeatState.MapPoint((Integer)t.get(0), (Integer)t.get(1), (Double)t.get(2)));
        }
        results.add(l);
        return results;
    }

    public void execute(TridentTuple tuple, List<HeatState.MapPoint> result, TridentCollector collector) {
        for (HeatState.MapPoint r:result) {
            collector.emit(new Values(r.x, r.y, r.intensity));
        }
    }
    public class MapPoint {
        public int x;
        public int y;
        public double intensity;
        public MapPoint(int x, int y, double intensity) {
            this.x = x;
            this.y = y;
            this.intensity = intensity;
        }
    }

}

