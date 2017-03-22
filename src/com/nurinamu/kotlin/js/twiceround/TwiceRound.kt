package com.nurinamu.kotlin.js.twiceround

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
    var maxOffset = 100
    var queryStr = "twice"
    var isLandscape: Boolean = false
    var currentOffset: Int = 1
    lateinit var myDislikes: Array<String>

    fun init() {
        jQuery("#imgBtn").click(this::showBackground)   //TODO:Function reference
        jQuery("#clearBtn").click(this::clearAll)
        jQuery("#addApiBtn").click(this::addApi)
        jQuery("#listBtn").click(this::toggleStored)
        jQuery("#settingsBtn").click(this::toggleSettings)
        jQuery("#disLikeBtn").click(this::dislike)

        showBackground(null)
    }

    private fun showBackground(eventObject: JQueryEventObject?) {
        println("ShowBackGround")
        isLandscape = window.innerWidth > window.innerHeight
        println("init twiceround->isLandscape[$isLandscape]")
        jQuery("#imgList").css("display", "none")
        jQuery("#settings").css("display", "none")
        local.get(arrayOf("twice_cse_api_key", "twice_cse_cx"), this::handleStorage)
    }

    private fun handleStorage(data: Json?) {
        if (data == null) {
            println("data is not existed")
            return
        }

        data?.apply {
            if (get("twice_cse_api_key") != null && get("twice_cse_cx") != null) {
                execute(get("twice_cse_api_key") as String, get("twice_cse_cx") as String)
            } else {
                openApiInput()
            }
        }
    }

    private fun execute(apiKey: String, cx: String) {
        println("execute main function.")
        jQuery("#cseApi").`val`(apiKey) //WOW
        jQuery("#cx").`val`(cx)

        local.get("my_dislikes") {
            data ->
            data?.apply {
                myDislikes = get("my_dislides") as Array<String>? ?: emptyArray()

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
                                    setBackground(it["url"] as String, it["thumbnail"] as String)
                                    setOffset(offset + 1, {})
                                } else {
                                    setOffset(offset + 1, {
                                        execute(apiKey, cx)
                                    })
                                }
                            }
                        } else {
                            println("get search results -> $offset")
                            ext.jQuery.ajax(CSERequest(apiKey, cx, offset))
                        }
                    }
                }
            }
        }
    }

    private fun setOffset(offset: Int, callback: () -> Unit) {
        var decided = offset
        if (offset > maxOffset) {
            decided = 1
        }
        local.set(TwiceOffset(decided), callback)
    }

    private fun setBackground(url: String, thumbnail: String) {
        setOriginInfo(url)
        var background = jQuery("body").css("background-image", "url('$thumbnail')")

        var tmpImg = jQuery("<img>")
        tmpImg.attr("src", url)
        tmpImg.on("load") { jQueryEventObject: JQueryEventObject, any: Any ->
            println("org img is loaded")
            background.css("background-image", "url('$url')")
        }

    }

    private fun setOriginInfo(url: String) {
        jQuery("#origin a").attr("href", url)
        jQuery("#origin a").text(url)


    }

    inner class CSERequest(val apiKey: String, val cx: String, val offset: Int?) : JQueryAjaxSettings {

        init {
            url = "https://www.googleapis.com/customsearch/v1?parameters"
            type = "GET"
            val injectData: dynamic = js("({})")
            injectData["key"] = apiKey
            injectData["cx"] = cx
            injectData["searchType"] = "image"
            injectData["imgSize"] = "xxlarge"
            injectData["q"] = queryStr
            injectData["start"] = offset
            data = injectData
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
                                    if (myDislikes.indexOf(link) < 0 &&
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
                        var updated: TwiceItems = TwiceItems(twiceItems)
                        local.set(updated) {
                            println("updated data!")
                            execute(apiKey, cx)
                            showImgList()
                        }
                    }
                } else {
                    if (offset != null) {
                        setOffset(1) {
                            execute(apiKey, cx)
                        }
                    } else {
                        println("img is not found")
                    }

                }
                return true
            }

            complete = fun(jqXHR: JQueryXHR, textStatus: String): Any {
                errorHandle(offset ?: 1, jqXHR, textStatus)
                return true
            }
        }


        fun createTwiceItem(url: String, isLandscape: Boolean, thumbnail: String): Json {
            val res: dynamic = js("({})")
            res["url"] = url
            res["isLandscape"] = isLandscape
            res["thumbnail"] = thumbnail
            return res
        }
    }

    fun alreadyIncluded(twiceItems: Array<Json>, url: String): Boolean {
        println("compare with $url")
        twiceItems.forEach {
            if (it["url"].toString() == url) {
                println("already included : $url")
                return true
            }
        }

        return false
    }

    private fun getOffset(handleOffset: (offset: Int) -> Unit) {
        local.get("twice_offset") {
            var twiceOffset: Int = (it?.get("twice_offset") ?: 1).toString().toInt()
            handleOffset(twiceOffset)
        }
    }

    private fun openApiInput() {
        jQuery("#settings").css("display", "block")
    }

    private fun toggleStored(eventObject: JQueryEventObject) {
        println("ToogleStored")
        if (jQuery("#imgList").css("display") == "none") {
            showImgList()
            jQuery("#imgList").css("display", "block")
        } else {
            jQuery("#imgList").css("display", "none")
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
                    showBackground(null)
                }
            }

        }
    }

    private fun errorHandle(offset:Int, xhr:JQueryXHR, textStatus:String) {
        val status = xhr.status.toInt()
        println("error respose : $status")

        if (offset != 1 && (status == 400 || status == 403)) {
            setOffset(1){
                showBackground(null)
            }
        } else if (offset == 1 && status == 403) {
            //backup recovery.
            recovery()
        }
    }


    private fun backup(twiceItems: Array<Json>) {
        local.set(TwiceBackupItems(twiceItems)) {
            println("backup is complted : " + twiceItems.size)
        }
    }

    private fun recovery() {
        println("try to recovery items")
        local.get("backup_items") {
            data ->
            val twiceItems = data["backup_items"] as Array<Json>
            if ( twiceItems != null && twiceItems.size > 0) {
                local.set(TwiceItems(twiceItems)) {
                    println("recovery is done.")
                    showBackground(null)
                }
            } else {
                println("There is no backup. --> need another action.")
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

    private fun dislike(eventObject: JQueryEventObject) {
        println("dislike")
        getOffset {
            offset ->
            local.get("twice_items") {
                data ->
                val twiceItems = data["twice_items"] as Array<Json>
                val currentKey = prevOffset(offset)
                val url = twiceItems[currentKey - 1].get("url") as String
                myDislikes.set(myDislikes.size, url)
                println(myDislikes)
                println("[" + currentKey + "] is disliked. : " + url)
                println("twiceImtes size : ${twiceItems.size}")

                var tmp = twiceItems.toMutableList()
                tmp.removeAt(currentKey - 1)
                local.set(TwiceItemsWithDislikes(tmp.toTypedArray(), myDislikes)) {
                    setOffset(currentKey) {
                        showBackground(null)
                    }
                }
            }
        }
    }

    private fun showImgList() {
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
                    if (currentOffset != (idx+1)) {
                        currentOffset = idx + 1
                        setBackground(jQuery(obj.target).attr("data-url"), it["thumbnail"] as String)
                        setOffset(nextOffset(idx + 1)) {
                            println("[${idx+1}] is selected")
                        }
                    } else {
                        println("already displayed [$currentOffset] == [${idx+1}]")
                    }
                })
                imgCell.append(newImg)
                imgRow.append(imgCell)
            }
            imgTable.append(imgRow)
        }
    }

    private fun nextOffset(offset: Int): Int {
        var updated = offset + 1
        if (updated > maxOffset) {
            return 1
        }
        return updated
    }

    private fun prevOffset(offset: Int): Int {
        var updated = offset - 1
        if (updated < 1) {
            return maxOffset
        }
        return updated
    }

}

@JsName("$")
external fun jQuery(selector: String): JQuery

data class TwiceApiKey(var twice_cse_api_key: String, var twice_cse_cx: String)

data class TwiceOffset(var twice_offset: Int)

data class TwiceItems(var twice_items: Array<Json>)

data class TwiceBackupItems(var backup_items: Array<Json>)

data class TwiceItemsWithDislikes(var twice_items: Array<Json>, var my_dislikes: Array<String>)