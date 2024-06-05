package androidx.iot.utils;

public class PairsStore implements Runnable{

    private Pairs pairs;
    private String comments;

    public PairsStore(Pairs pairs) {
        this.pairs = pairs;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public void run() {
        if (comments==null){
            pairs.store();
        }else{
            pairs.store(comments);
        }
    }
}
