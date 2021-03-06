/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hemf.record.emfplus;

import static org.apache.poi.hemf.record.emf.HemfFill.readXForm;
import static org.apache.poi.hemf.record.emfplus.HemfPlusDraw.readARGB;
import static org.apache.poi.hemf.record.emfplus.HemfPlusDraw.readPointF;
import static org.apache.poi.hemf.record.emfplus.HemfPlusDraw.readRectF;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.hemf.record.emfplus.HemfPlusHeader.EmfPlusGraphicsVersion;
import org.apache.poi.hemf.record.emfplus.HemfPlusImage.EmfPlusImage;
import org.apache.poi.hemf.record.emfplus.HemfPlusImage.EmfPlusWrapMode;
import org.apache.poi.hemf.record.emfplus.HemfPlusObject.EmfPlusObjectData;
import org.apache.poi.hemf.record.emfplus.HemfPlusObject.EmfPlusObjectType;
import org.apache.poi.hemf.record.emfplus.HemfPlusPath.EmfPlusPath;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInputStream;

public class HemfPlusBrush {
    /** The BrushType enumeration defines types of graphics brushes, which are used to fill graphics regions. */
    public enum EmfPlusBrushType {
        SOLID_COLOR(0X00000000, EmfPlusSolidBrushData::new),
        HATCH_FILL(0X00000001, EmfPlusHatchBrushData::new),
        TEXTURE_FILL(0X00000002, EmfPlusTextureBrushData::new),
        PATH_GRADIENT(0X00000003, EmfPlusPathGradientBrushData::new),
        LINEAR_GRADIENT(0X00000004, EmfPlusLinearGradientBrushData::new)
        ;

        public final int id;
        public final Supplier<? extends EmfPlusBrushData> constructor;

        EmfPlusBrushType(int id, Supplier<? extends EmfPlusBrushData> constructor) {
            this.id = id;
            this.constructor = constructor;
        }

        public static EmfPlusBrushType valueOf(int id) {
            for (EmfPlusBrushType wrt : values()) {
                if (wrt.id == id) return wrt;
            }
            return null;
        }
    }

    public enum EmfPlusHatchStyle {
        HORIZONTAL(0X00000000),
        VERTICAL(0X00000001),
        FORWARD_DIAGONAL(0X00000002),
        BACKWARD_DIAGONAL(0X00000003),
        LARGE_GRID(0X00000004),
        DIAGONAL_CROSS(0X00000005),
        PERCENT_05(0X00000006),
        PERCENT_10(0X00000007),
        PERCENT_20(0X00000008),
        PERCENT_25(0X00000009),
        PERCENT_30(0X0000000A),
        PERCENT_40(0X0000000B),
        PERCENT_50(0X0000000C),
        PERCENT_60(0X0000000D),
        PERCENT_70(0X0000000E),
        PERCENT_75(0X0000000F),
        PERCENT_80(0X00000010),
        PERCENT_90(0X00000011),
        LIGHT_DOWNWARD_DIAGONAL(0X00000012),
        LIGHT_UPWARD_DIAGONAL(0X00000013),
        DARK_DOWNWARD_DIAGONAL(0X00000014),
        DARK_UPWARD_DIAGONAL(0X00000015),
        WIDE_DOWNWARD_DIAGONAL(0X00000016),
        WIDE_UPWARD_DIAGONAL(0X00000017),
        LIGHT_VERTICAL(0X00000018),
        LIGHT_HORIZONTAL(0X00000019),
        NARROW_VERTICAL(0X0000001A),
        NARROW_HORIZONTAL(0X0000001B),
        DARK_VERTICAL(0X0000001C),
        DARK_HORIZONTAL(0X0000001D),
        DASHED_DOWNWARD_DIAGONAL(0X0000001E),
        DASHED_UPWARD_DIAGONAL(0X0000001F),
        DASHED_HORIZONTAL(0X00000020),
        DASHED_VERTICAL(0X00000021),
        SMALL_CONFETTI(0X00000022),
        LARGE_CONFETTI(0X00000023),
        ZIGZAG(0X00000024),
        WAVE(0X00000025),
        DIAGONAL_BRICK(0X00000026),
        HORIZONTAL_BRICK(0X00000027),
        WEAVE(0X00000028),
        PLAID(0X00000029),
        DIVOT(0X0000002A),
        DOTTED_GRID(0X0000002B),
        DOTTED_DIAMOND(0X0000002C),
        SHINGLE(0X0000002D),
        TRELLIS(0X0000002E),
        SPHERE(0X0000002F),
        SMALL_GRID(0X00000030),
        SMALL_CHECKER_BOARD(0X00000031),
        LARGE_CHECKER_BOARD(0X00000032),
        OUTLINED_DIAMOND(0X00000033),
        SOLID_DIAMOND(0X00000034)
        ;


        public final int id;

        EmfPlusHatchStyle(int id) {
            this.id = id;
        }

        public static EmfPlusHatchStyle valueOf(int id) {
            for (EmfPlusHatchStyle wrt : values()) {
                if (wrt.id == id) return wrt;
            }
            return null;
        }

    }

    public interface EmfPlusBrushData {
        /**
         * This flag is meaningful in EmfPlusPathGradientBrushData objects.
         *
         * If set, an EmfPlusBoundaryPathData object MUST be specified in the BoundaryData field of the brush data object.
         * If clear, an EmfPlusBoundaryPointData object MUST be specified in the BoundaryData field of the brush data object.
         */
        BitField PATH = BitFieldFactory.getInstance(0x00000001);

        /**
         * This flag is meaningful in EmfPlusLinearGradientBrushData objects , EmfPlusPathGradientBrushData objects,
         * and EmfPlusTextureBrushData objects.
         *
         * If set, a 2x3 world space to device space transform matrix MUST be specified in the OptionalData field of
         * the brush data object.
         */
        BitField TRANSFORM = BitFieldFactory.getInstance(0x00000002);

        /**
         * This flag is meaningful in EmfPlusLinearGradientBrushData and EmfPlusPathGradientBrushData objects.
         *
         * If set, an EmfPlusBlendColors object MUST be specified in the OptionalData field of the brush data object.
         */
        BitField PRESET_COLORS = BitFieldFactory.getInstance(0x00000004);

        /**
         * This flag is meaningful in EmfPlusLinearGradientBrushData and EmfPlusPathGradientBrushData objects.
         *
         * If set, an EmfPlusBlendFactors object that specifies a blend pattern along a horizontal gradient MUST be
         * specified in the OptionalData field of the brush data object.
         */
        BitField BLEND_FACTORS_H = BitFieldFactory.getInstance(0x00000008);

        /**
         * This flag is meaningful in EmfPlusLinearGradientBrushData objects.
         *
         * If set, an EmfPlusBlendFactors object that specifies a blend pattern along a vertical gradient MUST be
         * specified in the OptionalData field of the brush data object.
         */
        BitField BLEND_FACTORS_V = BitFieldFactory.getInstance(0x00000010);

        /**
         * This flag is meaningful in EmfPlusPathGradientBrushData objects.
         *
         * If set, an EmfPlusFocusScaleData object MUST be specified in the OptionalData field of the brush data object.
         */
        BitField FOCUS_SCALES = BitFieldFactory.getInstance(0x00000040);

        /**
         * This flag is meaningful in EmfPlusLinearGradientBrushData, EmfPlusPathGradientBrushData, and
         * EmfPlusTextureBrushData objects.
         *
         * If set, the brush MUST already be gamma corrected; that is, output brightness and intensity have been
         * corrected to match the input image.
         */
        BitField IS_GAMMA_CORRECTED = BitFieldFactory.getInstance(0x00000080);

        /**
         * This flag is meaningful in EmfPlusTextureBrushData objects.
         *
         * If set, a world space to device space transform SHOULD NOT be applied to the texture brush.
         */
        BitField DO_NOT_TRANSFORM = BitFieldFactory.getInstance(0x00000100);

        long init(LittleEndianInputStream leis, long dataSize) throws IOException;
    }

    /** The EmfPlusBrush object specifies a graphics brush for filling regions. */
    public static class EmfPlusBrush implements EmfPlusObjectData {
        private final EmfPlusGraphicsVersion version = new EmfPlusGraphicsVersion();
        private EmfPlusBrushType brushType;
        private EmfPlusBrushData brushData;

        @Override
        public long init(LittleEndianInputStream leis, long dataSize, EmfPlusObjectType objectType, int flags) throws IOException {
            long size = version.init(leis);

            brushType = EmfPlusBrushType.valueOf(leis.readInt());
            size += LittleEndianConsts.INT_SIZE;
            assert(brushType != null);

            size += (brushData = brushType.constructor.get()).init(leis, dataSize-size);

            return size;
        }
    }

    /** The EmfPlusSolidBrushData object specifies a solid color for a graphics brush. */
    public static class EmfPlusSolidBrushData implements EmfPlusBrushData {
        private Color solidColor;
        @Override
        public long init(LittleEndianInputStream leis, long dataSize) throws IOException {
            solidColor = readARGB(leis.readInt());
            return LittleEndianConsts.INT_SIZE;
        }
    }


    /** The EmfPlusHatchBrushData object specifies a hatch pattern for a graphics brush. */
    public static class EmfPlusHatchBrushData implements EmfPlusBrushData {
        private EmfPlusHatchStyle style;
        private Color foreColor, backColor;
        public long init(LittleEndianInputStream leis, long dataSize) {
            style = EmfPlusHatchStyle.valueOf(leis.readInt());
            foreColor = readARGB(leis.readInt());
            backColor = readARGB(leis.readInt());
            return 3*LittleEndianConsts.INT_SIZE;
        }
    }

    /** The EmfPlusLinearGradientBrushData object specifies a linear gradient for a graphics brush. */
    public static class EmfPlusLinearGradientBrushData implements EmfPlusBrushData {
        private int dataFlags;
        private EmfPlusWrapMode wrapMode;
        private Rectangle2D rect = new Rectangle2D.Double();
        private Color startColor, endColor;
        private AffineTransform transform;
        private double[] positions;
        private Color[] blendColors;
        private double[] positionsV;
        private double[] blendFactorsV;
        private double[] positionsH;
        private double[] blendFactorsH;

        @Override
        public long init(LittleEndianInputStream leis, long dataSize) throws IOException {
            // A 32-bit unsigned integer that specifies the data in the OptionalData field.
            // This value MUST be composed of BrushData flags
            dataFlags = leis.readInt();

            // A 32-bit signed integer from the WrapMode enumeration that specifies whether to paint the area outside
            // the boundary of the brush. When painting outside the boundary, the wrap mode specifies how the color
            // gradient is repeated.
            wrapMode = EmfPlusWrapMode.valueOf(leis.readInt());

            int size = 2*LittleEndianConsts.INT_SIZE;
            size += readRectF(leis, rect);

            // An EmfPlusARGB object that specifies the color at the starting/ending boundary point of the linear gradient brush.
            startColor = readARGB(leis.readInt());
            endColor = readARGB(leis.readInt());

            // skip reserved1/2 fields
            leis.skipFully(2*LittleEndianConsts.INT_SIZE);

            size += 4*LittleEndianConsts.INT_SIZE;

            if (TRANSFORM.isSet(dataFlags)) {
                size += readXForm(leis, (transform = new AffineTransform()));
            }

            final boolean isPreset = PRESET_COLORS.isSet(dataFlags);
            final boolean blendH = BLEND_FACTORS_H.isSet(dataFlags);
            final boolean blendV = BLEND_FACTORS_V.isSet(dataFlags);
            if (isPreset && (blendH || blendV)) {
                throw new RuntimeException("invalid combination of preset colors and blend factors v/h");
            }

            size += (isPreset) ? readColors(leis, d -> positions = d, c -> blendColors = c) : 0;
            size += (blendV) ? readFactors(leis, d -> positionsV = d, f -> blendFactorsV = f) : 0;
            size += (blendH) ? readFactors(leis, d -> positionsH = d, f -> blendFactorsH = f) : 0;

            return size;
        }
    }

    /** The EmfPlusPathGradientBrushData object specifies a path gradient for a graphics brush. */
    public static class EmfPlusPathGradientBrushData implements EmfPlusBrushData {
        private int dataFlags;
        private EmfPlusWrapMode wrapMode;
        private Color centerColor;
        private final Point2D centerPoint = new Point2D.Double();
        private Color[] surroundingColor;
        private EmfPlusPath boundaryPath;
        private Point2D[] boundaryPoints;
        private AffineTransform transform;
        private double[] positions;
        private Color[] blendColors;
        private double[] blendFactorsH;
        private Double focusScaleX, focusScaleY;

        @Override
        public long init(LittleEndianInputStream leis, long dataSize) throws IOException {
            // A 32-bit unsigned integer that specifies the data in the OptionalData field.
            // This value MUST be composed of BrushData flags
            dataFlags = leis.readInt();

            // A 32-bit signed integer from the WrapMode enumeration that specifies whether to paint the area outside
            // the boundary of the brush. When painting outside the boundary, the wrap mode specifies how the color
            // gradient is repeated.
            wrapMode = EmfPlusWrapMode.valueOf(leis.readInt());

            // An EmfPlusARGB object that specifies the center color of the path gradient brush, which is the color
            // that appears at the center point of the brush. The color of the brush changes gradually from the
            // boundary color to the center color as it moves from the boundary to the center point.
            centerColor = readARGB(leis.readInt());

            int size = 3*LittleEndianConsts.INT_SIZE;
            size += readPointF(leis, centerPoint);

            // An unsigned 32-bit integer that specifies the number of colors specified in the SurroundingColor field.
            // The surrounding colors are colors specified for discrete points on the boundary of the brush.
            final int colorCount = leis.readInt();

            // An array of SurroundingColorCount EmfPlusARGB objects that specify the colors for discrete points on the
            // boundary of the brush.
            surroundingColor = new Color[colorCount];
            for (int i=0; i<colorCount; i++) {
                surroundingColor[i] = readARGB(leis.readInt());
            }
            size += (colorCount+1) * LittleEndianConsts.INT_SIZE;

            // The boundary of the path gradient brush, which is specified by either a path or a closed cardinal spline.
            // If the BrushDataPath flag is set in the BrushDataFlags field, this field MUST contain an
            // EmfPlusBoundaryPathData object; otherwise, this field MUST contain an EmfPlusBoundaryPointData object.
            if (PATH.isSet(dataFlags)) {
                // A 32-bit signed integer that specifies the size in bytes of the BoundaryPathData field.
                int pathDataSize = leis.readInt();
                size += LittleEndianConsts.INT_SIZE;

                // An EmfPlusPath object that specifies the boundary of the brush.
                size += (boundaryPath = new EmfPlusPath()).init(leis, pathDataSize, EmfPlusObjectType.PATH, 0);
            } else {
                // A 32-bit signed integer that specifies the number of points in the BoundaryPointData field.
                int pointCount = leis.readInt();
                size += LittleEndianConsts.INT_SIZE;

                // An array of BoundaryPointCount EmfPlusPointF objects that specify the boundary of the brush.
                boundaryPoints = new Point2D[pointCount];
                for (int i=0; i<pointCount; i++) {
                    size += readPointF(leis, boundaryPoints[i] = new Point2D.Double());
                }
            }

            // An optional EmfPlusTransformMatrix object that specifies a world space to device space transform for
            // the path gradient brush. This field MUST be present if the BrushDataTransform flag is set in the
            // BrushDataFlags field of the EmfPlusPathGradientBrushData object.
            if (TRANSFORM.isSet(dataFlags)) {
                size += readXForm(leis, (transform = new AffineTransform()));
            }

            // An optional blend pattern for the path gradient brush. If this field is present, it MUST contain either
            // an EmfPlusBlendColors object, or an EmfPlusBlendFactors object, but it MUST NOT contain both.
            final boolean isPreset = PRESET_COLORS.isSet(dataFlags);
            final boolean blendH = BLEND_FACTORS_H.isSet(dataFlags);
            if (isPreset && blendH) {
                throw new RuntimeException("invalid combination of preset colors and blend factors h");
            }

            size += (isPreset) ? readColors(leis, d -> positions = d, c -> blendColors = c) : 0;
            size += (blendH) ? readFactors(leis, d -> positions = d, f -> blendFactorsH = f) : 0;

            // An optional EmfPlusFocusScaleData object that specifies focus scales for the path gradient brush.
            // This field MUST be present if the BrushDataFocusScales flag is set in the BrushDataFlags field of the
            // EmfPlusPathGradientBrushData object.
            if (FOCUS_SCALES.isSet(dataFlags)) {
                // A 32-bit unsigned integer that specifies the number of focus scales. This value MUST be 2.
                int focusScaleCount = leis.readInt();
                if (focusScaleCount != 2) {
                    throw new RuntimeException("invalid focus scale count");
                }
                // A floating-point value that defines the horizontal/vertical focus scale.
                // The focus scale MUST be a value between 0.0 and 1.0, exclusive.
                focusScaleX = (double)leis.readFloat();
                focusScaleY = (double)leis.readFloat();
                size += 3*LittleEndianConsts.INT_SIZE;
            }

            return size;
        }
    }

    /** The EmfPlusTextureBrushData object specifies a texture image for a graphics brush. */
    public static class EmfPlusTextureBrushData implements EmfPlusBrushData {
        private int dataFlags;
        private EmfPlusWrapMode wrapMode;
        private AffineTransform transform;
        private EmfPlusImage image;

        @Override
        public long init(LittleEndianInputStream leis, long dataSize) throws IOException {
            // A 32-bit unsigned integer that specifies the data in the OptionalData field.
            // This value MUST be composed of BrushData flags.
            dataFlags = leis.readInt();

            // A 32-bit signed integer from the WrapMode enumeration that specifies how to repeat the texture image
            // across a shape, when the image is smaller than the area being filled.
            wrapMode = EmfPlusWrapMode.valueOf(leis.readInt());

            int size = 2*LittleEndianConsts.INT_SIZE;

            if (TRANSFORM.isSet(dataFlags)) {
                size += readXForm(leis, (transform = new AffineTransform()));
            }

            if (dataSize > size) {
                size += (image = new EmfPlusImage()).init(leis, dataSize-size, EmfPlusObjectType.IMAGE, 0);
            }

            return size;
        }
    }

    private static int readPositions(LittleEndianInputStream leis, Consumer<double[]> pos) {
        final int count = leis.readInt();
        int size = LittleEndianConsts.INT_SIZE;

        double[] positions = new double[count];
        for (int i=0; i<count; i++) {
            positions[i] = leis.readFloat();
            size += LittleEndianConsts.INT_SIZE;
        }

        pos.accept(positions);
        return size;
    }

    private static int readColors(LittleEndianInputStream leis, Consumer<double[]> pos, Consumer<Color[]>  cols) {
        int[] count = { 0 };
        int size = readPositions(leis, p -> { count[0] = p.length; pos.accept(p); });
        Color[] colors = new Color[count[0]];
        for (int i=0; i<colors.length; i++) {
            colors[i] = readARGB(leis.readInt());
        }
        cols.accept(colors);
        return size + colors.length * LittleEndianConsts.INT_SIZE;
    }

    private static int readFactors(LittleEndianInputStream leis, Consumer<double[]> pos, Consumer<double[]> facs) {
        int[] count = { 0 };
        int size = readPositions(leis, p -> { count[0] = p.length; pos.accept(p); });
        double[] factors = new double[count[0]];
        for (int i=0; i<factors.length; i++) {
            factors[i] = leis.readFloat();
        }
        facs.accept(factors);
        return size + factors.length * LittleEndianConsts.INT_SIZE;
    }
}
