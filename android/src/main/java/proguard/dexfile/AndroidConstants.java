/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.dexfile;

/**
 * Android-related constants.
 *
 * @author Eric Lafortune
 */
public class AndroidConstants
{
    // Android API versions.
    public static final int ANDROID_B     =  1; // Android 1.0, Base.
    public static final int ANDROID_B_1_1 =  2; // Android 1.1.
    public static final int ANDROID_C     =  3; // Android 1.2, Cupcake.
    public static final int ANDROID_D     =  4; // Android 1.3, Donut.
    public static final int ANDROID_E     =  5; // Android 2.0, Eclair.
    public static final int ANDROID_E_0_1 =  6;
    public static final int ANDROID_E_MR1 =  7;
    public static final int ANDROID_F     =  8; // Android 2.2, Froyo.
    public static final int ANDROID_G     =  9; // Android 2.3, Gingerbread.
    public static final int ANDROID_G_MR1 = 10;
    public static final int ANDROID_H     = 11; // Android 3.0, Honeycomb.
    public static final int ANDROID_H_MR1 = 12;
    public static final int ANDROID_H_MR2 = 13;
    public static final int ANDROID_I     = 14; // Android 4.0, Ice Cream Sandwich.
    public static final int ANDROID_I_MR1 = 15;
    public static final int ANDROID_J     = 16; // Android 4.1, Jelly Bean.
    public static final int ANDROID_J_MR1 = 17;
    public static final int ANDROID_J_MR2 = 18;
    public static final int ANDROID_K     = 19; // Android 4.4, KitKat.
    public static final int ANDROID_K_MR1 = 20;
    public static final int ANDROID_L     = 21; // Android 5.0, Lollipop.
    public static final int ANDROID_L_MR1 = 22;
    public static final int ANDROID_M     = 23; // Android 6.0, Marshmallow.
    public static final int ANDROID_N     = 24; // Android 7.0, Nougat.
    public static final int ANDROID_N_MR1 = 25;
    public static final int ANDROID_O     = 26; // Android 8.0, Oreo.
    public static final int ANDROID_O_MR1 = 27;
    public static final int ANDROID_P     = 28; // Android 9.0.
    public static final int ANDROID_Q     = 29; // Android 10.0.
    public static final int ANDROID_R     = 30; // Android 11.0.

    public static final String ZIP_META_INF_EXPRESSION = "META-INF/**";

    // Apk files.
    public static final String APK_FILE_EXTENSION = ".apk";

    public static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
    public static final String RESOURCES_ARSC       = "resources.arsc";
    public static final String CLASSES_PREFIX       = "classes";
    public static final String DEX_FILE_EXTENSION   = ".dex";
    public static final String CLASSES_DEX          = "classes.dex";

    public static final String APK_ASSETS_PREFIX    = "assets/";
    public static final String APK_RES_PREFIX       = "res/";
    public static final String APK_LIB_PREFIX       = "lib/";

    public static final String APK_CLASSES_DEX_EXPRESSION  = "classes*.dex";
    public static final String APK_ASSETS_FILE_EXPRESSION  = "assets/**";
    public static final String APK_RES_FILE_EXPRESSION     = "res/**";
    public static final String APK_RES_XML_FILE_EXPRESSION = "!res/raw*/*,res/**.xml";
    public static final String APK_LIB_FILE_EXPRESSION     = "lib/**.so";
    public static final String APK_DYLIB_FILE_EXPRESSION   = "lib/**.dylib";
    public static final String APK_XML_FILE_EXPRESSION     = ANDROID_MANIFEST_XML + ',' +
                                                             APK_RES_XML_FILE_EXPRESSION;

    // Obb files.
    public static final String OBB_FILE_EXTENSION = ".obb";

    // Aab files.
    public static final String AAB_FILE_EXTENSION = ".aab";

    public static final String RESOURCES_PB             = "resources.pb";
    public static final String ASSETS_PB                = "assets.pb";
    public static final String NATIVE_PB                = "native.pb";
    public static final String BUNDLE_CONFIG_PB         = "BundleConfig.pb";
    public static final String PROGUARD_MAP             = "proguard.map";

    public static final String AAB_BASE                 = "base";
    public static final String AAB_BASE_PREFIX          = "base/";
    public static final String AAB_ROOT                 = "root";
    public static final String AAB_ROOT_PREFIX          = "root/";
    public static final String AAB_BASE_MANIFEST_PREFIX = "base/manifest/";
    public static final String AAB_BASE_RES_PREFIX      = "base/res/";
    public static final String AAB_BASE_DEX_PREFIX      = "base/dex/";
    public static final String AAB_BASE_ROOT_PREFIX     = "base/root/";
    public static final String AAB_METADATA_PREFIX      = "BUNDLE-METADATA/";
    public static final String AAB_OBFUSCATION_PREFIX   = "BUNDLE-METADATA/com.android.tools.build.obfuscation/";
    public static final String AAB_BUNDLETOOL_PREFIX    = "BUNDLE-METADATA/com.android.tools.build.bundletool/";
    public static final String MAIN_DEX_LIST_TXT        = "mainDexList.txt";

    public static final String AAB_ANDROID_MANIFEST_XML_SUFFIX= "/manifest/AndroidManifest.xml";
    public static final String AAB_RESOURCES_PB_SUFFIX        = "/resources.pb";
    public static final String AAB_RESOURCES_ARSC_SUFFIX      = "/resources.arsc";
    public static final String AAB_MANIFEST_INFIX             = "/manifest/";
    public static final String AAB_DEX_INFIX                  = "/dex/";
    public static final String AAB_ROOT_INFIX                 = "/root/";

    public static final String AAB_BUNDLE_METADATA_EXPRESSION      = "BUNDLE-METADATA/**";
    public static final String AAB_ANDROID_MANIFEST_XML_EXPRESSION = "*/manifest/AndroidManifest.xml";
    public static final String AAB_RESOURCES_PB_EXPRESSION         = "*/resources.pb";
    public static final String AAB_ASSETS_PB_EXPRESSION            = "*/assets.pb";
    public static final String AAB_NATIVE_PB_EXPRESSION            = "*/native.pb";
    public static final String AAB_RESOURCES_ARSC_EXPRESSION       = "*/resources.arsc";
    public static final String AAB_CLASSES_DEX_EXPRESSION          = "*/dex/classes*.dex";
    public static final String AAB_ASSETS_FILE_EXPRESSION          = "*/assets/**";
    public static final String AAB_RES_FILE_EXPRESSION             = "*/res/**";
    public static final String AAB_RES_XML_FILE_EXPRESSION         = "*/res/**.xml"; // Raw files are also binary.
    public static final String AAB_LIB_FILE_EXPRESSION             = "*/lib/**.so";
    public static final String AAB_DYLIB_FILE_EXPRESSION           = "*/lib/**.dylib";
    public static final String AAB_RESOURCES_EXPRESSION            = AAB_RESOURCES_PB_EXPRESSION + ',' +
                                                                     AAB_RESOURCES_ARSC_EXPRESSION;
    public static final String AAB_XML_FILE_EXPRESSION             = AAB_ANDROID_MANIFEST_XML_EXPRESSION + ',' +
                                                                     AAB_RES_XML_FILE_EXPRESSION;

    public static final String FEATURE_ZIP_CLASSES_DEX_EXPRESSION = "dex/classes*.dex";

    // Aar files.
    public static final String AAR_FILE_EXTENSION = ".aar";

    public static final String AAR_JNI_PREFIX = "jni/";

    public static final String AAR_JNI_FILE_EXPRESSION   = "jni/**.so";
    public static final String AAR_JNI_DYFILE_EXPRESSION = "jni/**.dylib";

    public static final String AAR_R_TXT = "R.txt";

    public static final String AAR_RESOURCE_FILES_ALLOW_SHRINKING = "annotations.zip";

    // Mix of apk/aab/aar code and resource files.
    public static final String ARSC_EXPRESSION                      = "**.arsc";
    public static final String DEX_EXPRESSION                       = "**.dex";

    public static final String ANDROID_MANIFEST_XML_EXPRESSION      = ANDROID_MANIFEST_XML + ',' +
                                                                      AAB_ANDROID_MANIFEST_XML_EXPRESSION;
    public static final String BASE_ANDROID_MANIFEST_XML_EXPRESSION = ANDROID_MANIFEST_XML + ',' +
                                                                      AAB_BASE + AAB_ANDROID_MANIFEST_XML_SUFFIX;
    public static final String RESOURCES_PB_EXPRESSION              = RESOURCES_PB + ',' +
                                                                      AAB_RESOURCES_PB_EXPRESSION;
    public static final String RESOURCES_EXPRESSION                 = ARSC_EXPRESSION + ',' +
                                                                      RESOURCES_PB_EXPRESSION;
    public static final String ROOT_RESOURCES_EXPRESSION            = RESOURCES_ARSC + ',' +
                                                                      RESOURCES_PB;
    public static final String BASE_RESOURCES_EXPRESSION            = RESOURCES_ARSC + ',' +
                                                                      AAB_BASE_PREFIX + RESOURCES_ARSC + ',' +
                                                                      AAB_BASE_PREFIX + RESOURCES_PB;
    public static final String CLASSES_DEX_EXPRESSION               = APK_CLASSES_DEX_EXPRESSION + ',' +
                                                                      AAB_CLASSES_DEX_EXPRESSION + ',' +
                                                                      FEATURE_ZIP_CLASSES_DEX_EXPRESSION;
    public static final String ASSETS_FILE_EXPRESSION               = APK_ASSETS_FILE_EXPRESSION + ',' +
                                                                      AAB_ASSETS_FILE_EXPRESSION;
    public static final String RES_FILE_EXPRESSION                  = APK_RES_FILE_EXPRESSION + ',' +
                                                                      AAB_RES_FILE_EXPRESSION;
    public static final String RES_XML_FILE_EXPRESSION              = APK_RES_XML_FILE_EXPRESSION + ',' +
                                                                      AAB_RES_XML_FILE_EXPRESSION;
    public static final String LIB_FILE_EXPRESSION                  = APK_LIB_FILE_EXPRESSION + ',' +
                                                                      AAB_LIB_FILE_EXPRESSION + ',' +
                                                                      AAR_JNI_FILE_EXPRESSION;
    public static final String APK_AAR_LIB_FILE_EXPRESSION          = APK_LIB_FILE_EXPRESSION + ',' +
                                                                      AAR_JNI_FILE_EXPRESSION;
    public static final String LIB_DYFILE_EXPRESSION                = APK_DYLIB_FILE_EXPRESSION + ',' +
                                                                      AAB_DYLIB_FILE_EXPRESSION + ',' +
                                                                      AAR_JNI_DYFILE_EXPRESSION;
    public static final String XML_FILE_EXPRESSION                  = ANDROID_MANIFEST_XML_EXPRESSION + ',' +
                                                                      RES_XML_FILE_EXPRESSION;
    public static final String META_INF_SIGNING_INFO                = "META-INF/*.MF,META-INF/*.SF,META-INF/*.RSA";

    public static final String BUNDLE_METADATA_EXPRESSION           = ZIP_META_INF_EXPRESSION + ',' +
                                                                      AAB_BUNDLE_METADATA_EXPRESSION;

    // Type & name constants.
    public static final String TYPE_ANDROID_CONTENT_CONTEXT                                 = "Landroid/content/Context;";
    public static final String NAME_ANDROID_CONTENT_CONTEXT                                 = "android/content/Context";
    public static final String FIELD_NAME_ACTIVITY_SERVICE                                  = "activity";
    public static final String NAME_ANDROID_CONTEXT_WRAPPER                                 = "android/content/ContextWrapper";

    public static final String METHOD_NAME_ATTACH_BASE_CONTEXT                              = "attachBaseContext";
    public static final String METHOD_TYPE_ATTACH_BASE_CONTEXT                              = "(" + TYPE_ANDROID_CONTENT_CONTEXT + ")V";

    public static final String METHOD_NAME_ON_CREATE                                        = "onCreate";
    public static final String METHOD_TYPE_ON_CREATE                                        = "()V";

    public static final String TYPE_ANDROID_CONTENT_RES_RESOURCES                           = "Landroid/content/res/Resources;";
    public static final String METHOD_NAME_GET_RESOURCES                                    = "getResources";
    public static final String METHOD_TYPE_GET_RESOURCES                                    = "()" + TYPE_ANDROID_CONTENT_RES_RESOURCES;

    public static final String METHOD_NAME_GET_BASE_CONTEXT                                 = "getBaseContext";
    public static final String METHOD_TYPE_GET_BASE_CONTEXT                                 = "()" + TYPE_ANDROID_CONTENT_CONTEXT;

    public static final String METHOD_NAME_GET_APPLICATION_CONTEXT                          = "getApplicationContext";
    public static final String METHOD_TYPE_GET_APPLICATION_CONTEXT                          = "()" + TYPE_ANDROID_CONTENT_CONTEXT;

    public static final String CLASS_NAME_ANDROID_APP_ACTIVITYTHREAD                        = "android.app.ActivityThread";
    public static final String METHOD_NAME_CURRENTAPPLICATION                               = "currentApplication";

    public static final String TYPE_JAVA_LANG_RUNTIME                                       = "Ljava/lang/Runtime;";
    public static final String METHOD_NAME_EXEC                                             = "exec";

    public static final String NAME_ANDROID_OS_PROCESS                                      = "android/os/Process";
    public static final String TYPE_ANDROID_OS_PROCESS                                      = "Landroid/os/Process;";
    public static final String METHOD_NAME_MYPID                                            = "myPid";
    public static final String METHOD_NAME_MYUID                                            = "myUid";
    public static final String METHOD_NAME_KILLPROCESS                                      = "killProcess";

    public static final String METHOD_NAME_FORMAT                                           = "format";

    public static final String CLASS_NAME_ANDROID_OS_BUILD_VERSION                          = "android.os.Build$VERSION";
    public static final String FIELD_NAME_SDK_INT                                           = "SDK_INT";
    public static final String FIELD_TYPE_SDK_INT                                           = "I";

    public static final String METHOD_NAME_GET_SYSTEM_SERVICE                               = "getSystemService";

    public static final String CLASS_NAME_ANDROID_APP_ACTIVITYMANAGER                       = "android.app.ActivityManager";
    public static final String METHOD_NAME_GETAPPTASKS                                      = "getAppTasks";
    public static final String METHOD_NAME_GETRUNNINGAPPPROCESSES                           = "getRunningAppProcesses";

    public static final String CLASS_NAME_ANDROID_APP_APPLICATION                           = "android.app.Application";

    public static final String CLASS_NAME_ANDROID_APP_ACTIVITYMANAGER_APPTASK               = "android.app.ActivityManager$AppTask";
    public static final String CLASS_NAME_ANDROID_APP_ACTIVITYMANAGER_RUNNINGAPPPROCESSINFO = "android.app.ActivityManager$RunningAppProcessInfo";
    public static final String METHOD_NAME_FINISHANDREMOVETASK                              = "finishAndRemoveTask";
    public static final String FIELD_NAME_PID                                               = "pid";

    public static final String FIELD_NAME_ITERATOR                                          = "iterator";
    public static final String FIELD_TYPE_ITERATOR                                          = "()Ljava/util/Iterator;";

    public static final String NAME_JAVA_UTIL_ITERATOR                                      = "java/util/Iterator";
    public static final String METHOD_NAME_HASNEXT                                          = "hasNext";
    public static final String METHOD_TYPE_HASNEXT                                          = "()Z";
    public static final String METHOD_NAME_NEXT                                             = "next";
    public static final String METHOD_TYPE_NEXT                                             = "()Ljava/lang/Object;";


    // Resource paths.
    public static final String RES_FONT           = "res/font*/*";
    public static final String RES_DRAWABLE       = "res/drawable**";
    public static final String RES_XML            = "res/xml**";
    public static final String RES_NAVIGATION     = "res/navigation**";
    public static final String RES_LAYOUT         = "res/layout**";
    public static final String RES_MENU           = "res/menu**";
    public static final String RES_TRANSITION     = "res/transition**";
    public static final String RES_DRAWABLE_XML   = "res/drawable*/*.xml";
    public static final String RES_LAYOUT_XML     = "res/layout*/*.xml";
    public static final String RES_MENU_XML       = "res/menu*/*.xml";
    public static final String RES_NAVIGATION_XML = "res/navigation*/*.xml";
    public static final String RES_TRANSITION_XML = "res/transition*/*.xml";

    // Native library naming conventions.
    public static final String STANDARD_NATIVE_LIBRARY_PATH = "lib/*/lib*.so,jni/*/lib*.so";
    public static final String ALL_LIB_FILE_EXPRESSION      = "**.so";
}
