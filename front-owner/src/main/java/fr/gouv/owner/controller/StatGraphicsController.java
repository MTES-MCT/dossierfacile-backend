package fr.gouv.owner.controller;

import fr.gouv.owner.model.KeyStatistics;
import fr.gouv.owner.service.TenantService;
import fr.gouv.owner.repository.TenantRepository;
import fr.gouv.owner.utils.UtilsLocatio;

import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.XYChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class StatGraphicsController {

    private static final String KEY_NB_ACCOUNTS_CREATED = "nb_accounts_created";
    private static final String KEY_NB_DOC_UPLOADED = "nb_docs_uploaded";
    private static final String KEY_PCT_SATISFACTION = "pct_satisfaction";
    private static final String FILE5_STAT_KEY = "file5";
    private static final String FILE4_STAT_KEY = "file4";
    private static final String FILE3_STAT_KEY = "file3";
    private static final String FILE2_STAT_KEY = "file2";
    private static final String FILE1_STAT_KEY = "file1";

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private TenantService tenantService;
    @Value("${default.stats.graphic.width:600}")
    private Integer defaultGraphicWidth;
    @Value("${default.stats.graphic.height:500}")
    private Integer defaultGraphicHeight;
    @Value("${default.stats.graphic.nb.samples:10}")
    private Integer defaultGraphicNbSamples;

    @GetMapping(path = "/stats")
    public String stats(Model model) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        model.addAttribute(KEY_NB_ACCOUNTS_CREATED, tenantRepository.countAllTenantAccount());
        model.addAttribute(KEY_NB_DOC_UPLOADED, tenantService.countUploadedFiles());
        model.addAttribute(KEY_PCT_SATISFACTION, tenantService.countOverallSatisfaction() * 100);

        return "stats";
    }

    @GetMapping(path = "/stats/uploads.png")
    public void doPostGraphicsUploads(HttpServletResponse response,
                                      @RequestParam(value = "width", required = false) Integer width,
                                      @RequestParam(value = "height", required = false) Integer height,
                                      @RequestParam(defaultValue = "false") boolean detailFiles,
                                      @RequestParam(value = "nbSamples", required = false) Integer nbSamples) throws IOException {

        if (width == null) {
            width = defaultGraphicWidth;
        }
        if (height == null) {
            height = defaultGraphicHeight;
        }
        if (nbSamples == null) {
            nbSamples = defaultGraphicNbSamples;
        }
        Map<KeyStatistics, Map<String, Long>> stats = tenantService.statistics();
        List<KeyStatistics> sortedDatesTenant = UtilsLocatio.sortedDates(new ArrayList<KeyStatistics>(stats.keySet()));
        Object[] result = generateSerie(nbSamples, sortedDatesTenant.get(0));
        List<KeyStatistics> keys = (List<KeyStatistics>) result[0];
        double[] xData = (double[]) result[1];
        Map<Double, Object> xlabels = (Map<Double, Object>) result[2];
        double[] yData = new double[xData.length];
        double[] file1 = new double[xData.length];
        double[] file2 = new double[xData.length];
        double[] file3 = new double[xData.length];
        double[] file4 = new double[xData.length];
        double[] file5 = new double[xData.length];
        for (int i = 0; i < keys.size(); i++) {
            KeyStatistics key = keys.get(i);
            if (stats.containsKey(key)) {
                Map<String, Long> m = stats.get(key);
                log.info("stat record {}", m);
                aggregagte(m, yData, file1, i, FILE1_STAT_KEY);
                aggregagte(m, yData, file2, i, FILE2_STAT_KEY);
                aggregagte(m, yData, file3, i, FILE3_STAT_KEY);
                aggregagte(m, yData, file4, i, FILE4_STAT_KEY);
                aggregagte(m, yData, file5, i, FILE5_STAT_KEY);
            } else {
                yData[i] = 0.0;
            }
        }

        XYChart chart = new XYChart(width, height);
        chart.setTitle("Documents déposés par semaine");
        chart.setXAxisTitle("Semaines");
        if (detailFiles) {
            chart.setTitle("Documents déposés par semaine par type de documents");
            chart.addSeries("CNI", xData, file1);
            chart.addSeries("Hébergement", xData, file2);
            chart.addSeries("Contrat de travail", xData, file3);
            chart.addSeries("Impôts", xData, file4);
            chart.addSeries("Ressources", xData, file5);
            chart.getStyler().setSeriesColors(new Color[]{new Color(4, 31, 84),
                    Color.red, Color.green, Color.yellow, Color.orange});
        } else {
            chart.addSeries("Total", xData, yData);
            chart.getStyler().setSeriesColors(new Color[]{new Color(4, 31, 84)});
        }
        chart.setXAxisLabelOverrideMap(xlabels);
        exportChart(response, width, height, chart);
    }

    @GetMapping(path = "/stats/tenants-created.png")
    public void doPostGraphicsTenantCreated(HttpServletResponse response,
                                            @RequestParam(value = "width", required = false) Integer width,
                                            @RequestParam(value = "height", required = false) Integer height,
                                            @RequestParam(value = "nbSamples", required = false) Integer nbSamples) throws IOException {

        if (width == null) {
            width = defaultGraphicWidth;
        }
        if (height == null) {
            height = defaultGraphicHeight;
        }
        if (nbSamples == null) {
            nbSamples = defaultGraphicNbSamples;
        }
        Map<KeyStatistics, Map<String, Long>> stats = tenantService.acountCreationStatistics();
        List<KeyStatistics> sortedDatesTenant = UtilsLocatio.sortedDates(new ArrayList<KeyStatistics>(stats.keySet()));
        Object[] result = generateSerie(nbSamples, sortedDatesTenant.get(0));
        List<KeyStatistics> keys = (List<KeyStatistics>) result[0];
        double[] xData = (double[]) result[1];
        Map<Double, Object> xlabels = (Map<Double, Object>) result[2];
        double[] yData = new double[xData.length];
        for (int i = 0; i < keys.size(); i++) {
            KeyStatistics key = keys.get(i);
            if (stats.containsKey(key) && stats.get(key).containsKey("creation")) {
                yData[i] = stats.get(key).get("creation");
            }
        }
        XYChart chart = new XYChart(width, height);
        chart.setTitle("Comptes locataires créés par semaine");
        chart.addSeries("comptes", xData, yData);
        chart.getStyler().setSeriesColors(new Color[]{new Color(4, 31, 84)});
        chart.setXAxisLabelOverrideMap(xlabels);
        chart.setXAxisTitle("Semaines");
        exportChart(response, width, height, chart);
    }

    private Object[] generateSerie(int len, KeyStatistics first) {
        List<KeyStatistics> keys = new ArrayList<>();
        KeyStatistics key = null;
        Map<Double, Object> xlabels = new HashMap<>();
        double[] xData = new double[len];
        for (int i = 0; i < len; i++) {
            xData[len - i - 1] = i;
            if (key == null) {
                key = first;
                xlabels.put((double) (len - i - 1), (key.getWeek() + 1) + " / " + key.getYear());
            } else {
                if (key.getWeek() > 0) {
                    key = new KeyStatistics(key.getWeek() - 1, key.getYear());
                    xlabels.put((double) (len - i - 1), key.getWeek() + 1);
                    if (key.getWeek() == 0) {
                        xlabels.put((double) (len - i - 1), (key.getWeek() + 1) + " / " + key.getYear());
                    }
                } else {
                    key = new KeyStatistics(51, key.getYear() - 1);
                    xlabels.put((double) (len - i - 1), key.getWeek() + 1);
                }
            }
            keys.add(key);
        }
        return new Object[]{keys, xData, xlabels};
    }

    private void exportChart(HttpServletResponse response, int width, int height, XYChart chart) throws IOException {
        chart.getStyler().setBaseFont(new Font("Arial", Font.PLAIN, 18));
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridLinesColor(Color.BLACK);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setPlotGridHorizontalLinesVisible(true);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.getStyler().setAxisTicksMarksVisible(false);
        chart.getStyler().setAntiAlias(true);
        if (chart.getSeriesMap().size() == 1) {
            chart.getStyler().setLegendVisible(false);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        chart.paint(g, width, height);
        g.dispose();
        response.setContentType("image/png");
        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);
        os.flush();
    }

    private void aggregagte(Map<String, Long> values, double[] total, double[] serie, int i,
                            String statName) {
        if (values.containsKey(statName)) {
            total[i] += values.get(statName).doubleValue();
            serie[i] = values.get(statName).doubleValue();
        }
    }
}
