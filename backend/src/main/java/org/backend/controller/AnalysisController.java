package org.backend.controller;

import org.backend.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/analysis/")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @RequestMapping("index/")
    public String index(Model model) {
        Map<String, Object> analysisResult = analysisService.analyzeUserActivity();
        String hourlyPieChartBase64 = analysisService.generatePieChart((Map<String, Long>) analysisResult.get("hourlyUserCount"), "User Activity by Hour");
        String addressPieChartBase64 = analysisService.generatePieChart((Map<String, Long>) analysisResult.get("addressUserCount"), "User Activity by Location");
        String hourlyLineChartBase64 = analysisService.generateLineChart((Map<String, Long>) analysisResult.get("hourlyUserCount"), "User Activity by Hour");

        model.addAttribute("analysisResult", analysisResult);
        model.addAttribute("hourlyPieChartBase64", hourlyPieChartBase64);
        model.addAttribute("addressPieChartBase64", addressPieChartBase64);
        model.addAttribute("hourlyLineChartBase64", hourlyLineChartBase64);

        return "index";
    }
}