# JavaWebRTCSample

`server` / `client` の2モードで動作するターミナルチャットサンプルです。

- `server`: チャット受信と `/getip` API を提供
- `client`: `/getip` でサーバーIPを取得して接続

## Build

```powershell
.\gradlew.bat clean build
```

## Run (Server)

```powershell
java -jar .\build\libs\JavaWebRTCSample-1.0-SNAPSHOT.jar server 7000 8080
```

## Run (Client)

```powershell
java -jar .\build\libs\JavaWebRTCSample-1.0-SNAPSHOT.jar client http://<server-host>:8080 7000
```

`/getip` レスポンス形式:

```json
{ "ip": "x.x.x.x" }
```

