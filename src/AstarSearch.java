import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.lang.Math;

public class AstarSearch {
    private static class Impl {
        // Number of nodes
        int n;
        // Coordinates of nodes (Wont change)
        long[] x;
        long[] y;
        // Actual Graph and edge weights (Wont change)
        ArrayList<Integer>[][] adj;
        ArrayList<Integer>[][] cost;
        
        long[] forwardEucldist;
         //The new EdgeWeights recomputed for Astar search
         ArrayList<Long>[] newForwardCost;
         
        long[] frwdistance;
        PriorityQueue<Entry> frwqueue;
        boolean[] frwProcessed; 
        HashSet<Integer> workset;
        final long INFINITY = Long.MAX_VALUE / 4;

        Impl(int n) {
            this.n = n;
            x = new long[n];
            y = new long[n];
            workset = new HashSet<Integer>();
            frwdistance = new long[n]; 
            forwardEucldist = new long[n]; 
            frwProcessed = new boolean[n]; 
            
            newForwardCost =  (ArrayList<Long>[]) new ArrayList[n]; 
            for (int i = 0; i < n; i++) {
                newForwardCost[i] = new ArrayList<Long>();
                frwProcessed[i] = false;
            }
            
            for (int i = 0; i < n; ++i) {
                frwdistance[i] = INFINITY;
                forwardEucldist[i] = INFINITY;
            }
            frwqueue = new PriorityQueue<Entry>(n);
        }

        void clear() {
            for (int v: workset) {
                frwdistance[v] = INFINITY;
                frwProcessed[v] = false;
                forwardEucldist[v] = INFINITY;
            }
            for (int i = 0; i < n; i++) {
                newForwardCost[i].clear();
            }
            workset.clear();
            frwqueue.clear();
        }

        Long getEucldistance(int s, int t) {
            if (forwardEucldist[s] == INFINITY)
                    forwardEucldist[s] =  (long) (Math.sqrt(Math.pow((x[s] - x[t]), 2) + Math.pow((y[s] - y[t]), 2)));   
            return forwardEucldist[s];
        }
        
        // Returns the distance from s to t in the graph.
        long query(int s, int t) {
            clear();
            ArrayList<Integer>[] forwardGraph = adj[0];
            ArrayList<Integer>[] frwCost = cost[0];
            frwdistance[s] = 0L;
            frwqueue.add(new Entry(s, 0L));
            workset.add(s);
            Long newdist;
            while (!frwqueue.isEmpty()) {
                      Entry curr = frwqueue.poll();
                      if(curr.node == t)
                          break;
                      //If it isn't already processed
                      if (!frwProcessed[curr.node]) {
                          frwProcessed[curr.node] = true;
                          for (int i = 0; i <  forwardGraph[curr.node].size(); i++) {
                              int neighbor = forwardGraph[curr.node].get(i);
                               if (frwdistance[neighbor] > (newdist = frwdistance[curr.node] + frwCost[curr.node].get(i)- 
                                   getEucldistance(curr.node, t) + getEucldistance(neighbor, t))) {
                                      frwdistance[neighbor] = newdist;
                                      frwqueue.add(new Entry(neighbor, newdist));
                                      workset.add(neighbor);
                               }
                          }
                      }
                     
            }
            if (frwdistance[t] >= INFINITY)
                return -1;
            return (frwdistance[t] + getEucldistance(s,t) - getEucldistance(t, t));
        }

        class Entry implements Comparable<Entry>
        {
            long cost;
            int node;
          
            public Entry(int node, long cost)
            {
                this.cost = cost;
                this.node = node;
            }
         
            public int compareTo(Entry other)
            {
                return cost < other.cost ? -1 : cost > other.cost ? 1 : 0;
            }
            
            public int  getNode() {
               return node;
            }
            public Long getCost() {
                return cost;
            }
            
        }
    }

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        Impl DistWithCoords = new Impl(n);
        DistWithCoords.adj = (ArrayList<Integer>[][])new ArrayList[2][];
        DistWithCoords.cost = (ArrayList<Integer>[][])new ArrayList[2][];
        for (int side = 0; side < 2; ++side) {
            DistWithCoords.adj[side] = (ArrayList<Integer>[])new ArrayList[n];
            DistWithCoords.cost[side] = (ArrayList<Integer>[])new ArrayList[n];
            for (int i = 0; i < n; i++) {
                DistWithCoords.adj[side][i] = new ArrayList<Integer>();
                DistWithCoords.cost[side][i] = new ArrayList<Integer>();
            }
        }

        for (int i = 0; i < n; i++) { 
            int x, y;
            x = in.nextInt();
            y = in.nextInt();
            DistWithCoords.x[i] = x;
            DistWithCoords.y[i] = y;
        }

        for (int i = 0; i < m; i++) {
            int x, y, c;
            x = in.nextInt();
            y = in.nextInt();
            c = in.nextInt();
            DistWithCoords.adj[0][x - 1].add(y - 1);
            DistWithCoords.cost[0][x - 1].add(c);
            DistWithCoords.adj[1][y - 1].add(x - 1);
            DistWithCoords.cost[1][y - 1].add(c);
        }

        int t = in.nextInt();

        for (int i = 0; i < t; i++) {
            int u, v;
            u = in.nextInt();
            v = in.nextInt();
            System.out.println(DistWithCoords.query(u-1, v-1));
        }
    }
}

