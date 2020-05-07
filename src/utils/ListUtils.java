package utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;

import java.util.List;

public interface ListUtils {

    static <T> HashMultimap<T, Integer> getOccurrenceMap(List<T> inputList) {
        HashMultiset<T> inputSet = HashMultiset.create(inputList);
        HashMultimap<T, Integer> outMap = HashMultimap.create();

        for (T elementName : inputSet.elementSet()) {
            int occurence = inputSet.count(elementName);
            outMap.put(elementName, occurence);
        }

        return outMap;
    }
}
