package org.mangorage.game.util;

public final class RenderUtil {
    public static float[] rotateUVs(float[] originalUVs, int rotationDegrees) {
        // rotationDegrees must be one of {0, 90, 180, 270}
        float[][] uvPoints = new float[6][2];
        for (int i = 0; i < 6; i++) {
            uvPoints[i][0] = originalUVs[i*2];
            uvPoints[i][1] = originalUVs[i*2 + 1];
        }

        // Function to rotate a single UV point clockwise by 90 degrees
        // UV coords are in [0,1] range. Rotation is about center (0.5, 0.5).
        for (int i = 0; i < 6; i++) {
            float u = uvPoints[i][0] - 0.5f;
            float v = uvPoints[i][1] - 0.5f;
            float rotatedU = 0, rotatedV = 0;

            switch (rotationDegrees) {
                case 0:
                    rotatedU = u; rotatedV = v;
                    break;
                case 90:
                    rotatedU = v;
                    rotatedV = -u;
                    break;
                case 180:
                    rotatedU = -u;
                    rotatedV = -v;
                    break;
                case 270:
                    rotatedU = -v;
                    rotatedV = u;
                    break;
                default:
                    throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270");
            }
            uvPoints[i][0] = rotatedU + 0.5f;
            uvPoints[i][1] = rotatedV + 0.5f;
        }

        float[] rotatedUVs = new float[12];
        for (int i = 0; i < 6; i++) {
            rotatedUVs[i*2] = uvPoints[i][0];
            rotatedUVs[i*2 + 1] = uvPoints[i][1];
        }
        return rotatedUVs;
    }
}
