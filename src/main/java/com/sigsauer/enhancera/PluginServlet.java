package com.sigsauer.enhancera;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.velocity.VelocityManager;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class PluginServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(PluginServlet.class);

    private String project = "SCHO";

    @JiraImport
    private final VelocityManager velocityManager;

    PluginServlet(VelocityManager velocityManager) {
        this.velocityManager = velocityManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Issue> issues = searchIssues(project);
        Map<String, Integer> sortedWords = sort(issues);

        Map<String, Object> context = Maps.newHashMap();
        context.put("proj",project);
        context.put("map",sortedWords);

        String content = velocityManager.getEncodedBody("/","servlet.vm","UTF-8",context);
        resp.setContentType("text/html");
        resp.getWriter().write(content);
        resp.getWriter().close();
    }

    private List<Issue> searchIssues(String projectKey) {
        JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser loggedInUser = authenticationContext.getLoggedInUser();
        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        SearchService.ParseResult parseResult = searchService.parseQuery(
                loggedInUser,
                "project = "+projectKey);
        SearchResults results = null;
        try {
            results = searchService.search(
                    loggedInUser,
                    parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
        } catch (SearchException e) {
            e.printStackTrace();
        }

        return results.getIssues();
    }

    private Map<String, Integer> sort(List<Issue> issues) {
        List<String> words = new ArrayList<>();
        Map<String, Integer> repWords = Maps.newHashMap();
        for (Issue issue: issues) {
            words.addAll(Arrays.asList(issue.getSummary().toLowerCase().replaceAll("[^a-z ]","")
                    .split(" ")));
            words.addAll(Arrays.asList(issue.getDescription().toLowerCase().replaceAll("[^a-z ]","")
                    .split(" ")));
        }

        words.forEach(w -> {
            if (!repWords.containsKey(w))
                repWords.put(w, 1);
            else
                repWords.replace(w, repWords.get(w) + 1);
        });

        List<Map.Entry<String, Integer>> list = new ArrayList<>(repWords.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<String, Integer> sWords = new LinkedHashMap<>();
        for (int i = list.size()-1; i > 0 ; i--) {
            sWords.put(list.get(i).getKey(), list.get(i).getValue());
        }

        return sWords;
    }
}
