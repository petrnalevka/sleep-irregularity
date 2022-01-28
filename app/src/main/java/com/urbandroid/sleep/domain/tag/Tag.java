package com.urbandroid.sleep.domain.tag;

import android.graphics.drawable.Drawable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Tag {

    FOOD("food"),
    SPORT("sport"),
    ALCOHOL("alcohol"),
    STRESS("stress"),
    CAFFEINE("caffeine"),
    LOVE("love"),
    MED("med"),
    DREAM("dream"),
    TALK("talk"),
    SNORE("snore"),
    LAUGH("laugh"),
    SICK("sick"),
    WORK("work"),
    CPAP("cpap"),
    BABY("baby"),
    NOTE("note"),
    NEWMOON("newmoon"),
    FULLMOON("fullmoon"),
    RAIN("rain"),
    STORM("storm"),
    CLOUDY("cloudy"),
    COLD("cold"),
    HOT("hot"),
    WATCH("watch"),
    GEO1("geo1"),
    GEO2("geo2"),
    GEO3("geo3"),
    GEO0("geo0"),
    HOME("home"),
    CLOUD("cloud"),
    GOODDREAM("gooddream"),
    BADDREAM("baddream"),
    LULLABY("lullaby"),
    MENSES("menses"),
    SONAR("sonar"),
    PHASER("phaser"),
    PAIR("pair"),
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark")
    ;

//    private static final String PATTERN_EMOJI = "[\\ud83c\\udc00-\\ud83c\\udfff|\\ud83d\\udc00-\\ud83d\\udfff|\\u2600-\\u27ff]";
    private static final String PATTERN_EMOJI = "((?:[\\u2700-\\u27bf]|(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|[\\ud800\\udc00-\\uDBFF\\uDFFF]|[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|[\\ud83c\\udffb-\\ud83c\\udfff])?(?:\\u200d(?:[^\\ud800-\\udfff]|(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|[\\ud800\\udc00-\\uDBFF\\uDFFF]|[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|[\\ud83c\\udffb-\\ud83c\\udfff])?)*|[\\u0023-\\u0039]\\ufe0f?\\u20e3|\\u3299|\\u3297|\\u303d|\\u3030|\\u24c2|[\\ud83c\\udd70-\\ud83c\\udd71]|[\\ud83c\\udd7e-\\ud83c\\udd7f]|\\ud83c\\udd8e|[\\ud83c\\udd91-\\ud83c\\udd9a]|[\\ud83c\\udde6-\\ud83c\\uddff]|[\\ud83c\\ude01-\\ud83c\\ude02]|\\ud83c\\ude1a|\\ud83c\\ude2f|[\\ud83c\\ude32-\\ud83c\\ude3a]|[\\ud83c\\ude50-\\ud83c\\ude51]|\\u203c|\\u2049|[\\u25aa-\\u25ab]|\\u25b6|\\u25c0|[\\u25fb-\\u25fe]|\\u00a9|\\u00ae|\\u2122|\\u2139|\\ud83c\\udc04|[\\u2600-\\u26FF]|\\u2b05|\\u2b06|\\u2b07|\\u2b1b|\\u2b1c|\\u2b50|\\u2b55|\\u231a|\\u231b|\\u2328|\\u23cf|[\\u23e9-\\u23f3]|[\\u23f8-\\u23fa]|\\ud83c\\udccf|\\u2934|\\u2935|[\\u2190-\\u21ff])+";
    private static final String TAG_NAME_PATTERN_STRING = "(#((\\p{L}|\\p{N}|\\.\\d|,\\d|-)+))";
    private static final String TAG_QUANTITY_VALUE_SUFFIX = "(_([a-zA-Z0-9|-]+))?";
    private static final String TAG_VALUE_GROUP_SUFFIX = "_([a-zA-Z0-9|-]+)";

    public static final Pattern TAG_NAME_PATTERN = Pattern.compile(TAG_NAME_PATTERN_STRING + "|" + PATTERN_EMOJI);
    public static final Pattern TAG_QUANTITY_VALUE_PATTERN = Pattern.compile(TAG_NAME_PATTERN_STRING + "|" + PATTERN_EMOJI + TAG_QUANTITY_VALUE_SUFFIX);
    public static final Pattern TAG_QUANTITY_VALUE_PATTERN_NO_EMOJI = Pattern.compile(TAG_NAME_PATTERN_STRING + TAG_QUANTITY_VALUE_SUFFIX);
    public static final Pattern TAG_VALUE_GROUP_PATTERN = Pattern.compile(TAG_NAME_PATTERN_STRING + "|" + PATTERN_EMOJI + TAG_VALUE_GROUP_SUFFIX);

    public static final Pattern KNOWN_TAG_QUANTITY_VALUE_PATTERN;

    static {
        KNOWN_TAG_QUANTITY_VALUE_PATTERN = getRemoveTagsPattern(getAllTagStrings());
    }

    private boolean implicit = false;

    private boolean canBeAddedByUser = true;

    private final String tagName;

    private final String tagString;

    private static Tag[] geoTags = new Tag[] {HOME, GEO1, GEO2, GEO3};

    public String getTagName() {
        return tagName;
    }

    public String getTagString() {
        return tagString;
    }

    public boolean isImplicit() {
        return implicit;
    }

    public boolean canBeAddedByUser() {
        return canBeAddedByUser;
    }

    Tag(String tagName, boolean implicit) {
        this.tagName = tagName;
        this.tagString = "#"+tagName;
        this.implicit = implicit;
    }

    Tag(String tagName, boolean implicit, boolean canBeAddedByUser) {
        this.tagName = tagName;
        this.tagString = "#"+tagName;
        this.implicit = implicit;
        this.canBeAddedByUser = canBeAddedByUser;
    }

    Tag(String tagName) {
        this.tagName = tagName;
        this.tagString = "#"+tagName;
    }



    public static Set<String> getRemovedTags(String comment, String newComment) {
        Set<String> oldTags = Tag.getTags(comment);
        Set<String> newTags = Tag.getTags(newComment);

        for (String newTag : newTags) {
            oldTags.remove(newTag);
        }

        return oldTags;
    }

    public static boolean isValidTag(String tagName) {
        return TAG_NAME_PATTERN.matcher("#"+tagName).matches();
    }

    public static Set<String> getTags(String text) {
        return getTags(TAG_NAME_PATTERN, text);
    }

    public static Set<String> getTags(Pattern pattern, String text) {
        Set<String> tags = new LinkedHashSet<String>();

        if (text == null) {
            return tags;
        }

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String match = matcher.group(0);
            if (match.substring(0,1).equals("#")) {
                match = match.substring(1,match.length());
            }
            tags.add(match);
        }

        return tags;
    }

    public static String replaceTag(String text, String from, String to) {
        return text.replaceAll("\\#"+from+"(\\s|_|)", "#"+to+"$1");
    }

    public static Tag nullSafeParseTag(String tagName) {
        Tag tag = null;
        try {
            tag = Tag.valueOf(tagName.toUpperCase());
        } catch (IllegalArgumentException ignore) {
        }
        return tag;
    }


    private static String[] getTagsStrings(Tag... tags) {
        List<String> result = new LinkedList<String>();
        for (Tag tag : tags) {
            result.add(tag.getTagString());
        }
        return result.toArray(new String[0]);
    }

    private static String[] getImplicitTagStrings() {
        List<String> result = new LinkedList<String>();
        for (Tag tag : values()) {
            if (tag.isImplicit()) {
                result.add(tag.getTagString());
            }
        }
        return result.toArray(new String[0]);
    }

    private static String[] getKnownTagStrings() {
        List<String> result = new LinkedList<String>();
        for (Tag tag : values()) {
            result.add(tag.getTagString());
        }
        return result.toArray(new String[0]);
    }

    private static String[] getAllTagStrings() {
        List<String> result = new LinkedList<String>();
        for (Tag tag : values()) {
            result.add(tag.getTagString());
        }
        return result.toArray(new String[0]);
    }

    private static String[] implicitTagStrings = getImplicitTagStrings();

    private static String[] knownTagStrings = getKnownTagStrings();


    public static String filterTags(String text) {
        if (text == null) {
            return null;
        }

        return removeKnownTags(text);
    }

    public static String highlightTags(String text) {
        if (text == null) {
            return null;
        }

        if (KNOWN_TAG_QUANTITY_VALUE_PATTERN.matcher(text).replaceAll("").trim().length() == 0) {
            return "";
        }

        return TAG_QUANTITY_VALUE_PATTERN_NO_EMOJI.matcher(text).replaceAll("<b>$1</b>");
    }

    public static String collapseTags(String text) {
        if (text == null) {
            return null;
        }

        return TAG_QUANTITY_VALUE_PATTERN.matcher(text).replaceAll("T");
    }


    private final static Pattern removeDoubleWhitespace = Pattern.compile("  ");

    // TODO: Or is there a better way to do that? Perhaps directly in the tag removal?
    private static String removeDoubleWhitespace(String text) {
        Matcher matcher = removeDoubleWhitespace.matcher(text);
        if (!matcher.matches()) {
            return text;
        }

        return matcher.replaceAll(" ");
    }

    public static String removeTag(String comment, String tagString) {
        Pattern tagReplacePattern = Pattern.compile(tagString + "(_\\d+x|_[a-zA-Z0-9|-]+)?(\\s|$|)");
        return removeTagsUsingPattern(comment, tagReplacePattern);
        //return comment.replaceAll(tagString + "(\\s|$)", "").replaceAll(tagString + "_\\d+x(\\s|$)", "").replaceAll(tagString + "_[a-zA-Z0-9|-]+(\\s|$)", "").replaceAll("  ", " ");
    }

    private static Pattern getRemoveTagsPattern(String... tagStrings) {
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("(");
        boolean first = true;
        for (String tagString : tagStrings) {
            if (!first) {
                patternBuilder.append("|");
            }
            patternBuilder.append(tagString);
            first = false;
        }
        patternBuilder.append(")");
        patternBuilder.append("(_\\d+x|_[a-zA-Z0-9|-]+)?(\\s|$)");
        return Pattern.compile(patternBuilder.toString());
    }

    // Optimized version to remove multiple tags at once.
    private static String removeTags(String comment, String... tagStrings) {
        return removeTagsUsingPattern(comment, getRemoveTagsPattern(tagStrings));
        //return comment.replaceAll(tagString + "(\\s|$)", "").replaceAll(tagString + "_\\d+x(\\s|$)", "").replaceAll(tagString + "_[a-zA-Z0-9|-]+(\\s|$)", "").replaceAll("  ", " ");
    }

    private static Pattern removeGeoTagsPattern = getRemoveTagsPattern(getTagsStrings(HOME, GEO0, GEO1, GEO2, GEO3));

    private static String removeGeoTags(String comment) {
        return removeTagsUsingPattern(comment, removeGeoTagsPattern);
    }

    private static Pattern removeImplicitTagsPattern = getRemoveTagsPattern(implicitTagStrings);

    private static Pattern removeKnownTagsPattern = getRemoveTagsPattern(knownTagStrings);

    private static String removeImplicitTags(String comment) {
        return removeTagsUsingPattern(comment, removeImplicitTagsPattern);
    }

    private static String removeKnownTags(String comment) {
        return removeTagsUsingPattern(comment, removeKnownTagsPattern);
    }

    private static String removeTagsUsingPattern(String comment, Pattern pattern) {
        Matcher m = pattern.matcher(comment);
        String result = m.replaceAll("");
        //Logger.logInfo("XXXX USING PATTER: " + pattern.toString());
        //Logger.logInfo("FROM: " + comment + "|");
        // Not sure if needed? We remove trailing white-space, but this does not solve the problem if there is white-space at the end?
        result = removeDoubleWhitespace(result);
        //Logger.logInfo("POST: " + result + "|");
        return result;
    }

    public static boolean hasTag(String comment, Tag tag) {
        return hasTag(comment, tag.getTagString());
    }

    public static boolean hasTag(String comment, String tagString) {
        if (comment == null) {
            return false;
        }
        return comment.contains(tagString);
    }

    public static boolean hasOneOfTags(String comment, Tag... tag) {
        for (Tag tag1 : tag) {
            if (hasTag(comment, tag1)) {
                return true;
            }
        }
        return false;
    }

    private static String matchGroup(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(group);
        }

        return null;
    }

    private static boolean matches(String patternString, String text) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);

        return matcher.find();
    }

    private static Drawable emptyDrawable;

    
    
    public static Set<String> getComplementStandardTags(Set<String> tags) {
        final Set<String> complementTags = new LinkedHashSet<String>();
        String tagName;
        for (Tag tag: values) {
            tagName = tag.getTagName();
            if (tags == null || !tags.contains(tagName)) {
                complementTags.add(tagName);
            }
        }
        return complementTags;
    }

    private static Tag[] values = values();

}
