package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;
 
import java.io.IOException;
import java.time.Instant;


@SpringBootApplication
@RestController
public class Application {

  static class Self {
    public String href;

    @Override
    public String toString() {
      return "Self{" +
              "href='" + href + '\'' +
              '}';
    }
  }

  static class Links {
    public Self self;

    @Override
    public String toString() {
      return "Links{" +
              "self=" + self +
              '}';
    }
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;

    @Override
    public String toString() {
      return "PlayerState{" +
              "x=" + x +
              ", y=" + y +
              ", direction='" + direction + '\'' +
              ", wasHit=" + wasHit +
              ", score=" + score +
              '}';
    }
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;

    @Override
    public String toString() {
      return "Arena{" +
              "dims=" + dims +
              ", state=" + state +
              '}';
    }
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;

    @Override
    public String toString() {
      return "ArenaUpdate{" +
              "_links=" + _links +
              ", arena=" + arena +
              '}';
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    System.out.println(arenaUpdate);
    writeCommittedStream.send(arenaUpdate.arena);
    String[] commands = new String[]{"F", "R", "L", "T"};
    PlayerState me = getMe(arenaUpdate);
    List<PlayerState> others = getOthers(arenaUpdate);
    if (me.wasHit) {
      return escape(me, others, arenaUpdate.arena);
    } else {
      return getStrategy(me, others);
    }
  }

  private String escape(PlayerState myState, List<PlayerState> otherStates, Arena arena){
    PlayerState next = calculateNextMove(myState);
    if (otherStates.stream().anyMatch(s -> calcDist(next, s) == 0)) {
      return new Random().nextBoolean() ? "R" : "L";
    } else {
      if (nextMovePossible(next, arena)) {
        return "F";
      } else {
        return new Random().nextBoolean() ? "R" : "L";
      }
    }
  }

  private boolean nextMovePossible(PlayerState state, Arena arena) {
    return state.x >= 0 && state.y >= 0 && state.x < arena.dims.get(0) && state.y < arena.dims.get(1);
  }

  private PlayerState calculateNextMove(PlayerState me) {
    int x = me.x;
    int y = me.y;
    switch(me.direction) {
      case "N":
        y -= 1;
        break;
      case "S":
        y += 1;
        break;
      case "W":
        x -= 1;
        break;
      case "E":
        x += 1;
        break;
    }
    PlayerState next = new PlayerState();
    next.x = x;
    next.y = y;
    return next;
  }

  private PlayerState getMe(ArenaUpdate arena) {
    return arena.arena.state.get(arena._links.self.href);
  }

  private List<PlayerState> getOthers(ArenaUpdate arena) {
    return arena.arena.state.entrySet().stream().filter(e -> !e.getKey().equals(arena._links.self.href))
            .map(s -> s.getValue())
            .collect(Collectors.toList());
  }

  private String getStrategy(PlayerState myState, List<PlayerState> otherStates) {
    List<PlayerState> playersInLine = getPlayersInLine(myState, otherStates);

    if (playersInLine.stream().anyMatch(s -> calcDist(myState, s) <= 3)) {
      return "T";
    } else {
      String leftDir = getLeftDir(myState.direction);
      String rightDir = getRightDir(myState.direction);
      String oppositeDir = getRightDir(rightDir);
      List<PlayerState> leftPlayers = getPlayersInLineForDir(myState, leftDir, otherStates);
      List<PlayerState> rightPlayers = getPlayersInLineForDir(myState, rightDir, otherStates);
      List<PlayerState> oppositePlayers = getPlayersInLineForDir(myState, oppositeDir, otherStates);
      if (thereArePlayersInDirectionAndRange(3, myState, leftPlayers)) {
        return "L";
      } else if (thereArePlayersInDirectionAndRange(3, myState, rightPlayers)) {
        return "R";
      } else if (thereArePlayersInDirectionAndRange(3, myState, oppositePlayers)) {
        return "R";
      } else if (thereArePlayersInDirectionAndRange(6, myState, playersInLine)) {
        return "F";
      } else if (thereArePlayersInDirectionAndRange(6, myState, leftPlayers)) {
        return "L";
      } else if (thereArePlayersInDirectionAndRange(6, myState, rightPlayers)) {
        return "R";
      } else {
        return new Random().nextBoolean() ? "L" : "R";
      }
    }
  }

  private boolean thereArePlayersInDirectionAndRange(int range, PlayerState myState, List<PlayerState> others) {
    return !others.isEmpty() && others.stream().anyMatch(s -> calcDist(myState, s) <= range);
  }

  private int calcDist(PlayerState a, PlayerState b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
  }

  private String getLeftDir(String dir) {
    switch (dir) {
      case "N":
        return "W";
      case "S":
        return "E";
      case "E":
        return "N";
      case "W":
        return "S";
    }
    return dir;
  }

  private String getRightDir(String dir) {
    switch (dir) {
      case "N":
        return "E";
      case "S":
        return "W";
      case "E":
        return "S";
      case "W":
        return "N";
    }
    return dir;
  }

  private List<PlayerState> getPlayersInLine(PlayerState myState, List<PlayerState> otherStates){
    return getPlayersInLineForDir(myState, myState.direction, otherStates);
  }

  private List<PlayerState> getPlayersInLineForDir(PlayerState myState, String dir, List<PlayerState> otherStates){
    switch (dir) {
      case "N":
        return otherStates.stream().filter(s -> Objects.equals(myState.x, s.x) && myState.y - s.y > 0).collect(Collectors.toList());
      case "S":
        return otherStates.stream().filter(s -> Objects.equals(myState.x, s.x) && myState.y - s.y < 0).collect(Collectors.toList());
      case "E":
        return otherStates.stream().filter(s -> Objects.equals(myState.y, s.y) && myState.x - s.x < 0).collect(Collectors.toList());
      case "W":
        return otherStates.stream().filter(s -> Objects.equals(myState.y, s.y) && myState.x - s.x > 0).collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  static class WriteCommittedStream {

    final JsonStreamWriter jsonStreamWriter;

    public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {

      try (BigQueryWriteClient client = BigQueryWriteClient.create()) {

        WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
        TableName parentTable = TableName.of(projectId, datasetName, tableName);
        CreateWriteStreamRequest createWriteStreamRequest =
                CreateWriteStreamRequest.newBuilder()
                        .setParent(parentTable.toString())
                        .setWriteStream(stream)
                        .build();

        WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);

        jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
      }
    }

    public ApiFuture<AppendRowsResponse> send(Arena arena) {
      Instant now = Instant.now();
      JSONArray jsonArray = new JSONArray();

      arena.state.forEach((url, playerState) -> {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("x", playerState.x);
        jsonObject.put("y", playerState.y);
        jsonObject.put("direction", playerState.direction);
        jsonObject.put("wasHit", playerState.wasHit);
        jsonObject.put("score", playerState.score);
        jsonObject.put("player", url);
        jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
        jsonArray.put(jsonObject);
      });

      return jsonStreamWriter.append(jsonArray);
    }

  }

  final String projectId = ServiceOptions.getDefaultProjectId();
  final String datasetName = "snowball";
  final String tableName = "events";

  final WriteCommittedStream writeCommittedStream;

  public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
    writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
  }
}

