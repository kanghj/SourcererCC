package models;

import java.util.*;

import com.google.common.collect.Maps;
import indexbased.EmbeddingsComparison;
import utility.Util;
import indexbased.SearchManager;

public class CloneValidator implements IListener, Runnable {

    @Override
    public void run() {
        try {
            CandidatePair candidatePair = SearchManager.verifyCandidateQueue.remove();

            this.validate(candidatePair);
        } catch (NoSuchElementException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void validate(CandidatePair candidatePair)
            throws InterruptedException {
        if (candidatePair.candidateTokens != null && candidatePair.candidateTokens.trim().length() > 0) {

            int similarity = this.updateSimilarity(candidatePair.queryBlock,
                    candidatePair.candidateTokens,
                    candidatePair.computedThreshold,
                    candidatePair.candidateSize, candidatePair.simInfo);
            if (similarity > 0) {
                ClonePair cp = new ClonePair(candidatePair.queryBlock.getId(), candidatePair.candidateId);

                SearchManager.reportCloneQueue.put(cp);
            }

            candidatePair.queryBlock = null;
            candidatePair.simInfo = null;

        } else {
            System.out.println("tokens not found for document");
        }
    }

    private int updateSimilarity(QueryBlock queryBlock, String tokens,
            int computedThreshold, int candidateSize, CandidateSimInfo simInfo) {
        int similarity = simInfo.similarity;

        Map<String, TokenInfo> map = new HashMap<>(queryBlock.getPrefixMap());
        map.putAll(queryBlock.getSuffixMap());

        try {
            Set<Map.Entry<String, Integer>> tokenStringSet = new HashSet<Map.Entry<String, Integer>>();
            Set<Map.Entry<String, Integer>> queryStringSet = new HashSet<Map.Entry<String, Integer>>();

            for (Map.Entry<String, TokenInfo> entry : map.entrySet()) {
                queryStringSet.add(Maps.immutableEntry(entry.getKey(), entry.getValue().getFrequency()));
            }


            for (String tokenfreqFrame : tokens.split("::")) {
                String[] tokenFreqInfo = tokenfreqFrame.split(":");

                tokenStringSet.add(Maps.immutableEntry(tokenFreqInfo[0], Integer.valueOf(tokenFreqInfo[1])));

//                TokenInfo tokenInfo  = map.get(tokenFreqInfo[0]);
            }

//            float[] tokenVectorSum = EmbeddingsComparison.sum(tokenStringSet);
//            float[] queryVectorSum = EmbeddingsComparison.sum(queryStringSet);

            if (EmbeddingsComparison.cosineSimilarityExceedsThreshold(tokenStringSet, queryStringSet, 900)) {
//                System.err.println("found ");
                return 1;
            }  else {
//                System.err.println("NOT found ");
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("possible error in the format. tokens: "
                    + tokens);
        }
        return -1;
    }

    private int updateSimilarityHelper(
            TokenInfo tokenInfo, int similarity, int candidatesTokenFreq) {
        similarity += Math.min(tokenInfo.getFrequency(), candidatesTokenFreq);
        // System.out.println("similarity: "+ similarity);
        return similarity;
    }
}
