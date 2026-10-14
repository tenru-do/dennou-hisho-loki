package com.example.rokidkeyboardbridge;
final class SearchGroundingPolicy {
 static boolean requiresSources(boolean explicitSearch,int actualQueries){return explicitSearch||actualQueries>0;}
}
