var form = document.getElementById('message-form');
var messageField = document.getElementById('message');
var messagesList = document.getElementById('messages');
var chatRoomList = document.getElementById('chatrooms');
var userList = document.getElementById('users');
var socketStatus = document.getElementById('status');
var openCloseBtn = document.getElementById('openclose');
var clearBtn = document.getElementById('clear');
var crefresh = document.getElementById('crefresh');
var urefresh = document.getElementById('urefresh');
var createBtn = document.getElementById('create');
var createField = document.getElementById('chatroom');
var chatRoomCreateButton = document.getElementById('chatroomCreate');
var login = document.getElementById("login");
var loginStatus = document.getElementById("loginStatus");
var loginBtn = document.getElementById("loginBtn");
var userStatus = document.getElementById('userStatus');
var messageShow = document.getElementById("messageshow")
var navStatus = document.getElementById("navStatus")
var sendBtn = document.getElementById("send")
var wall = document.getElementById("body");
var leftBody = document.getElementById("leftbody")
var ftr1 = document.getElementById("ftr1")
var ftr2 = document.getElementById("ftr2")
var isLoggedIn = false;
var ip = window.location.hostname;
var address = ip + ":8000"
var selected = "";
var room = "";
var prefix = "";
var socket;
var username = "";

var ht = leftBody.clientHeight - ftr1.clientHeight - ftr2.clientHeight;
wall.setAttribute("style", "height:" + ht + "px;")

onElementHeightChange(document.body, function () {
    ht = leftBody.clientHeight - ftr1.clientHeight - ftr2.clientHeight;
    wall.setAttribute("style", "height:" + ht + "px;")
});

createSocket();

loadLoginModal()

emojify.setConfig({

    // emojify_tag_type : 'select',// Only run emojify.js on this element
    only_crawl_id: null,            // Use to restrict where emojify.js applies
    img_dir: '/pictures/images/emoji',  // Directory for emoji images
    ignored_tags: {                // Ignore the following tags
        'SCRIPT': 1,
        'TEXTAREA': 1,
        'A': 1,
        'PRE': 1,
        'CODE': 1
    }
});

form.onsubmit = function (e) {
    e.preventDefault();

    var message = messageField.value;

    if (message == null || message.length == 0)
        return;

    if (message.indexOf("/join") == 0)
        room = message.split(" ", 2)[1].trim();

    if (message == "/leave")
        room = "";

    socket.send(prefix + message);

    emojify.run(messagesList);

    messageField.value = '';

    return false;
};

chatRoomCreateButton.onclick = function (e) {
    e.preventDefault();
    $("#myModal").modal("show")
    createField.focus();
}

function loadLoginModal() {
    $("#loginModal").modal({backdrop: 'static', keyboard: false})
    login.focus();
    openCloseBtn.disabled = true;
}

openCloseBtn.onclick = function (e) {
    e.preventDefault();
    if (socket.readyState != 1) {
        createSocket();
        messagesList.innerHTML = "";
        openCloseBtn.className = "btn btn-danger glyphicon glyphicon-remove-sign";
    }
    else {
        socket.close();
    }
    return false;
};

clearBtn.onclick = function (e) {
    e.preventDefault();
    messagesList.innerHTML = "";
    return false;
};


crefresh.onclick = function (e) {
    e.preventDefault();
    renderChatrooms()
    return;
};

function renderChatrooms() {
    var data = httpGet("/api/chatRooms")
    chatRoomList.innerHTML = "";
    var result = $.parseJSON(data);
    var count = 0;
    $.each(result, function (k, v) {
        if (k == room)
            chatRoomList.innerHTML += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left 	glyphicon glyphicon-leaf">' +
                '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font" style="word-wrap: break-word;">' + k + '<span class="pull-right">' + v + ' Users</span></strong>' +
                '</div><span  class="pull-right">Current</span> </div></li>';
        else
            chatRoomList.innerHTML += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left 	glyphicon glyphicon-leaf">' +
                '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font" style="word-wrap: break-word;">' + k + '<span class="pull-right">' + v + ' Users</span></strong>' +
                '</div><button type="submit" class="btn btn-primary pull-right joinClass" id="send' + k + '">Join</button> </div></li>';

        count++;
    });

    if (count == 0)
        chatRoomList.innerHTML = '<ul class="chat"><li class="clearfix"><p>No Chatrooms Active</p></div></li></ul>';

    var classname = document.getElementsByClassName("joinClass");

    var join = function (e) {
        e.preventDefault();
        var eventid = e.currentTarget.id + "";
        if (eventid.indexOf("send") == 0) {
            messagesList.innerHTML = "";
            eventid = eventid.replace("send", "");
            socket.send("/join " + eventid);
            room = eventid + "";
            renderChatrooms()
            renderUsers()

        }
    };

    for (var i = 0; i < classname.length; i++) {
        classname[i].addEventListener('click', join, false);
    }
}

urefresh.onclick = function (e) {
    e.preventDefault();
    renderUsers()
    return;
};

createBtn.onclick = function (e) {
    e.preventDefault();

    if (createField.value.length == 0)
        return;

    room = createField.value;

    socket.send("/join " + createField.value);

    $("#myModal").modal("hide")

    renderChatrooms()
    renderUsers()
    return;
};

function renderUsers() {
    if (selected != "")
        selected = document.querySelector('input[name = "usr"]:checked').value;
    var isSelected = false;
    userList.innerHTML = "";
    if (room.length == 0) {
        userList.innerHTML += '<ul class="chat"><li class="clearfix"><p>No Chatroom Selected</p></div></li></ul>';
        return;
    }
    var data = httpGet("/api/users/" + room)

    var temp = ""
    var parsed = JSON.parse(data);

    if (parsed.length == 0) {
        userList.innerHTML += '<ul class="chat"><li class="clearfix"><p>No Users Active</p></div></li></ul>';
        return;
    }
    for (var i = 0; i < parsed.length; i++) {
        if (parsed[i] == username.toLowerCase())
            continue;
        if (parsed[i] == selected) {
            temp += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left  glyphicon glyphicon-user">' +
                '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font" style="word-wrap: break-word;">' + parsed[i] + '</strong>' +
                '<input type = "radio"  name = "usr" class="radio-inline pull-right" value = "' + parsed[i] + '" checked></div></div></li>';
            isSelected = true;
        }
        else {
            temp += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left  glyphicon glyphicon-user">' +
                '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font" style="word-wrap: break-word;">' + parsed[i] + '</strong>' +
                '<input type = "radio"  name = "usr" class="radio-inline pull-right" value = "' + parsed[i] + '"></div></div></li>';
        }
    }
    if (isSelected)
        userList.innerHTML += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left  glyphicon glyphicon-globe">' +
            '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font">Group</strong>' +
            '<input type = "radio"  name = "usr" class="radio-inline pull-right" value = "" ></div></div></li>';
    else {
        userList.innerHTML += '<li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left  glyphicon glyphicon-globe">' +
            '</span><div class="chat-body clearfix"><div class="header"><strong class="primary-font">Group</strong>' +
            '<input type = "radio"  name = "usr" class="radio-inline pull-right" value = "" checked></div></div></li>';
        selected = "";
    }

    userList.innerHTML += temp
    if (isSelected) {
        prefix = "/user" + selected;
        userStatus.innerHTML = '<strong>USERS</strong>' + '<span class="pull-right">CURRENT: ' + selected + '</span>';
    } else {
        prefix = ""
        userStatus.innerHTML = '<strong>USERS</strong>' + '<span class="pull-right">CURRENT: Group</span>';
    }
}

userList.onchange = function () {
    prefix = document.querySelector('input[name = "usr"]:checked').value;

    selected = prefix
    if (prefix.trim().length == 0) {
        prefix = "";
        userStatus.innerHTML = 'Users' + '<span class="pull-right">Current: Group</span>';
    } else {
        userStatus.innerHTML = 'Users' + '<span class="pull-right">Current: ' + prefix + '</span>';
        prefix = '/user ' + prefix + " ";
    }
}

function getTime() {
    var currentTime = new Date()
    var month = currentTime.getMonth() + 1
    var day = currentTime.getDate()
    var year = currentTime.getFullYear()
    var hours = currentTime.getHours();
    var minutes = currentTime.getMinutes();
    var tm = "";
    if (hours > 12)
        tm = "PM";
    else
        tm = "AM";
    hours = hours % 12;
    hours = (hours < 10) ? '0' + hours : hours;
    month = (month < 10) ? '0' + month : month;
    day = (day < 10) ? '0' + day : day;
    minutes = (minutes < 10) ? '0' + minutes : minutes;
    return hours + ":" + minutes + " " + tm + " " + month + "/" + day + "/" + year;
}

function httpGet(theUrl) {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("GET", theUrl, false); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

loginBtn.onclick = function (e) {
    e.preventDefault();
    username = login.value
    socket.send(login.value);

    return;
};

function createSocket() {
    socket = new WebSocket("ws://" + ip + ":8000" + "/socket");

    socket.onerror = function (error) {
        console.log('WebSocket Error: ' + error);
    };

    socket.onopen = function (event) {
        socketStatus.innerHTML = 'Connected to: ' + address
        isLoggedIn = false;
        chatRoomList.innerHTML = "";
        userList.innerHTML = "";
        messagesList.innerHTML = "";
        loadLoginModal()
    };

    socket.onmessage = function (event) {
        var result = $.parseJSON(event.data);

        if (!isLoggedIn) {
            if (result["message"].indexOf('WELCOME') == 0) {
                isLoggedIn = true;
                openCloseBtn.disabled = false;
                $("#loginModal").modal('hide')
                navStatus.innerHTML = '   <span style="font-size:2.0em;" class="glyphicon glyphicon-user">  ' + username + '</span>';
                loginStatus.innerHTML=''
            }
            else {
                loginStatus.innerHTML = result["message"];
                return;
            }
        }
        if (result['from'] == "YOU")
            messagesList.innerHTML = '<ul class="chat"><li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left glyphicon glyphicon-cloud-upload">' +
                '</span><div class="chat-body clearfix" style="word-wrap: break-word;"><div class="header"><strong class="primary-font">From: ' + result['from'] +
                '</strong> <small class="pull-right text-muted"><span class="glyphicon glyphicon-time"></span>' + getTime() + '</small></div>' +
                '<p>To: ' + result['to'] + '</p><p>Message:<br> ' + result['message'] + '</p></div></li></ul>' + messagesList.innerHTML
        else
            messagesList.innerHTML = '<ul class="chat"><li class="left clearfix"><span style="font-size:3.0em;" class="chat-img pull-left glyphicon glyphicon-cloud-download">' +
                '</span><div class="chat-body clearfix" style="word-wrap: break-word;"><div class="header"><strong class="primary-font">From: ' + result['from'] +
                '</strong> <small class="pull-right text-muted"><span class="glyphicon glyphicon-time"></span>' + getTime() + '</small></div>' +
                '<p>To: ' + result['to'] + '</p><p>Message:<br> ' + result['message'] + '</p></div></li></ul>' + messagesList.innerHTML
        renderChatrooms()
        renderUsers()
        emojify.run(document.getElementById('messages'));

        //scroll to bottom of messages
        // var elem = document.getElementById("body");
        // elem.scrollTop = elem.scrollHeight;
    };

    socket.onclose = function (event) {
        socketStatus.innerHTML = 'Disconnected from WebSocket.';
        selected=""
        navStatus.innerHTML = "";
        loginStatus.innerHTML=''
        socketStatus.className = '';
        messagesList.innerHTML = "";
        chatRoomList.innerHTML = "";
        userList.innerHTML = "";
        login.innerHTML = "";
        userStatus.innerHTML = "Users"
        isLoggedIn = false;
        username = "";
        room = "";
        prefix = "";
        openCloseBtn.className = "btn btn-success glyphicon glyphicon-plus-sign";
    };
}

function onElementHeightChange(elm, callback) {
    var lastHeight = elm.clientHeight, newHeight;
    (function run() {
        newHeight = elm.clientHeight;
        if (lastHeight != newHeight)
            callback();
        lastHeight = newHeight;

        if (elm.onElementHeightChangeTimer)
            clearTimeout(elm.onElementHeightChangeTimer);

        elm.onElementHeightChangeTimer = setTimeout(run, 200);
    })();
}

login.addEventListener('keypress', function (event) {
    if (event.keyCode == 13) {
        event.preventDefault();
        loginBtn.click();
    }
});


createField.addEventListener('keypress', function (event) {
    if (event.keyCode == 13) {
        event.preventDefault();
        createBtn.click();
    }
});



