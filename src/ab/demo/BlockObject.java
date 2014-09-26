package ab.demo;

import ab.vision.ABType;

import java.awt.geom.Rectangle2D;

public class BlockObject {

    int blockNumber;
    Rectangle2D blockShape;
    ABType blockMaterial;

    public BlockObject(){
    }

    public int getBlockNumber(){
        return blockNumber;
    }

    public Rectangle2D getBlockShape(){
        return blockShape;
    }

    public ABType getBlockMaterial(){
        return blockMaterial;
    }

    public void setBlockNumber(int mBlockNumber){
        blockNumber = mBlockNumber;
    }

    public void setBlockMaterial(ABType mBlockMaterial){
        blockMaterial = mBlockMaterial;
    }

    public void setBlockShape(Rectangle2D mBlockShape){
        blockShape = mBlockShape;
    }



}
