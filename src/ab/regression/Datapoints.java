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
import java.util.List;

/**
 * Created by mayank on 20/10/14.
 */
public class Datapoints {

    String type;
    Boolean feasibility;
    Double pweight, aweight, distance;
    int score;
    public ActionRobot aRobot =new ActionRobot();




    // get score before and after a shot and get the difference for score of that shot
    public int getScore()
    {
        StateUtil util = new StateUtil();
        int temp_score = util._getScore(ActionRobot.proxy);
        return temp_score;
    }


    public void objectType(BufferedImage screenshot) {

        Vision vision = new Vision(screenshot);
        //  List<BlockObject> obj = blockStructure(vision);
        List<ABObject> blocks = vision.findBlocksRealShape();
        ABType birdType =aRobot.getBirdTypeOnSling();
        for(int i=0;i<blocks.size();i++){
            System.out.println(blocks.get(i).getType()+" "+blocks.get(i).getFrame()+" "+birdType);
        }

    }

    public ABType getType(ABObject block) {
        return block.getType();
    }

    public double getArea(ABObject block) {
        return block.getHeight()*block.getWidth();
    }

    public double getMinPigDistance(ABObject block , List<ABObject> pigs) {
        double min = 99999;
        for (int i = 0 ; i < pigs.size() ; i++) {
            if(distance(block.getCenter(), pigs.get(i).getCenter()) <= min){
                min = distance(block.getCenter(), pigs.get(i).getCenter());
            }
        }
        return min;
    }

    public double aboveBlocksWeight(ABObject block , List<ABObject> pigs , List<ABObject> blocks) {

        double totalArea = 0;
        for(int i=0;i<pigs.size();i++) {
            blocks.add(pigs.get(i));
        }

        for(int j=0;j<blocks.size();j++) {
            if(ABUtil.isSupport(block, blocks.get(j))) {
                totalArea = totalArea + getArea(blocks.get(j));
            }
        }
        return totalArea;
    }

    public double distance(Point p1, Point p2) {
        return Math
                .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }

}
