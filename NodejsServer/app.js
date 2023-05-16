const express = require('express')
const { v4: uuidv4 } = require('uuid')
const WebSocket = require('ws')
const socketsClients = new Map()

const app = express()
const port = process.env.PORT || 8888
let posX;
let posY;

const httpServer = app.listen(port, appListen)
function appListen () {
  console.log(`Listening for HTTP queries on: http://localhost:${port}`)
}

const wss = new WebSocket.Server({ server: httpServer })
console.log(`Listening for WebSocket queries on ${port}`)

// What to do when a websocket client connects
wss.on('connection', (ws, req) => {
  console.log('socketsClients Map:');
  for (const [key, value] of socketsClients.entries()) {
    console.log(`  ${key} => ${value}`);
  }

  // Add client to the clients list

  const IP = req.socket.remoteAddress;
  console.log("Client connected with IP: " + IP)

  /* Con client number podemos contar cuantos clientes se han identificado */

  /* El codigo uuidv4 es un codigo generado aleatoriamente, tiene tal cantidad de carateres
  que es practicamente imposible que se repita */
  const id = uuidv4()
  console.log("id assigned: " + id)
  const metadata = {IP, id}
  socketsClients.set(ws, metadata)
  
  /* Cuando se desconecta un cliente, se quitara tambien el usuario */
  ws.on("close", () => {
    socketsClients.delete(ws)
    let idClientDisconnected = metadata.id
    console.log("Client disconnected: "+idClientDisconnected);
  })

  // What to do when a client message is received
  ws.on('message', (bufferedMessage) => {
    console.log("Message received from client: " + bufferedMessage)
    let rst;

    var messageAsString = bufferedMessage.toString()
    var messageAsObject = {}

    try { 
      messageAsObject = JSON.parse(messageAsString) 
      posX = messageAsObject.posX;
      posY = messageAsObject.posY;
      console.log("The sprite is in X: " + posX + " Y: " + posY);
    } catch (e) { 
      console.log("Could not parse bufferedMessage from WS message") 
    }

    // console.log("Will respond " +JSON.stringify(rst));
    // ws.send(JSON.stringify(rst));
  })
})

// Send a message to all websocket clients
async function broadcast (obj) {
  // console.log("Broadcasting message to all clients: " + JSON.stringify(obj))
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      var messageAsString = JSON.stringify(obj)
      client.send(messageAsString)
    }
  })
}







