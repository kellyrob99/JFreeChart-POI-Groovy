package org.kar

import groovy.json.JsonBuilder
import org.junit.Test

class ExcelBuilderTest
{
    @Test
    public void testNaturalResourcesCanadaData()
    {
        final File data = new File(getClass().classLoader.getResource('comp_68e.xls').toURI())
        Map map = new NaturalResourcesCanadaExcelParser().convertToMap(data)
        new File('NaturalResourcesCanadaNewSeedlings.json').withWriter {Writer writer ->
            writer << new JsonBuilder(map).toPrettyString()
        }
    }
}
