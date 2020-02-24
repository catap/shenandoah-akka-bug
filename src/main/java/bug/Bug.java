package bug;

public class Bug {

    public static void main(String[] args) throws Exception {
        ClusterManager.restartClusterManager();

        Thread.sleep(Integer.MAX_VALUE);
    }
}
