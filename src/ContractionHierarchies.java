import java.util.Scanner;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class ContractionHierarchies {
    private static class Impl {
        // See the descriptions of these fields in the Bi-directional Dijkstra
        int n;
        ArrayList<Integer>[][] adj;
        ArrayList<Long>[][] cost;
        Long[][] distance;
        ArrayList<PriorityQueue<Entry>> queue;
        boolean[] visited;
        ArrayList<Integer> workset;
        final Long INFINITY = Long.MAX_VALUE / 4;
        HashSet<Integer> frwProcessed;
        HashSet<Integer> revProcessed;
        
        /*
         * My NEW ADJUSTMENT
         */
        List<Map<Integer, Long>> newFrwGraph;
        List<Map<Integer, Long>> newRevGraph;
       
        // Position of the node in the node ordering
        Integer[] rank;
        // Level of the node for level heuristic in the node ordering
        Long[] level;
        //Number of contracted neighbors for cn heuristic
        Long[] cn;
        //
        Integer[] hops;

        Impl(int n) {
            this.n = n;
            visited = new boolean[n];
            Arrays.fill(visited, false);
            workset = new ArrayList<Integer>();
            frwProcessed = new HashSet<Integer>();
            revProcessed = new HashSet<Integer>();
            rank = new Integer[n];
            hops = new Integer[n];
            level = new Long[n];
            cn = new Long[n];
            distance = new Long[][] {new Long[n], new Long[n]};
            for (int i = 0; i < n; ++i) {
                distance[0][i] = distance[1][i] = INFINITY;
                level[i] = 0L;
                cn[i] = 0L;
                rank[i] = 0;
                hops[i] = 0;
            }
            queue = new ArrayList<PriorityQueue<Entry>>();
            queue.add(new PriorityQueue<Entry>(n));
            queue.add(new PriorityQueue<Entry>(n));
        }

        // Preprocess the graph
        void preprocess() {
            // This priority queue will contain pairs (importance, node) with the least important node in the head
            PriorityQueue<Entry> q = new PriorityQueue<Entry>(n);
            //Represents order of contraction of the nodes
            int contractionOrder = 0;
            for (int i = 0; i < n; i++) {
                q.add(new Entry(computeImportance(i), i));
            }
            while (!q.isEmpty()) {
                int node = q.poll().node;
                Long newImprt;
                /**
                 * Its important to still check if the queue is empty, cos in a situation where the only node
                 * in the queue has been polled above, using peek will return null.
                 */
                 while (!q.isEmpty() &&((newImprt = computeImportance(node)) > q.peek().cost)) {
                    
                    q.add(new Entry(newImprt, node));
                    node = q.poll().node;
                }
                contract(node);
                contractionOrder++; 
                rank[node] = contractionOrder;
            }
            computeNewGraph();
        }
        
        void contract (int v) {
            for (int i : adj[0][v]) {
                level[i] = Math.max(level[i], level[v] + 1);
                cn[i] = cn[i] + 1;
            }
            boolean addShortcuts = true;
            calcShortcutsToAdd(v, addShortcuts);
        }
        void add_edge(int side, int u, int v, Long c) {
            for (int i = 0; i < adj[side][u].size(); ++i) {
                int w = adj[side][u].get(i);
                if (w == v) {
                    Long cc = Math.min(cost[side][u].get(i), c);
                    cost[side][u].set(i, cc);
                    return;
                }
            }
            adj[side][u].add(v);
            cost[side][u].add(c);
        }

        void apply_shortcut(Shortcut sc) {
            add_edge(0, sc.u, sc.v, sc.cost);
            add_edge(1, sc.v, sc.u, sc.cost);
        }

        void clear() {
            for (int v : workset) {
                distance[0][v] = distance[1][v] = INFINITY;
                visited[v] = false;
                hops[v] = 0;
            }
            workset.clear();
            queue.get(0).clear();
            queue.get(1).clear();
            frwProcessed.clear();
            revProcessed.clear();
        }

        void mark_visited(int u) {
            visited[u] = true;
            workset.add(u);
        }

        // See the description of this method in the starter for friend_suggestion
        boolean visit(int side, int v, Long dist) {
            // Implement this method yourself
            return false;
        }                

        // Add the shortcuts corresponding to contracting node v. Return v's importance.
        Long computeImportance(int v) {
         
            // Compute the node importance in the end
            Long[] shortcut = calcShortcutsToAdd(v, false);
            Long shortcuts = shortcut[0]; 
            Long shortcutCover = shortcut[1];
            Long vlevel = level[v];
            Long neighbors = cn[v];
            
            // Compute the correct values for the above heuristics before computing the node importance
            Long importance = (shortcuts - adj[0][v].size() - adj[1][v].size()) + neighbors + shortcutCover + vlevel;
            return importance;
        }

        //Find number of shortcuts to be added and shortcut cover
        //Add the shortcuts to the graph if addShortcuts is set to true
        Long[] calcShortcutsToAdd(int v, boolean addShortcuts) {
             //Long estimate = calcLimitingDist(v);
             List<Integer> incoming = new ArrayList<Integer>();
             List<Integer> outgoing = new ArrayList<Integer>();
             for (int i = 0; i < adj[1][v].size(); i++) {
                 //If it hasn't been contracted
                 if (rank[adj[1][v].get(i)] == 0) 
                     incoming.add(adj[1][v].get(i));
             }
             for (int i = 0; i < adj[0][v].size(); i++) {
                 //If it hasn't been contracted
                 if (rank[adj[0][v].get(i)] == 0) 
                     outgoing.add(adj[0][v].get(i));
             }
             
             Long estimate = calcLimitingDist(v);
             Long shortcuts = 0L;
             Long shortcutCover = 0L;
             Long[] array = new Long[2];
             Set<Integer> witnessFound;
             //From all nodes with incoming edges into v, find witness paths to all of v's outgoing neighbors
             //If found, remove the outgoing neighbor 
             for (int i : incoming) {
                 Set<Integer> targets = new HashSet<Integer>(outgoing);
                 //Find witness paths (Find shorter paths than l(u,v) + l(v, w)) ensuring the run excludes node v
                 witnessFound = dijkstraRun(i, targets, v, estimate);
                 shortcuts += targets.size() - witnessFound.size();
                 shortcutCover += targets.size() - witnessFound.size();
                 if (targets.size() - witnessFound.size() > 0)
                     shortcutCover += 1; //Add the i node itself
                 
                 if (addShortcuts) {
                     for (int j : targets) {
                         if(!witnessFound.contains(j))
                         apply_shortcut(new Shortcut (i, j, getDist(i, v, 0) + getDist(v, j, 0)));
                     }
                 }
                 //Clear the workset after every dijkstra run.
              
             }
             array[0] = shortcuts;
             array[1] = shortcutCover;
             return array;
        }
        
        Long calcLimitingDist (int v) {
            Long maxDist = 0L;
            for (int u : adj[1][v]) {
                 for (int w : adj[0][v]) {
                     if (getDist(u, v, 0) + getDist(v, w, 0) > maxDist)
                         maxDist = getDist(u, v, 0) + getDist(v, w, 0);
                 }
            }
            /**
             * Implement a better limiting distance
             */
            return maxDist;
        }
        
        Set<Integer> dijkstraRun (int s, Set<Integer> targets, int nodeToExclude, Long estimate) {
             Set<Integer> witnessFound = new HashSet<Integer>();
             distance[0][s] = 0L;
             PriorityQueue<Entry> workingQueue = queue.get(0);
             workingQueue.add(new Entry(0L, s));
             mark_visited(s);
             Long distSToContractingNode = getDist(s, nodeToExclude, 0);
             while (!workingQueue.isEmpty())  {
                 int node = workingQueue.poll().node;
                 //ADD the hops back
                 if (distance[0][node] > estimate || hops[node] > 6)
                     break;
                 if (!frwProcessed.contains(node)) {
                      //If a witness path is found from s(a predecessor of nodeToExclude) 
                      // to a target (a successor of nodeToExclude), remove the target.
                      if(targets.contains(node) && distance[0][node] <  distSToContractingNode + getDist(nodeToExclude, node, 0))
                        witnessFound.add(node);
                      frwProcessed.add(node);
                      for (int i = 0; i < adj[0][node].size(); i++) {
                          int neigbor = adj[0][node].get(i);
                          long newDist;
                         if(distance[0][neigbor] > (newDist = distance[0][node] + cost[0][node].get(i)) && neigbor != nodeToExclude) {
                             distance[0][neigbor] = newDist;
                             workingQueue.add(new Entry(distance[0][neigbor], neigbor));
                             hops[neigbor] = hops[node] + 1;
                             if (!visited[neigbor])
                                 mark_visited(neigbor);
                         }
                             
                     }
                 }
             }
             clear();
             return witnessFound;
        }
        
        //Get Distance between two neighbors 
        //**node2 must be an outgoing neighbor from node1 (node1--> node2)
        Long getDist (int node1, int node2, int side) {
                 int position = 0;
                 long currDist = INFINITY;
                 for (int i = 0; i < adj[side][node1].size(); i++) {
                        if (adj[side][node1].get(i) == node2) {
                            position = i;
                        }
                 }
            return cost[side][node1].get(position);
        }
        
        void computeNewGraph() {
            newFrwGraph = new ArrayList<Map<Integer, Long>>();
            newRevGraph = new ArrayList<Map<Integer, Long>>();
            
            for (int i = 0; i < adj[0].length; i++) {
                Map<Integer, Long> map = new HashMap<>();
                for (int j = 0; j < adj[0][i].size(); j++) {
                    int neighbor = adj[0][i].get(j);
                    long edgeCost = cost[0][i].get(j);
                   if (rank[neighbor] > rank[i]) {
                       map.put(neighbor, edgeCost);
                   }
                }
                newFrwGraph.add(map);
            }
            
            for (int i = 0; i < adj[1].length; i++) {
                Map<Integer, Long> map = new HashMap<>();
                for (int j = 0; j < adj[1][i].size(); j++) {
                    int neighbor = adj[1][i].get(j);
                    long edgeCost = cost[1][i].get(j);
                   if (rank[neighbor] > rank[i]) {
                       map.put(neighbor, edgeCost);
                   }
                }
                newRevGraph.add(map);
            }
        }
        
        // Returns the distance from s to t in the graph
        Long query(int s, int t) {
            if (s == t) {
                return 0L;
            }
            distance[0][s] = 0L; distance[1][t] = 0L;
            mark_visited(s); mark_visited(t);
            Long estimate = INFINITY;
            PriorityQueue<Entry> frwq = new PriorityQueue<Entry>();
            PriorityQueue<Entry> revq = new PriorityQueue<Entry>();
            frwq.add(new Entry(0L, s)); revq.add(new Entry(0L, t));
            int node;
            Long newDist;
            while (!frwq.isEmpty() || !revq.isEmpty())  {
                if (!frwq.isEmpty())  {
                    node = frwq.poll().node;
                    if (distance[0][node] < estimate) {
                         if (!frwProcessed.contains(node))  {
                             for (int u : newFrwGraph.get(node).keySet()) {
                                if (distance[0][u] > (newDist = distance[0][node] + newFrwGraph.get(node).get(u))) {
                                    distance[0][u] = newDist;
                                    frwq.add(new Entry(distance[0][u], u));
                                    mark_visited(u);
                                }  
                           }
                           frwProcessed.add(node);
                           if (revProcessed.contains(node) && distance[0][node] + distance[1][node] < estimate)
                               estimate = distance[0][node] + distance[1][node];
                        }
                    }
                }
                
                if (!revq.isEmpty()) {
                    node = revq.poll().node;
                    if (distance[1][node] < estimate) {
                       
                       if(!revProcessed.contains(node))  {
                           for (int u : newRevGraph.get(node).keySet()) {
                                 if (distance[1][u] > (newDist = distance[1][node] + newRevGraph.get(node).get(u))) {
                                     distance[1][u] = newDist;
                                     revq.add(new Entry(distance[1][u], u));
                                     mark_visited(u);
                                 }
                            }
                           revProcessed.add(node);
                            if (frwProcessed.contains(node) && distance[0][node] + distance[1][node] < estimate)
                                estimate = distance[0][node] + distance[1][node];
                         
                       }
                    }
                }
            }
            clear();
            return estimate == INFINITY ? -1 : estimate;            
        }

        class Entry implements Comparable<Entry>
        {
            Long cost;
            int node;
          
            public Entry(Long cost, int node)
            {
                this.cost = cost;
                this.node = node;
            }
         
            public int compareTo(Entry other)
            {
                if (cost == other.cost) {
                    return node < other.node ? -1 : node > other.node ? 1: 0;
                }
                return cost < other.cost ? -1 : cost > other.cost ? 1 : 0;
            }
        }

        class Shortcut
        {
            int u;
            int v;
            Long cost;

            public Shortcut(int u, int v, Long c)
            {
                this.u = u;
                this.v = v;
                cost = c;
            }
        }
    }

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        Impl ch = new Impl(n);
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[][] tmp1 = (ArrayList<Integer>[][])new ArrayList[2][];
        ch.adj = tmp1;
        @SuppressWarnings("unchecked")
        ArrayList<Long>[][] tmp2 = (ArrayList<Long>[][])new ArrayList[2][];
        ch.cost = tmp2;
        for (int side = 0; side < 2; ++side) {
            @SuppressWarnings("unchecked")
            ArrayList<Integer>[] tmp3 = (ArrayList<Integer>[])new ArrayList[n];
            ch.adj[side] = tmp3;
            @SuppressWarnings("unchecked")
            ArrayList<Long>[] tmp4 = (ArrayList<Long>[])new ArrayList[n];
            ch.cost[side] = tmp4;
            for (int i = 0; i < n; i++) {
                ch.adj[side][i] = new ArrayList<Integer>();
                ch.cost[side][i] = new ArrayList<Long>();
            }
        }

        for (int i = 0; i < m; i++) {
            int x, y;
            Long c;
            x = in.nextInt();
            y = in.nextInt();
            c = in.nextLong();
            ch.adj[0][x - 1].add(y - 1);
            ch.cost[0][x - 1].add(c);
            ch.adj[1][y - 1].add(x - 1);
            ch.cost[1][y - 1].add(c);
        }

         ch.preprocess();
         System.out.println("Ready");

        int t = in.nextInt();

        for (int i = 0; i < t; i++) {
            int u, v;
            u = in.nextInt();
            v = in.nextInt();
            System.out.println(ch.query(u-1, v-1));
        }
        in.close();
    }
}

