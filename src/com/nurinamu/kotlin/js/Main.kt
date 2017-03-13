package com.nurinamu.kotlin.js

import ext.JQuery

/**
 * Created by nurinamu on 2017. 3. 5..
 */

@JsName("$")
external fun jQuery(selector: String): JQuery


fun main(argv: Array<String>) {
    println("new package kotlin.")

    println(jQuery("span").text())

}