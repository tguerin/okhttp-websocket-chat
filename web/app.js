var path = require('path');
var express = require('express');
var app = express();
var http = require('http').Server(app);

var WebSocketServer = require('ws').Server;

var wss = new WebSocketServer({port: 5000});

var clients = {};
var currentConnectionId = 0;

app.use(express.static(path.join(__dirname, 'public')));

app.get('/', function (req, res) {
    res.sendFile(__dirname + '/index.html');
});

wss.on('connection', function connection(socket) {
    var connectionId = "connection" + ++currentConnectionId;
    console.log('User connected with id ' + connectionId);
    clients[connectionId] = socket;

    socket.on('close', function () {
        console.log('User with id ' + connectionId + ' has disconnected');
        delete clients[connectionId];
    });

    socket.on('message', function (msg) {
        console.log('message: ' + msg);
        for (var clientId in clients) {
            clients[clientId].send(msg, function ack(error) {
                if (error) {
                    delete clients[clientId];
                }
            });
        }
    });
});

http.listen(3000, "0.0.0.0", function () {
    console.log('listening on *:3000');
});