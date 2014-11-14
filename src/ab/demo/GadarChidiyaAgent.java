package ab.demo;

import ab.database.DBoperations;
import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.regression.Datapoints;
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
 * Created by mayank on 23/10/14.
 */
public class GadarChidiyaAgent {


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
    Datapoints dp;
    int maxScore = 0;
    double chosenAngle;
    double a,b,c,d,e,f;
    Shot shot;
    ABType bird_onSling;
    // a standalone implementation of the Naive Agent
    public GadarChidiyaAgent() {

        aRobot = new ActionRobot();
        bird_onSling = aRobot.getBirdTypeOnSling();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        randomGenerator = new Random();
        dp = new Datapoints();
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
                    aRobot.loadLevel(++currentLevel);
                    // make a new trajectory planner whenever a new level is entered
                    tp = new TrajectoryPlanner();
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

        List<ABObject> objlist = new LinkedList<ABObject>();
        List<ABObject> temp = new LinkedList<ABObject>();


        temp = vision.findPigsRealShape();
        objlist.addAll(temp);
        temp.clear();
        temp = vision.findBlocksRealShape();
        objlist.addAll(temp);

        Point pt = null;

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
        int dx = 0,dy=0;
        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {
            for (int i = 0; i < objlist.size(); i++) {
                ABObject currentObject = objlist.get(i);
                Point center = currentObject.getCenter();
                double aWeight = dp.aboveBlocksWeight(currentObject, objlist);
                double distance = dp.getMinPigDistance(currentObject, vision.findPigsRealShape());
                double pWeight = dp.getArea(currentObject);
                double weakness = dp.getWeakness(currentObject, aRobot.getBirdTypeOnSling());
                Point refPoint = tp.getReferencePoint(sling);
                Point releasePoint;
                ArrayList<Double> angles = new ArrayList<Double>();
                HashMap<Double, Point> map = new HashMap<Double, Point>();
                ArrayList<Point> pts = tp.estimateLaunchPoint(sling, center);

                for (int j = 0; j < pts.size(); j++) {
                    releasePoint = pts.get(j);
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                releasePoint);
                        angles.add(releaseAngle);
                        map.put(releaseAngle, releasePoint);
                    }
                }

                for (int j = 0; j < angles.size(); j++) {
                    int score = (int) (a * angles.get(j) + b * pWeight + c * aWeight + d * distance + e * weakness + f);
                    if (score > maxScore) {
                        maxScore = score;
                        chosenAngle = angles.get(j);
                    }
                }
                pt = map.get(chosenAngle);

                if (pt != null) {
                    System.out.println("Release Point: " + pt);
                    System.out.println("Release Angle: "
                            + Math.toDegrees(chosenAngle));

                    int tapInterval = 0;

                    Point[] x_cord = new Point[objlist.size()];
                    for (int j = 0; j < objlist.size(); j++) {
                        x_cord[j] = objlist.get(j).getCenter();
                    }

                    Arrays.sort(x_cord, new Comparator<Point>() {
                        public int compare(Point a, Point b) {
                            int xComp = Integer.compare(a.x, b.x);
                            if (xComp == 0)
                                return Integer.compare(a.y, b.y);
                            else
                                return xComp;
                        }
                    });

                    Point left_most = x_cord[0];

                    switch (bird_onSling)
                    //switch (ABType.YellowBird)
                    {

                        case RedBird:
                            tapInterval = 0;
                            break;               // start of trajectory
                        case YellowBird:
                            tapInterval = 80 + randomGenerator.nextInt(10);
                            break; // 80-90% of the way
                        case WhiteBird:
                            tapInterval = 80 + randomGenerator.nextInt(10);
                            break; // 80-90% of the way
                        case BlackBird:
                            tapInterval = 75 + randomGenerator.nextInt(15);
                            break; // 75-90% of the way
                        case BlueBird:
                            tapInterval = 65 + randomGenerator.nextInt(15);
                            break; // 65-80% of the way
                        default:
                            tapInterval = 60;
                    }

                    int tapTime = tp.getTapTime(sling, pt, left_most, tapInterval);
                    dx = (int) pt.getX() - refPoint.x;
                    dy = (int) pt.getY() - refPoint.y;

                    shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
                } else {
                    System.err.println("No Release Point Found");
                    return state;
                }


            }
            // Here the code will go for taking the shot after finding suitable block from regression equation
            {
                ActionRobot.fullyZoomOut();
                screenshot = ActionRobot.doScreenShot();
                vision = new Vision(screenshot);
                Rectangle _sling = vision.findSlingshotMBR();
                if (_sling != null) {
                    double scale_diff = Math.pow((sling.width - _sling.width), 2) + Math.pow((sling.height - _sling.height), 2);
                    if (scale_diff < 25) {
                        if (dx < 0) {
                            aRobot.cshoot(shot);
                            state = aRobot.getState();
                            if (state == GameStateExtractor.GameState.PLAYING) {
                                screenshot = ActionRobot.doScreenShot();
                                vision = new Vision(screenshot);
                                java.util.List<Point> traj = vision.findTrajPoints();
                                tp.adjustTrajectory(traj, sling, pt);
                            }
                        }
                    } else
                        System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                } else
                    System.out.println("no sling detected, can not execute the shot, will re-segement the image");
            }

            objlist.clear();
            temp.clear();

        }
        return state;
    }

    public static void main(String args[]) {

        GadarChidiyaAgent gca = new GadarChidiyaAgent();
        if (args.length > 0)
            gca.currentLevel = Integer.parseInt(args[0]);
        gca.run();

    }



}
