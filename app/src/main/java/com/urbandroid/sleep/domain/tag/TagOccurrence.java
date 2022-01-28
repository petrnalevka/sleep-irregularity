package com.urbandroid.sleep.domain.tag;

public class TagOccurrence implements Comparable<TagOccurrence> {

    private String tagName;

    private int occurrence;

    public TagOccurrence(String tagName, int occurrence) {
        this.tagName = tagName;
        this.occurrence = occurrence;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public int getOccurrence() {
        return occurrence;
    }

    public void incOccurence() {
        occurrence++;
    }

    @Override
    public int compareTo(TagOccurrence tagOccurrence) {
        return tagOccurrence.getOccurrence() - getOccurrence();
    }
}
