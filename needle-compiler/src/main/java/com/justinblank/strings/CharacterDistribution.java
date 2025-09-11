package com.justinblank.strings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterDistribution {

    public static final CharacterDistribution ENGLISH;
    public static final CharacterDistribution JSON;
    public static final CharacterDistribution HTML;
    public static final CharacterDistribution DEFAULT;
    public static final CharacterDistribution CODE;

    private final double[] weights;

    static {
        // Based on sherlockholmesascii
        ENGLISH = buildEnglishCharacterDistribution();
        // Based on tweets.js from a twitter archive
        JSON = buildJsonCharacterDistribution();
        // Based on webster's dictionary from the silesia corpus
        HTML = buildHtmlCharacterDistribution();
        // Based on https://github.com/source-foundry/code-corpora
        CODE = buildCodeCharacterDistribution();
        DEFAULT = average(List.of(ENGLISH, JSON, HTML, CODE));
    }

    private static CharacterDistribution buildHtmlCharacterDistribution() {
        double[] weights = new double[128];
        weights[0] = 0.0000;
        weights[1] = 0.0000;
        weights[2] = 0.0000;
        weights[3] = 0.0000;
        weights[4] = 0.0000;
        weights[5] = 0.0000;
        weights[6] = 0.0000;
        weights[7] = 0.0000;
        weights[8] = 0.0000;
        weights[9] = 0.0000;
        weights[10] = 0.0225;
        weights[11] = 0.0000;
        weights[12] = 0.0000;
        weights[13] = 0.0225;
        weights[14] = 0.0000;
        weights[15] = 0.0000;
        weights[16] = 0.0000;
        weights[17] = 0.0000;
        weights[18] = 0.0000;
        weights[19] = 0.0000;
        weights[20] = 0.0000;
        weights[21] = 0.0000;
        weights[22] = 0.0000;
        weights[23] = 0.0000;
        weights[24] = 0.0000;
        weights[25] = 0.0000;
        weights[26] = 0.0000;
        weights[27] = 0.0000;
        weights[28] = 0.0000;
        weights[29] = 0.0000;
        weights[30] = 0.0000;
        weights[31] = 0.0000;
        weights[32] = 0.0930;
        weights[33] = 0.0001;
        weights[34] = 0.0033;
        weights[35] = 0.0001;
        weights[36] = 0.0000;
        weights[37] = 0.0000;
        weights[38] = 0.0025;
        weights[39] = 0.0003;
        weights[40] = 0.0044;
        weights[41] = 0.0044;
        weights[42] = 0.0028;
        weights[43] = 0.0004;
        weights[44] = 0.0115;
        weights[45] = 0.0026;
        weights[46] = 0.0231;
        weights[47] = 0.0361;
        weights[48] = 0.0001;
        weights[49] = 0.0008;
        weights[50] = 0.0007;
        weights[51] = 0.0003;
        weights[52] = 0.0002;
        weights[53] = 0.0001;
        weights[54] = 0.0001;
        weights[55] = 0.0001;
        weights[56] = 0.0001;
        weights[57] = 0.0001;
        weights[58] = 0.0002;
        weights[59] = 0.0071;
        weights[60] = 0.0726;
        weights[61] = 0.0000;
        weights[62] = 0.0726;
        weights[63] = 0.0029;
        weights[64] = 0.0000;
        weights[65] = 0.0023;
        weights[66] = 0.0013;
        weights[67] = 0.0015;
        weights[68] = 0.0007;
        weights[69] = 0.0008;
        weights[70] = 0.0011;
        weights[71] = 0.0008;
        weights[72] = 0.0007;
        weights[73] = 0.0009;
        weights[74] = 0.0002;
        weights[75] = 0.0001;
        weights[76] = 0.0012;
        weights[77] = 0.0009;
        weights[78] = 0.0004;
        weights[79] = 0.0013;
        weights[80] = 0.0010;
        weights[81] = 0.0001;
        weights[82] = 0.0010;
        weights[83] = 0.0028;
        weights[84] = 0.0024;
        weights[85] = 0.0002;
        weights[86] = 0.0002;
        weights[87] = 0.0005;
        weights[88] = 0.0000;
        weights[89] = 0.0001;
        weights[90] = 0.0003;
        weights[91] = 0.0023;
        weights[92] = 0.0000;
        weights[93] = 0.0023;
        weights[94] = 0.0000;
        weights[95] = 0.0000;
        weights[96] = 0.0010;
        weights[97] = 0.0378;
        weights[98] = 0.0142;
        weights[99] = 0.0204;
        weights[100] = 0.0248;
        weights[101] = 0.0632;
        weights[102] = 0.0197;
        weights[103] = 0.0099;
        weights[104] = 0.0236;
        weights[105] = 0.0584;
        weights[106] = 0.0005;
        weights[107] = 0.0054;
        weights[108] = 0.0241;
        weights[109] = 0.0113;
        weights[110] = 0.0366;
        weights[111] = 0.0495;
        weights[112] = 0.0284;
        weights[113] = 0.0026;
        weights[114] = 0.0326;
        weights[115] = 0.0380;
        weights[116] = 0.0380;
        weights[117] = 0.0185;
        weights[118] = 0.0052;
        weights[119] = 0.0119;
        weights[120] = 0.0012;
        weights[121] = 0.0070;
        weights[122] = 0.0005;
        weights[123] = 0.0001;
        weights[124] = 0.0004;
        weights[125] = 0.0001;
        weights[126] = 0.0000;
        weights[127] = 0.0000;
        return new CharacterDistribution(weights);
    }

    private static CharacterDistribution buildEnglishCharacterDistribution() {
        double[] weights = new double[128];
        weights[0] = 0.0000;
        weights[1] = 0.0000;
        weights[2] = 0.0000;
        weights[3] = 0.0000;
        weights[4] = 0.0000;
        weights[5] = 0.0000;
        weights[6] = 0.0000;
        weights[7] = 0.0000;
        weights[8] = 0.0000;
        weights[9] = 0.0000;
        weights[10] = 0.0000;
        weights[11] = 0.0000;
        weights[12] = 0.0000;
        weights[13] = 0.0000;
        weights[14] = 0.0000;
        weights[15] = 0.0000;
        weights[16] = 0.0000;
        weights[17] = 0.0000;
        weights[18] = 0.0000;
        weights[19] = 0.0000;
        weights[20] = 0.0000;
        weights[21] = 0.0000;
        weights[22] = 0.0000;
        weights[23] = 0.0000;
        weights[24] = 0.0000;
        weights[25] = 0.0000;
        weights[26] = 0.0000;
        weights[27] = 0.0000;
        weights[28] = 0.0000;
        weights[29] = 0.0000;
        weights[30] = 0.0000;
        weights[31] = 0.0000;
        weights[32] = 0.1419;
        weights[33] = 0.0000;
        weights[34] = 0.0000;
        weights[35] = 0.0000;
        weights[36] = 0.0000;
        weights[37] = 0.0000;
        weights[38] = 0.0000;
        weights[39] = 0.0000;
        weights[40] = 0.0000;
        weights[41] = 0.0000;
        weights[42] = 0.0000;
        weights[43] = 0.0000;
        weights[44] = 0.0000;
        weights[45] = 0.0000;
        weights[46] = 0.0000;
        weights[47] = 0.0000;
        weights[48] = 0.0000;
        weights[49] = 0.0000;
        weights[50] = 0.0000;
        weights[51] = 0.0000;
        weights[52] = 0.0000;
        weights[53] = 0.0000;
        weights[54] = 0.0000;
        weights[55] = 0.0000;
        weights[56] = 0.0000;
        weights[57] = 0.0000;
        weights[58] = 0.0000;
        weights[59] = 0.0000;
        weights[60] = 0.0000;
        weights[61] = 0.0000;
        weights[62] = 0.0000;
        weights[63] = 0.0000;
        weights[64] = 0.0000;
        weights[65] = 0.0337;
        weights[66] = 0.0000;
        weights[67] = 0.0000;
        weights[68] = 0.0000;
        weights[69] = 0.0000;
        weights[70] = 0.0000;
        weights[71] = 0.0000;
        weights[72] = 0.0000;
        weights[73] = 0.0000;
        weights[74] = 0.0000;
        weights[75] = 0.0000;
        weights[76] = 0.0000;
        weights[77] = 0.0337;
        weights[78] = 0.0000;
        weights[79] = 0.0000;
        weights[80] = 0.0000;
        weights[81] = 0.0000;
        weights[82] = 0.0000;
        weights[83] = 0.0408;
        weights[84] = 0.0000;
        weights[85] = 0.0000;
        weights[86] = 0.0000;
        weights[87] = 0.0338;
        weights[88] = 0.0000;
        weights[89] = 0.0000;
        weights[90] = 0.0000;
        weights[91] = 0.0000;
        weights[92] = 0.0000;
        weights[93] = 0.0000;
        weights[94] = 0.0000;
        weights[95] = 0.0000;
        weights[96] = 0.0000;
        weights[97] = 0.1355;
        weights[98] = 0.0000;
        weights[99] = 0.0034;
        weights[100] = 0.0337;
        weights[101] = 0.0437;
        weights[102] = 0.0000;
        weights[103] = 0.0000;
        weights[104] = 0.0374;
        weights[105] = 0.0337;
        weights[106] = 0.0000;
        weights[107] = 0.0034;
        weights[108] = 0.0371;
        weights[109] = 0.0000;
        weights[110] = 0.0338;
        weights[111] = 0.0709;
        weights[112] = 0.0000;
        weights[113] = 0.0000;
        weights[114] = 0.1419;
        weights[115] = 0.0338;
        weights[116] = 0.0742;
        weights[117] = 0.0000;
        weights[118] = 0.0000;
        weights[119] = 0.0000;
        weights[120] = 0.0000;
        weights[121] = 0.0337;
        weights[122] = 0.0000;
        weights[123] = 0.0000;
        weights[124] = 0.0000;
        weights[125] = 0.0000;
        weights[126] = 0.0000;
        weights[127] = 0.0000;
        return new CharacterDistribution(weights);
    }

    private static CharacterDistribution buildJsonCharacterDistribution() {
        double[] weights = new double[128];
        weights[0] = 0.0000;
        weights[1] = 0.0000;
        weights[2] = 0.0000;
        weights[3] = 0.0000;
        weights[4] = 0.0000;
        weights[5] = 0.0000;
        weights[6] = 0.0000;
        weights[7] = 0.0000;
        weights[8] = 0.0000;
        weights[9] = 0.0000;
        weights[10] = 0.0342;
        weights[11] = 0.0000;
        weights[12] = 0.0000;
        weights[13] = 0.0000;
        weights[14] = 0.0000;
        weights[15] = 0.0000;
        weights[16] = 0.0000;
        weights[17] = 0.0000;
        weights[18] = 0.0000;
        weights[19] = 0.0000;
        weights[20] = 0.0000;
        weights[21] = 0.0000;
        weights[22] = 0.0000;
        weights[23] = 0.0000;
        weights[24] = 0.0000;
        weights[25] = 0.0000;
        weights[26] = 0.0000;
        weights[27] = 0.0000;
        weights[28] = 0.0000;
        weights[29] = 0.0000;
        weights[30] = 0.0000;
        weights[31] = 0.0000;
        weights[32] = 0.3440;
        weights[33] = 0.0001;
        weights[34] = 0.0780;
        weights[35] = 0.0000;
        weights[36] = 0.0000;
        weights[37] = 0.0000;
        weights[38] = 0.0000;
        weights[39] = 0.0004;
        weights[40] = 0.0001;
        weights[41] = 0.0001;
        weights[42] = 0.0000;
        weights[43] = 0.0007;
        weights[44] = 0.0207;
        weights[45] = 0.0017;
        weights[46] = 0.0035;
        weights[47] = 0.0053;
        weights[48] = 0.0165;
        weights[49] = 0.0133;
        weights[50] = 0.0114;
        weights[51] = 0.0086;
        weights[52] = 0.0085;
        weights[53] = 0.0088;
        weights[54] = 0.0077;
        weights[55] = 0.0074;
        weights[56] = 0.0077;
        weights[57] = 0.0073;
        weights[58] = 0.0267;
        weights[59] = 0.0000;
        weights[60] = 0.0014;
        weights[61] = 0.0014;
        weights[62] = 0.0014;
        weights[63] = 0.0001;
        weights[64] = 0.0006;
        weights[65] = 0.0006;
        weights[66] = 0.0002;
        weights[67] = 0.0006;
        weights[68] = 0.0003;
        weights[69] = 0.0016;
        weights[70] = 0.0003;
        weights[71] = 0.0002;
        weights[72] = 0.0002;
        weights[73] = 0.0013;
        weights[74] = 0.0004;
        weights[75] = 0.0001;
        weights[76] = 0.0002;
        weights[77] = 0.0005;
        weights[78] = 0.0003;
        weights[79] = 0.0002;
        weights[80] = 0.0005;
        weights[81] = 0.0001;
        weights[82] = 0.0010;
        weights[83] = 0.0007;
        weights[84] = 0.0027;
        weights[85] = 0.0008;
        weights[86] = 0.0001;
        weights[87] = 0.0007;
        weights[88] = 0.0001;
        weights[89] = 0.0001;
        weights[90] = 0.0008;
        weights[91] = 0.0048;
        weights[92] = 0.0031;
        weights[93] = 0.0048;
        weights[94] = 0.0000;
        weights[95] = 0.0144;
        weights[96] = 0.0000;
        weights[97] = 0.0220;
        weights[98] = 0.0042;
        weights[99] = 0.0089;
        weights[100] = 0.0160;
        weights[101] = 0.0449;
        weights[102] = 0.0083;
        weights[103] = 0.0054;
        weights[104] = 0.0077;
        weights[105] = 0.0300;
        weights[106] = 0.0004;
        weights[107] = 0.0011;
        weights[108] = 0.0184;
        weights[109] = 0.0072;
        weights[110] = 0.0213;
        weights[111] = 0.0181;
        weights[112] = 0.0065;
        weights[113] = 0.0002;
        weights[114] = 0.0208;
        weights[115] = 0.0230;
        weights[116] = 0.0398;
        weights[117] = 0.0105;
        weights[118] = 0.0024;
        weights[119] = 0.0067;
        weights[120] = 0.0020;
        weights[121] = 0.0049;
        weights[122] = 0.0005;
        weights[123] = 0.0044;
        weights[124] = 0.0000;
        weights[125] = 0.0044;
        weights[126] = 0.0000;
        weights[127] = 0.0000;
        return new CharacterDistribution(weights);
    }

    static CharacterDistribution buildCodeCharacterDistribution() {
        double[] weights = new double[128];
        weights[0] = 0.0170;
        weights[1] = 0.0018;
        weights[2] = 0.0013;
        weights[3] = 0.0013;
        weights[4] = 0.0009;
        weights[5] = 0.0008;
        weights[6] = 0.0012;
        weights[7] = 0.0004;
        weights[8] = 0.0009;
        weights[9] = 0.0215;
        weights[10] = 0.0374;
        weights[11] = 0.0002;
        weights[12] = 0.0003;
        weights[13] = 0.0002;
        weights[14] = 0.0003;
        weights[15] = 0.0006;
        weights[16] = 0.0005;
        weights[17] = 0.0003;
        weights[18] = 0.0003;
        weights[19] = 0.0004;
        weights[20] = 0.0003;
        weights[21] = 0.0003;
        weights[22] = 0.0003;
        weights[23] = 0.0002;
        weights[24] = 0.0004;
        weights[25] = 0.0003;
        weights[26] = 0.0002;
        weights[27] = 0.0002;
        weights[28] = 0.0003;
        weights[29] = 0.0003;
        weights[30] = 0.0002;
        weights[31] = 0.0002;
        weights[32] = 0.1494;
        weights[33] = 0.0013;
        weights[34] = 0.0057;
        weights[35] = 0.0013;
        weights[36] = 0.0023;
        weights[37] = 0.0009;
        weights[38] = 0.0017;
        weights[39] = 0.0031;
        weights[40] = 0.0100;
        weights[41] = 0.0099;
        weights[42] = 0.0042;
        weights[43] = 0.0011;
        weights[44] = 0.0112;
        weights[45] = 0.0039;
        weights[46] = 0.0222;
        weights[47] = 0.0078;
        weights[48] = 0.0255;
        weights[49] = 0.0172;
        weights[50] = 0.0109;
        weights[51] = 0.0089;
        weights[52] = 0.0086;
        weights[53] = 0.0079;
        weights[54] = 0.0090;
        weights[55] = 0.0081;
        weights[56] = 0.0100;
        weights[57] = 0.0272;
        weights[58] = 0.0055;
        weights[59] = 0.0042;
        weights[60] = 0.0016;
        weights[61] = 0.0064;
        weights[62] = 0.0026;
        weights[63] = 0.0006;
        weights[64] = 0.0012;
        weights[65] = 0.0048;
        weights[66] = 0.0022;
        weights[67] = 0.0038;
        weights[68] = 0.0031;
        weights[69] = 0.0053;
        weights[70] = 0.0027;
        weights[71] = 0.0014;
        weights[72] = 0.0024;
        weights[73] = 0.0038;
        weights[74] = 0.0004;
        weights[75] = 0.0010;
        weights[76] = 0.0033;
        weights[77] = 0.0029;
        weights[78] = 0.0030;
        weights[79] = 0.0037;
        weights[80] = 0.0031;
        weights[81] = 0.0012;
        weights[82] = 0.0042;
        weights[83] = 0.0050;
        weights[84] = 0.0049;
        weights[85] = 0.0020;
        weights[86] = 0.0019;
        weights[87] = 0.0013;
        weights[88] = 0.0011;
        weights[89] = 0.0009;
        weights[90] = 0.0005;
        weights[91] = 0.0026;
        weights[92] = 0.0026;
        weights[93] = 0.0025;
        weights[94] = 0.0003;
        weights[95] = 0.0103;
        weights[96] = 0.0011;
        weights[97] = 0.0286;
        weights[98] = 0.0077;
        weights[99] = 0.0177;
        weights[100] = 0.0157;
        weights[101] = 0.0510;
        weights[102] = 0.0136;
        weights[103] = 0.0088;
        weights[104] = 0.0097;
        weights[105] = 0.0281;
        weights[106] = 0.0011;
        weights[107] = 0.0036;
        weights[108] = 0.0180;
        weights[109] = 0.0109;
        weights[110] = 0.0281;
        weights[111] = 0.0255;
        weights[112] = 0.0130;
        weights[113] = 0.0014;
        weights[114] = 0.0308;
        weights[115] = 0.0274;
        weights[116] = 0.0401;
        weights[117] = 0.0143;
        weights[118] = 0.0050;
        weights[119] = 0.0042;
        weights[120] = 0.0080;
        weights[121] = 0.0058;
        weights[122] = 0.0012;
        weights[123] = 0.0039;
        weights[124] = 0.0012;
        weights[125] = 0.0039;
        weights[126] = 0.0002;
        weights[127] = 0.0001;
        return new CharacterDistribution(weights);
    }

    public CharacterDistribution(double[] weights) {
        this.weights = weights;
    }

    public static CharacterDistribution average(List<CharacterDistribution> distributions) {
        double[] weights = new double[128];
        for (var distribution : distributions) {
            for (int i = 0; i < 128; i++) {
                weights[i] += distribution.weights[i];
            }
        }
        for (int i = 0; i < 128; i++) {
            weights[i] /= distributions.size();
        }
        return new CharacterDistribution(weights);
    }

    public double weight(boolean[] bytes) {
        double weight = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i]) {
                weight += weights[i];
            }
        }
        return weight;
    }

    private static Map<Integer, Integer> toCharMap(String contents) {
        Map<Integer, Integer> charCount = new HashMap<>();
        for (int i = 0; i < contents.length(); i++) {
            Integer charInt = (int) contents.charAt(i);
            charCount.putIfAbsent(charInt, 0);
            charCount.put(charInt, charCount.get(charInt) + 1);
        }
        return charCount;
    }

    private static Map<Integer, Integer> toCharMap(InputStream contents) throws IOException {
        Map<Integer, Integer> charCount = new HashMap<>();
        int c = 0;
        while (c != -1) {
            c = contents.read();
            charCount.putIfAbsent(c, 0);
            charCount.put(c, charCount.get(c) + 1);
        }
        return charCount;
    }

     private static CharacterDistribution fromMap(Map<Integer, Integer> distribution) {
        int total = 0;
        for (Map.Entry<Integer, Integer> e : distribution.entrySet()) {
            total += e.getValue();
        }
        double[] frequencies = new double[128];
        for (int i = 0; i < 128; i++) {
            Integer count = distribution.get(i);
            if (count == null) {
                count = 0;
            }
            frequencies[i] = count / (double) total;
        }
        return new CharacterDistribution(frequencies);
    }
}
