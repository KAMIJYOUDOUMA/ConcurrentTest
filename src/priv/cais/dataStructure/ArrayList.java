package priv.cais.dataStructure;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class ArrayList<E> extends AbstractList<E> implements List<E>{
    //默认容量
    private final int DEFAULT_CAPACITY = 10;
    //默认元素
    private final Object[] EMPTY_ELEMENTDATE = {};
    //元素
    transient Object[] elementData;
    //大小
    private int size;

    public ArrayList(int initialCapacity) {
        if(initialCapacity > 0){
            this.elementData = new Object[initialCapacity];
        }else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATE;
        }else {
            throw new IllegalArgumentException("初始容量错误:" + initialCapacity);
        }
    }

    public ArrayList() {
        this.elementData = EMPTY_ELEMENTDATE;
    }

    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if((size = elementData.length) != 0){
            if(elementData.getClass() != Object[].class){
                elementData = Arrays.copyOf(elementData, size, Object[].class);
            }
        }else{
            this.elementData = EMPTY_ELEMENTDATE;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size){
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        }
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public boolean add(E e) {
        ensureCapacity(size + 1);
        //实际赋值
        elementData[size++] = e;
        return true;
    }

    @Override
    public void add(int index, E element) {
        rangeCheck(index);
        ensureCapacity(size + 1);
        System.arraycopy(elementData, index, elementData, index + 1,
                size - index);
        elementData[index] = element;
        size++;
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 保证容量
     * @param capacity
     */
    private void ensureCapacity(int capacity) {
        //元素为空数组，容量取最小为10
        if(elementData == EMPTY_ELEMENTDATE){
            capacity = Math.max(capacity, DEFAULT_CAPACITY);
        }
        //当所需容量大于当前数组长度时，扩容
        if(capacity > elementData.length){
            int oldCapacity = elementData.length;
            //扩容1.5倍
            int newCapacity = oldCapacity + oldCapacity >> 1;
            //扩容后还是小于所设容量，将所设容量设为新容量
            if(newCapacity < capacity){
                newCapacity = capacity;
            }
            //当扩容后大于数组最大大小
            else if (newCapacity > MAX_ARRAY_SIZE) {
                newCapacity = capacity > MAX_ARRAY_SIZE ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
            }
            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }

    @Override
    public E remove(int index) {
        rangeCheck(index);
        E oldValue = elementData(index);
        System.arraycopy(elementData, index + 1, elementData, index, size - index - 1);
        elementData[--size] = null;
        return oldValue;
    }

    @Override
    public boolean remove(Object o) {
        if(o == null){
            for (int index = 0; index < size; index++){
                if (elementData[index] == null ) {
                    System.arraycopy(elementData, index + 1, elementData, index, size - index - 1);
                    elementData[--size] = null;
                    return true;
                }
            }
        }else {
            for (int index = 0; index < size; index++){
                if (o.equals(elementData[index])) {
                    System.arraycopy(elementData, index + 1, elementData, index, size - index - 1);
                    elementData[--size] = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int addNum = a.length;
        ensureCapacity(size + addNum);
        System.arraycopy(a, 0, elementData, size, addNum);
        size += addNum;
        return addNum != 0;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheck(index);
        Object[] a = c.toArray();
        int addNum = a.length;
        ensureCapacity(size + addNum);
        if(size > index){
            System.arraycopy(elementData, index, elementData, size + addNum, size - index);
        }
        System.arraycopy(a, 0, elementData, index, addNum);
        size += addNum;
        return addNum != 0;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, false);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }

    private boolean batchRemove(Collection<?> c, boolean complement) {
        final Object[] elementData = this.elementData;
        int r=0,w=0;
        boolean modified = false;
        try {
            for(; r < size; r++){
                //判断是含有保持还是不含保持（true:包含保持，false：不包含保持）
                if(c.contains(elementData[r]) == complement){
                    elementData[w++] = elementData[r];
                }
            }
        } finally {
            if(r != size){
                System.arraycopy(elementData, r, elementData, w, size - r);
                w += size - r;
            }
            if (w != size) {
                // clear to let GC do its work
                for (int i = w; i < size; i++)
                    elementData[i] = null;
                size = w;
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        for(int i=0;i < size;i++){
            elementData[i] = null;
        }
        size = 0;
    }

    @Override
    public E get(int index) {
        rangeCheck(index);
        return elementData(index);
    }

    /**
     * 范围check，防止数组越界
     * @param index
     */
    private void rangeCheck(int index) {
        if(index >= size || index < 0){
            throw new IndexOutOfBoundsException("Index:" + index + ",Size:" + size);
        }
    }

    @Override
    public E set(int index, E element) {
        rangeCheck(index);
        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }





    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = size - 1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        // TODO Auto-generated method stub
        return null;
    }


    //统一解除警告
    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

}
