package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import java.time.Clock;
import java.util.stream.Collectors;

public class CrawlAction extends RecursiveAction {
    String url;
    int maxDepth;
    Clock clock;
    List<Pattern> ignoredUrls;
    PageParserFactory parserFactory;

    public CrawlAction(String url, int maxDepth, Clock clock, List<Pattern> ignoredUrls,
                       PageParserFactory parserFactory) {
        this.url = url;
        this.maxDepth = maxDepth;
        this.clock=clock;
        this.ignoredUrls=ignoredUrls;
        this.parserFactory=parserFactory;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(ParallelWebCrawler.deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (ParallelWebCrawler.visitedUrls.contains(url)) {
            return;
        }
        ParallelWebCrawler.visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (ParallelWebCrawler.counts.containsKey(e.getKey())) {
                ParallelWebCrawler.counts.put(e.getKey(), e.getValue() + ParallelWebCrawler.counts.get(e.getKey()));
            } else {
                ParallelWebCrawler.counts.put(e.getKey(), e.getValue());
            }
        }
        List<CrawlAction> actions=
                 result.getLinks()
                .stream().map(link->new CrawlAction(link, maxDepth-1, clock, ignoredUrls, parserFactory))
                .collect(Collectors.toList());
        invokeAll(actions);
    }
}
