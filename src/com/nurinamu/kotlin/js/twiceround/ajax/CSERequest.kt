package com.nurinamu.kotlin.js.twiceround.ajax

import com.nurinamu.kotlin.js.twiceround.TwiceItems
import com.nurinamu.kotlin.js.twiceround.TwiceRound
import com.nurinamu.kotlin.js.twiceround.TwiceRound.Companion.maxOffset
import com.nurinamu.kotlin.js.twiceround.TwiceRound.Companion.queryStr
import com.nurinamu.kotlin.js.twiceround.setOffset
import ext.JQueryAjaxSettings
import ext.JQueryXHR
import ext.chrome.local
import kotlin.js.Json

/**
 * Created by nurinamu on 2017. 3. 22..
 */
class CSERequest(val apiKey: String, val cx: String, val offset: Int?, val twiceRound: TwiceRound) : JQueryAjaxSettings {

    init {
        url = "https://www.googleapis.com/customsearch/v1?parameters"
        type = "GET"
        data = createInjectData(apiKey, cx, offset, queryStr)
        success = fun(resp: Any, textStatus: String, jqXHR: JQueryXHR): Any {
            println("success invoked from $offset")
            var respJson: Json = resp as Json
            if (respJson["items"] != null) {
                local.get("twice_items") {
                    val twiceItems: Array<Json> = (it["twice_items"] as Array<Json>?) ?: emptyArray()

                    (respJson["items"] as Array<Json>)
                        .forEach {
                            val link = it["link"] as String
                            val image = it["image"] as Json
                            println("each : $link")
                            if (twiceItems.size < maxOffset) {
                                if (twiceRound.myDislikes.indexOf(link) < 0 &&
                                    !alreadyIncluded(twiceItems, link)) {
                                    twiceItems.set(twiceItems.size, createTwiceItem(
                                        link,
                                        image["width"] as Int > image["height"] as Int,
                                        image["thumbnailLink"] as String
                                    ))
                                    println("added : $link")
                                } else {
                                    println("skip : $link")
                                }
                            } else {
                                return@forEach  //TODO
                            }
                        }
                    local.set(TwiceItems(twiceItems)) {
                        println("updated data!")
                        twiceRound.execute(apiKey, cx)
                        twiceRound.showImgList()
                    }
                }
            } else {
                offset?.let {
                    setOffset(1) {
                        twiceRound.execute(apiKey, cx)
                    }
                } ?: println("img is not found")
            }
            return true
        }

        complete = fun(jqXHR: JQueryXHR, textStatus: String): Any {
            errorHandle(offset ?: 1, jqXHR, textStatus)
            return true
        }
    }

    private fun createInjectData(apiKey: String, cx: String, offset: Int?, queryStr: String) {
        val injectData: dynamic = js("({})")
        injectData["key"] = apiKey
        injectData["cx"] = cx
        injectData["searchType"] = "image"
        injectData["imgSize"] = "xxlarge"
        injectData["q"] = queryStr
        injectData["start"] = offset
        return injectData
    }

    private fun createTwiceItem(url: String, isLandscape: Boolean, thumbnail: String): Json {
        val res: dynamic = js("({})")
        res["url"] = url
        res["isLandscape"] = isLandscape
        res["thumbnail"] = thumbnail
        return res
    }

    private fun errorHandle(offset: Int, xhr: JQueryXHR, textStatus: String) {
        val status = xhr.status.toInt()
        println("error respose : $status")

        if (offset != 1 && (status == 400 || status == 403)) {
            setOffset(1) {
                twiceRound.showTwice(null)
            }
        } else if (offset == 1 && status == 403) {
            //backup recovery.
            twiceRound.recovery()
        }
    }

    private fun alreadyIncluded(twiceItems: Array<Json>, url: String): Boolean {
        println("compare with $url")
        twiceItems.forEach {
            if (it["url"].toString() == url) {
                println("already included : $url")
                return true
            }
        }

        return false
    }
}