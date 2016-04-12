# сервер -> клиент

### Ожидание начала игры

```
    {
        "action":"enemyWaiting",
        "body":{
            "login":"player555"
        }
    }
 ```
### Начало игры

```
    {
        "action":"start",
        "body": {
            "myName": "player555"
            "enemyName": "player777"
        }
    }
```
### Ход соперника

```
    {
        "action":"wait",
        "body": {
            "login":"player777"
        }
    }
```
### Ход текущего игрока

```
    {
        "action":"act",
        "body": {
            "login":"player777"
        }
    }
```
### Конец игры и вывод победителя

```
    {
        "action":"game_over",
        "body": {
            "winerLogin":"player777"
        }
    }
```

# клиент -> сервер

### Тип игры

```
ws = new WebSocket("ws://localhost:8080/gameplay");
function sendMessage() {
    var message = "{
        "action":"choose_type",
        "body":{
            "login":"player777",
            "type": "single / multi"
        }
    }"
    ws.send(message);
}
```
### JSON с координатами всех расставленных пользователем кораблей

```
var message = "{
    "action":"set_ships",
    "body":{
        "login":"player777",
        "four-decked":[[1,1],[1,2],[1,3],[1,4]],
        "three-decked":[[[1,1],[1,2],[1,3]],[[1,1],[1,2],[1,3]]],
        "two-decked":[[[1,1],[1,2]],[[1,1],[1,2]],[[1,1],[1,2]]],
        "one-decked":[[1,1],[1,1],[1,1],[1,1]]
    }
}"
```
### Подстрелить соперника

```
 var message = " {
    "action":"shoot",
    "body":{
        "login":"player777",
        "coordinates":[1,1]
    }
}"
```


