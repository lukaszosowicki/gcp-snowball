package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
}

