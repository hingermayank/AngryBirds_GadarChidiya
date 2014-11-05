package ab.demo;

import ab.database.DBoperations;
import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.regression.Datapoints;
import ab.utils.ABUtil;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor;
import ab.vision.Vision;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Created by mayank on 25/10/14.
 */
public class RandomShootAgent implements Runnable {


    private ActionRobot aRobot;
    private Random randomGenerator;
    public int currentLevel = 1;
    public static int time_limit = 12;
    private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
    TrajectoryPlanner tp;
    int order=0;
    private boolean firstrun = true;
    //private boolean firstShot;
    private Point prevTarget;
    // a standalone implementation of the Naive Agent
    public RandomShootAgent() {
/*
        if(firstrun) {
            try {
                DBoperations dbop = new DBoperations();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
*/
        aRobot = new ActionRobot();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        randomGenerator = new Random();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }


    // run the client
    public void run() {

        aRobot.loadLevel(currentLevel);
        while (true) {
            GameStateExtractor.GameState state = solve();
            if (state == GameStateExtractor.GameState.WON) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int score = StateUtil.getScore(ActionRobot.proxy);
                if(!scores.containsKey(currentLevel))
                    scores.put(currentLevel, score);
                else
                {
                    if(scores.get(currentLevel) < score)
                        scores.put(currentLevel, score);
                }
                int totalScore = 0;
                for(Integer key: scores.keySet()){

                    totalScore += scores.get(key);
                    System.out.println(" Level " + key
                            + " Score: " + scores.get(key) + " ");
                }
                System.out.println("Total Score: " + totalScore);
                if(currentLevel<=7) {
                    aRobot.loadLevel(++currentLevel);
                    // make a new trajectory planner whenever a new level is entered
                    tp = new TrajectoryPlanner();

                    // first shot on this level, try high shot first
                    //firstShot = true;
                }
            } else if (state == GameStateExtractor.GameState.LOST) {
                System.out.println("Restart");
                aRobot.restartLevel();
            } else if (state == GameStateExtractor.GameState.LEVEL_SELECTION) {
                System.out
                        .println("Unexpected level selection page, go to the last current level : "
                                + currentLevel);
                aRobot.loadLevel(currentLevel);
            } else if (state == GameStateExtractor.GameState.MAIN_MENU) {
                System.out
                        .println("Unexpected main menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel(currentLevel);
            } else if (state == GameStateExtractor.GameState.EPISODE_MENU) {
                System.out
                        .println("Unexpected episode menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel(currentLevel);
            }

        }

    }

    private double distance(Point p1, Point p2) {
        return Math
                .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }

    public GameStateExtractor.GameState solve()
    {

        // capture Image
        BufferedImage screenshot = ActionRobot.doScreenShot();

        // process image
        Vision vision = new Vision(screenshot);

        // find the slingshot
        Rectangle sling = vision.findSlingshotMBR();

        // objlist contains realshape MBR of all pigs and blocks
        List<ABObject> objlist = new LinkedList<ABObject>();
        List<ABObject> temp = new LinkedList<ABObject>();

        temp = vision.findPigsRealShape();
        objlist.addAll(temp);
        temp.clear();
        temp = vision.findBlocksRealShape();
        objlist.addAll(temp);

        int temp_score = StateUtil._getScore(ActionRobot.proxy);
        int score_before_shoot;
        int score_after_shoot;

        // confirm the slingshot
        while (sling == null && aRobot.getState() == GameStateExtractor.GameState.PLAYING) {
            System.out
                    .println("No slingshot detected. Please remove pop up or zoom out");
            ActionRobot.fullyZoomOut();
            screenshot = ActionRobot.doScreenShot();
            vision = new Vision(screenshot);
            sling = vision.findSlingshotMBR();
        }



        GameStateExtractor.GameState state = aRobot.getState();

        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {

            if (!objlist.isEmpty()) {

                Point releasePoint = null;
                Datapoints dp = new Datapoints();
                Shot shot = new Shot();
                int dx,dy;
                {
                    // pick a object at index random
                    score_before_shoot = dp.getScore();
                    System.out.println("Score before shoot = " + score_before_shoot);

                    int objlistsize = objlist.size();
                    System.out.println("objlistsize = " + objlistsize);

                    int randomNum = (int)(Math.random() * (objlistsize));
                    System.out.println("randomNum = "+randomNum);

                    ABObject randobj = objlist.get(randomNum);

                    ABType objtype = dp.getType(randobj);
                    System.out.println("obj type = "+ objtype.toString());

                    Double objarea = dp.getArea(randobj);
                    System.out.println("Area = " + objarea);

                    Double minpigdist = dp.getMinPigDistance(randobj , vision.findPigsRealShape());
                    System.out.println("Min. pig distance = " + minpigdist);

                    ABType bird_onSling = aRobot.getBirdTypeOnSling();
                    double objweakness = dp.getWeakness(randobj, bird_onSling);
                    System.out.println("Weakness of block = " + objweakness);


                    Point _tpt = randobj.getCenter();// if the target is very close to before, randomly choose a point near it

                    ABUtil utility = new ABUtil();
                    boolean feasible = utility.isReachable(vision, _tpt, shot);
                    System.out.println("Feasible = "+ feasible);

                    // estimate the trajectory
                    ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);

                    //release point for random shoot
                    releasePoint = pts.get((int)(Math.random() * pts.size()));

                    // Get the reference point
                    Point refPoint = tp.getReferencePoint(sling);

                    //Calculate the tapping time according the bird type
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                releasePoint);
                        System.out.println("Release Point: " + releasePoint);
                        System.out.println("Release Angle: "
                                + Math.toDegrees(releaseAngle));
                        int tapInterval = 0;
                        switch (aRobot.getBirdTypeOnSling())
                        {

                            case RedBird:
                                tapInterval = 0; break;               // start of trajectory
                            case YellowBird:
                                tapInterval = 65 + randomGenerator.nextInt(25);break; // 65-90% of the way
                            case WhiteBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlackBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlueBird:
                                tapInterval =  65 + randomGenerator.nextInt(20);break; // 65-85% of the way
                            default:
                                tapInterval =  60;
                        }

                        int tapTime = tp.getTapTime(sling, releasePoint, _tpt, tapInterval);
                        dx = (int)releasePoint.getX() - refPoint.x;
                        dy = (int)releasePoint.getY() - refPoint.y;
                        shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
                    }
                    else
                    {
                        System.err.println("No Release Point Found");
                        return state;
                    }
                }

                // check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
                {
                    ActionRobot.fullyZoomOut();
                    screenshot = ActionRobot.doScreenShot();
                    vision = new Vision(screenshot);
                    Rectangle _sling = vision.findSlingshotMBR();
                    if(_sling != null)
                    {
                        double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
                        if(scale_diff < 25)
                        {
                            if(dx < 0)
                            {
                                aRobot.cshoot(shot);
                                state = aRobot.getState();
                                score_after_shoot = dp.getScore();
                                System.out.println("Score after shoot = " + score_after_shoot);
                                System.out.println("score in that chance = " + (score_after_shoot-score_before_shoot));
                                if ( state == GameStateExtractor.GameState.PLAYING )
                                {
                                    screenshot = ActionRobot.doScreenShot();
                                    vision = new Vision(screenshot);
                                    java.util.List<Point> traj = vision.findTrajPoints();
                                    tp.adjustTrajectory(traj, sling, releasePoint);
                                }
                            }
                        }
                        else
                            System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                    }
                    else
                        System.out.println("no sling detected, can not execute the shot, will re-segement the image");
                }

                objlist.clear();
                temp.clear();
            }

        }
        return state;
    }

    public static void main(String args[]) {

        RandomShootAgent rsa = new RandomShootAgent();
        if (args.length > 0)
            rsa.currentLevel = Integer.parseInt(args[0]);
        rsa.run();

    }

}
