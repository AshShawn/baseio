/*
 * Copyright 2015-2017 GenerallyCloud.com
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
package com.generallycloud.baseio.collection;

/**
 * @author wangkai
 *
 */
public class ObjectPool<V> {

    private ObjectPoolFactory<V> factory;

    private V[]                  vs;

    private int                  capacity;

    private int                  size;

    @SuppressWarnings("unchecked")
    public void init(ObjectPoolFactory<V> factory, int capacity) {
        this.capacity = capacity;
        this.factory = factory;
        this.vs = (V[]) new Object[capacity];
        for (int i = 0; i < capacity; i++) {
            vs[i] = factory.newInstance();
        }
        size = capacity;
    }

    public V pop() {
        if (size == 0) {
            return factory.newInstance();
        }
        return vs[--size];
    }

    public void push(V v) {
        if (size == capacity) {
            return;
        }
        vs[size++] = v;
    }

}
