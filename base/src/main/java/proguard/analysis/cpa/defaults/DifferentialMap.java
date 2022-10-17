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

package proguard.analysis.cpa.defaults;

import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A differential representation of maps. Maps are stored as trees with full maps as roots and action nodes. Action nodes define either element deletion or insertion.
 * The value of the map is defined as the root map with action nodes between it and the current node applied to it.
 * Any action node can be used to form a root of another tree with an equivalent semantic. A predicate on when to collapse can be provided for creating root nodes automatically upon insertion or
 * removal.
 *
 * @author Dmitry Ivanov
 */
public class DifferentialMap<K, V>
    implements Map<K, V>
{

    private         DifferentialMapNode<K, V>        node;
    protected final Predicate<DifferentialMap<K, V>> shouldCollapse;

    /**
     * Create an empty differential map.
     */
    public DifferentialMap()
    {
        this(Collections.emptyMap(), m -> false);
    }

    /**
     * Create a differential map from another map.
     *
     * @param m initial map
     *          if it is a differential map, the collapse criterion will be copied from it
     */
    public DifferentialMap(Map<K, V> m)
    {
        this(m, m instanceof DifferentialMap ? ((DifferentialMap<K, V>) m).shouldCollapse : map -> false);
    }

    /**
     * Create a differential map from another map and a collapse criterion.
     *
     * @param m              initial map
     * @param shouldCollapse whether the map should collapse into a root node
     */
    public DifferentialMap(Map<K, V> m, Predicate<DifferentialMap<K, V>> shouldCollapse)
    {
        node = m instanceof DifferentialMap ? ((DifferentialMap<K, V>) m).node : new DifferentialMapRootNode<>(m);
        this.shouldCollapse = shouldCollapse;
    }

    /**
     * Changes the internal representation by applying action nodes to a copy of the root.
     */
    public void collapse()
    {
        if (node instanceof DifferentialMapRootNode)
        {
            return;
        }
        node = new DifferentialMapRootNode<>(node.reconstructFullMap());
    }

    /**
     * Returns the depth of the action node with regard to the root map.
     */
    public int getDepth()
    {
        return node.depth;
    }

    // implementations for Map

    @Override
    public int size()
    {
        return node.size();
    }

    @Override
    public boolean isEmpty()
    {
        return node.size() == 0;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return node.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return node.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return node.get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value)
    {
        node = new DifferentialMapInnerNode<>(node, new PutAction<>(key, value));
        if (shouldCollapse.test(this))
        {
            collapse();
        }
        return value;
    }

    @Override
    public V remove(Object key)
    {
        V answer = get(key);
        node = new DifferentialMapInnerNode<>(node, new RemoveAction<K, V>((K) key));
        if (shouldCollapse.test(this))
        {
            collapse();
        }
        return answer;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m)
    {
        m.forEach(this::put);
    }

    @Override
    public void clear()
    {
        node = new DifferentialMapRootNode<>(Collections.emptyMap());
    }

    @NotNull
    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @NotNull
    @Override
    public Collection<V> values()
    {
        return new ValueCollection();
    }

    @NotNull
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        return new EntrySet();
    }

    // implementations for Object

    @Override
    public int hashCode()
    {
        return node.reconstructFullMap().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj || obj instanceof DifferentialMap && node == ((DifferentialMap) obj).node)
        {
            return true;
        }
        if (!(obj instanceof Map))
        {
            return false;
        }
        return node.reconstructFullMap().equals(obj);
    }

    /**
     * A base class for map nodes supporting some map interfaces.
     */
    private static abstract class DifferentialMapNode<K, V>
    {

        public final int depth;

        private DifferentialMapNode(int depth)
        {
            this.depth = depth;
        }

        /**
         * Returns the size of the map defined by this node.
         */
        public abstract int size();

        /**
         * Checks whether the given key belongs to the map defined by this node.
         */
        public abstract boolean containsKey(Object key);

        /**
         * Checks whether the given value belongs to the map defined by this node.
         */
        public abstract boolean containsValue(Object value);

        /**
         * Returns a value corresponding to the key or null if there is no mapping.
         */
        public abstract V get(Object key);

        /**
         * Returns a hash map representation of the map defined by this node.
         */
        public abstract Map<K, V> reconstructFullMap();

        /**
         * Returns a tuple of the root map and the stack of actions.
         */
        public abstract RootAndActions getRootAndActions();

        /**
         * Returns a tuple of disjoint removed keys and (re)assigned mappings.
         */
        public GenAndKill getGenAndKill(Stack<Action<K, V>> actionStack)
        {
            Map<K, V> gen  = new HashMap<>();
            Set<K>    kill = new HashSet<>();
            while (!actionStack.isEmpty())
            {
                Action<K, V> action = actionStack.pop();
                if (action instanceof PutAction)
                {
                    kill.remove(action.key);
                    gen.put(action.key, ((PutAction<K, V>) action).value);
                    continue;
                }
                kill.add(action.key);
                gen.remove(action.key);
            }
            return new GenAndKill(gen, kill);
        }

        /**
         * A tuple of the root map and the action stack.
         */
        protected class RootAndActions
        {

            public final Map<K, V>           rootMap;
            public final Stack<Action<K, V>> actionStack;

            public RootAndActions(Map<K, V> rootMap, Stack<Action<K, V>> actionStack)
            {
                this.rootMap     = rootMap;
                this.actionStack = actionStack;
            }
        }

        /**
         * A disjoint tuple of removed keys and (re)assigned mappings.
         */
        protected class GenAndKill
        {

            public final Map<K, V> gen;
            public final Set<K>    kill;

            public GenAndKill(Map<K, V> gen, Set<K> kill)
            {
                this.gen  = gen;
                this.kill = kill;
            }
        }
    }

    /**
     * A root node holding the initial map.
     */
    private static class DifferentialMapRootNode<K, V> extends DifferentialMapNode<K, V>
    {

        public final Map<K, V> rootMap;

        private DifferentialMapRootNode(Map<K, V> rootMap)
        {
            super(0);
            this.rootMap = rootMap;
        }

        // implementations for DifferentialMapNode

        @Override
        public int size()
        {
            return rootMap.size();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return rootMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            return rootMap.containsValue(value);
        }

        @Override
        public V get(Object key)
        {
            return rootMap.get(key);
        }

        @Override
        public RootAndActions getRootAndActions()
        {
            return new RootAndActions(rootMap, new Stack<>());
        }

        @Override
        public Map<K, V> reconstructFullMap()
        {
            return rootMap;
        }
    }

    /**
     * An inner node representing an action performed over the map represented by its parent.
     */
    private static class DifferentialMapInnerNode<K, V>
        extends DifferentialMapNode<K, V>
    {

        public final      DifferentialMapNode<K, V> parent;
        public final      Action< K, V>             action;
        private transient int                       size;

        private DifferentialMapInnerNode(DifferentialMapNode<K, V> parent, Action<K, V> action)
        {
            super(parent.depth + 1);
            this.parent = parent;
            this.action = action;
            size = -1;
        }

        // implementations for DifferentialMapNode

        @Override
        public int size()
        {
            if (size >= 0)
            {
                return size;
            }
            RootAndActions rootAndActions = getRootAndActions();
            GenAndKill     genAndKill     = getGenAndKill(rootAndActions.actionStack);
            return size = (int) (rootAndActions.rootMap.size()
                                 - genAndKill.kill.stream().filter(rootAndActions.rootMap::containsKey).count()
                                 + genAndKill.gen.keySet().stream().filter(k -> !rootAndActions.rootMap.containsKey(k)).count());
        }

        @Override
        public boolean containsKey(Object key)
        {
            RootAndActions rootAndActions = getRootAndActions();
            GenAndKill     genAndKill     = getGenAndKill(rootAndActions.actionStack);
            return genAndKill.gen.containsKey(key) || !genAndKill.kill.contains(key) && rootAndActions.rootMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            RootAndActions rootAndActions = getRootAndActions();
            GenAndKill     genAndKill     = getGenAndKill(rootAndActions.actionStack);
            return genAndKill.gen.containsValue(value) || rootAndActions.rootMap.entrySet().stream().filter(e -> !genAndKill.kill.contains(e.getKey())).map(Entry::getValue).anyMatch(value::equals);
        }

        @Override
        public V get(Object key)
        {
            RootAndActions rootAndActions = getRootAndActions();
            GenAndKill     genAndKill     = getGenAndKill(rootAndActions.actionStack);
            V genValue;
            return genAndKill.kill.contains(key)
                   ? null
                   : (genValue = genAndKill.gen.get(key)) == null
                     ? rootAndActions.rootMap.get(key)
                     : genValue;
        }

        @Override
        public RootAndActions getRootAndActions()
        {
            Stack<Action<K, V>> actionStack = new Stack<>();
            DifferentialMapNode<K, V> n = this;
            while (n instanceof DifferentialMapInnerNode)
            {
                DifferentialMapInnerNode<K, V> innerNode = (DifferentialMapInnerNode<K, V>) n;
                actionStack.push(innerNode.action);
                n = innerNode.parent;
            }
            return new RootAndActions(((DifferentialMapRootNode<K, V>) n).rootMap, actionStack);
        }

        @Override
        public Map<K, V> reconstructFullMap()
        {
            RootAndActions rootAndActions = getRootAndActions();
            GenAndKill     genAndKill     = getGenAndKill(rootAndActions.actionStack);
            Map<K, V>      answer         = new HashMap<>(rootAndActions.rootMap);
            genAndKill.kill.forEach(answer::remove);
            answer.putAll(genAndKill.gen);
            return answer;
        }
    }

    /**
     * A base class for actions being performed on the root map.
     */
    private static abstract class Action<K, V>
    {

        public final K key;

        protected Action(K key)
        {
            this.key = key;
        }
    }

    /**
     * An action representing a map insertion.
     */
    private static class PutAction<K, V>
        extends Action<K, V>
    {

        public final V value;

        public PutAction(K key, V value)
        {
            super(key);
            this.value = value;
        }
    }

    /**
     * An action representing a key deletion.
     */
    private static class RemoveAction<K, V>
        extends Action<K, V>
    {

        public RemoveAction(K key)
        {
            super(key);
        }
    }

    /**
     * A backed value collection view on the map.
     */
    private class ValueCollection
        extends AbstractCollection<V>
    {

        // implementations for AbstractCollection

        @Override
        public int size()
        {
            return DifferentialMap.this.size();
        }

        @Override
        public void clear()
        {
            DifferentialMap.this.clear();
        }

        @NotNull
        @Override
        public Iterator<V> iterator()
        {
            return new ValueIterator();
        }

        @Override
        public boolean contains(Object o)
        {
            return containsValue(o);
        }

        /**
         * An iterator over the value collection.
         */
        private class ValueIterator
            implements Iterator<V>
        {

            private final Iterator<Entry<K, V>> entryIterator = entrySet().iterator();

            // implementations for Iterator

            @Override
            public V next()
            {
                return entryIterator.next().getValue();
            }

            @Override
            public void remove()
            {
                entryIterator.remove();
            }

            @Override
            public boolean hasNext()
            {
                return entryIterator.hasNext();
            }
        }
    }

    /**
     * A backed key set view on the map.
     */
    private class KeySet extends AbstractSet<K>
    {

        // implementations for Set

        @Override
        public int size()
        {
            return DifferentialMap.this.size();
        }

        @Override
        public void clear()
        {
            DifferentialMap.this.clear();
        }

        @NotNull
        @Override
        public Iterator<K> iterator()
        {
            return new KeyIterator();
        }

        @Override
        public boolean contains(Object o)
        {
            return DifferentialMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object key) {
            return DifferentialMap.this.remove(key) != null;
        }

        /**
         * An iterator over the key set.
         */
        private class KeyIterator
            implements Iterator<K>
        {

            private final Iterator<Entry<K, V>> entryIterator = entrySet().iterator();

            // implementations for Iterator

            @Override
            public K next()
            {
                return entryIterator.next().getKey();
            }

            @Override
            public void remove()
            {
                entryIterator.remove();
            }

            @Override
            public boolean hasNext()
            {
                return entryIterator.hasNext();
            }
        }
    }

    /**
     * A backed entry set view on the map.
     */
    private class EntrySet extends AbstractSet<Entry<K,V>>
    {

        // implementations for Set

        @Override
        public int size()
        {
            return DifferentialMap.this.size();
        }

        @Override
        public void clear()
        {
            DifferentialMap.this.clear();
        }

        @Override
        @NotNull
        public Iterator<Entry<K,V>> iterator()
        {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o)
        {
            if (!(o instanceof Map.Entry))
            {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V candidateValue = get(e.getKey());
            return candidateValue != null && new SimpleEntry<>(e.getKey(), candidateValue).equals(e);
        }

        @Override
        public boolean remove(Object o)
        {
            return DifferentialMap.this.remove(o) != null;
        }

        /**
         * An iterator for the entry set.
         */
        private class EntryIterator
            implements Iterator<Map.Entry<K, V>>
        {

            private final Set<K>                                   passedKeys     = new HashSet<>();
            private final DifferentialMapNode<K, V>.RootAndActions rootAndActions = node.getRootAndActions();
            private final Iterator<Action<K, V>>                   actionIterator = rootAndActions.actionStack.iterator();
            private       Iterator<Map.Entry<K, V>>                rootIterator   = null;
            private       Entry<K, V>                              current        = null;
            private       Entry<K, V>                              next           = null;

            // implementations for Iterator

            @Override
            public Entry<K, V> next()
            {
                current = next == null ? peekNext() : next;
                if (current == null)
                {
                    throw new NoSuchElementException();
                }
                next = null;
                return current;
            }

            @Override
            public void remove()
            {
                if (current == null)
                {
                    throw new IllegalStateException();
                }
                DifferentialMap.this.remove(current.getKey());
                current = null;
            }

            @Override
            public boolean hasNext()
            {
                return (next == null ? next = peekNext() : next) != null;
            }

            private Entry<K, V> peekNext()
            {
                while (actionIterator.hasNext())
                {
                    Action<K, V> action = actionIterator.next();
                    if (passedKeys.contains(action.key))
                    {
                        continue;
                    }
                    if (action instanceof RemoveAction)
                    {
                        passedKeys.add(action.key);
                        continue;
                    }
                    passedKeys.add(action.key);
                    return new DifferentialEntry(action.key, ((PutAction<K, V>) action).value);
                }
                if (rootIterator == null)
                {
                    rootIterator = rootAndActions.rootMap.entrySet()
                                                         .stream()
                                                         .filter(e -> !passedKeys.contains(e.getKey()))
                                                         .iterator();
                }
                return rootIterator.hasNext() ? new DifferentialEntry(rootIterator.next()) : null;
            }
        }
    }

    /**
     * A map entry backed by the differential map.
     */
    private class DifferentialEntry
        extends SimpleEntry<K, V>
    {

        public DifferentialEntry(K key, V value)
        {
            super(key, value);
        }

        public DifferentialEntry(Entry<? extends K, ? extends V> entry)
        {
            super(entry);
        }

        @Override
        public V setValue(V value)
        {
            return put(getKey(), value);
        }
    }
}
