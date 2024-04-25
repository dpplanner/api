package com.dp.dplanner.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class DefaultConverter {

    /**
     * By bean copy for converting list
     *
     * @param sourceList
     * @param target
     * @param <S>
     * @param <D>
     * @return
     */
    public static <S, D> List<D> convert(List<S> sourceList, Class<D> target) {
        try {
            if (CollectionUtils.isEmpty(sourceList)) {
                return new ArrayList<>();
            } else {
                List<D> list = new ArrayList();
                Iterator iterator = sourceList.iterator();

                while (iterator.hasNext()) {
                    S source = (S) iterator.next();
                    D dest = target.getDeclaredConstructor().newInstance();
                    BeanUtils.copyProperties(source, dest, getNullProperties(source));
                    list.add(dest);
                }

                return list;
            }
        } catch (InstantiationException e) {
            throw new RuntimeException("Cast " + sourceList.getClass() + " to " + target + " error.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cast " + sourceList.getClass() + " to " + target + " error.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cast " + sourceList.getClass() + " to " + target + " error.", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cast " + sourceList.getClass() + " to " + target + " error.", e);
        }
    }



    /**
     * Object conversion by bean copy
     *
     * @param source
     * @param target
     * @param <S>
     * @param <D>
     * @return
     */
    public static <S, D> D convert(S source, Class<D> target) {
        try{
            if (ObjectUtils.isEmpty(source)) {
                return null;
            }
            D dest = target.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, dest, getNullProperties(source));
            return dest;
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cast " + source.getClass() + " to " + target + " error.", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cast " + source.getClass() + " to " + target + " error.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cast " + source.getClass() + " to " + target + " error.", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cast " + source.getClass() + " to " + target + " error.", e);
        }
    }


    private static <S> String[] getNullProperties(S source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        HashSet<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (ObjectUtils.isEmpty(srcValue)) {
                emptyNames.add(pd.getName());
            }
        }

        return emptyNames.toArray(new String[emptyNames.size()]);

    }
}
