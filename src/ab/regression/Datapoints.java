package ab.regression;

import ab.demo.NaiveAgent;
import ab.demo.other.ActionRobot;
import ab.utils.ABUtil;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;

/**
 * Created by mayank on 20/10/14.
 */
public class Datapoints {

    String type;
    Boolean feasibility;
    Double pweight, aweight, distance;
    int score;
    //public ActionRobot aRobot =new ActionRobot();

    // get score before and after a shot and get the difference for score of that shot
    public int getScore() {
        StateUtil util = new StateUtil();
        int temp_score = util._getScore(ActionRobot.proxy);
        return temp_score;
    }


      //  ABType birdType =aRobot.getBirdTypeOnSling();

    public ABType getTypes(ABObject block) {
        return block.getType();
    }

    public double getArea(ABObject block) {
        int constant = 0;
        switch (block.getType()) {
            case Pig:
                constant = 10;
                break;
            case TNT:
                constant = 10;
                break;
            default:
                constant = 1;
        }
        return constant * (block.getHeight() * block.getWidth());
    }

    public double getMinPigDistance(ABObject block, List<ABObject> pigs) {
        double min = 99999;
        double normalization_factor = 100.0;
        for (int i = 0; i < pigs.size(); i++) {
            if (distance(block.getCenter(), pigs.get(i).getCenter()) <= min) {
                min = distance(block.getCenter(), pigs.get(i).getCenter());
            }
        }
        return min / normalization_factor;
    }

    public double aboveBlocksWeight(ABObject block, List<ABObject> blocks) {

        double totalArea = 0;

        for (int j = 0; j < blocks.size(); j++) {
            if (ABUtil.isSupport(block, blocks.get(j))) {
                totalArea = totalArea + getArea(blocks.get(j));
            }
        }


        return totalArea + above( block, blocks);
    }

    public double above(ABObject block , List<ABObject> blocks) {
        List<ABObject> aboveBlocks = new ArrayList<ABObject>();
        double minDistanceBlockArea = 99999;
        for(int i=0;i<blocks.size();i++) {
            if(blocks.get(i).getCenter().getX() > (block.getCenter().getX() - block.getWidth()/2) && blocks.get(i).getCenter().getX() < (block.getCenter().getX() + block.getWidth()/2) && block.getCenterY() > blocks.get(i).getCenterY()) {
                aboveBlocks.add(blocks.get(i));
            }
        }
        for(int j=0;j<aboveBlocks.size();j++) {
            if(Math.abs(block.getCenterY() - aboveBlocks.get(j).getCenterY()) < minDistanceBlockArea) {
                minDistanceBlockArea = getArea(aboveBlocks.get(j));
            }
        }
       // System.out.println("sizeAbovev "+ aboveBlocks.size());
        return minDistanceBlockArea;
    }

    public double distance(Point p1, Point p2) {
        return Math
                .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }

    public double getWeakness(ABObject block, ABType bird) {
        HashMap<ABType, Double> wood = new HashMap<ABType, Double>();
        HashMap<ABType, Double> ice = new HashMap<ABType, Double>();
        HashMap<ABType, Double> stone = new HashMap<ABType, Double>();

        wood.put(ABType.RedBird, 0.8);
        wood.put(ABType.BlueBird, 0.8);
        wood.put(ABType.YellowBird, 0.4);
        wood.put(ABType.WhiteBird, 0.8);
        wood.put(ABType.BlackBird, 0.4);

        ice.put(ABType.RedBird, 0.8);
        ice.put(ABType.BlueBird, 0.4);
        ice.put(ABType.YellowBird, 0.8);
        ice.put(ABType.WhiteBird, 0.8);
        ice.put(ABType.BlackBird, 0.4);

        stone.put(ABType.RedBird, 0.2);
        stone.put(ABType.BlueBird, 0.1);
        stone.put(ABType.YellowBird, 0.1);
        stone.put(ABType.WhiteBird, 0.8);
        stone.put(ABType.BlackBird, 0.8);

        String blocktype = block.toString().toLowerCase();
        if (blocktype == "wood") {
            return wood.get(bird);
        } else if (blocktype == "ice") {
            return ice.get(bird);
        } else if (blocktype == "stone") {
            return stone.get(bird);
        }
        else {
            return 0;
        }

    }
}