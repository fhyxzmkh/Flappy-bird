package org.backend.service;

import org.backend.model.User;
import org.backend.repository.UserRepository;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> analyzeUserActivity() {
        List<User> users = (List<User>) userRepository.findAll();

        // 分析哪个时段用户最多
        Map<String, Long> hourlyUserCount = users.stream()
                .collect(Collectors.groupingBy(user -> String.valueOf(user.getTime().toLocalDateTime().getHour()), Collectors.counting()));

        String busiestHour = hourlyUserCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // 分析哪个地点用户最多
        Map<String, Long> addressUserCount = users.stream()
                .collect(Collectors.groupingBy(User::getAddress, Collectors.counting()));

        String busiestLocation = addressUserCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Object> result = new HashMap<>();
        result.put("busiestHour", busiestHour);
        result.put("busiestLocation", busiestLocation);
        result.put("hourlyUserCount", hourlyUserCount);
        result.put("addressUserCount", addressUserCount);

        return result;
    }

    public String generatePieChart(Map<String, Long> data, String title) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        data.forEach(dataset::setValue);

        // 设置支持中文字符的字体
        StandardChartTheme chartTheme = new StandardChartTheme("CN");
        chartTheme.setExtraLargeFont(new Font("Noto Sans CJK SC", Font.BOLD, 20));
        chartTheme.setLargeFont(new Font("Noto Sans CJK SC", Font.BOLD, 14));
        chartTheme.setRegularFont(new Font("Noto Sans CJK SC", Font.PLAIN, 12));
        ChartFactory.setChartTheme(chartTheme);

        JFreeChart chart = ChartFactory.createPieChart(
                title,
                dataset,
                true,
                true,
                false
        );

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 500, 300);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateLineChart(Map<String, Long> data, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((hour, count) -> dataset.addValue(count, "用户数量", hour));

        // 设置支持中文字符的字体
        StandardChartTheme chartTheme = new StandardChartTheme("CN");
        chartTheme.setExtraLargeFont(new Font("Noto Sans CJK SC", Font.BOLD, 20));
        chartTheme.setLargeFont(new Font("Noto Sans CJK SC", Font.BOLD, 14));
        chartTheme.setRegularFont(new Font("Noto Sans CJK SC", Font.PLAIN, 12));
        ChartFactory.setChartTheme(chartTheme);

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                "小时",
                "用户数量",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 800, 400);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}