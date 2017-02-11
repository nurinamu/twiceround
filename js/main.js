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

    main();
}

var twiceApiKey;
var isLandscape;

function main() {
    isLandscape = window.innerWidth > window.innerHeight;
    console.log("init twiceround->isLandscape[" + isLandscape + "]");
    getFromStorage("twice_cse_api_key", function (data) {
        if (data) {
            twiceApiKey = data.twice_cse_api_key;
            if (twiceApiKey) {
                start(twiceApiKey);
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
    if (newKey != '') {
        twiceApiKey = newKey;
        setToStorage({
            "twice_cse_api_key": newKey
        }, function () {
            start();
        });
    }
}

function start(apiKey) {
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
                    start(apiKey);
                }
            } else {
                console.log("get search results ->" + offset);
                cse(apiKey, offset);
            }
        });
    });
}


/**
 * google custom search engine을 사용
 */
function cse(apiKey, offset) {
    $.ajax({
        url: 'https://www.googleapis.com/customsearch/v1?parameters',
        type: 'GET',
        data: {
            key: apiKey,
            cx: '017778292064153678241:lc5ig_122zu',
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
                        start(apiKey);
                        showImgList();
                    });
                });
            } else {
                if (offset) {
                    setOffset(1, function () {
                        start(apiKey);
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