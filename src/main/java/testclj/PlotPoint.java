package main.java.testclj;

import backtype.storm.tuple.Values;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by christoph on 12/01/16.
 */
public class PlotPoint extends BaseFunction {
    public PlotPoint() {
    }
    public void execute(TridentTuple tuple, TridentCollector collector) {
        double intensity = tuple.getDoubleByField("intensity");
        double smoothing = tuple.getDoubleByField("smoothing");
        int radius = tuple.getIntegerByField("radius");
        ArrayList<Integer> size = new ArrayList<Integer>((Collection) tuple.getValueByField("size"));
        ArrayList<Integer> coords = new ArrayList<Integer>((Collection) tuple.getValueByField("coords"));

        int xp = coords.get(0) - radius;
        int yp = coords.get(1) - radius;

        // TODO cache this or something for common radii
        double[][] it = getIntensity(radius, smoothing);

        for (int x=0; x<it.length; x++) {
            for (int y = 0; y < it[x].length; y++) {
                // note that anything bigger than the size of the current page gets cropped off. Maybe it shouldn't.
                if (x + xp > 0 && y + yp > 0 && x + xp < size.get(0) && y + yp< size.get(1)) {
                    // no point in emitting 0-updates
                    if (intensity * it[x][y] > 0) {
                        collector.emit(new Values(x+xp, y+yp, new Double(intensity*it[x][y])));
                    } else {
//                        System.out.println("Didn't emit a 0 at " + (x+xp) + ", " + (y+yp));
                    }
                }
            }
        }
    }

    private double[][] getIntensity(int radius, double smoothing) {
        double[][] data = new double[radius * 2][radius*2];
        for (int x=0; x<data.length; x++) {
            for (int y=0; y<data[x].length; y++) {
                int x1 = x - radius;
                int y1 = y - radius;
                double d = Math.sqrt(x1*x1 + y1*y1);
                if (d < smoothing*radius) {
                    data[x][y] = 1;
                } else if (d < radius) {
                    data[x][y] = (1 - (d - radius*smoothing) / (radius - smoothing*radius));
                } else {
                    data[x][y] = 0;
                }
            }
        }
        return data;
    }
}
