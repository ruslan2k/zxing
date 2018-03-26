/*
 * Copyright 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

import java.util.Arrays;
import java.util.Map;

import java.util.List;
import java.util.ArrayList;

//import com.mashape.unirest.http.HttpResponse;
//import com.mashape.unirest.http.JsonNode;
//import com.mashape.unirest.http.Unirest;
//import com.mashape.unirest.http.exceptions.UnirestException;
//import org.json.JSONArray;
//import org.json.JSONObject;


/**
 * <p>Decodes PharmaCode</p>
 *
 * @author Ruslan Karachun
 * @see PharmaCodeReader
 */
public final class PharmaCodeReader extends OneDReader {

  // Note that 'abcd' are dummy characters in place of control characters.
  static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*";
  private static final char[] ALPHABET = ALPHABET_STRING.toCharArray();

  /**
   * These represent the encodings of characters, as patterns of wide and narrow bars.
   * The 9 least-significant bits of each int correspond to the pattern of wide and narrow.
   */
  static final int[] CHARACTER_ENCODINGS = {
      0x114, 0x148, 0x144, 0x142, 0x128, 0x124, 0x122, 0x150, 0x112, 0x10A, // 0-9
      0x1A8, 0x1A4, 0x1A2, 0x194, 0x192, 0x18A, 0x168, 0x164, 0x162, 0x134, // A-J
      0x11A, 0x158, 0x14C, 0x146, 0x12C, 0x116, 0x1B4, 0x1B2, 0x1AC, 0x1A6, // K-T
      0x196, 0x19A, 0x16C, 0x166, 0x136, 0x13A, // U-Z
      0x12E, 0x1D4, 0x1D2, 0x1CA, 0x16E, 0x176, 0x1AE, // - - %
      0x126, 0x1DA, 0x1D6, 0x132, 0x15E, // Control chars? $-*
  };
  private static final int ASTERISK_ENCODING = CHARACTER_ENCODINGS[47];

  private final StringBuilder decodeRowResult;
  private final int[] counters;

  public PharmaCodeReader() {
    decodeRowResult = new StringBuilder(20);
    counters = new int[6];
  }

  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws NotFoundException, ChecksumException, FormatException {
    //try {
    //  Unirest.post("http://dev.aptinfo.net:8080")
    //    .field("key", "value")
    //    .asJson();
    //}
    //catch (Exception e) {
    //  System.out.println("Exception occurred");
    //}
    class PixelInterval {
      private boolean color;
      private int length = 0;
      private int similar = 0;
      private int small_cnt = 0;
      private int large_cnt = 0;

      public PixelInterval (boolean c, int l) {
        color = c;
        length = l;
      }

      public boolean getColor() {
        return color;
      }

      public int getLength () {
        return length;
      }

      public void incSimilar () {
        similar ++;
      }

      public void incSmall () {
        small_cnt ++;
      }

      public void incLarge () {
        large_cnt ++;
      }
    }

    List<PixelInterval> gaps = new ArrayList<PixelInterval>();

    boolean color = row.get(0);
    boolean isBlack = true;
    boolean isWhite = false;
    int end = row.getSize();
    int num = 0;

    for (int i=0; i<end; i++) {
      boolean currentColor = row.get(i);
      if (currentColor == color) {
        num ++;
      } else {
        gaps.add(new PixelInterval(color, num));
        color = currentColor;
        num = 1;
      }
    }
    gaps.add(new PixelInterval(color, num));

    int gaps_length = gaps.size();
    for (int i=0; i<gaps_length; i++) {
      PixelInterval primary = gaps.get(i);
      boolean p_color = primary.getColor();
      int p_num = primary.getLength();    // количество пикселей
      for (int j=0; j<gaps_length; j++) {
        if (i == j) { continue; }
        int s_num = gaps.get(j).getLength();
        boolean s_color = gaps.get(j).getColor();
        double multiplier = (p_num > s_num) ? ((double)p_num / s_num) : ((double)s_num / p_num);
        System.out.println("multiplier: " + multiplier);
        if ((p_color == isWhite) && (s_color == isWhite)) { // WHITE WHITE
          if (multiplier <= 1.2222) {
            primary.incSimilar();
          } else {
            System.out.println("NOT SIMILAR");
          }
        } else if ((p_color == isWhite) && (s_color == isBlack)) {  // WHITE BLACK
          if ((multiplier > 1.5) && (multiplier < 3.6667) && (p_num > s_num)) {
            // White and small black
            primary.incSimilar();
          } else if ((multiplier > 1.2727) && (multiplier < 2.7778) && (p_num < s_num)) {
            // White and large black
            primary.incSimilar();
          } else {
            System.out.println("NOT SIMILAR");
          }
        } else if ((p_color == isBlack) && (s_color == isWhite)) {  // BLACK WHITE
          if ((multiplier > 1.5) && (multiplier < 3.6667) && (p_num < s_num)) {
            // Small black and white
            primary.incSimilar();
            primary.incSmall();
          } else if ((multiplier > 1.2727) && (multiplier < 2.7778) && (p_num > s_num)) {
            // large black and white
            primary.incSimilar();
            primary.incLarge();
          } else {
            System.out.println("NOT SIMILAR");
          }
        } else if ((p_color == isBlack) && (s_color == isBlack)) {
          if ((multiplier > 2.3333) && (multiplier < 4.6667)) {
            primary.incSimilar();
            if (p_num > s_num) {
              primary.incLarge();
            } else {
              primary.incSmall();
            }
          } else if (multiplier < 2) {
            primary.incSimilar();
          } else {
            System.out.println("NOT SIMILAR");
          }
        } else {
          System.out.println("UNKNOWN COLORS");
        }
      }     // j
    }   // i

    boolean b = false;
    int[] start = findAsteriskPattern(row);
    // Read off white space
    int nextStart = row.getNextSet(start[1]);

    System.out.print("["+ end +"]");
    for (int i=0; i<end; i++) {
        System.out.print(" row["+ i +"]="+ row.get(i) +" ");
    }

    int[] theCounters = counters;
    Arrays.fill(theCounters, 0);
    StringBuilder result = decodeRowResult;
    result.setLength(0);

    char decodedChar;
    int lastStart;
    do {
      recordPattern(row, nextStart, theCounters);
      int pattern = toPattern(theCounters);
      if (pattern < 0) {
        throw NotFoundException.getNotFoundInstance();
      }
      decodedChar = patternToChar(pattern);
      result.append(decodedChar);
      lastStart = nextStart;
      for (int counter : theCounters) {
        nextStart += counter;
      }
      // Read off white space
      nextStart = row.getNextSet(nextStart);
    } while (decodedChar != '*');
    result.deleteCharAt(result.length() - 1); // remove asterisk

    int lastPatternSize = 0;
    for (int counter : theCounters) {
      lastPatternSize += counter;
    }

    // Should be at least one more black module
    if (nextStart == end || !row.get(nextStart)) {
      throw NotFoundException.getNotFoundInstance();
    }

    if (result.length() < 2) {
      // false positive -- need at least 2 checksum digits
      throw NotFoundException.getNotFoundInstance();
    }

    checkChecksums(result);
    // Remove checksum digits
    result.setLength(result.length() - 2);

    String resultString = decodeExtended(result);

    float left = (start[1] + start[0]) / 2.0f;
    float right = lastStart + lastPatternSize / 2.0f;
    return new Result(
        resultString,
        null,
        new ResultPoint[]{
            new ResultPoint(left, rowNumber),
            new ResultPoint(right, rowNumber)
        },
        BarcodeFormat.PHARMA_CODE
    );

  }

  private int[] findAsteriskPattern(BitArray row) throws NotFoundException {
    int width = row.getSize();
    int rowOffset = row.getNextSet(0);

    Arrays.fill(counters, 0);
    int[] theCounters = counters;
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = theCounters.length;

    int counterPosition = 0;
    for (int i = rowOffset; i < width; i++) {
      if (row.get(i) != isWhite) {
        theCounters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (toPattern(theCounters) == ASTERISK_ENCODING) {
            return new int[]{patternStart, i};
          }
          patternStart += theCounters[0] + theCounters[1];
          System.arraycopy(theCounters, 2, theCounters, 0, counterPosition - 1);
          theCounters[counterPosition - 1] = 0;
          theCounters[counterPosition] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        theCounters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static int toPattern(int[] counters) {
    int sum = 0;
    for (int counter : counters) {
      sum += counter;
    }
    int pattern = 0;
    int max = counters.length;
    for (int i = 0; i < max; i++) {
      int scaled = Math.round(counters[i] * 9.0f / sum);
      if (scaled < 1 || scaled > 4) {
        return -1;
      }
      if ((i & 0x01) == 0) {
        for (int j = 0; j < scaled; j++) {
          pattern = (pattern << 1) | 0x01;
        }
      } else {
        pattern <<= scaled;
      }
    }
    return pattern;
  }

  private static char patternToChar(int pattern) throws NotFoundException {
    for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
      if (CHARACTER_ENCODINGS[i] == pattern) {
        return ALPHABET[i];
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static String decodeExtended(CharSequence encoded) throws FormatException {
    int length = encoded.length();
    StringBuilder decoded = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = encoded.charAt(i);
      if (c >= 'a' && c <= 'd') {
        if (i >= length - 1) {
          throw FormatException.getFormatInstance();
        }
        char next = encoded.charAt(i + 1);
        char decodedChar = '\0';
        switch (c) {
          case 'd':
            // +A to +Z map to a to z
            if (next >= 'A' && next <= 'Z') {
              decodedChar = (char) (next + 32);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'a':
            // $A to $Z map to control codes SH to SB
            if (next >= 'A' && next <= 'Z') {
              decodedChar = (char) (next - 64);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'b':
            if (next >= 'A' && next <= 'E') {
              // %A to %E map to control codes ESC to USep
              decodedChar = (char) (next - 38);
            } else if (next >= 'F' && next <= 'J') {
              // %F to %J map to ; < = > ?
              decodedChar = (char) (next - 11);
            } else if (next >= 'K' && next <= 'O') {
              // %K to %O map to [ \ ] ^ _
              decodedChar = (char) (next + 16);
            } else if (next >= 'P' && next <= 'S') {
              // %P to %S map to { | } ~
              decodedChar = (char) (next + 43);
            } else if (next >= 'T' && next <= 'Z') {
              // %T to %Z all map to DEL (127)
              decodedChar = 127;
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'c':
            // /A to /O map to ! to , and /Z maps to :
            if (next >= 'A' && next <= 'O') {
              decodedChar = (char) (next - 32);
            } else if (next == 'Z') {
              decodedChar = ':';
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
        }
        decoded.append(decodedChar);
        // bump up i again since we read two characters
        i++;
      } else {
        decoded.append(c);
      }
    }
    return decoded.toString();
  }

  private static void checkChecksums(CharSequence result) throws ChecksumException {
    int length = result.length();
    checkOneChecksum(result, length - 2, 20);
    checkOneChecksum(result, length - 1, 15);
  }

  private static void checkOneChecksum(CharSequence result, int checkPosition, int weightMax)
      throws ChecksumException {
    int weight = 1;
    int total = 0;
    for (int i = checkPosition - 1; i >= 0; i--) {
      total += weight * ALPHABET_STRING.indexOf(result.charAt(i));
      if (++weight > weightMax) {
        weight = 1;
      }
    }
    if (result.charAt(checkPosition) != ALPHABET[total % 47]) {
      throw ChecksumException.getChecksumInstance();
    }
  }

}
