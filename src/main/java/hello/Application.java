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
      return commands[new Random().nextInt(3)];
    } else {
      return getStrategy(me, others);
    }
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
    } else if (!playersInLine.isEmpty()) {
      return "F";
    } else if(new Random().nextInt(2) == 0) {
      return "R";
    } else {
      return "L";
    }
  }

  private int calcDist(PlayerState a, PlayerState b) {
    return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
  }

  private List<PlayerState> getPlayersInLine(PlayerState myState, List<PlayerState> otherStates){
    switch (myState.direction) {
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

