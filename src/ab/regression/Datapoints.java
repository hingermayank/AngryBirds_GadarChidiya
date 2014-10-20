package ab.regression;

import ab.demo.other.ActionRobot;
import ab.utils.StateUtil;

/**
 * Created by mayank on 20/10/14.
 */
public class Datapoints {

    String type;
    Boolean feasibility;
    Double pweight, aweight, distance;
    int score;

    // get score before and after a shot and get the difference for score of that shot
    public int getScore()
    {
        StateUtil util = new StateUtil();
        int temp_score = util._getScore(ActionRobot.proxy);
        return temp_score;
    }

}
