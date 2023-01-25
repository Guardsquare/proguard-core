/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.analysis.datastructure.callgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import proguard.classfile.MethodDescriptor;
import proguard.classfile.MethodSignature;

/**
 * This class serves as a static collection of entry points for an app. An entry point is a method that is provided
 * by the Android framework and can be hooked by the app to be called at a specific point in the app lifecycle.
 *
 * <p>The list of well known entry points has been curated using the
 * <a href="https://developer.android.com/guide/components/fundamentals?hl=en#Components">official documentation</a>
 * and a <a href="https://github.com/xxv/android-lifecycle">third-party collection</a>.
 *
 * @author Samuel Hopstock
 */
public class EntryPoint
{

    public enum Type
    {
        ACTIVITY, SERVICE, BROADCAST_RECEIVER, CONTENT_PROVIDER
    }

    public static final List<EntryPoint> ENTRY_POINTS_ACTIVITY           = Arrays.asList(
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "<clinit>"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "<init>"),
        // General lifecycle
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onCreate"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onStart"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onRestart"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onResume"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onPause"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onStop"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onDestroy"),
        // Additional known entrypoints
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onAttachFragment"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onContentChanged"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onActivityResult"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onRestoreInstanceState"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onPostCreate"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onPostResume"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onAttachedToWindow"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onCreateOptionsMenu"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onPrepareOptionsMenu"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onUserInteraction"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onUserLeaveHint"),
        new EntryPoint(Type.ACTIVITY, "android.app.Activity", "onSaveInstanceState")
    );
    public static final List<EntryPoint> ENTRY_POINTS_FRAGMENT           = Arrays.asList(
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "<clinit>"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "<init>"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onInflate"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onAttach"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onCreate"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onCreateView"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onViewCreated"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onActivityCreated"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onViewStateRestored"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onStart"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onResume"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onCreateOptionsMenu"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onPrepareOptionsMenu"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onPause"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onSaveInstanceState"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onStop"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onDestroyView"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onDestroy"),
        new EntryPoint(Type.ACTIVITY, "android.app.Fragment", "onDetach")
    );
    public static final List<EntryPoint> ENTRY_POINTS_SERVICE            = Arrays.asList(
        new EntryPoint(Type.SERVICE, "android.app.Service", "<clinit>"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "<init>"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onCreate"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onStartCommand"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onBind"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onUnbind"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onRebind"),
        new EntryPoint(Type.SERVICE, "android.app.Service", "onDestroy")
    );
    public static final List<EntryPoint> ENTRY_POINTS_BROADCAST_RECEIVER = Arrays.asList(
        new EntryPoint(Type.BROADCAST_RECEIVER, "android.content.BroadcastReceiver", "<clinit>"),
        new EntryPoint(Type.BROADCAST_RECEIVER, "android.content.BroadcastReceiver", "<init>"),
        new EntryPoint(Type.BROADCAST_RECEIVER, "android.content.BroadcastReceiver", "onReceive")
    );
    public static final List<EntryPoint> ENTRY_POINTS_CONTENT_PROVIDER   = Arrays.asList(
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "<clinit>"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "<init>"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "onCreate"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "query"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "insert"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "update"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "delete"),
        new EntryPoint(Type.CONTENT_PROVIDER, "android.content.ContentProvider", "getType")
    );
    public static final List<EntryPoint> WELL_KNOWN_ENTRYPOINTS          = Stream.of(
        ENTRY_POINTS_ACTIVITY,
        ENTRY_POINTS_FRAGMENT,
        ENTRY_POINTS_SERVICE,
        ENTRY_POINTS_BROADCAST_RECEIVER,
        ENTRY_POINTS_CONTENT_PROVIDER
    ).flatMap(Collection::stream).collect(Collectors.toList());
    public static final Set<String>      WELL_KNOWN_ENTRYPOINT_CLASSES   = WELL_KNOWN_ENTRYPOINTS.stream()
                                                                                                 .map(e -> e.className)
                                                                                                 .collect(Collectors.toSet());

    public final String className;
    public final String methodName;
    public final Type   type;

    public EntryPoint(Type type, String className, String methodName)
    {
        this.type = type;
        this.className = className;
        this.methodName = methodName;
    }

    public static List<EntryPoint> getEntryPointsForType(Type type)
    {
        switch(type)
        {
            case ACTIVITY:
                return ENTRY_POINTS_ACTIVITY;
            case SERVICE:
                return ENTRY_POINTS_SERVICE;
            case BROADCAST_RECEIVER:
                return ENTRY_POINTS_BROADCAST_RECEIVER;
            case CONTENT_PROVIDER:
                return ENTRY_POINTS_CONTENT_PROVIDER;
            default:
                throw new IllegalStateException("Unsupported entry point type");
        }
    }

    public MethodSignature toSignature()
    {
        return new MethodSignature(className.replace('.', '/'), methodName, (MethodDescriptor) null);
    }

    @Override
    public String toString()
    {
        return toSignature().toString() + " (" + type + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        EntryPoint that = (EntryPoint) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(className, methodName);
    }
}
