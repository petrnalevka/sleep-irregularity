package com.urbandroid.sleep.domain.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TagMap {

    private HashMap<String, TagOccurrence> map = new HashMap<String, TagOccurrence>();

    public TagMap() {
    }

    public void putTags(Set<String> tags) {
        for (String tag : tags) {
            TagOccurrence occurrence = map.get(tag);
            if (occurrence == null) {
                occurrence = new TagOccurrence(tag, 0);
                map.put(tag, occurrence);
            }
            occurrence.incOccurence();

        }
    }

    public List<String> getSorted() {
        List<TagOccurrence> sorted = new ArrayList<TagOccurrence>();
        sorted.addAll(map.values());
        List<String> result = new ArrayList<String>();
        Collections.sort(sorted);

        for (TagOccurrence tagOccurrence : sorted) {
//            Logger.logInfo("Sorted " + tagOccurrence.getTagName() + " count " +tagOccurrence.getOccurrence());
            result.add(tagOccurrence.getTagName());
        }

        return result;
    }

    public List<String> getSortedWithBuildIn() {
        List<String> result = getSorted();
        Tag[] buildInTagArray = Tag.values();
        for (Tag tag : buildInTagArray) {
            if (!result.contains(tag.getTagName())) {
                result.add(tag.getTagName());
            }
        }



        return result;
    }

}
