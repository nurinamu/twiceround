//import $ from 'jquery';
var $ = require('jquery');
window.$ = window.jquery = $;

$("#imgBtn").click(main);
$("#clearBtn").click(cse);
$("#appendBtn").click(addMoreImage);

function main() {
    console.log("init twiceround");
    chrome.storage.sync.get("twice_items", function(data){
        console.log(data);
        if (data && data.twice_items.length > 0) {
            console.log("get from cache!");
            setBackground(data.twice_items);
        } else {
            console.log("get from search results!");
            cse();
        }
    });
}


/**
 * google custom search engine을 사용
 */
function cse(prevLinks) {
    $.ajax({
        url : 'https://www.googleapis.com/customsearch/v1?parameters',
        type : 'GET',
        data : {
            key : '',
            cx : '',
            searchType : 'image',
            imgSize : 'xxlarge',
            fields : "items(link)",
            q : '트와이스 나연'
        },
        success : function(resp){
            console.log(resp);
            var links = new Array();
            for(key in resp.items) {
                links.push(resp.items[key].link);
            }

            storeData(links);

            setBackground(links);
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

function setBackground(items) {
    $("body").css('background-image','url("'+selectData(items)+'")');
}

function selectData(datas) {
    return datas[Math.floor(Math.random()*datas.length)];
}