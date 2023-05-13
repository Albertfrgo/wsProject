package com.mygdx.ws;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

import org.json.JSONException;
import org.json.JSONObject;

public class wsProject extends ApplicationAdapter {
	//Definim primer quantes columnes i files tenim a la imatge amb el
	// conjunt d'imatges que conformaran l'animació
	private static final int frameColumns = 5;
	private static final int frameRows = 2;

	//Objectes que farem servir
	Animation<TextureRegion> walkAnimation;
	Texture walkSheet;
	SpriteBatch spriteBatch;
	private OrthographicCamera camera;

	//Variable per controlar el temps
	float stateTime;

	//Variables per a conexio WS
	WebSocket webSocket;
	String url = "localhost";
	int port = 8888;
	float lastSend =0;

	//Variables per moviment de l'sprite, info que enviarem per websocket al servidor
	private float posX, posY;
	private int direction;
	private Rectangle rectangleUp, rectangleDown, rectangleLeft, rectangleRight;
	private float speed = 100;
	private Vector3 posXY, directionLeft, directionRight, directionUp, directionDown, iddle;


	@Override
	public void create() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, 800, 480);

		//Carreguem l'sprite com a Texture
		//walkSheet = new Texture(Gdx.files.internal("sprite-animation4.png"));
		walkSheet = new Texture(Gdx.files.internal("sprite3.png"));

		//Per al cas del nostre sprite, podem dividirlo en 30 seccions, 6 x 5
		// ja que totes les imatges estan centrades i tenen la mateixa mida
		TextureRegion[][] tmp = TextureRegion.split(walkSheet,
				walkSheet.getWidth() / frameColumns, walkSheet.getHeight() / frameRows);

		//Un cop hem dividit l'sprite, colocarem aquestes imatges dividides a la
		// TextureRegion, el nostre conjunt de fotogrames
		TextureRegion[] walkFrames = new TextureRegion[frameColumns * frameRows];
		int pos = 0;
		for (int i = 0; i < frameRows; i++) {
			for (int j = 0; j < frameColumns; j++) {
				walkFrames[pos++] = tmp[i][j];
			}
		}

		//Inicialitzem l'animacio amb el conjunt de frames i
		// un interval de temps de canvi entre cada frame
		walkAnimation = new Animation<TextureRegion>(0.125f, walkFrames);

		//Instanciem l'spriteBatch, on mostrarem la nostra animacio, el temps començara a 0
		spriteBatch = new SpriteBatch();
		stateTime = 0f;

		//Moviments de l'sprite

		posXY = new Vector3(512 - (walkSheet.getWidth() / 2) + 300, 50, 0);

		rectangleUp = new Rectangle(0, 480 * 2 / 3, 800, 480 / 3);
		rectangleDown = new Rectangle(0, 0, 800, 480 / 3);
		rectangleLeft = new Rectangle(0, 0, 800 / 3, 480);
		rectangleRight = new Rectangle(800 * 2 / 3, 0, 800 / 3, 480);

		directionUp = new Vector3(0, 1, 0);
		directionDown = new Vector3(0, -1, 0);
		directionRight = new Vector3(1, 0, 0);
		directionLeft = new Vector3(-1, 0, 0);
		iddle = new Vector3(0, 0, 0);
		direction = 1;

		if (Gdx.app.getType() == Application.ApplicationType.Android) {
			System.out.println("Creating websocket in android...");
			url = "10.0.2.2";
			webSocket = WebSockets.newSocket(WebSockets.toWebSocketUrl(url, port));
			webSocket.setSendGracefully(false);
			webSocket.addListener((WebSocketListener) new MyWSListener());
			webSocket.connect();
			webSocket.send("Connecting");
		}else{
			System.out.println("Creating websocket...");
			webSocket = WebSockets.newSocket(WebSockets.toWebSocketUrl(url, port));
			webSocket.setSendGracefully(false);
			webSocket.addListener((WebSocketListener) new MyWSListener());
			webSocket.connect();
			webSocket.send("Connecting");
		}
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void render() {
		camera.update();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stateTime += Gdx.graphics.getDeltaTime();

		//A partir del temps deduim el frame que toca en cada moment en l'animació
		TextureRegion currentFrame = walkAnimation.getKeyFrame(stateTime, true);

		if(direction == -1){
			currentFrame.flip(true, false);
		}else{
			currentFrame.flip(false, false);
		}

		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		spriteBatch.draw(currentFrame, posXY.x += ((getSpriteDirection().x * speed) * Gdx.graphics.getDeltaTime()), posXY.y += ((getSpriteDirection().y * speed) * Gdx.graphics.getDeltaTime()));
		spriteBatch.end();

		currentFrame.flip(currentFrame.isFlipX(), currentFrame.isFlipY());

		if(stateTime - lastSend > 1.0f){
			if(webSocket != null){
				try {
					lastSend = stateTime;
					JSONObject messageToSend = new JSONObject();
					messageToSend.put("posX", posXY.x);
					messageToSend.put("posY", posXY.y);
					webSocket.send(messageToSend.toString());
					System.out.println("Sent info: " + messageToSend.toString());
				} catch (JSONException e) {
					throw new RuntimeException(e);
				}
			}else{
				//System.out.println("WebSocket not initialized");
			}

		}
	}


	private Vector3 getSpriteDirection() {
		for (int i = 0; i < 10; i++) {
			if (Gdx.input.isTouched(i)) {
				Vector3 touchXY = new Vector3();
				touchXY.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
				camera.unproject(touchXY);
				if (rectangleUp.contains(touchXY.x, touchXY.y)) {
					return directionUp;
				} else if (rectangleDown.contains(touchXY.x, touchXY.y)) {
					return directionDown;
				} else if (rectangleLeft.contains(touchXY.x, touchXY.y)) {
					direction = -1;
					return directionLeft;
				} else if (rectangleRight.contains(touchXY.x, touchXY.y)) {
					direction = 1;
					return directionRight;
				}
			}
		}
		return iddle;
	}


	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
		walkSheet.dispose();
	}
}

class MyWSListener implements WebSocketListener{

	public boolean onOpen(WebSocket webSocket){
		System.out.println("WebSocket Opening");
		return false;
	}

	public boolean onClose(WebSocket webSocket, int closeCode, String reason){
		System.out.println("Closing WebSocket");
		return false;
	}

	public boolean onMessage(WebSocket webSocket, String packet){
		System.out.println("Sending Message: "+packet);
		return false;
	}

	public boolean onMessage(WebSocket webSocket, byte[] packet){
		System.out.println("Seding Message: "+packet.toString());
		return false;
	}

	public boolean onError(WebSocket webSocket, Throwable error){
		System.out.println("ERROR: "+error.toString());
		return false;
	}

}

