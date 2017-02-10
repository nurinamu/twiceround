//import $ from 'jquery';
var $ = require('jquery');
window.$ = window.jquery = $;
init();

function init() {
    $("#imgBtn").click(main);
    $("#clearBtn").click(cse);
    $("#appendBtn").click(addMoreImage);
    $("#addApiBtn").click(addApi);
    $("#listBtn").click(showStored);
}

var offset = 0;

function main() {
    console.log("init twiceround");
    chrome.storage.sync.get("twice_meta", function(data){
        if (data) {
            if (data.twice_meta && data.twice_meta.apiKey) {
                start(data.twice_meta.apiKey);
            } else {
                openApiInput();
            }
        } else {
            console.log("data is not existed!");
        }
    });

}

function openApiInput() {
    $("#settings").css("display","block");
}

function addApi() {
    var newKey = $.trim($("#cseApi").val());
    if (newKey != '') {
        chrome.storage.sync.set({
            "twice_meta" : {
                apiKey : newKey
            }
        }, function() {
           start(newKey);
        });
    }
}

function start(apiKey) {
    chrome.storage.sync.get("twice_items", function(data){
        console.log(data);
        if (data && data.twice_items.length > 0) {
            console.log("get from cache!");
            setBackgroundWithArr(data.twice_items);
        } else {
            console.log("get from search results!");
            cse(apiKey);
        }
    });
}


/**
 * google custom search engine을 사용
 */
function cse(apiKey) {
    $.ajax({
        url : 'https://www.googleapis.com/customsearch/v1?parameters',
        type : 'GET',
        data : {
            key : apiKey,
            cx : '014447932292512077827:p1xwixchex4',
            searchType : 'image',
            imgSize : 'xxlarge',
            fields : "items(link)",
            q : '나연'
        },
        success : function(resp){
            console.log(resp);
            var links = new Array();
            for(key in resp.items) {
                links.push(resp.items[key].link);
            }

            storeData(links);

            setBackgroundWithArr(links);
        }
    })
}

function addMoreImage() {

}

function storeData(data) {
    chrome.storage.sync.set({twice_items:data}, function(){
        console.log("saved! -> "+data.length);
    });
}

function setBackgroundWithArr(items) {
    $("body").css('background-image','url("'+selectData(items)+'")');
}

function setBackground(url) {
    $("body").css('background-image','url("'+url+'")');
}

function selectData(datas) {
    return datas[Math.floor(Math.random()*datas.length)];
}

function showStored() {
    chrome.storage.sync.get("twice_items", function(data){
        console.log(data);
        if (data && data.twice_items.length > 0) {
            console.log("get from cache to list!");
            imgTable = $("<table></table>");
            imgRow = $("<tr></tr>");
            for (key in data.twice_items) {
                imgCell = $("<td></td>");
                newImg = $("<img></img>");
                newImg.attr("src", data.twice_items[key]);
                newImg.click(function(obj){
                   setBackground(obj.target.src);
                });
                imgCell.append(newImg);
                imgRow.append(imgCell);
            }
            imgTable.append(imgRow);
            $("#imgList").append(imgTable);
        }
    });
}