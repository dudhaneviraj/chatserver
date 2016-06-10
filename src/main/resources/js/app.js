var form = document.getElementById('message-form');

var messageField = document.getElementById('message');
var messagesList = document.getElementById('messages');
var socketStatus = document.getElementById('status');
var closeBtn = document.getElementById('close');
var openBtn = document.getElementById('open');
var clearBtn = document.getElementById('clear');

var ip = window.location.hostname;

address = ip + ":8000"

socket = new WebSocket("ws://" + ip + ":8000" + "/socket");

socket.onerror = function (error) {
    console.log('WebSocket Error: ' + error);
};

socket.onopen = function (event) {
    socketStatus.innerHTML = 'Connected to: ' + address;
    socketStatus.className = 'open';
    messagesList.innerHTML += '<li class="received"><span>Received:</span>' + "LOGIN NAME?" + '</li>';
};

socket.onmessage = function (event) {
    var message = event.data;
    messagesList.innerHTML += '<li class="received"><span>Received:</span>' + message + '</li>';
};

socket.onclose = function (event) {
    socketStatus.innerHTML = 'Disconnected from WebSocket.';
    socketStatus.className = 'closed';
};

form.onsubmit = function (e) {
    e.preventDefault();

    var message = messageField.value;

    socket.send(message);

    messagesList.innerHTML += '<li class="sent"><span>Sent:</span>' + message + '</li>';

    messageField.value = '';

    return false;
};


closeBtn.onclick = function (e) {
    e.preventDefault();

    socket.close();

    return false;
};

openBtn.onclick = function (e) {
    e.preventDefault();

    socket.close();

    socket = new WebSocket("ws://" + ip + ":8000" + "/socket");

    socket.onerror = function (error) {
        console.log('WebSocket Error: ' + error);
    };

    socket.onopen = function (event) {
        socketStatus.innerHTML = 'Connected to: ' + address;
        socketStatus.className = 'open';
        messagesList.innerHTML += '<li class="received"><span>Received:</span>' + "LOGIN NAME?" + '</li>';

    };

    socket.onmessage = function (event) {
        var message = event.data;
        messagesList.innerHTML += '<li class="received"><span>Received:</span>' +
            message + '</li>';
    };

    socket.onclose = function (event) {
        socketStatus.innerHTML = 'Disconnected from WebSocket.';
        socketStatus.className = 'closed';
    };

    return true;
};

clearBtn.onclick = function (e) {
    e.preventDefault();
    document.getElementById("messages").innerHTML = "";
    return false;
};


function isKeyPressed(event) {
    if (event.ctrlKey && event.keyCode == 13) {
        event.preventDefault();
        var message = messageField.value;

        if (message.length > 0) {
            socket.send(message);

            messagesList.innerHTML += '<li class="sent"><span>Sent:</span>' + message + '</li>';

            messageField.value = '';

        }
        return false;
    }
}