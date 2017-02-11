//import $ from 'jquery';
var $ = require('jquery');
var maxOffset = 100;
window.$ = window.jquery = $;
init();

function init() {
    $("#imgBtn").click(main);
    $("#clearBtn").click(clearAll);
    $("#addApiBtn").click(addApi);
    $("#listBtn").click(toggleStored);
    $("#settingsBtn").click(toggleSettings);

    main();
}

var isLandscape;

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
            q: 'twice',
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
                            thumbnail : resp.items[key].image.thumbnailLink
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
        }
    })
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
                newImg = $("<img></img>");
                newImg.attr("src", data.twice_items[key].thumbnail);
                newImg.click(function (obj) {
                    setBackground(data.twice_items[key].url);
                });
                imgCell.append(newImg);
                imgRow.append(imgCell);
            }
            imgTable.append(imgRow);
        }
    });
}

function clearAll() {
    storeItems([], function () {
        setOffset(1, function () {
            console.log("clear all data");
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