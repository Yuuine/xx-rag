package yuuine.xxrag.common.util;

import java.util.List;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static float[] toFloatArray(List<Double> doubleList) {
        if (doubleList == null || doubleList.isEmpty()) {
            return new float[0];
        }
        
        float[] vector = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            Double value = doubleList.get(i);
            if (value != null) {
                vector[i] = value.floatValue();
            }
        }
        return vector;
    }

    public static boolean isEmpty(float[] vector) {
        return vector == null || vector.length == 0;
    }
}
