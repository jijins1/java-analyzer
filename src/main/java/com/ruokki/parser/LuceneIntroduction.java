package com.ruokki.parser;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

public class LuceneIntroduction {
    public static void main(String[] args) {
        final Set<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");
        final Set<String> strings2 = new HashSet<>();
        strings2.add("b");
        final Sets.SetView<String> intersection = Sets.intersection(strings, strings2);
        final Sets.SetView<String> difference = Sets.difference(strings, strings2);

        System.out.println(intersection);
        System.out.println(difference);

    }
    // ...
}