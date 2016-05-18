package com.demo.demogalleries;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by caominhvu on 5/18/16.
 */
public class CategoriesUtils {
    private static HashMap<String, Integer> mCategories = new LinkedHashMap<>();
    static {

        mCategories.put("Abstract", 10);
        mCategories.put("Animals", 11);
        mCategories.put("Black and White", 5);
        mCategories.put("Celebrities", 1);
        mCategories.put("City and Architecture", 9);
        mCategories.put("Commercial", 15);
        mCategories.put("Concert", 16);
        mCategories.put("Family", 20);
        mCategories.put("Fashion", 14);
        mCategories.put("Film", 2);
        mCategories.put("Fine Art", 24);
        mCategories.put("Food", 23);
        mCategories.put("Journalism", 3);
        mCategories.put("Landscapes", 8);
        mCategories.put("Macro", 12);
        mCategories.put("Nature", 18);
        mCategories.put("Nude", 4);
        mCategories.put("People", 7);
        mCategories.put("Performing Arts", 19);
        mCategories.put("Sport", 17);
        mCategories.put("Still Life", 6);
        mCategories.put("Street", 21);
        mCategories.put("Transportation", 26);
        mCategories.put("Travel", 13);
        mCategories.put("Underwater", 22);
        mCategories.put("Urban Exploration", 27);
        mCategories.put("Wedding", 25);
        mCategories.put("Uncategorized", 0);
    }

    public static int getId(String category) {
         return mCategories.containsKey(category) ? mCategories.get(category) : -1;
    }

    public static String[] getCategories() {
        return mCategories.keySet().toArray(new String[mCategories.size()]);
    }
}
