/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.classfile;

/**
 * Known internal version numbers of class files.
 *
 * @author Eric Lafortune
 */
public class VersionConstants {
  public static final int MAGIC = 0xCAFEBABE;

  public static final int CLASS_VERSION_1_0_MAJOR = 45;
  public static final int CLASS_VERSION_1_0_MINOR = 3;
  public static final int CLASS_VERSION_1_2_MAJOR = 46;
  public static final int CLASS_VERSION_1_2_MINOR = 0;
  public static final int CLASS_VERSION_1_3_MAJOR = 47;
  public static final int CLASS_VERSION_1_3_MINOR = 0;
  public static final int CLASS_VERSION_1_4_MAJOR = 48;
  public static final int CLASS_VERSION_1_4_MINOR = 0;
  public static final int CLASS_VERSION_1_5_MAJOR = 49;
  public static final int CLASS_VERSION_1_5_MINOR = 0;
  public static final int CLASS_VERSION_1_6_MAJOR = 50;
  public static final int CLASS_VERSION_1_6_MINOR = 0;
  public static final int CLASS_VERSION_1_7_MAJOR = 51;
  public static final int CLASS_VERSION_1_7_MINOR = 0;
  public static final int CLASS_VERSION_1_8_MAJOR = 52;
  public static final int CLASS_VERSION_1_8_MINOR = 0;
  public static final int CLASS_VERSION_1_9_MAJOR = 53;
  public static final int CLASS_VERSION_1_9_MINOR = 0;
  public static final int CLASS_VERSION_10_MAJOR = 54;
  public static final int CLASS_VERSION_10_MINOR = 0;
  public static final int CLASS_VERSION_11_MAJOR = 55;
  public static final int CLASS_VERSION_11_MINOR = 0;
  public static final int CLASS_VERSION_12_MAJOR = 56;
  public static final int CLASS_VERSION_12_MINOR = 0;
  public static final int CLASS_VERSION_13_MAJOR = 57;
  public static final int CLASS_VERSION_13_MINOR = 0;
  public static final int CLASS_VERSION_14_MAJOR = 58;
  public static final int CLASS_VERSION_14_MINOR = 0;
  public static final int CLASS_VERSION_15_MAJOR = 59;
  public static final int CLASS_VERSION_15_MINOR = 0;
  public static final int CLASS_VERSION_16_MAJOR = 60;
  public static final int CLASS_VERSION_16_MINOR = 0;
  public static final int CLASS_VERSION_17_MAJOR = 61;
  public static final int CLASS_VERSION_17_MINOR = 0;
  public static final int CLASS_VERSION_18_MAJOR = 62;
  public static final int CLASS_VERSION_18_MINOR = 0;
  public static final int CLASS_VERSION_19_MAJOR = 63;
  public static final int CLASS_VERSION_19_MINOR = 0;
  public static final int CLASS_VERSION_20_MAJOR = 64;
  public static final int CLASS_VERSION_20_MINOR = 0;
  public static final int CLASS_VERSION_21_MAJOR = 65;
  public static final int CLASS_VERSION_21_MINOR = 0;
  public static final int CLASS_VERSION_22_MAJOR = 66;
  public static final int CLASS_VERSION_22_MINOR = 0;
  public static final int CLASS_VERSION_23_MAJOR = 67;
  public static final int CLASS_VERSION_23_MINOR = 0;
  public static final int PREVIEW_VERSION_MINOR = 65535;

  public static final int CLASS_VERSION_1_0 =
      (CLASS_VERSION_1_0_MAJOR << 16) | CLASS_VERSION_1_0_MINOR;
  public static final int CLASS_VERSION_1_2 =
      (CLASS_VERSION_1_2_MAJOR << 16) | CLASS_VERSION_1_2_MINOR;
  public static final int CLASS_VERSION_1_3 =
      (CLASS_VERSION_1_3_MAJOR << 16) | CLASS_VERSION_1_3_MINOR;
  public static final int CLASS_VERSION_1_4 =
      (CLASS_VERSION_1_4_MAJOR << 16) | CLASS_VERSION_1_4_MINOR;
  public static final int CLASS_VERSION_1_5 =
      (CLASS_VERSION_1_5_MAJOR << 16) | CLASS_VERSION_1_5_MINOR;
  public static final int CLASS_VERSION_1_6 =
      (CLASS_VERSION_1_6_MAJOR << 16) | CLASS_VERSION_1_6_MINOR;
  public static final int CLASS_VERSION_1_7 =
      (CLASS_VERSION_1_7_MAJOR << 16) | CLASS_VERSION_1_7_MINOR;
  public static final int CLASS_VERSION_1_8 =
      (CLASS_VERSION_1_8_MAJOR << 16) | CLASS_VERSION_1_8_MINOR;
  public static final int CLASS_VERSION_1_9 =
      (CLASS_VERSION_1_9_MAJOR << 16) | CLASS_VERSION_1_9_MINOR;
  public static final int CLASS_VERSION_10 =
      (CLASS_VERSION_10_MAJOR << 16) | CLASS_VERSION_10_MINOR;
  public static final int CLASS_VERSION_11 =
      (CLASS_VERSION_11_MAJOR << 16) | CLASS_VERSION_11_MINOR;
  public static final int CLASS_VERSION_12 =
      (CLASS_VERSION_12_MAJOR << 16) | CLASS_VERSION_12_MINOR;
  public static final int CLASS_VERSION_13 =
      (CLASS_VERSION_13_MAJOR << 16) | CLASS_VERSION_13_MINOR;
  public static final int CLASS_VERSION_14 =
      (CLASS_VERSION_14_MAJOR << 16) | CLASS_VERSION_14_MINOR;
  public static final int CLASS_VERSION_15 =
      (CLASS_VERSION_15_MAJOR << 16) | CLASS_VERSION_15_MINOR;
  public static final int CLASS_VERSION_16 =
      (CLASS_VERSION_16_MAJOR << 16) | CLASS_VERSION_16_MINOR;
  public static final int CLASS_VERSION_17 =
      (CLASS_VERSION_17_MAJOR << 16) | CLASS_VERSION_17_MINOR;
  public static final int CLASS_VERSION_18 =
      (CLASS_VERSION_18_MAJOR << 16) | CLASS_VERSION_18_MINOR;
  public static final int CLASS_VERSION_19 =
      (CLASS_VERSION_19_MAJOR << 16) | CLASS_VERSION_19_MINOR;
  public static final int CLASS_VERSION_20 =
      (CLASS_VERSION_20_MAJOR << 16) | CLASS_VERSION_20_MINOR;
  public static final int CLASS_VERSION_21 =
      (CLASS_VERSION_21_MAJOR << 16) | CLASS_VERSION_21_MINOR;
  public static final int CLASS_VERSION_22 =
      (CLASS_VERSION_22_MAJOR << 16) | CLASS_VERSION_22_MINOR;
  public static final int CLASS_VERSION_23 =
      (CLASS_VERSION_23_MAJOR << 16) | CLASS_VERSION_23_MINOR;

  public static final int MAX_SUPPORTED_VERSION =
      (CLASS_VERSION_23_MAJOR << 16) | PREVIEW_VERSION_MINOR;
}
