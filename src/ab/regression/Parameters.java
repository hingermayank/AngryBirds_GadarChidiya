package ab.regression;

/**
 * Created by mayank on 8/11/14.
 */
public class Parameters {

    String type;
    Double pweight, aweight, distance, weakness, angle;
    int score;

    public void setType(String type) {
        this.type = type;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    public void setPweight(Double pweight) {
        this.pweight = pweight;
    }

    public String getType() {
        return type;
    }

    public Double getPweight() {
        return pweight;
    }

    public Double getAweight() {
        return aweight;
    }

    public Double getDistance() {
        return distance;
    }

    public Double getWeakness() {
        return weakness;
    }

    public Double getAngle() {
        return angle;
    }

    public int getScore() {
        return score;
    }

    public void setAweight(Double aweight) {
        this.aweight = aweight;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public void setWeakness(Double weakness) {
        this.weakness = weakness;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
