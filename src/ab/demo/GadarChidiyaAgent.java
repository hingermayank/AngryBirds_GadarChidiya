package ab.demo;

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
 * Created by mayank on 23/10/14.
 */
public class GadarChidiyaAgent implements Runnable{


    public ActionRobot aRobot;
    private Random randomGenerator;
    public int currentLevel = 8;
    private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
    TrajectoryPlanner tp;
    // a standalone implementation of the Naive Agent

    public GadarChidiyaAgent() {

        aRobot = new ActionRobot();
        tp = new TrajectoryPlanner();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();
        randomGenerator = new Random();

    }


    // run the client
    public void run() {

        aRobot.loadLevel(currentLevel);
        while (true) {
            GameStateExtractor.GameState state = solve();
            if (state == GameStateExtractor.GameState.WON){
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

    public double distance(Point p1, Point p2) {
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

        ABType slingbird = aRobot.getBirdTypeOnSling();

        // confirm the slingshot
        while (sling == null && aRobot.getState() == GameStateExtractor.GameState.PLAYING) {
            System.out.println("No slingshot detected. Please remove pop up or zoom out");
            ActionRobot.fullyZoomOut();
            screenshot = ActionRobot.doScreenShot();
            vision = new Vision(screenshot);
            sling = vision.findSlingshotMBR();
        }

        // get all the pigs
        List<ABObject> pigs = vision.findPigsRealShape();
        List<ABObject> blocks = vision.findBlocksRealShape();

        blocks.addAll(pigs);

        GameStateExtractor.GameState state = aRobot.getState();

        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {

            if (!blocks.isEmpty()) {

                Point releasePoint = null;
                Shot shot = new Shot();

                // Get the reference point
                Point refPoint = tp.getReferencePoint(sling);
                Datapoints dp = new Datapoints();
                int overall_score = Integer.MIN_VALUE;

                Point _tpt = null;
                ArrayList<Point> temp_points = new ArrayList<Point>();
                int dx,dy;
                {
                    for(int i=0; i<blocks.size(); i++) {

                        int type_val1 = 0, type_val2 = 0, type_val3=0;
                        Double aweight, pweight, distance;
                        ABObject block_sel = blocks.get(i);
                        String block_type = block_sel.getType().toString().toLowerCase();
                        System.out.println("Block type = " + block_type);

                        if (block_type.equals("unknown"))
                            continue;

                        _tpt = block_sel.getCenter();

                        // estimate the trajectory
                        ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);

                        if (!pts.isEmpty()) {
                            if (block_type.equals("stone")) {
                                type_val1 = 1;
                                type_val2 = 1;
                            } else if (block_type.equals("pig")) {
                                type_val1 = 1;
                                type_val2 = 1;
                                type_val3 =1;
                            }
                            else if(block_type.equals("ice"))
                            {
                                type_val1 = 1;
                            }
                            pweight = dp.getArea(block_sel) / 1000.0;
                            System.out.println("Pweight = " + pweight);

                            aweight = dp.aboveBlocksWeight(block_sel, blocks) / 1000.0;
                            System.out.println("Aweight = " + aweight);

                            distance = dp.getMinPigDistance(block_sel, pigs);
                            System.out.println("Distance = " + distance);

                            //int temp_score = (int) ((-2046.0067 * type_val1) + (7540.1106 * type_val2) + (-1622.5479 * pweight) + (-1264.6563 * aweight) + (-989.9095 * distance) + (-4738.9017 * weakness) + 13956.9713);
                            //int temp_score = (int)((-5916.1889*angle) + (-1797.6726*aweight)+ (-2082.9804*distance) + 11527.6183);
                            int score = (int)((-95.6263*type_val1) + (225.8844*type_val2) + (3546.7931*type_val3) + (-1447.3454*pweight) + (-1342.0356*aweight) + (-1062.7967*distance) + 8289.6684);
                            if(score > overall_score)
                            {
                                temp_points.clear();
                                overall_score = score;
                                temp_points.addAll(pts);
                                System.out.println("Overall score = " + overall_score);
                            }
                        } else if (pts.isEmpty()) {
                            System.out.println("No release point found for the target");
                            System.out.println("Try a shot with 45 degree");
                            releasePoint = tp.findReleasePoint(sling, Math.PI / 4);
                        }

                    }

                    Shot temp_shot1, temp_shot2;
                    int temp_dx1, temp_dy1, temp_dx2, temp_dy2;
                    if(temp_points.size() == 2)
                    {
                        Collections.sort(temp_points, new Comparator<Point>() {
                            public int compare(Point a, Point b) {
                                int xComp = Integer.compare(a.x, b.x);
                                if (xComp == 0)
                                    return Integer.compare(a.y, b.y);
                                else
                                    return xComp;
                            }
                        });
                        temp_dx1 = (int) temp_points.get(0).getX() - refPoint.x;
                        temp_dy1 = (int) temp_points.get(0).getY() - refPoint.y;
                        temp_dx2 = (int) temp_points.get(1).getX() - refPoint.x;
                        temp_dy2 = (int) temp_points.get(1).getY() - refPoint.y;
                        temp_shot1 = new Shot(refPoint.x, refPoint.y, temp_dx1, temp_dy1, 0);
                        temp_shot2 = new Shot(refPoint.x, refPoint.y, temp_dx2, temp_dy2, 0);
                        if (ABUtil.isReachable(vision, _tpt, temp_shot1)) {
                            System.out.println("first point reachable.");
                            releasePoint = temp_points.get(0);
                        } else if (ABUtil.isReachable(vision, _tpt, temp_shot2)) {
                            System.out.println("second point reachable.");
                            releasePoint = temp_points.get(1);
                        } else {
                            System.out.println("first point chosen.");
                            releasePoint = temp_points.get(0);
                        }
                    }
                    else
                    {
                        releasePoint = temp_points.get(0);
                        System.out.println("one release point.");
                    }
                    //Calculate the tapping time according the bird type
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                releasePoint);
                        System.out.println("Release Point: " + releasePoint);
                        System.out.println("Release Angle: " + Math.toDegrees(releaseAngle));
                        int tapInterval = 0;

                        int blocksize = blocks.size();

                        Point[] x_cord = new Point[blocksize];
                        for(int i=0; i<blocksize; i++)
                        {
                            x_cord[i] = blocks.get(i).getCenter();
                        }

                        Arrays.sort(x_cord, new Comparator<Point>() {
                            public int compare(Point a, Point b) {
                                int xComp = Integer.compare(a.x, b.x);
                                if(xComp == 0)
                                    return Integer.compare(a.y, b.y);
                                else
                                    return xComp;
                            }
                        });

                        Point left_most = x_cord[0];

                        switch (slingbird)
                        {

                            case RedBird:
                                tapInterval = 0; break;               // start of trajectory
                            case YellowBird:
                                tapInterval = 80 + randomGenerator.nextInt(10);break; // 80-90% of the way
                            case WhiteBird:
                                tapInterval =  80 + randomGenerator.nextInt(10);break; // 80-90% of the way
                            case BlackBird:
                                tapInterval =  75 + randomGenerator.nextInt(15);break; // 75-90% of the way
                            case BlueBird:
                                tapInterval =  65 + randomGenerator.nextInt(15);break; // 65-80% of the way
                            default:
                                tapInterval =  60;
                        }

                        int tapTime = tp.getTapTime(sling, releasePoint, left_most, tapInterval);
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
                                if ( state == GameStateExtractor.GameState.PLAYING )
                                {
                                    screenshot = ActionRobot.doScreenShot();
                                    vision = new Vision(screenshot);
                                    List<Point> traj = vision.findTrajPoints();
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

            }

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
