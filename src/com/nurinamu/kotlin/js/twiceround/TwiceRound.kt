package com.nurinamu.kotlin.js.twiceround

import com.nurinamu.kotlin.js.twiceround.ajax.CSERequest
import ext.JQuery
import ext.JQueryAjaxSettings
import ext.JQueryEventObject
import ext.JQueryXHR
import ext.chrome.local
import kotlin.browser.window
import kotlin.js.Json

/**
 * Created by nurinamu on 2017. 3. 13..
 */
class TwiceRound {

    companion object {
        var maxOffset = 100
        var queryStr = "twice"
    }

    var isLandscape: Boolean = false
    var currentOffset: Int = 1
    lateinit var myDislikes: Array<String>

    fun init() {
        jQuery("#imgBtn").click(this::showTwice)   //TODO:Function reference
        jQuery("#clearBtn").click(this::clearAll)
        jQuery("#addApiBtn").click(this::addApi)
        jQuery("#listBtn").click(this::toogleImgList)
        jQuery("#settingsBtn").click(this::toggleSettings)
        jQuery("#disLikeBtn").click(this::dislike)

        showTwice(null)
    }

    fun execute(apiKey: String, cx: String) {
        println("execute main function.")
        jQuery("#cseApi").`val`(apiKey) //WOW
        jQuery("#cx").`val`(cx)

        local.get("my_dislikes") {
            myDislikes = it["my_dislides"] as Array<String>? ?: emptyArray()

            getOffset {
                offset ->
                local.get("twice_items") {
                    val twiceItems = it["twice_items"] as Array<Json>
                    println("current Size : ${twiceItems.size}")
                    if (twiceItems.size >= offset) {
                        println("get from cache! -> $offset")
                        twiceItems[offset - 1]?.let {
                            if (isLandscape == (it["isLandscape"] as Boolean?) ?: false) {
                                currentOffset = offset - 1
                                setBackgroundImage(it["url"] as String, it["thumbnail"] as String)
                                setOffset(offset + 1, {})
                            } else {
                                setOffset(offset + 1, {
                                    execute(apiKey, cx)
                                })
                            }
                        }
                    } else {
                        println("get search results -> $offset")
                        ext.jQuery.ajax(CSERequest(apiKey, cx, offset, this))
                    }
                }
            }
        }
    }

    fun showTwice(eventObject: JQueryEventObject?) {
        println("ShowBackGround")
        isLandscape = window.innerWidth > window.innerHeight
        println("init twiceround->isLandscape[$isLandscape]")
        jQuery("#imgList").css("display", "none")
        jQuery("#settings").css("display", "none")
        local.get(arrayOf("twice_cse_api_key", "twice_cse_cx"), this::loadApiKey)
    }

    private fun setBackgroundImage(url: String, thumbnail: String) {
        url.setOriginInfo()
        var background = jQuery("body").css("background-image", "url('$thumbnail')")

        var tmpImg = jQuery("<img>")
        tmpImg.attr("src", url)
        tmpImg.on("load") { jQueryEventObject: JQueryEventObject, any: Any ->
            println("org img is loaded")
            background.css("background-image", "url('$url')")
        }

    }

    private fun clearAll(eventObject: JQueryEventObject) {
        println("clearAll")
        local.get("twice_items") {
            try {
                if (it["twice_items"] != null && (it["twice_items"] as Array<Json>).size > 0) {
                    backup(it["twice_items"] as Array<Json>)
                }
            } catch (e: Exception) {
                println(e.message)
            }

            local.set(TwiceItems(emptyArray())) {
                setOffset(1) {
                    println("clear all data")
                    showTwice(null)
                }
            }

        }
    }

    private fun backup(twiceItems: Array<Json>) {
        local.set(TwiceBackupItems(twiceItems)) {
            println("backup is complted : " + twiceItems.size)
        }
    }

    fun recovery() {
        println("try to recovery items")
        local.get("backup_items") {
            val twiceItems = it["backup_items"] as Array<Json>
            if (twiceItems != null && twiceItems.size > 0) {
                local.set(TwiceItems(twiceItems)) {
                    println("recovery is done.")
                    showTwice(null)
                }
            } else {
                println("There is no backup. --> need another action.")
            }
        }
    }

    private fun loadApiKey(data: Json) {
        data.apply {
            if (get("twice_cse_api_key") != null && get("twice_cse_cx") != null) {
                execute(get("twice_cse_api_key") as String, get("twice_cse_cx") as String)
            } else {
                openSettings()
            }
        }
    }

    private fun addApi(eventObject: JQueryEventObject) {
        println("addApi")
        var newKey = (jQuery("#cseApi").`val`() as String).trim()
        var newCx = (jQuery("#cx").`val`() as String).trim()
        if (newKey.isBlank()) {
            window.alert("api key is needed.")
            return
        }
        if (newCx.isBlank()) {
            window.alert("cx value is needed");
            return
        }
        local.set(TwiceApiKey(newKey, newCx)) {
            execute(newKey, newCx)
        }
    }

    private fun toggleSettings(eventObject: JQueryEventObject) {
        println("toggleSettings")
        jQuery("#settings").toggle()
    }

    private fun openSettings() {
        jQuery("#settings").css("display", "block")
    }

    private fun dislike(eventObject: JQueryEventObject) {
        println("dislike")
        getOffset {
            offset ->
            local.get("twice_items") {
                data ->
                val twiceItems = data["twice_items"] as Array<Json>
                val currentKey = offset.prevOffset()
                val url = twiceItems[currentKey - 1].get("url") as String
                myDislikes.set(myDislikes.size, url)
                println(myDislikes)
                println("[$currentKey] is disliked. : $url")
                println("twiceImtes size : ${twiceItems.size}")

                var tmp = twiceItems.toMutableList()
                tmp.removeAt(currentKey - 1)
                local.set(TwiceItemsWithDislikes(tmp.toTypedArray(), myDislikes)) {
                    setOffset(currentKey) {
                        showTwice(null)
                    }
                }
            }
        }
    }

    fun showImgList() {
        println("showImgList")
        local.get("twice_items") {
            data ->
            println("get from cache to list!")
            var twiceItems = data["twice_items"] as Array<Json>
            var imgTable: dynamic = jQuery("#imgList table")
            var imgRow: dynamic = jQuery("<tr></tr>")
            twiceItems?.apply {
                if (imgTable[0].rows.length > 0) {
                    imgTable[0].deleteRow(0)
                }
            }.forEachIndexed {
                idx, it ->
                val imgCell: dynamic = jQuery("<td></td>")
                val newImg: dynamic = jQuery("<img />")
                newImg.attr("src", it["thumbnail"])
                newImg.attr("data-url", it["url"])
                newImg.click({
                    obj ->
                    println("click!! $idx : ${obj.target}")
                    if (currentOffset != (idx + 1)) {
                        currentOffset = idx + 1
                        setBackgroundImage(jQuery(obj.target).attr("data-url"), it["thumbnail"] as String)
                        setOffset((idx + 1).nextOffset()) {
                            println("[${idx + 1}] is selected")
                        }
                    } else {
                        println("already displayed [$currentOffset] == [${idx + 1}]")
                    }
                })
                imgCell.append(newImg)
                imgRow.append(imgCell)
            }
            imgTable.append(imgRow)
        }
    }

    private fun toogleImgList(eventObject: JQueryEventObject) {
        println("ToogleStored")
        if (jQuery("#imgList").css("display") == "none") {
            showImgList()
            jQuery("#imgList").css("display", "block")
        } else {
            jQuery("#imgList").css("display", "none")
        }
    }

    private fun String.setOriginInfo() {
        jQuery("#origin a").attr("href", this)
        jQuery("#origin a").text(this)
    }

    private fun Int.nextOffset(): Int = if ((this + 1) > maxOffset) 1 else (this + 1)

    private fun Int.prevOffset(): Int = if ((this - 1) < 1) maxOffset else (this - 1)

}

@JsName("$")
external fun jQuery(selector: String): JQuery

data class TwiceApiKey(var twice_cse_api_key: String, var twice_cse_cx: String)

data class TwiceOffset(var twice_offset: Int)

data class TwiceItems(var twice_items: Array<Json>)

data class TwiceBackupItems(var backup_items: Array<Json>)

data class TwiceItemsWithDislikes(var twice_items: Array<Json>, var my_dislikes: Array<String>)

fun getOffset(handleOffset: (offset: Int) -> Unit) {
    local.get("twice_offset") {
        handleOffset((it["twice_offset"] ?: 1) as Int)
    }
}

fun setOffset(offset: Int, callback: () -> Unit) {
    local.set(TwiceOffset(offset.validOffset(TwiceRound.maxOffset)), callback)
}

fun Int.validOffset(maxOffset: Int): Int = if (this > maxOffset) 1 else this