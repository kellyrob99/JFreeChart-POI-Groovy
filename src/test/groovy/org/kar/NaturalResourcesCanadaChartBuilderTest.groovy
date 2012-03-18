/*
 * Created by IntelliJ IDEA.
 * User: krobinson
 * Date: 12-03-17
 * Time: 4:32 PM
 */
package org.kar;

import org.junit.Test;

/**
 * NaturalResourcesCanadaChartBuilderTest
 *
 * @author krobinson
 */
public class NaturalResourcesCanadaChartBuilderTest
{
    @Test
    public void testBuildChart() throws Exception
    {
        new NaturalResourcesCanadaChartBuilder().buildAllChartsByArea(new File(getClass().classLoader.getResource('NaturalResourcesCanadaNewSeedlings.json').toURI()).absolutePath, 'pages')
    }

    @Test
    public void testBuildHtml()
    {
        def html = new NaturalResourcesCanadaChartBuilder().buildHtml('pages')
        File file = new File('pages', 'naturalResourcesCanadaSeedlingsCharts.html')
        if(file.exists())
        {
            file.delete()
        }
        file.withWriter{Writer writer ->
            writer << html
        }
    }
}