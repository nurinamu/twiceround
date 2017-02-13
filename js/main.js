//import $ from 'jquery';
var $ = require('jquery');
var maxOffset = 100;
var queryStr = 'twice';
window.$ = window.jquery = $;
init();

function init() {
    $("#imgBtn").click(main);
    $("#clearBtn").click(clearAll);
    $("#addApiBtn").click(addApi);
    $("#listBtn").click(toggleStored);
    $("#settingsBtn").click(toggleSettings);
    $("#disLikeBtn").click(dislike);

    main();
}

var isLandscape;
var currentOffset;

function main() {
    isLandscape = window.innerWidth > window.innerHeight;
    console.log("init twiceround->isLandscape[" + isLandscape + "]");
    getFromStorage(["twice_cse_api_key", "twice_cse_cx"], function (data) {
        if (data) {
            if (data.twice_cse_api_key && data.twice_cse_cx) {
                start(data.twice_cse_api_key, data.twice_cse_cx);
            } else {
                openApiInput();
            }
        } else {
            console.log("data is not existed!");
        }
    });

}

function openApiInput() {
    $("#settings").css("display", "block");
}

function addApi() {
    var newKey = $.trim($("#cseApi").val());
    var newCx = $.trim($("#cx").val());
    if (newKey == '') {
        alert("api key is needed.");
        return;
    }
    if (newCx == '') {
        alert("cx value is needed");
        return
    }
    if (newKey != '') {
        setToStorage({
            "twice_cse_api_key": newKey,
            "twice_cse_cx": newCx
        }, function () {
            start(newKey, newCx);
        });
    }
}

function start(apiKey, cx) {
    $("#cseApi").val(apiKey);
    $("#cx").val(cx);

    getOffset(function (offset) {
        getFromStorage("twice_items", function (data) {
            console.log(data);
            if (data.twice_items && data.twice_items.length >= offset) {
                console.log("get from cache! ->" + offset);
                if (isLandscape == data.twice_items[offset - 1].isLandscape) {
                    setBackground(data.twice_items[offset - 1].url);
                    setOffset(offset + 1, function () {
                    });
                } else {
                    setOffset(offset + 1, function () {
                    });
                    console.log("not matched orientation. go next");
                    start(apiKey, cx);
                }
            } else {
                console.log("get search results ->" + offset);
                cse(apiKey, cx, offset);
            }
        });
    });
}


/**
 * google custom search engine을 사용
 */
function cse(apiKey, cx, offset) {
    $.ajax({
        url: 'https://www.googleapis.com/customsearch/v1?parameters',
        type: 'GET',
        data: {
            key: apiKey,
            cx: cx,
            searchType: 'image',
            imgSize: 'xxlarge',
            q: queryStr,
            start: offset
        },
        success: function (resp) {
            console.log(resp);
            if (resp.items) {
                getFromStorage("twice_items", function (data) {
                    var twiceItems = data.twice_items;
                    if (twiceItems == undefined) {
                        twiceItems = [];
                    }
                    for (key in resp.items) {
                        twiceItems.push({
                            url: resp.items[key].link,
                            isLandscape: resp.items[key].image.width > resp.items[key].image.height,
                            thumbnail: resp.items[key].image.thumbnailLink
                        });
                    }
                    storeItems(twiceItems, function () {
                        start(apiKey, cx);
                        showImgList();
                    });
                });
            } else {
                if (offset) {
                    setOffset(1, function () {
                        start(apiKey, cx);
                    });
                } else {
                    console.log("img is not found");
                }

            }

        },
        complete: function (xhr, textStatus) {
            console.log("respose : " + xhr.status());
            if (offset == 1 && xhr.status == 403) {
                //backup recovery.
                recovery();
            }
        }
    })
}

function backup(items) {
    console.log("try to backup items");
    setToStorage({backup_items: items}, function () {
        console.log("backup is complted : " + items.length);
    });
}

function recovery() {
    console.log("try to recovery items");
    getFromStorage("backup_items", function (data) {
        if (data.backup_items && data.backup_items.length > 0) {
            storeItems(data.backup_items, function () {
                console.log("recovery is done.");
                main();
            });
        } else {
            console.log("There is no backup. --> need another action.");
        }
    });
}

function storeItems(data, callback) {
    setToStorage({twice_items: data}, callback);
}

function setBackgroundWithArr(items) {
    $("body").css('background-image', 'url("' + selectData(items) + '")');
}

function getOffset(callback) {
    getFromStorage("twice_offset", function (data) {
        if (!isNaN(data.twice_offset)) {
            callback(data.twice_offset);
        } else {
            callback(1);
        }
    });
}

function setOffset(offset, callback) {
    if (offset > maxOffset) {
        offset = 1;
    }
    setToStorage({twice_offset: offset}, callback);
}

function getFromStorage(key, callback) {
    chrome.storage.local.get(key, callback);
}

function setToStorage(data, callback) {
    chrome.storage.local.set(data, callback);
}

function setBackground(url) {
    setOriginInfo(url);
    $("body").css('background-image', 'url("' + url + '")');
}

function selectData(datas) {
    return datas[Math.floor(Math.random() * datas.length)];
}

function toggleStored() {
    if ($("#imgList").css("display") == "none") {
        showImgList();
        $("#imgList").css("display", "block");
    } else {
        $("#imgList").css("display", "none");
    }

}

function showImgList() {
    getFromStorage("twice_items", function (data) {
        console.log(data);
        if (data.twice_items && data.twice_items.length > 0) {
            console.log("get from cache to list!");
            imgTable = $("#imgList table");
            if (imgTable[0].rows.length > 0) {
                imgTable[0].deleteRow(0);
            }
            imgRow = $("<tr></tr>");
            for (key in data.twice_items) {
                imgCell = $("<td></td>");
                newImg = $("<img />");
                newImg.attr("src", data.twice_items[key].thumbnail);
                newImg.attr("data-url", data.twice_items[key].url);
                newImg.attr("data-offset", (parseInt(key) + 1));
                newImg.click(function (obj) {
                    setBackground($(obj.target).attr("data-url"));
                    setOffset(nextOffset(parseInt($(obj.target).attr("data-offset"))), function () {
                        console.log("[" + $(obj.target).attr("data-offset") + "] is selected");
                    });
                });
                imgCell.append(newImg);
                imgRow.append(imgCell);
            }
            imgTable.append(imgRow);
        }
    });
}

function clearAll() {
    getFromStorage("twice_items", function (data) {
        if (data.twice_items && data.twice_items.length > 0) {
            backup(data.twice_items);
        }
        storeItems([], function () {
            setOffset(1, function () {
                console.log("clear all data");
            });
        });
    });
}

function toggleSettings() {
    $("#settings").toggle();
}

function setOriginInfo(url) {
    $("#origin a").attr("href", url);
    $("#origin a").text(url);
}

function dislike() {
    getOffset(function (offset) {
        console.log("[" + prevOffset(offset) + "] is disliked.");
    });
}

function nextOffset(offset) {
    if (++offset > maxOffset) {
        offset = 1;
    }
    return offset;
}

function prevOffset(offset) {
    if (--offset < 1) {
        offset = maxOffset;
    }
    return offset;
}