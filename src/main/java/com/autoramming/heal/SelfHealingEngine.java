package com.autoramming.heal;

import org.openqa.selenium.*;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.util.*;
import java.util.stream.Collectors;

public class SelfHealingEngine {

    private static final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    private static double similarityThreshold = 0.5;

    public static WebElement findBestMatch(WebDriver driver, String locatorName, String expectedTag) {

        List<WebElement> elements = driver.findElements(By.xpath("//*"));

        double maxScore = 0.0;
        WebElement bestMatch = null;

        for (WebElement el : elements) {
            try {
                if (!el.isDisplayed()) continue;

                String tag = el.getTagName();
                if (expectedTag != null && !expectedTag.equalsIgnoreCase(tag)) continue;

                String combinedAttributes = collectAttributes(el);

                double score = similarityScore(locatorName, combinedAttributes);

                if (score > maxScore && score >= similarityThreshold) {
                    maxScore = score;
                    bestMatch = el;
                }

            } catch (StaleElementReferenceException ignored) {
            }
        }

        System.out.println("Best Score: " + maxScore + " (Threshold: " + similarityThreshold + ")");
        return bestMatch;
    }

    public static void setSimilarityThreshold(double threshold) {
        similarityThreshold = threshold;
    }

    public static double getSimilarityThreshold() {
        return similarityThreshold;
    }

    private static String collectAttributes(WebElement el) {
        return String.join(" ",
                safe(el.getAttribute("id")),
                safe(el.getAttribute("name")),
                safe(el.getAttribute("class")),
                safe(el.getAttribute("aria-label")),
                safe(el.getText())
        );
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }

    private static double similarityScore(String input, String target) {

        List<String> tokens1 = tokenize(input);
        List<String> tokens2 = tokenize(target);

        Map<String, Integer> freq1 = getFrequency(tokens1);
        Map<String, Integer> freq2 = getFrequency(tokens2);

        Set<String> allWords = new HashSet<>();
        allWords.addAll(freq1.keySet());
        allWords.addAll(freq2.keySet());

        int dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (String word : allWords) {
            int f1 = freq1.getOrDefault(word, 0);
            int f2 = freq2.getOrDefault(word, 0);

            dotProduct += f1 * f2;
            norm1 += Math.pow(f1, 2);
            norm2 += Math.pow(f2, 2);
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        String[] tokens = tokenizer.tokenize(text.toLowerCase());

        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(t -> t.length() > 1)
                .collect(Collectors.toList());
    }

    private static Map<String, Integer> getFrequency(List<String> tokens) {
        Map<String, Integer> map = new HashMap<>();

        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }

        return map;
    }


}
