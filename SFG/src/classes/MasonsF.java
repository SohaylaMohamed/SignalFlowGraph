package classes;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedSubgraph;

public class MasonsF {

    ////////////////////////
    private List<GraphPath<String, DefaultWeightedEdge>> forwardP;
    private List<Double> gainFP;
    private List<Double> deltaFP;
    private List<List<List<Integer>>> nonTouchingF = new ArrayList<>();
    private double delta;
    private List<List<String>> individualLoops;
    private List<Double> loopsGain;
    ////////////////////////////////

    private GraphC g = GraphC.getInstance();

    /**
     * Solve mason's formula
     *
     * @param source
     * @param destination
     * @return
     */
    public double solveG(String source, String destination) {
        if (source.equals(destination)) { //TODO added
            throw new RuntimeException("Same vertix");
        }
        individualLoops = g.findSimpleCycles();
        nonTouchingF = new ArrayList<>();
        loopsGain = new ArrayList<>();
        nonTouchingF = getAllNon(individualLoops);
        delta = computeDelta(nonTouchingF, this.individualLoops);
        forwardP = g.getAllPaths(source, destination);
        int numOfFP = forwardP.size();
        gainFP = computeGain(numOfFP);
        deltaFP = deltaEachFP(numOfFP);
        double numerator = 0;
        for (int i = 0; i < numOfFP; i++) {
            numerator += (gainFP.get(i) * deltaFP.get(i));
        }
        return numerator / delta;
    }



    private Set<String> getAllVertices(Set<DefaultWeightedEdge> edges) {
        List<String> v = new ArrayList<>();
        for(DefaultWeightedEdge e : edges) {
            String v1  = g.getGraph().getEdgeSource(e);
            String v2  = g.getGraph().getEdgeTarget(e);
            if(!v.contains(v1)) {
                v.add(v1);
            }
            if(!v.contains(v2)) {
                v.add(v2);
            }
        }
        return  new HashSet<>(v);
    }

    /**
     * find delta for each fprward path
     *
     * @param numOfFP
     * @return
     */

    private List<Double> deltaEachFP(int numOfFP) {
        List<Double> delta = new ArrayList<>();
        for (int i = 0; i < numOfFP; i++) {
            Set<String> vFP = new HashSet<>(forwardP.get(i).getVertexList());
            Set<String> remain = getUntouchedGraph(vFP);
            //remain.removeAll(vFP);
            DirectedSubgraph<String, DefaultWeightedEdge> subG = new DirectedSubgraph<String, DefaultWeightedEdge>(
                    (DirectedGraph<String, DefaultWeightedEdge>) g.getGraph(), remain);
            SzwarcfiterLauerSimpleCycles<String, DefaultWeightedEdge> cycleFind = new SzwarcfiterLauerSimpleCycles<>();
            cycleFind.setGraph(subG);
            List<List<String>> cycles = cycleFind.findSimpleCycles();
            List<List<List<Integer>>> non = new ArrayList<>();
            non = getAllNon(cycles);
            delta.add(computeDelta(non, cycles));
        }
        return delta;
    }

    private Set<String> getUntouchedGraph(Set<String> vFP) {
        List<String> gV = new ArrayList<>(g.getVertices());
        for (String v : vFP) {
            if (gV.contains(v)) {
                gV.remove(v);
            }
        }
        return new HashSet<String>(gV);
    }

    /**
     * compute Mn: gain of FP number n
     *
     * @param numOfFP
     * @return
     */
    public List<Double> computeGain(int numOfFP) {
        List<Double> gains = new ArrayList<>();
        for (int i = 0; i < numOfFP; i++) {
            List<DefaultWeightedEdge> es = forwardP.get(i).getEdgeList();
            double sum = 1;
            for (DefaultWeightedEdge e : es) {
                sum *= g.getGraph().getEdgeWeight(e);
            }
            gains.add(sum);
        }
        return gains;
    }

    /**
     * find delta for all graphs
     *
     * @return
     */
    public double computeDelta(List<List<List<Integer>>> nonTouching, List<List<String>> individualLoops) {
        if(individualLoops.isEmpty()) {
            return  1;
        }
        double total = 1 - computeTotalCyclesGain(individualLoops);
        if(nonTouching.isEmpty()) {
            return total;
        }
        double f = 2;
        List<Integer> loops = new ArrayList<>();
        for(List<List<Integer>> l1 : nonTouching) {
            double sum = 0;
            for(List<Integer> l2 : l1) {
                double loopsG = 1;
                for(Integer i : l2) {
                    loopsG *= gainOfLoop(individualLoops.get(i));
                }
                sum += loopsG;
            }
            total += (sum * Math.pow(-1, f));
            f++;
        }
        return total;
    }


    private boolean checkIfAllNonTouching(boolean[][] checkTouch, List<Integer> loops) {
        for (int i = 1; i < loops.size() - 1; i++) {
            for (int j = i + 1; j < loops.size(); j++) {
                if (checkTouch[loops.get(i)][loops.get(j)]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Total gain of cycles
     *
     * @param cycles
     * @return
     */

    private double computeTotalCyclesGain(List<List<String>> cycles) {
        int total = 0;
        for (List<String> vL : cycles) {
            Double g = gainOfLoop(vL);
            if(loopsGain.size() != individualLoops.size()) {
                loopsGain.add(g);
            }
            total += g;
        }
        return total;
    }

    private double gainOfLoop(List<String> vL) {
        List<DefaultWeightedEdge> edges = new ArrayList<>();
        for (int i = 0; i < vL.size(); i++) {
            if (i < vL.size() - 1) {
                edges.add(g.getGraph().getEdge(vL.get(i), vL.get(i + 1)));
            } else {
                edges.add(g.getGraph().getEdge(vL.get(i), vL.get(0)));
            }
        }
        int sum = 1;
        for (DefaultWeightedEdge e : edges) {
            sum *= g.getGraph().getEdgeWeight(e);
        }
        return sum;

    }

    /**
     * gets the total gain for n non touching loops
     *
     * @param loops
     * @return
     */
    public double computeWeightOfLoop(List<Integer> loops, List<List<String>> cycles) {
        int total = 1;
        for (Integer i : loops) {
            total *= loopsGain.get(i);
        }
        return total;
    }

    /**
     * check the untouching and touching loops
     *
     * @param cycles
     * @return
     */
    public boolean[][] untouchedLoops(List<List<String>> cycles) {
        boolean[][] checkTouch = new boolean[cycles.size()][cycles.size()];
        int numberOfCycles = cycles.size();
        for (int i = 0; i < numberOfCycles - 1; i++) {
            List<String> vLoop1 = cycles.get(i);
            int sizeL1 = vLoop1.size();
            for (int j = 0; j < sizeL1; j++) {
                for (int k = i + 1; k < numberOfCycles; k++) {
                    if (checkTouch[i][k]) {
                        continue;
                    }
                    if (cycles.get(k).contains(vLoop1.get(j))) {
                        checkTouch[i][k] = true;
                    }
                }
            }
        }
        return checkTouch;
    }

    //////////////////////////////////////////////////
    public List<Double> getGainFP() {
        return gainFP;
    }

    public List<GraphPath<String, DefaultWeightedEdge>> getForwardP() {
        return forwardP;
    }
    public List<Double> getDeltaFP() {
        return deltaFP;
    }

    public List<List<List<Integer>>> getNonTouching() {
        return nonTouchingF;
    }

    public double getDelta() {
        return delta;
    }

    public List<List<String>> getIndividualLoops() {
        return individualLoops;
    }

    public List<Double> getLoopsGain() {
        return loopsGain;
    }

    private List<List<List<Integer>>> delta(boolean[][] check, List<List<Integer>> loops, List<List<List<Integer>>> nonTouching){
        if(nonTouching.size() == check.length) {
            return nonTouching;
        }
        List<Integer> ins = new ArrayList<>();
        nonTouching.add(new ArrayList<List<Integer>>());
        for(List<Integer> l : loops) {
            List<Integer> temp = new ArrayList<>(l);
            Integer startIn = l.get(l.size()-1);
            for(int i = startIn+1; i< check.length; i++) {
                temp.add(i);
                if(checkIfAllNonTouching(check, temp)) {
                    nonTouching.get(nonTouching.size()-1).add(new ArrayList<>(temp));
                    temp.remove(temp.size()-1);
                }
            }
        }
        if(nonTouching.get(nonTouching.size()-1).isEmpty()) {
            nonTouching.remove(nonTouching.size()-1);
            return nonTouching;
        }
        delta(check, nonTouching.get(nonTouching.size()-1), nonTouching);

        return nonTouching;
    }
    private List<List<List<Integer>>> getAllNon(List<List<String>> cycles) {
        List<List<List<Integer>>> nonTouching = new ArrayList<>();
        boolean[][] checkTouch = this.untouchedLoops(cycles);
        int f = 2;
        double sum = 0;
        double total = 1 - computeTotalCyclesGain(cycles);
        List<Integer> loops = new ArrayList<>();
        nonTouching.add(new ArrayList<List<Integer>>());

        for (int i = 0; i < checkTouch.length - 1; i++) {
            int count = 1;
            loops.clear();
            for (int k = i + 1; k < checkTouch[i].length; k++) {
                if (!checkTouch[i][k]) {
                    count++;
                    if (!loops.contains(i))
                        loops.add(i);
                    loops.add(k);

                }
                if (count == 2) {
                    nonTouching.get(0).add(new ArrayList<>(loops));
                    count = 1;
                    loops.clear();
                }
            }
        }
        if(nonTouching.get(0).isEmpty()) {
            nonTouching.remove(0);
            return nonTouching;
        }
        nonTouching = delta(checkTouch, nonTouching.get(0), nonTouching);
        return  nonTouching;
    }
}