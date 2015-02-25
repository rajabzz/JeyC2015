package core.data;


/**
 * Created by rajabzz on 2/3/15.
 */
public class DynamicGameObject extends GameObject {

    public static final String NAME = "dynamic";

    public static final int CELL_OTHER_INDEX_TEAM_ID = 0;

    private final int teamId;

    public DynamicGameObject(int teamId) {
        this.teamId = teamId;
    }

    public int getTeamId() {
        return teamId;
    }

}
