import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;

public class BidirectionalDijkstra {
    private static class Impl {
        // Number of nodes
        int n;
        ArrayList<Integer>[] forwardGraph;
        ArrayList<Integer>[] forwardCost;
        
        ArrayList<Integer>[] reverseGraph;
        ArrayList<Integer>[] reverseCost;
        
        long[] frwdistance;
        long[] revdistance;
        // Two priority queues, one for forward and one for backward search.
        PriorityQueue<Entry> frwqueue;
        PriorityQueue<Entry> revqueue;
        
        HashSet<Integer> frwProcessed; 
        HashSet<Integer> revProcessed;
        
        HashSet<Integer> visited;
        
        final Long INFINITY = Long.MAX_VALUE / 4;

        Impl(int n) {
            this.n = n;
            frwProcessed = new HashSet<Integer>();
            revProcessed = new HashSet<Integer>();
            frwdistance = new long[n];
            revdistance = new long[n];
            frwqueue = new PriorityQueue<Entry>();
            revqueue = new PriorityQueue<Entry>();
            visited = new HashSet<>();
            for (int i = 0; i < n; i++) {
                frwdistance[i] = revdistance[i] = INFINITY;
            }
        }

        // Reinitialize the data structures before new query after the previous query
        void clear() {
            for (int i : visited) {
                frwdistance[i] = revdistance[i] = INFINITY;
            }
            frwProcessed.clear();
            revProcessed.clear();
            frwqueue.clear();
            revqueue.clear();
            visited.clear();
        }


        // Returns the distance from s to t in the graph.
        Long query(int s, int t) {
            clear();
            frwdistance[s] = 0L;
            revdistance[t] = 0L;
            frwqueue.add(new Entry(s, 0L));
            revqueue.add(new Entry(t, 0L));
            visited.add(s); visited.add(t);
            while (!frwqueue.isEmpty() || !revqueue.isEmpty()) {
                  if (!frwqueue.isEmpty())  {
                      Entry curr = frwqueue.poll();
                      //If it isn't already processed
                      if (!frwProcessed.contains(curr.node)) {
                          frwProcessed.add(curr.node);
                          for (int i = 0; i <  forwardGraph[curr.node].size(); i++) {
                              int neighbor = forwardGraph[curr.node].get(i);
                               if (frwdistance[neighbor] > frwdistance[curr.node] + forwardCost[curr.node].get(i)) {
                                   Long newdist = frwdistance[curr.node] + forwardCost[curr.node].get(i);
                                   frwdistance[neighbor] = newdist;
                                   frwqueue.add(new Entry(neighbor, newdist));
                                   visited.add(neighbor);
                               }   
                          }
                      }
                      if (revProcessed.contains(curr.node))
                          break;
                  }
                  
                  if (!revqueue.isEmpty())  {
                      Entry curr = revqueue.poll();
                      //If it isn't already processed
                      if (!revProcessed.contains(curr.node)) {
                          revProcessed.add(curr.node);
                          for (int i = 0; i <  reverseGraph[curr.node].size(); i++) {
                              int neighbor = reverseGraph[curr.node].get(i);
                               if (revdistance[neighbor] > revdistance[curr.node] + reverseCost[curr.node].get(i)) {
                                   Long newdist = revdistance[curr.node] + reverseCost[curr.node].get(i);
                                   revdistance[neighbor] = newdist;
                                   revqueue.add(new Entry(neighbor, newdist));
                                   visited.add(neighbor);
                               }    
                          }
                      }
                      if (frwProcessed.contains(curr.node))
                          break;
                  }
            }
            
           return findDistanceFromProcessedNodes();
        }

        Long findDistanceFromProcessedNodes () {
            Long minDistance = INFINITY;
            for (int i : frwProcessed) {
                if (frwdistance[i] + revdistance[i] < minDistance)
                    minDistance = frwdistance[i] + revdistance[i];
            }
            for (int i : revProcessed) {
                if (frwdistance[i] + revdistance[i] < minDistance)
                    minDistance = frwdistance[i] + revdistance[i];
            }
            if(minDistance >= INFINITY)
                return -1L;
            return minDistance;
        }
        
        class Entry implements Comparable<Entry>
        {
            Long cost;
            int node;
          
            public Entry(int node, Long cost)
            {
                this.cost = cost;
                this.node = node;
            }
         
            public int compareTo(Entry other)
            {
                return cost < other.cost ? -1 : cost > other.cost ? 1 : 0;
            }
        }
    }

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        Impl bidij = new Impl(n);
            bidij.forwardGraph = (ArrayList<Integer>[])new ArrayList[n];
            bidij.reverseGraph = (ArrayList<Integer>[])new ArrayList[n];
            bidij.forwardCost = (ArrayList<Integer>[])new ArrayList[n];
            bidij.reverseCost = (ArrayList<Integer>[])new ArrayList[n];
            for (int i = 0; i < n; i++) {
                bidij.forwardGraph[i] = new ArrayList<Integer>();
                bidij.reverseGraph[i] = new ArrayList<Integer>();
                bidij.forwardCost[i] = new ArrayList<Integer>();
                bidij.reverseCost[i] = new ArrayList<Integer>();
            }

        for (int i = 0; i < m; i++) {
            int x, y, c;
            x = in.nextInt();
            y = in.nextInt();
            c = in.nextInt();
            bidij.forwardGraph[x - 1].add(y - 1);
            bidij.forwardCost[x - 1].add(c);
            bidij.reverseGraph[y - 1].add(x - 1);
            bidij.reverseCost[y - 1].add(c);
        }

        int t = in.nextInt();

        for (int i = 0; i < t; i++) {
            int u, v;
            u = in.nextInt();
            v = in.nextInt();
            System.out.println(bidij.query(u-1, v-1));
        }
    }
}

