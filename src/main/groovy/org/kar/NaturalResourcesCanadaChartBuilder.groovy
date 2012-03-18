package org.kar

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.awt.Color
import org.jfree.chart.title.TextTitle
import org.jfree.data.time.Year
import com.thecoderscorner.groovychart.chart.*
import org.jfree.chart.*
import static org.kar.NaturalResourcesCanadaExcelParser.*
import static groovyx.gpars.GParsPool.withPool

/*
 * Work Created: 12-03-17 by krobinson
 */
class NaturalResourcesCanadaChartBuilder
{
    static final String FILE_PREFIX = 'naturalResourcesCanadaNewSeedlings'
    private static final String CSS = '''
body{
    background-color:#b0c4de;
}
.area{
    border: black solid thin;
    padding: 10px;
    margin: 5px;
}
.rounded-corners {
     -moz-border-radius: 10px;
    -webkit-border-radius: 10px;
    -khtml-border-radius: 10px;
    border-radius: 10px;
}
'''
    private static final String JQUERY_FUNCTION = '''
        var toggleBinder = function(){
            var $div = $(this)
            $div.children('h2').each(function(){
                $(this).click(function(){
                    $div.children('img').each(function(){
                        $(this).slideToggle(1500);
                    });
                });
            });
        };
        $('div').each(toggleBinder);
'''

    def sanitizeName(String name)
    {
        name.replaceAll('\\s', '').replaceAll('\\*', '')
    }

    def buildAllChartsByArea(filename, outputDirectory)
    {
        def data
        new File(filename).withReader {Reader reader ->
            data = new JsonSlurper().parse(reader)
        }
        assert data

        GROUPINGS.each { group ->
            withPool {
                AREAS.eachParallel { area ->
                    ChartBuilder builder = new ChartBuilder();
                    String title = sanitizeName("$group-$area")
                    TimeseriesChart chart = builder.timeserieschart(title: group,
                            timeAxisLabel: 'Year',
                            valueAxisLabel: 'Number of Seedlings(1000s)',
                            legend: true,
                            tooltips: false,
                            urls: false
                    ) {
                        timeSeriesCollection {
                            data."$group".each { species ->
                                Set years = (species.keySet() - 'name').collect {it as int}
                                timeSeries(name: species.name, timePeriodClass: 'org.jfree.data.time.Year') {
                                    years.sort().each { year ->
                                        final nl = species."$year"."$area"
                                        //check that it's a numeric value
                                        if (!(nl instanceof String))
                                        {
                                            add(period: new Year(year), value: nl)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    JFreeChart innerChart = chart.chart
                    String longName = PROVINCE_SHORT_FORM_MAPPINGS.find {it.value == area}.key
                    innerChart.addSubtitle(new TextTitle(longName))
                    innerChart.setBackgroundPaint(Color.white)
                    innerChart.plot.setBackgroundPaint(Color.lightGray.brighter())
                    [Color.BLUE, Color.GREEN, Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.BLACK, Color.RED].eachWithIndex { color, int index ->
                        innerChart.XYPlot.renderer.setSeriesPaint(index, color)
                    }
                    def fileTitle = "$FILE_PREFIX-${title}.png"
                    File outputDir = new File(outputDirectory)
                    if (!outputDir.exists())
                    {
                        outputDir.mkdirs()
                    }
                    File file = new File(outputDir, fileTitle)
                    if (file.exists())
                    {
                        file.delete()
                    }
                    ChartUtilities.saveChartAsPNG(file, innerChart, 550, 300)
                }
            }
        }
    }

    def buildHtml(inputDirectory)
    {
        File inputDir = new File(inputDirectory)
        assert inputDir.exists()
        Writer writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)
        builder.html {
            head {
                title('Number of Seedlings Planted by Ownership, Species')
                style(type: "text/css") {
                    mkp.yield(CSS)
                }
            }
            body {
                ul {
                    AREAS.each { area ->
                        String areaName = sanitizeName(area)
                        div(class: 'area rounded-corners', id: areaName) {
                            h2(PROVINCE_SHORT_FORM_MAPPINGS.find {it.value == area}.key)
                            inputDir.eachFileMatch(~/.*$areaName\.png/) {
                                img(src: it.name)
                            }
                        }
                    }
                }
                script(type: 'text/javascript', src: 'https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js', '')
                script(type: 'text/javascript') {
                    mkp.yield(JQUERY_FUNCTION)
                }
            }
        }
        writer.toString()
    }
}