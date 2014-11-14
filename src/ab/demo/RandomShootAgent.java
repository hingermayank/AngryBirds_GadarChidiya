package ab.demo;

import ab.database.BaseClassDB;
import ab.database.DBoperations;
import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.regression.Datapoints;
import ab.regression.Parameters;
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
    public int currentLevel = 21;
    public static int time_limit = 12;
    private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
    TrajectoryPlanner tp;
    int chance_score;
    private boolean firstrun = true;
    //private boolean firstShot;
    private Point prevTarget;
    private int total_birds;
    private int sub_score =0;
    private int score_before_shoot;
    private int score_after_shoot;
    private Datapoints dp;
    private ABObject randobj;
    public Parameters para;
    public BaseClassDB base;
    private DBoperations dbop;
    private Point _tpt;
    int counter;
    ABType bird_onSling;

    // a standalone implementation of the Naive Agent
    public RandomShootAgent() {

        aRobot = new ActionRobot();
        bird_onSling = aRobot.getBirdTypeOnSling();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        randomGenerator = new Random();
        para = new Parameters();
        base = new BaseClassDB();
        bird_onSling = aRobot.getBirdTypeOnSling();

        try {
            dbop = new DBoperations();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }


    // run the client
    public void run() {

        aRobot.loadLevel(currentLevel);
        while (true) {
            GameStateExtractor.GameState state = solve();
            if (state == GameStateExtractor.GameState.WON) {
                sub_score = 10000 * counter;
                int temp1 = score_before_shoot+sub_score;
                System.out.println("Sub score = " + sub_score);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int score = StateUtil.getScore(ActionRobot.proxy);
                System.out.println("Final Score = " + score);

                //score_after_shoot = score-sub_score;
                chance_score = score -temp1;
                para.setScore(chance_score);
                System.out.println("score in that chance after won= " + chance_score);

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

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                base.inset(para.getType(), para.getAngle(), para.getReachable(), para.getPweight(), para.getAweight(), para.getDistance(), para.getWeakness(), para.getScore());
                aRobot.loadLevel(currentLevel);
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

        // objlist contains realshape MBR of all pigs and blocks
        List<ABObject> objlist = new LinkedList<ABObject>();
        List<ABObject> temp = new LinkedList<ABObject>();
        List<ABObject> birdslist = new LinkedList<ABObject>();

        temp = vision.findPigsRealShape();
        objlist.addAll(temp);
        temp.clear();
        temp = vision.findBlocksRealShape();
        objlist.addAll(temp);

        birdslist = vision.findBirdsRealShape();
        if(birdslist != null) {
            total_birds = birdslist.size();
            counter = total_birds;
        }
        //int temp_score = StateUtil._getScore(ActionRobot.proxy);


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
        if (sling != null)
        {

            if (!objlist.isEmpty()) {

                Point releasePoint = null;
                dp = new Datapoints();
                Shot shot = new Shot();
                int dx,dy;
                {
                    int objlistsize = objlist.size();
                   // System.out.println("objlistsize = " + objlistsize);


                    int randomNum = randomGenerator.nextInt(objlistsize);
                   // int randomNum = (int)(Math.random() * (objlistsize-1));
                  //  System.out.println("randomNum = "+randomNum);

                    randobj = objlist.get(randomNum);

                    _tpt = randobj.getCenter();// if the target is very close to before, randomly choose a point near it

                    // estimate the trajectory
                    ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);

                    System.out.println(" pts size = " + pts.size());
                    while(pts.size() == 0){
                        randomNum = randomGenerator.nextInt(objlistsize);
                        randobj = objlist.get(randomNum);
                        _tpt = randobj.getCenter();
                        pts = tp.estimateLaunchPoint(sling, _tpt);
                    }

                    int temp_rand = randomGenerator.nextInt(pts.size());
                    //int temp_rand = (int) Math.random()*(pts.size()-1);
                    System.out.println("Temp rand = " + temp_rand);
                    //release point for random shoot
                    releasePoint = pts.get(temp_rand);

                    // Get the reference point
                    Point refPoint = tp.getReferencePoint(sling);

                    //Calculate the tapping time according the bird type
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling,
                                releasePoint);
                        para.setAngle(Math.toDegrees(releaseAngle)/100.0);
                        System.out.println("Release Point: " + releasePoint);
                        System.out.println("Release Angle: "
                                + Math.toDegrees(releaseAngle));
                        int tapInterval = 0;

                        Point[] x_cord = new Point[objlistsize];
                        for(int i=0; i<objlistsize; i++)
                        {
                            x_cord[i] = objlist.get(i).getCenter();
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

                        switch (bird_onSling)
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

                        boolean reachable = ABUtil.isReachable(vision, _tpt, shot);
                        if(reachable)
                        {
                            para.setReachable(1);
                        }
                        else
                        {
                            para.setReachable(0);
                        }


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
                                score_before_shoot = dp.getScore();
                                System.out.println("Score before shoot = " + score_before_shoot);

                                ABType objtype = dp.getTypes(randobj);
                                para.setType(objtype.toString());
                                System.out.println("obj type = "+ objtype.toString());

                                Double objarea = dp.getArea(randobj);
                                para.setPweight(objarea/1000.0);
                                System.out.println("Area = " + objarea);

                                Double minpigdist = dp.getMinPigDistance(randobj , vision.findPigsRealShape());
                                para.setDistance(minpigdist);
                                System.out.println("Min. pig distance = " + minpigdist);

                                System.out.println("Bird on sling = " + bird_onSling.toString());

                                double objweakness = dp.getWeakness(randobj, bird_onSling);
                                para.setWeakness(objweakness);
                                System.out.println("Weakness of block = " + objweakness);

                                Double aweight = dp.above(randobj , objlist);
                                para.setAweight(aweight/1000.0);
                                System.out.println("Above block WEIGHT = " + aweight);

                                counter--;
                                System.out.println("Total birds left = "  + counter);

                                aRobot.cshoot(shot);

                                state = aRobot.getState();
                                if ( state == GameStateExtractor.GameState.PLAYING )
                                {
                                    score_after_shoot = dp.getScore();
                                    System.out.println("Score after shoot = " + score_after_shoot);
                                    chance_score = score_after_shoot - score_before_shoot;
                                    para.setScore(chance_score);
                                    System.out.println("score in that chance = " + chance_score);
                                    base.inset(para.getType(), para.getAngle(), para.getReachable(), para.getPweight(), para.getAweight(), para.getDistance(), para.getWeakness(), para.getScore());

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
