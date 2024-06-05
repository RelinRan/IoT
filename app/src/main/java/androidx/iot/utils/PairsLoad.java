package androidx.iot.utils;

public class PairsLoad implements Runnable{

    private Pairs pairs;

    public PairsLoad(Pairs pairs) {
        this.pairs = pairs;
    }

    @Override
    public void run() {
        pairs.load();
    }

}
