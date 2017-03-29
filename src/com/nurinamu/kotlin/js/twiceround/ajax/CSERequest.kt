package com.nurinamu.kotlin.js.twiceround.ajax

import com.nurinamu.kotlin.js.twiceround.TwiceCursor
import com.nurinamu.kotlin.js.twiceround.TwiceItems
import com.nurinamu.kotlin.js.twiceround.TwiceRound
import com.nurinamu.kotlin.js.twiceround.TwiceRound.Companion.maxOffset
import com.nurinamu.kotlin.js.twiceround.TwiceRound.Companion.queryStr
import com.nurinamu.kotlin.js.twiceround.getOffset
import com.nurinamu.kotlin.js.twiceround.setOffset
import ext.JQueryAjaxSettings
import ext.JQueryXHR
import ext.chrome.local
import kotlin.js.Json

/**
 * Created by nurinamu on 2017. 3. 22..
 */
class CSERequest(val apiKey: String, val cx: String, val cursor: Int, val twiceRound: TwiceRound) : JQueryAjaxSettings {

    init {
        url = "https://www.googleapis.com/customsearch/v1?parameters"
        type = "GET"
        data = createInjectData(apiKey, cx, cursor, queryStr)
        success = fun(resp: Any, textStatus: String, jqXHR: JQueryXHR): Any {
            println("success invoked from cursor : $cursor")
            var nextCursor = cursor
            var respJson: Json = resp as Json
            if (respJson["items"] != null) {
                local.get("twice_items") {
                    val twiceItems: Array<Json> = (it["twice_items"] as Array<Json>?) ?: emptyArray()

                    (respJson["items"] as Array<Json>)
                        .forEach {
                            val link = it["link"] as String
                            val image = it["image"] as Json
//                            println("each : $link")
                            if (twiceItems.size < maxOffset) {
                                if (twiceRound.myDislikes.indexOf(link) < 0 &&
                                    !alreadyIncludedShort(twiceItems, link)) {
                                    twiceItems.set(twiceItems.size, createTwiceItem(
                                        link,
                                        image["width"] as Int > image["height"] as Int,
                                        image["thumbnailLink"] as String
                                    ))
                                    println("added : $link")
                                } else {
                                    println("skip : $link - increase offset")
                                }
                                nextCursor++
                            } else {
                                return@forEach  //TODO
                            }
                        }
                    local.set(TwiceCursor(nextCursor)) {
                        println("updated Cursor : $nextCursor")
                        local.set(TwiceItems(twiceItems)) {
//                            println("updated data!")
                            twiceRound.execute(apiKey, cx)
                            twiceRound.showImgList()
                        }
                    }
                }
            } else {
                cursor?.let {
                    setOffset(1) {
                        twiceRound.execute(apiKey, cx)
                    }
                } ?: println("img is not found")
            }
            return true
        }

        complete = fun(jqXHR: JQueryXHR, textStatus: String): Any {
            errorHandle(jqXHR, textStatus)
            return true
        }
    }

    private fun createInjectData(apiKey: String, cx: String, cursor: Int?, queryStr: String) {
        val injectData: dynamic = js("({})")
        injectData["key"] = apiKey
        injectData["cx"] = cx
        injectData["searchType"] = "image"
        injectData["imgSize"] = "xxlarge"
        injectData["q"] = queryStr
        injectData["start"] = cursor
        return injectData
    }

    private fun createTwiceItem(url: String, isLandscape: Boolean, thumbnail: String): Json {
        val res: dynamic = js("({})")
        res["url"] = url
        res["isLandscape"] = isLandscape
        res["thumbnail"] = thumbnail
        return res
    }

    private fun errorHandle(xhr: JQueryXHR, textStatus: String) {
        getOffset {
            offset ->
            val status = xhr.status.toInt()
            println("respose : $status")

            if (offset != 1 && (status == 400 || status == 403)) {
                setOffset(1) {
                    twiceRound.showTwice(null)
                }
            } else if (offset == 1 && status == 403) {
                //backup recovery.
                twiceRound.recovery()
            }
        }
    }

    private fun alreadyIncluded(twiceItems: Array<Json>, url: String): Boolean
        = twiceItems.
        apply { println("compare with $url") }.
        any { it["url"].toString() == url }.
        also { if (it) println("already included : $url") }

    private fun alreadyIncludedShort(twiceItems: Array<Json>, url: String): Boolean
        = twiceItems.any { it["url"].toString() == url }

}