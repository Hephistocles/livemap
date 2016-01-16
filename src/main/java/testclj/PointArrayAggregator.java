package main.java.testclj;

import backtype.storm.tuple.Values;
import org.apache.storm.shade.com.twitter.chill.Base64;
import storm.trident.operation.BaseAggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by christoph on 15/01/16.
 */
public class PointArrayAggregator extends BaseAggregator<PointArrayAggregator.PointArray> {
    public class PointArray {
//        double[] arr = null;
        BufferedImage img = null;
    }

    public PointArray init(Object batchId, TridentCollector collector) {
        return new PointArray();
    }

    public void aggregate(PointArray val, TridentTuple tuple, TridentCollector collector) {
        if (val.img == null) {
            // assume this is the first tuple, and resize the array appropriately
//            val.arr = new double[4 * tuple.getIntegerByField("sizex") * tuple.getIntegerByField("sizey")];
            val.img = new BufferedImage(tuple.getIntegerByField("sizex"), tuple.getIntegerByField("sizey"), BufferedImage.TYPE_INT_ARGB);
        }

        val.img.setRGB(((Number)tuple.getValueByField("x")).intValue(),
                ((Number)tuple.getValueByField("y")).intValue(),
                (((Number)tuple.getValueByField("a")).intValue()<< 24) |
                (((Number)tuple.getValueByField("r")).intValue() << 16 ) |
                (((Number)tuple.getValueByField("g")).intValue()<< 8) |
                ((Number)tuple.getValueByField("b")).intValue());
//        val.arr[tuple.getLongByField("pos").intValue()] = ((Number)tuple.getValueByField("val")).doubleValue();
    }

    public void complete(PointArray val, TridentCollector collector) {
        if (val == null || val.img == null) {
            collector.emit(new Values(""));
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Base64.OutputStream b64 = new Base64.OutputStream(os);
        try {
            ImageIO.write(val.img, "png", b64);
            collector.emit(new Values(os.toString("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        raster.setPixels(0,0,width,height,pixels);
//        collector.emit(new Values(Arrays.toString(val.arr)));
    }
}
