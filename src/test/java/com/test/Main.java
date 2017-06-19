package com.test;

import java.text.MessageFormat;

public class Main {

    public static void main(String[] args) {
        System.out.println(MessageFormat.format("{0}", new String[]{"ab","bc"}));
    }

}
