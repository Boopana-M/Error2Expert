-- CodeMemory Seed Data
-- Provides demo user, LeetCode problem catalog, and realistic mistake histories

-- 1. Demo User (password is 'password' BCrypt hashed)
INSERT INTO users (id, name, email, password_hash, created_at)
VALUES (1, 'Boopana', 'boopana@example.com', '$2a$10$7vN3Vp0lP7kUvT8p7D9Qe.5eL61yZ3xK8aP8tJzV9e1uR4eK0j7fG', NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Core Problem Catalog
INSERT INTO problems (id, leetcode_id, title, difficulty, url) VALUES
(1, '704', 'Binary Search', 'EASY', 'https://leetcode.com/problems/binary-search/'),
(2, '33', 'Search in Rotated Sorted Array', 'MEDIUM', 'https://leetcode.com/problems/search-in-rotated-sorted-array/'),
(3, '153', 'Find Minimum in Rotated Sorted Array', 'MEDIUM', 'https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/'),
(4, '35', 'Search Insert Position', 'EASY', 'https://leetcode.com/problems/search-insert-position/'),
(5, '200', 'Number of Islands', 'MEDIUM', 'https://leetcode.com/problems/number-of-islands/'),
(6, '133', 'Clone Graph', 'MEDIUM', 'https://leetcode.com/problems/clone-graph/'),
(7, '207', 'Course Schedule', 'MEDIUM', 'https://leetcode.com/problems/course-schedule/'),
(8, '261', 'Graph Valid Tree', 'MEDIUM', 'https://leetcode.com/problems/graph-valid-tree/'),
(9, '322', 'Coin Change', 'MEDIUM', 'https://leetcode.com/problems/coin-change/'),
(10, '70', 'Climbing Stairs', 'EASY', 'https://leetcode.com/problems/climbing-stairs/')
ON CONFLICT (id) DO NOTHING;

-- 2b. Problem Topics Mapping
INSERT INTO problem_topics (problem_id, topic) VALUES
(1, 'Binary Search'), (1, 'Array'),
(2, 'Binary Search'), (2, 'Array'),
(3, 'Binary Search'), (3, 'Array'),
(4, 'Binary Search'), (4, 'Array'),
(5, 'Depth-First Search'), (5, 'Breadth-First Search'), (5, 'Graph'), (5, 'Matrix'),
(6, 'Depth-First Search'), (6, 'Breadth-First Search'), (6, 'Graph'),
(7, 'Depth-First Search'), (7, 'Breadth-First Search'), (7, 'Graph'), (7, 'Topological Sort'),
(8, 'Depth-First Search'), (8, 'Breadth-First Search'), (8, 'Graph'), (8, 'Union Find'),
(9, 'Dynamic Programming'), (9, 'Breadth-First Search'),
(10, 'Dynamic Programming'), (10, 'Math')
ON CONFLICT (problem_id, topic) DO NOTHING;

-- 3. Historical Submissions (Binary Search boundary errors pattern)
INSERT INTO submissions (id, user_id, problem_id, code, language, result, attempt_number, submitted_at) VALUES
(1, 1, 1, 'class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length; while(l<r) { int m=(l+r)/2; if(nums[m]==target) return m; if(nums[m]<target) l=m; else r=m; } return -1; } }', 'java', 'TIME_LIMIT_EXCEEDED', 1, NOW() - INTERVAL '5 days'),
(2, 1, 1, 'class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length-1; while(l<=r) { int m=(l+r)/2; if(nums[m]==target) return m; if(nums[m]<target) l=m+1; else r=m-1; } return -1; } }', 'java', 'ACCEPTED', 2, NOW() - INTERVAL '5 days'),
(3, 1, 2, 'class Solution { public int search(int[] nums, int target) { int low=0, high=nums.length-1; while(low<=high) { int mid=low+(high-low)/2; if(nums[mid]==target) return mid; if(nums[low]<=nums[mid]) { if(target>=nums[low] && target<nums[mid]) high=mid; else low=mid; } } return -1; } }', 'java', 'WRONG_ANSWER', 1, NOW() - INTERVAL '4 days'),
(4, 1, 3, 'class Solution { public int findMin(int[] nums) { int left=0, right=nums.length-1; while(left<right) { int mid=left+(right-left)/2; if(nums[mid]>nums[right]) left=mid; else right=mid-1; } return nums[left]; } }', 'java', 'WRONG_ANSWER', 1, NOW() - INTERVAL '3 days'),
(5, 1, 5, 'class Solution { public int numIslands(char[][] grid) { int count=0; for(int i=0;i<grid.length;i++){ for(int j=0;j<grid[0].length;j++){ if(grid[i][j]==''1'') { count++; dfs(grid, i, j); } } } return count; } void dfs(char[][] g, int r, int c) { if(r<0||c<0||r>=g.length||c>=g[0].length) return; dfs(g, r+1, c); dfs(g, r-1, c); dfs(g, r, c+1); dfs(g, r, c-1); } }', 'java', 'RUNTIME_ERROR', 1, NOW() - INTERVAL '2 days'),
(6, 1, 7, 'class Solution { public boolean canFinish(int numCourses, int[][] prerequisites) { List<List<Integer>> adj = new ArrayList<>(); for(int i=0;i<numCourses;i++) adj.add(new ArrayList<>()); for(int[] p : prerequisites) adj.get(p[1]).add(p[0]); boolean[] visited = new boolean[numCourses]; for(int i=0;i<numCourses;i++) { if(!dfs(i, adj, visited)) return false; } return true; } boolean dfs(int node, List<List<Integer>> adj, boolean[] visited) { if(visited[node]) return false; visited[node] = true; for(int neighbor : adj.get(node)) { if(!dfs(neighbor, adj, visited)) return false; } return true; } }', 'java', 'WRONG_ANSWER', 1, NOW() - INTERVAL '1 days')
ON CONFLICT (id) DO NOTHING;

-- 4. Mistake Log with Explanations
INSERT INTO mistakes (id, submission_id, category, user_explanation, ai_explanation, confidence, created_at) VALUES
(1, 1, 'BOUNDARY_ERROR', 'I used l=m instead of l=m+1 and loop never terminated on boundary.', 'Infinite loop caused by improper low pointer advance during binary search.', 0.94, NOW() - INTERVAL '5 days'),
(2, 3, 'BOUNDARY_ERROR', 'I set high=mid instead of high=mid-1, got stuck at boundaries.', 'Incorrect binary search boundary management: interval updates omit +/- 1 offset.', 0.92, NOW() - INTERVAL '4 days'),
(3, 4, 'BOUNDARY_ERROR', 'I subtracted 1 from right when mid could be the minimum.', 'Premature elimination of potential answer by mismanaging binary search boundary.', 0.89, NOW() - INTERVAL '3 days'),
(4, 5, 'GRAPH_VISITED_ERROR', 'Forgot to mark visited in DFS so stack overflowed.', 'Missing visited-state transition in graph traversal leading to infinite recursion.', 0.96, NOW() - INTERVAL '2 days'),
(5, 6, 'GRAPH_VISITED_ERROR', 'Did not unmark visited or track path for cycle detection in directed graph.', 'Confusion between visited global state and recursion stack cycle state in DAG traversal.', 0.90, NOW() - INTERVAL '1 days')
ON CONFLICT (id) DO NOTHING;

-- 5. Longitudinal Weaknesses
INSERT INTO weaknesses (id, user_id, concept, mistake_category, frequency, confidence, mastery_score, first_observed, last_observed) VALUES
(1, 1, 'Binary Search', 'BOUNDARY_ERROR', 3, 0.92, 72.0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days'),
(2, 1, 'Graph Traversal', 'GRAPH_VISITED_ERROR', 2, 0.93, 78.5, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 days')
ON CONFLICT (id) DO NOTHING;

-- 6. Targeted Practice Recommendations
INSERT INTO recommendations (id, user_id, weakness_id, problem_id, reason, status, created_at) VALUES
(1, 1, 1, 4, 'Practice standard boundary condition termination with Search Insert Position (Easy)', 'PENDING', NOW() - INTERVAL '3 days'),
(2, 1, 1, 2, 'Re-attempt Search in Rotated Sorted Array focusing on left/right boundary updates', 'PENDING', NOW() - INTERVAL '3 days'),
(3, 1, 2, 8, 'Strengthen visited state tracking and cycle detection with Graph Valid Tree (Medium)', 'PENDING', NOW() - INTERVAL '1 days')
ON CONFLICT (id) DO NOTHING;

-- Reset identity sequence if using BIGSERIAL
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('problems_id_seq', (SELECT COALESCE(MAX(id), 1) FROM problems));
SELECT setval('submissions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM submissions));
SELECT setval('mistakes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM mistakes));
SELECT setval('weaknesses_id_seq', (SELECT COALESCE(MAX(id), 1) FROM weaknesses));
SELECT setval('recommendations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM recommendations));
